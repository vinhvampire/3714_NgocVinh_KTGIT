package com.Group117.hrm_system.service;

import com.Group117.hrm_system.entity.TaiKhoan;
import com.Group117.hrm_system.Repository.TaiKhoanRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.MailSendException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;
import java.time.LocalDateTime;

@Service
public class ResetPasswordService {

    @Autowired
    private JavaMailSender mailSender;

    @Autowired
    private TaiKhoanRepository taiKhoanRepository;

    // Hàm tổng hợp: Tạo mã -> Lưu DB -> Gửi Mail
    public boolean processForgotPassword(String username) {
        TaiKhoan tk = taiKhoanRepository.findByUsername(username);

        // Kiểm tra xem user có tồn tại và nhân viên có email không
        if (tk != null && tk.getNhanVien() != null) {
            // 1. Tạo token ngẫu nhiên (6 số cho dễ nhập hoặc UUID)
            String token = String.valueOf((int)(Math.random() * 900000 + 100000));

            // 2. Lưu vào DB và set hết hạn 15 phút
            tk.setResetToken(token);
            tk.setResetTokenExpiry(LocalDateTime.now().plusMinutes(15));
            taiKhoanRepository.save(tk);

            // 3. Gửi mail
            String emailNhanVien = tk.getNhanVien().getEmailCongViec();
            if (emailNhanVien == null || emailNhanVien.isBlank()) {
                return false;
            }
            sendResetToken(emailNhanVien, token);
            return true;
        }
        return false;
    }

    public void sendResetToken(String email, String token) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(email);
        message.setSubject("Mã khôi phục mật khẩu HRM");
        message.setText("Mã xác nhận của bạn là: " + token +
                "\nMã này có hiệu lực trong 15 phút. Vui lòng không cung cấp mã cho người khác.");

        try {
            mailSender.send(message);
        } catch (MailSendException e) {
            System.err.println("Lỗi gửi mail: " + e.getMessage());
        }
    }
}