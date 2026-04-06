package com.Group117.hrm_system.service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import com.Group117.hrm_system.Repository.TaiKhoanRepository;
import com.Group117.hrm_system.Repository.ThongBaoRepository;
import com.Group117.hrm_system.dto.NotificationDto;
import com.Group117.hrm_system.entity.TaiKhoan;
import com.Group117.hrm_system.entity.ThongBao;

@Service
public class NotificationService {

    public static final String TOPIC_NOTIFICATIONS = "/topic/notifications";
    public static final String QUEUE_USER_NOTIFICATIONS = "/queue/notifications";

    @Autowired
    private ThongBaoRepository thongBaoRepository;

    @Autowired
    private TaiKhoanRepository taiKhoanRepository;

    @Autowired
    private SimpMessagingTemplate messagingTemplate;

    public Optional<String> findUsernameByNhanVienId(String nhanVienId) {
        return taiKhoanRepository.findByNhanVien_Id(nhanVienId).map(TaiKhoan::getUsername);
    }

    public List<NotificationDto> listForUser(String username, int limit) {
        int cap = Math.min(Math.max(limit, 1), 200);
        return thongBaoRepository.findVisibleForUser(username, PageRequest.of(0, cap))
                .stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    public long countUnreadPrivate(String username) {
        return thongBaoRepository.countUnreadPrivate(username);
    }

    @Transactional
    public boolean markRead(Long id, String username) {
        return thongBaoRepository.markReadIfOwned(id, username) > 0;
    }

    @Transactional
    public int markAllReadPrivate(String username) {
        return thongBaoRepository.markAllReadPrivateForUser(username);
    }

    /**
     * Lưu DB trước, push STOMP sau khi transaction commit (tránh mất đồng bộ).
     */
    @Transactional
    public void notifyPrivate(String username, String loai, String tieuDe, String noiDung, String refPayload) {
        if (username == null || username.isBlank()) return;
        ThongBao tb = ThongBao.builder()
                .nguoiNhan(username.trim())
                .loai(loai)
                .tieuDe(tieuDe)
                .noiDung(noiDung)
                .refPayload(refPayload)
                .daDoc(false)
                .ngayTao(LocalDateTime.now())
                .build();
        tb = thongBaoRepository.save(tb);
        NotificationDto dto = toDto(tb);
        runAfterCommit(() -> messagingTemplate.convertAndSendToUser(username, QUEUE_USER_NOTIFICATIONS, dto));
    }

    @Transactional
    public void notifyBroadcast(String loai, String tieuDe, String noiDung, String refPayload) {
        ThongBao tb = ThongBao.builder()
                .nguoiNhan(null)
                .loai(loai)
                .tieuDe(tieuDe)
                .noiDung(noiDung)
                .refPayload(refPayload)
                .daDoc(false)
                .ngayTao(LocalDateTime.now())
                .build();
        tb = thongBaoRepository.save(tb);
        NotificationDto dto = toDto(tb);
        runAfterCommit(() -> messagingTemplate.convertAndSend(TOPIC_NOTIFICATIONS, dto));
    }

    /** Mỗi HR nhận một bản ghi riêng (đọc/chưa đọc đúng per-user). */
    @Transactional
    public void notifyAllHr(String loai, String tieuDe, String noiDung, String refPayload) {
        notifyAllByRole("HR", loai, tieuDe, noiDung, refPayload);
    }

    /** Mỗi Director nhận một bản ghi riêng. */
    @Transactional
    public void notifyAllDirectors(String loai, String tieuDe, String noiDung, String refPayload) {
        notifyAllByRole("DIRECTOR", loai, tieuDe, noiDung, refPayload);
    }

    /** Gửi thông báo cho tất cả tài khoản có role bất kỳ (internal helper). */
    @Transactional
    private void notifyAllByRole(String role, String loai, String tieuDe, String noiDung, String refPayload) {
        List<TaiKhoan> targets = taiKhoanRepository.findByRoleIgnoreCase(role);
        if (targets.isEmpty()) return;
        List<java.util.AbstractMap.SimpleEntry<String, NotificationDto>> pushList = new java.util.ArrayList<>();
        for (TaiKhoan tk : targets) {
            if (tk.getUsername() == null || tk.getUsername().isBlank()) continue;
            ThongBao row = ThongBao.builder()
                    .nguoiNhan(tk.getUsername())
                    .loai(loai)
                    .tieuDe(tieuDe)
                    .noiDung(noiDung)
                    .refPayload(refPayload)
                    .daDoc(false)
                    .ngayTao(LocalDateTime.now())
                    .build();
            row = thongBaoRepository.save(row);
            pushList.add(new java.util.AbstractMap.SimpleEntry<>(tk.getUsername(), toDto(row)));
        }
        runAfterCommit(() -> {
            for (var e : pushList) {
                messagingTemplate.convertAndSendToUser(e.getKey(), QUEUE_USER_NOTIFICATIONS, e.getValue());
            }
        });
    }

    private void runAfterCommit(Runnable r) {
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    r.run();
                }
            });
        } else {
            r.run();
        }
    }

    private NotificationDto toDto(ThongBao t) {
        return NotificationDto.builder()
                .id(t.getId())
                .nguoiNhan(t.getNguoiNhan())
                .tieuDe(t.getTieuDe())
                .noiDung(t.getNoiDung())
                .loai(t.getLoai())
                .daDoc(t.isDaDoc())
                .ngayTao(t.getNgayTao())
                .refPayload(t.getRefPayload())
                .build();
    }
}
