package com.Group117.hrm_system.entity;

import jakarta.persistence.*;
import lombok.Data;

@Entity
@Table(name = "chi_nhanh")
@Data
public class ChiNhanh {
    @Id
    @Column(name = "id")
    private String id; // VD: CN001

    @Column(name = "ten_chi_nhanh", nullable = false)
    private String tenChiNhanh;

    @Column(name = "dia_chi")
    private String diaChi;
}