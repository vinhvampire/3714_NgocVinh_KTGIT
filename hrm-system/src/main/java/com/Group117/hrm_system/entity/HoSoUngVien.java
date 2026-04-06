package com.Group117.hrm_system.entity;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

@Entity
@Table(name = "ho_so_ung_vien")
@Data
public class HoSoUngVien {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "ho_ten", nullable = false)
    private String hoTen;

    @Column(name = "email", nullable = false)
    private String email;

    @Column(name = "so_dien-thoai")
    private String soDienThoai;

    @Column(name = "cv_url")
    private String cvUrl; // Lưu link file hoặc tên file CV

    @Column(name = "trang_thai")
    private String trangThai = "CHO_DUYET"; // Trạng thái: CHO_DUYET, DAT, LOAI

    @Column(name = "ngay_nop")
    private LocalDateTime ngayNop = LocalDateTime.now();

    @ManyToOne
    @JoinColumn(name = "id_yeu_cau")
    private YeuCauTuyenDung yeuCauTuyenDung; // Ứng tuyển cho vị trí nào
}