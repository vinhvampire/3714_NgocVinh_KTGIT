package com.Group117.hrm_system.entity;

import jakarta.persistence.*;
import lombok.Data;
import java.util.Date;

@Entity
@Table(name = "lich_su_cong_tac")
@Data
public class LichSuCongTac {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "nhan_vien_id", nullable = false)
    @com.fasterxml.jackson.annotation.JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
    private NhanVien nhanVien;

    @Column(name = "tu_ngay")
    private Date tuNgay;

    @Column(name = "den_ngay")
    private Date denNgay;

    @Column(name = "phong_ban_cu")
    private String phongBanCu;

    @Column(name = "phong_ban_moi")
    private String phongBanMoi;

    @Column(name = "chuc_vu_cu")
    private String chucVuCu;

    @Column(name = "chuc_vu_moi")
    private String chucVuMoi;

    @Column(name = "ghi_chu", columnDefinition = "TEXT")
    private String ghiChu;
}