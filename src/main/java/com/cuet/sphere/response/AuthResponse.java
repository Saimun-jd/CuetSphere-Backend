package com.cuet.sphere.response;

public class AuthResponse {
    private String token;
    private boolean success;
    private String message;
    private String email;
    private String fullName;
    private String role;

    public AuthResponse() {
    }

    public AuthResponse(String token, boolean success) {
        this.token = token;
        this.success = success;
    }

    public AuthResponse(String token, boolean success, String message, String email, String fullName, String role) {
        this.token = token;
        this.success = success;
        this.message = message;
        this.email = email;
        this.fullName = fullName;
        this.role = role;
    }

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }

    public boolean isSuccess() {
        return success;
    }

    public void setSuccess(boolean success) {
        this.success = success;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getFullName() {
        return fullName;
    }

    public void setFullName(String fullName) {
        this.fullName = fullName;
    }

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }
} 