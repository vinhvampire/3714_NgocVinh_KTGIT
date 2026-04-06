package com.Group117.hrm_system.entity;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

@Entity
@Table(name = "lich_phong_van")
@Data
public class LichPhongVan {
    @Id
    @Column(name = "id_lich", length = 50)
    private String idLich;

    @Column(name = "thoi_gian")
    private LocalDateTime thoiGian;

    @Column(name = "dia_diem")
    private String diaDiem;

    @Column(name = "ghi_chu", columnDefinition = "TEXT")
    private String ghiChu;

    @ManyToOne
    @JoinColumn(name = "id_ung_vien")
    private HoSoUngVien ungVien;

    @Column(name = "nguoi_phong_van")
    private String nguoiPhongVan; // Có thể lưu tên hoặc ID nhân viên
}