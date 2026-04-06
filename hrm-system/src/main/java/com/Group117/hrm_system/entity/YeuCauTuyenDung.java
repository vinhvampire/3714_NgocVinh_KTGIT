package com.Group117.hrm_system.entity;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDate;

@Entity
@Table(name = "yeu_cau_tuyen_dung")
@Data
public class YeuCauTuyenDung {

    @Id
    @Column(name = "id_yeu_cau", length = 50)
    private String idYeuCau;

    @Column(name = "vi_tri_can_tuyen")
    private String viTriCanTuyen;

    @Column(name = "so_luong")
    private Integer soLuong;

    @Column(name = "trinh_do_yeu_cau", columnDefinition = "TEXT")
    private String trinhDoYeuCau;

    @Column(name = "trang_thai")
    private String trangThai = "PENDING";

    @Column(name = "mo_ta", columnDefinition = "TEXT")
    private String moTa;

    @Column(name = "ngay_yeu_cau")
    private LocalDate ngayYeuCau = LocalDate.now();

    @ManyToOne
    @JoinColumn(name = "phong_ban_id", referencedColumnName = "id")
    @com.fasterxml.jackson.annotation.JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
    private PhongBan phongBan;
}