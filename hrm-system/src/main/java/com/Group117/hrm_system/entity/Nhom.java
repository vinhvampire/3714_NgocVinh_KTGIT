package com.Group117.hrm_system.entity;

import jakarta.persistence.*;
import lombok.Data;

@Entity
@Table(name = "nhom")
@Data
public class Nhom {
    @Id
    @Column(name = "id")
    private String id; // VD: NH001

    @Column(name = "ten_nhom", nullable = false)
    private String tenNhom;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "phong_ban_id")
    @com.fasterxml.jackson.annotation.JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
    private PhongBan phongBan;
}