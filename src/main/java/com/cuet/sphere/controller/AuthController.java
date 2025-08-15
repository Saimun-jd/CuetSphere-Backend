package com.cuet.sphere.controller;

import com.cuet.sphere.config.JwtProvider;
import com.cuet.sphere.exception.UserException;
import com.cuet.sphere.model.User;
import com.cuet.sphere.model.User.Role;
import com.cuet.sphere.util.StudentEmailParser;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import com.cuet.sphere.repository.UserRepository;
import com.cuet.sphere.response.AuthResponse;
import com.cuet.sphere.response.SigninRequest;
import com.cuet.sphere.service.CustomUserDetailsService;
import java.time.LocalDateTime;

@RestController
@RequestMapping("/auth")
public class AuthController {
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private PasswordEncoder passwordEncoder;
    @Autowired
    private JwtProvider jwtProvider;
    @Autowired
    private CustomUserDetailsService customUserDetailsService;
    @Autowired
    private AuthenticationManager authenticationManager;

    @GetMapping("/test")
    public ResponseEntity<String> testEndpoint() {
        return ResponseEntity.ok("Auth endpoint is working!");
    }

    @PostMapping("/signup")
    public ResponseEntity<AuthResponse> createUserHandler(@RequestBody User user) throws UserException {
        try {
            System.out.println("=== SIGNUP REQUEST RECEIVED ===");
            System.out.println("Email: " + user.getEmail());
            System.out.println("Full Name: " + user.getFullName());
            System.out.println("Hall: " + user.getHall());
            System.out.println("Bio: " + user.getBio());
            
            String email = user.getEmail();
            String password = user.getPassword();
            String hashedPassword = passwordEncoder.encode(password);
            String fullName = user.getFullName();
            Role role = user.getRole() != null ? user.getRole() : Role.STUDENT;
            String hall = user.getHall();
            String bio = user.getBio();

            // Validate email format and extract batch, department, student ID
            if (email == null || !email.contains("@student.cuet.ac.bd")) {
                throw new UserException("Invalid email format. Must be a valid CUET student email.");
            }

            System.out.println("Parsing student email to extract batch, department, and student ID...");
            StudentEmailParser.StudentInfo studentInfo;
            try {
                studentInfo = StudentEmailParser.parseStudentEmail(email);
                System.out.println("Parsed student info - Batch: " + studentInfo.getBatch() + 
                                ", Department: " + studentInfo.getDepartment() + 
                                ", Student ID: " + studentInfo.getStudentId());
            } catch (IllegalArgumentException e) {
                throw new UserException("Invalid CUET student email format: " + e.getMessage());
            }

            System.out.println("Checking if email already exists...");
            User isEmailExist = userRepository.findUserByEmail(email);
            if (isEmailExist != null) {
                System.out.println("Email already exists: " + email);
                throw new UserException("Email already exists");
            }

            System.out.println("Creating new user...");
            User createdUser = new User();
            createdUser.setEmail(email);
            createdUser.setPassword(hashedPassword);
            createdUser.setFullName(fullName);
            createdUser.setRole(role);
            createdUser.setHall(hall);
            createdUser.setBio(bio);
            
            // Set the parsed values from email
            createdUser.setBatch(studentInfo.getBatch());
            createdUser.setDepartment(studentInfo.getDepartment());
            createdUser.setStudentId(studentInfo.getStudentId());
            
            // Set timestamps
            LocalDateTime now = LocalDateTime.now();
            createdUser.setCreatedAt(now);
            createdUser.setUpdatedAt(now);

            System.out.println("Saving user to database...");
            User savedUser = userRepository.save(createdUser);
            System.out.println("User saved with ID: " + savedUser.getId());

            System.out.println("Creating authentication token...");
            // Use the raw password for authentication, not the hashed one
            Authentication authentication = new UsernamePasswordAuthenticationToken(email, password);
            SecurityContextHolder.getContext().setAuthentication(authentication);

            System.out.println("Generating JWT token...");
            String token = jwtProvider.generateToken(authentication);
            AuthResponse authResponse = new AuthResponse(token, true, "User created successfully", email, fullName, role.name());

            System.out.println("=== SIGNUP SUCCESSFUL ===");
            return new ResponseEntity<>(authResponse, HttpStatus.CREATED);
        } catch (Exception e) {
            System.err.println("=== SIGNUP ERROR ===");
            System.err.println("Error: " + e.getMessage());
            e.printStackTrace();
            throw e;
        }
    }

    @PostMapping("/signin")
    public ResponseEntity<AuthResponse> signinHandler(@RequestBody SigninRequest signinRequest) throws UserException {
        try {
            System.out.println("=== SIGNIN REQUEST RECEIVED ===");
            System.out.println("Email: " + signinRequest.getEmail());

            String email = signinRequest.getEmail();
            String password = signinRequest.getPassword();

            if (email == null || email.trim().isEmpty() || password == null || password.trim().isEmpty()) {
                throw new UserException("Email and password are required");
            }

            // FIRST: Let's manually check if the user exists and password matches
            System.out.println("=== MANUAL VERIFICATION (FOR DEBUG) ===");
            User dbUser = userRepository.findUserByEmail(email);
            if (dbUser == null) {
                System.out.println("ERROR: User not found in database");
                throw new UserException("User not found");
            }

            System.out.println("User found in database:");
            System.out.println("- Email: " + dbUser.getEmail());
            System.out.println("- Name: " + dbUser.getFullName());
            System.out.println("- Password is null: " + (dbUser.getPassword() == null));
            System.out.println("- Password length: " + (dbUser.getPassword() != null ? dbUser.getPassword().length() : 0));

            // Test password matching manually
            boolean passwordMatches = passwordEncoder.matches(password, dbUser.getPassword());
            System.out.println("- Raw password: '" + password + "'");
            System.out.println("- Stored hash: " + (dbUser.getPassword() != null ? dbUser.getPassword().substring(0, Math.min(20, dbUser.getPassword().length())) + "..." : "null"));
            System.out.println("- Password matches: " + passwordMatches);

            if (!passwordMatches) {
                System.out.println("ERROR: Password verification failed at manual check");
                throw new UserException("Invalid password - manual check failed");
            }

            System.out.println("Manual verification successful, proceeding with Spring Security authentication...");

            // SECOND: Try Spring Security authentication
            System.out.println("=== SPRING SECURITY AUTHENTICATION ===");
            System.out.println("Creating UsernamePasswordAuthenticationToken...");

            try {
                // This will trigger CustomUserDetailsService.loadUserByUsername()
                Authentication authentication = authenticationManager.authenticate(
                        new UsernamePasswordAuthenticationToken(email, password)
                );

                System.out.println("Authentication successful!");
                System.out.println("- Principal: " + authentication.getPrincipal());
                System.out.println("- Authorities: " + authentication.getAuthorities());
                System.out.println("- Is authenticated: " + authentication.isAuthenticated());

                // Set the authentication in SecurityContext
                SecurityContextHolder.getContext().setAuthentication(authentication);

                System.out.println("Generating JWT token...");
                String token = jwtProvider.generateToken(authentication);

                String fullName = dbUser.getFullName();
                String role = dbUser.getRole() != null ? dbUser.getRole().name() : "STUDENT";

                AuthResponse authResponse = new AuthResponse(token, true, "Signin successful", email, fullName, role);

                System.out.println("=== SIGNIN SUCCESSFUL ===");
                return new ResponseEntity<>(authResponse, HttpStatus.OK);

            } catch (Exception springAuthException) {
                System.out.println("=== SPRING SECURITY AUTHENTICATION FAILED ===");
                System.out.println("Exception type: " + springAuthException.getClass().getName());
                System.out.println("Exception message: " + springAuthException.getMessage());
                springAuthException.printStackTrace();

                // Since manual verification worked but Spring Security failed,
                // there's likely an issue with CustomUserDetailsService
                throw new UserException("Authentication failed: " + springAuthException.getMessage());
            }

        } catch (BadCredentialsException e) {
            System.err.println("=== SIGNIN ERROR: Bad Credentials ===");
            System.err.println("Error: " + e.getMessage());
            throw new UserException("Invalid email or password");
        } catch (Exception e) {
            System.err.println("=== SIGNIN ERROR ===");
            System.err.println("Error: " + e.getMessage());
            e.printStackTrace();
            throw e;
        }
    }
}
