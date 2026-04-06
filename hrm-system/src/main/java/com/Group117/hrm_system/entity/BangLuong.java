package com.Group117.hrm_system.entity;

import jakarta.persistence.*;
import lombok.Data;

@Entity
@Table(name = "bang_luong")
@Data
public class BangLuong {
    @Id
    @Column(name = "id")
    private String id; // VD: BL001

    @Column(name = "ten_chuc_vu", nullable = false)
    private String tenChucVu;

    @Column(name = "luong_co_ban")
    private Double luongCoBan;

    @Column(name = "phu_cap_dinh_muc")
    private Double phuCapDinhMuc;
}