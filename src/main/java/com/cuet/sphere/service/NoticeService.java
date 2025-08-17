package com.cuet.sphere.service;

import com.cuet.sphere.model.Notice;
import com.cuet.sphere.model.User;
import com.cuet.sphere.repository.NoticeRepository;
import com.cuet.sphere.response.NoticeRequest;
import com.cuet.sphere.response.NoticeResponse;
import com.cuet.sphere.exception.UserException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class NoticeService {
    
    @Autowired
    private NoticeRepository noticeRepository;
    
    @Autowired
    private WebSocketService webSocketService;
    
    @Autowired
    private S3Service s3Service;
    
    public NoticeResponse createNotice(NoticeRequest noticeRequest, User sender) throws UserException {
        // Check if sender is CR
        if (!sender.isCR()) {
            throw new UserException("Only CR users can create notices");
        }
        
        Notice notice = new Notice();
        notice.setMessage(noticeRequest.getMessage());
        notice.setAttachment(noticeRequest.getAttachment());
        notice.setNoticeType(noticeRequest.getNoticeType());
        notice.setBatch(sender.getBatch());
        notice.setDepartment(sender.getDepartment());
        notice.setSender(sender);
        
        Notice savedNotice = noticeRepository.save(notice);
        NoticeResponse response = convertToResponse(savedNotice);
        
        // Send real-time notification
        webSocketService.sendNoticeToBatchAndDepartment(response);
        
        return response;
    }
    
    public List<NoticeResponse> getNoticesByUser(User user) {
        List<Notice> notices = noticeRepository.findByBatchAndDepartmentOrderByCreatedAtDesc(
            user.getBatch(), 
            user.getDepartment()
        );
        
        return notices.stream()
            .map(this::convertToResponse)
            .collect(Collectors.toList());
    }
    
    public List<NoticeResponse> getNoticesByUserAndType(User user, Notice.NoticeType noticeType) {
        List<Notice> notices = noticeRepository.findByBatchAndDepartmentAndNoticeTypeOrderByCreatedAtDesc(
            user.getBatch(), 
            user.getDepartment(), 
            noticeType
        );
        
        return notices.stream()
            .map(this::convertToResponse)
            .collect(Collectors.toList());
    }
    
    public List<NoticeResponse> getNoticesBySender(User sender) throws UserException {
        if (!sender.isCR()) {
            throw new UserException("Only CR users can view their sent notices");
        }
        
        List<Notice> notices = noticeRepository.findBySenderIdOrderByCreatedAtDesc(sender.getId());
        
        return notices.stream()
            .map(this::convertToResponse)
            .collect(Collectors.toList());
    }
    
    public NoticeResponse getNoticeById(Long noticeId, User user) throws UserException {
        Notice notice = noticeRepository.findById(noticeId)
            .orElseThrow(() -> new UserException("Notice not found"));
        
        // Check if user can access this notice (same batch and department)
        if (!notice.getBatch().equals(user.getBatch()) || !notice.getDepartment().equals(user.getDepartment())) {
            throw new UserException("Access denied: Notice not for your batch/department");
        }
        
        return convertToResponse(notice);
    }
    
    public void deleteNotice(Long noticeId, User user) throws UserException {
        Notice notice = noticeRepository.findById(noticeId)
            .orElseThrow(() -> new UserException("Notice not found"));
        
        // Check if user is the sender of the notice
        if (!notice.getSender().getId().equals(user.getId())) {
            throw new UserException("Access denied: You can only delete your own notices");
        }
        
        // Delete the attachment file from S3 if it exists
        if (notice.getAttachment() != null && !notice.getAttachment().isEmpty()) {
            s3Service.deleteFile(notice.getAttachment());
        }
        
        // Delete the notice from database
        noticeRepository.delete(notice);
    }
    
    private NoticeResponse convertToResponse(Notice notice) {
        NoticeResponse response = new NoticeResponse();
        response.setNoticeId(notice.getNoticeId());
        response.setDepartment(notice.getDepartment());
        response.setBatch(notice.getBatch());
        response.setMessage(notice.getMessage());
        response.setAttachment(notice.getAttachment());
        response.setNoticeType(notice.getNoticeType());
        response.setSenderName(notice.getSender().getFullName());
        response.setSenderEmail(notice.getSender().getEmail());
        response.setCreatedAt(notice.getCreatedAt());
        response.setUpdatedAt(notice.getUpdatedAt());
        return response;
    }
}
