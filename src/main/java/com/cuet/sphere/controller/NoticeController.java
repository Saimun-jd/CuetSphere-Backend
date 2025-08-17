package com.cuet.sphere.controller;

import com.cuet.sphere.model.User;
import com.cuet.sphere.model.Notice;
import com.cuet.sphere.response.NoticeRequest;
import com.cuet.sphere.response.NoticeResponse;
import com.cuet.sphere.service.NoticeService;
import com.cuet.sphere.exception.UserException;
import com.cuet.sphere.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Map;
import java.util.HashMap;

@RestController
@RequestMapping("/api/notices")
@CrossOrigin(origins = "*")
public class NoticeController {
    
    private static final Logger logger = LoggerFactory.getLogger(NoticeController.class);
    
    @Autowired
    private NoticeService noticeService;
    
    @Autowired
    private UserRepository userRepository;
    
    @PostMapping
    public ResponseEntity<?> createNotice(@Valid @RequestBody NoticeRequest noticeRequest) {
        try {
            logger.info("Creating notice with message: {}", noticeRequest.getMessage());
            logger.info("Notice type: {}", noticeRequest.getNoticeType());
            logger.info("Attachment: {}", noticeRequest.getAttachment());
            
            User currentUser = getCurrentUser();
            logger.info("Current user: {} ({})", currentUser.getFullName(), currentUser.getEmail());
            
            NoticeResponse notice = noticeService.createNotice(noticeRequest, currentUser);
            logger.info("Notice created successfully with ID: {}", notice.getNoticeId());
            return ResponseEntity.ok(notice);
        } catch (UserException e) {
            logger.error("UserException while creating notice: {}", e.getMessage());
            Map<String, String> error = new HashMap<>();
            error.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(error);
        } catch (RuntimeException e) {
            logger.error("RuntimeException while creating notice: {}", e.getMessage());
            Map<String, String> error = new HashMap<>();
            error.put("error", "Authentication failed: " + e.getMessage());
            return ResponseEntity.status(401).body(error);
        } catch (Exception e) {
            logger.error("Unexpected error while creating notice: {}", e.getMessage(), e);
            Map<String, String> error = new HashMap<>();
            error.put("error", "Internal server error: " + e.getMessage());
            return ResponseEntity.status(500).body(error);
        }
    }
    
    @GetMapping
    public ResponseEntity<List<NoticeResponse>> getNotices() {
        User currentUser = getCurrentUser();
        List<NoticeResponse> notices = noticeService.getNoticesByUser(currentUser);
        return ResponseEntity.ok(notices);
    }
    
    @GetMapping("/type/{noticeType}")
    public ResponseEntity<List<NoticeResponse>> getNoticesByType(@PathVariable String noticeType) {
        try {
            User currentUser = getCurrentUser();
            Notice.NoticeType type = Notice.NoticeType.valueOf(noticeType.toUpperCase());
            List<NoticeResponse> notices = noticeService.getNoticesByUserAndType(currentUser, type);
            return ResponseEntity.ok(notices);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        }
    }
    
    @GetMapping("/my")
    public ResponseEntity<List<NoticeResponse>> getMyNotices() {
        try {
            User currentUser = getCurrentUser();
            List<NoticeResponse> notices = noticeService.getNoticesBySender(currentUser);
            return ResponseEntity.ok(notices);
        } catch (UserException e) {
            return ResponseEntity.badRequest().build();
        }
    }
    
    @GetMapping("/{noticeId}")
    public ResponseEntity<NoticeResponse> getNoticeById(@PathVariable Long noticeId) {
        try {
            User currentUser = getCurrentUser();
            NoticeResponse notice = noticeService.getNoticeById(noticeId, currentUser);
            return ResponseEntity.ok(notice);
        } catch (UserException e) {
            return ResponseEntity.badRequest().build();
        }
    }
    
    @DeleteMapping("/{noticeId}")
    public ResponseEntity<Void> deleteNotice(@PathVariable Long noticeId) {
        try {
            User currentUser = getCurrentUser();
            noticeService.deleteNotice(noticeId, currentUser);
            return ResponseEntity.ok().build();
        } catch (UserException e) {
            return ResponseEntity.badRequest().build();
        }
    }
    
    // Test endpoint for debugging (no authentication required)
    @PostMapping("/test")
    public ResponseEntity<?> testNoticeCreation(@Valid @RequestBody NoticeRequest noticeRequest) {
        logger.info("Test endpoint called with message: {}", noticeRequest.getMessage());
        logger.info("Notice type: {}", noticeRequest.getNoticeType());
        logger.info("Attachment: {}", noticeRequest.getAttachment());
        
        Map<String, Object> response = new HashMap<>();
        response.put("message", "Test endpoint working");
        response.put("receivedData", noticeRequest);
        response.put("timestamp", System.currentTimeMillis());
        
        return ResponseEntity.ok(response);
    }
    
    private User getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        logger.info("Authentication object: {}", authentication);
        
        if (authentication != null && authentication.isAuthenticated()) {
            String email = authentication.getName();
            logger.info("Authenticated user email: {}", email);
            User user = userRepository.findUserByEmail(email);
            if (user != null) {
                logger.info("Found user: {} ({})", user.getFullName(), user.getEmail());
                return user;
            } else {
                logger.error("User not found in database for email: {}", email);
            }
        } else {
            logger.error("Authentication failed or user not authenticated");
        }
        throw new RuntimeException("User not authenticated");
    }
}
