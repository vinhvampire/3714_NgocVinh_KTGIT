package com.Group117.hrm_system.Controller;

import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.Group117.hrm_system.dto.NotificationDto;
import com.Group117.hrm_system.service.NotificationService;

@RestController
@RequestMapping("/api/notifications")
public class NotificationController {

    @Autowired
    private NotificationService notificationService;

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN','HR','EMPLOYEE','DIRECTOR')")
    public ResponseEntity<List<NotificationDto>> list(@RequestParam(defaultValue = "50") int limit) {
        String u = SecurityContextHolder.getContext().getAuthentication().getName();
        return ResponseEntity.ok(notificationService.listForUser(u, limit));
    }

    @GetMapping("/unread-count")
    @PreAuthorize("hasAnyRole('ADMIN','HR','EMPLOYEE','DIRECTOR')")
    public ResponseEntity<Map<String, Long>> unreadCount() {
        String u = SecurityContextHolder.getContext().getAuthentication().getName();
        return ResponseEntity.ok(Map.of("unread", notificationService.countUnreadPrivate(u)));
    }

    @PutMapping("/{id}/read")
    @PreAuthorize("hasAnyRole('ADMIN','HR','EMPLOYEE','DIRECTOR')")
    public ResponseEntity<?> markRead(@PathVariable Long id) {
        String u = SecurityContextHolder.getContext().getAuthentication().getName();
        if (!notificationService.markRead(id, u)) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok().build();
    }

    @PutMapping("/read-all")
    @PreAuthorize("hasAnyRole('ADMIN','HR','EMPLOYEE','DIRECTOR')")
    public ResponseEntity<Map<String, Integer>> markAllRead() {
        String u = SecurityContextHolder.getContext().getAuthentication().getName();
        int n = notificationService.markAllReadPrivate(u);
        return ResponseEntity.ok(Map.of("updated", n));
    }

    /** Chỉ ADMIN — lưu DB + broadcast /topic/notifications */
    @PostMapping("/broadcast")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> broadcast(@RequestBody Map<String, String> body) {
        String title = body.getOrDefault("title", "Thông báo hệ thống");
        String content = body.getOrDefault("content", "");
        notificationService.notifyBroadcast("SYSTEM_BROADCAST", title, content, null);
        return ResponseEntity.accepted().build();
    }
}
