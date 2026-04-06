package com.Group117.hrm_system.entity;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDate;

@Entity
@Table(name = "don_nghi_phep")
@Data
public class DonNghiPhep {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "nhan_vien_id")
    private NhanVien nhanVien;

    @Column(name = "loai_nghi")
    private String loaiNghi; // PH_NAM (Phép năm), NGHI_OM, KHONG_LUONG

    @Column(name = "tu_ngay")
    private LocalDate tuNgay;

    @Column(name = "den_ngay")
    private LocalDate denNgay;

    @Column(name = "ly_do", columnDefinition = "TEXT")
    private String lyDo;

    @Column(name = "trang_thai")
    private String trangThai = "CHO_DUYET"; // CHO_DUYET, DA_DUYET, TU_CHOI

    @ManyToOne
    @JoinColumn(name = "nguoi_duyet_id")
    private NhanVien nguoiDuyet; // Sẽ tự động tìm theo Workflow
}