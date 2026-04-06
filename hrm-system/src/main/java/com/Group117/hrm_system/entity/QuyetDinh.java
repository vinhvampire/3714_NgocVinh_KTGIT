package com.Group117.hrm_system.entity;

import jakarta.persistence.*;
import lombok.Data;
import java.util.Date;

@Entity
@Table(name = "quyet_dinh")
@Data
public class QuyetDinh {
    @Id
    @Column(name = "so_quyet_dinh")
    private String soQuyetDinh; // VD: QD-001/2026

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "nhan_vien_id", nullable = false)
    @com.fasterxml.jackson.annotation.JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
    private NhanVien nhanVien;

    // Phân loại: KHEN_THUONG, KY_LUAT, DIEU_CHUYEN, THOI_VIEC
    @Column(name = "loai_quyet_dinh", nullable = false)
    private String loaiQuyetDinh;

    @Column(name = "ngay_ky")
    private Date ngayKy;

    @Column(name = "noi_dung_quyet_dinh", columnDefinition = "TEXT")
    private String noiDungQuyetDinh;

    @Column(name = "nguoi_ky") // Có thể lưu tên hoặc ID của người có thẩm quyền
    private String nguoiKy;

    @Column(name = "noi_dung", columnDefinition = "TEXT")
    private String noiDung;

    @Column(name = "so_tien") // Dùng cho khen thưởng/kỷ luật bằng tiền
    private Double soTien;
}