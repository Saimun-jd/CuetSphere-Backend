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

import java.util.List;

@RestController
@RequestMapping("/api/notices")
@CrossOrigin(origins = "*")
public class NoticeController {
    
    @Autowired
    private NoticeService noticeService;
    
    @Autowired
    private UserRepository userRepository;
    
    @PostMapping
    public ResponseEntity<NoticeResponse> createNotice(@RequestBody NoticeRequest noticeRequest) {
        try {
            User currentUser = getCurrentUser();
            NoticeResponse notice = noticeService.createNotice(noticeRequest, currentUser);
            return ResponseEntity.ok(notice);
        } catch (UserException e) {
            return ResponseEntity.badRequest().build();
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
    
    private User getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.isAuthenticated()) {
            String email = authentication.getName();
            User user = userRepository.findUserByEmail(email);
            if (user != null) {
                return user;
            }
        }
        throw new RuntimeException("User not authenticated");
    }
}
