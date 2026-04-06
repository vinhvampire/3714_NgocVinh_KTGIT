package com.Group117.hrm_system.entity;

import jakarta.persistence.*;
import lombok.Data;

@Entity
@Table(name = "chuc_vu")
@Data
public class ChucVu {
    @Id
    @Column(name = "id")
    private String id; // VD: CV001

    @Column(name = "ten_chuc_vu", nullable = false)
    private String tenChucVu;
}