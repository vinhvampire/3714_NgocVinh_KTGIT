package com.Group117.hrm_system.entity;

import jakarta.persistence.*;
import lombok.Data;

@Entity
@Table(name = "phong_ban")
@Data
public class PhongBan {
    @Id
    @Column(name = "id")
    private String id; // VD: PB001

    @Column(name = "ten_phong_ban", nullable = false)
    private String tenPhongBan;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "chi_nhanh_id")
    @com.fasterxml.jackson.annotation.JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
    private ChiNhanh chiNhanh;
}