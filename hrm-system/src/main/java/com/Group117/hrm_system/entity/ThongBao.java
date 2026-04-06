package com.Group117.hrm_system.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "thong_bao")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ThongBao {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** null = thông báo chung (broadcast), hiển thị cho mọi user */
    @Column(name = "nguoi_nhan")
    private String nguoiNhan;

    @Column(name = "tieu_de", nullable = false, length = 500)
    private String tieuDe;

    @Column(name = "noi_dung", columnDefinition = "TEXT")
    private String noiDung;

    @Column(name = "loai", length = 64)
    private String loai;

    @Column(name = "da_doc")
    @Builder.Default
    private boolean daDoc = false;

    @Column(name = "ngay_tao")
    private LocalDateTime ngayTao;

    /** JSON nhỏ: ref id đơn, phiếu lương... */
    @Column(name = "ref_payload", length = 2000)
    private String refPayload;
}
