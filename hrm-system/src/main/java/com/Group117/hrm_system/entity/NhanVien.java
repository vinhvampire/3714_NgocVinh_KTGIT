package com.Group117.hrm_system.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.SQLRestriction;
import com.fasterxml.jackson.annotation.*;

import java.time.LocalDate;

@Entity
@Table(name = "nhan_vien")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@SQLDelete(sql = "UPDATE nhan_vien SET trang_thai_hoat_dong = 'DA_NGHI_VIEC' WHERE id = ?")
@SQLRestriction("trang_thai_hoat_dong != 'DA_NGHI_VIEC'")
public class NhanVien {

    @Id
    private String id; // Ví dụ: NV001

    @Column(name = "ho_ten", nullable = false)
    private String hoTen;

    @Column(name = "gioi_tinh")
    private String gioiTinh;

    @Column(name = "so_cccd", unique = true)
    private String soCccd;

    @Column(name = "ngay_cap", columnDefinition = "DATE")
    private LocalDate ngayCap;

    @Column(name = "noi_cap")
    private String noiCap;

    @Column(name = "ngay_sinh", columnDefinition = "DATE")
    private LocalDate ngaySinh;

    @Column(name = "so_dien_thoai")
    private String soDienThoai;

    @Column(name = "email_cong_viec", unique = true)
    private String emailCongViec;

    @Column(name = "dia_chi_tam_tru", columnDefinition = "TEXT")
    private String diaChiTamTru;

    @Column(name = "anh_dai_dien_url")
    private String anhDaiDienUrl;

    @Column(name = "ngay_vao_lam", columnDefinition = "DATE")
    private LocalDate ngayVaoLam;

    @Column(name = "so_ngay_phep_con_lai")
    @Builder.Default
    private Integer soNgayPhepConLai = 12;

    @Column(name = "trang_thai_hoat_dong")
    @Builder.Default
    private String trangThaiHoatDong = "DANG_LAM_VIEC";

    @Column(name = "he_so_luong")
    private Float heSoLuong;

    // --- MAPPING VỚI TỔ CHỨC (Module 3) ---
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "phong_ban_id")
    @JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
    private PhongBan phongBan;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "nhom_id")
    @JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
    private Nhom nhom;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "chuc_vu_id")
    @JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
    private ChucVu chucVu;

    // --- MAPPING VỚI BẢNG LƯƠNG (Module 5) ---
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "bang_luong_id")
    @JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
    private BangLuong bangLuong;

    // --- MAPPING SELF-REFERENCE (Quản lý trực tiếp) ---
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "nguoi_quan_ly_truoc_tiep_id")
    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
    @ToString.Exclude
    private NhanVien nguoiQuanLyTruocTiep;

    // --- MAPPING VỚI TÀI KHOẢN (Module 1) ---
    @OneToOne(mappedBy = "nhanVien", cascade = CascadeType.ALL)
    @JsonIgnore
    @ToString.Exclude
    private TaiKhoan taiKhoan;

    // Để frontend vẫn nhận được ID thay vì cả object nếu cần
    @JsonProperty("phongBanId")
    public String getPhongBanId() {
        return phongBan != null ? phongBan.getId() : null;
    }

    @JsonProperty("chucVuId")
    public String getChucVuId() {
        return chucVu != null ? chucVu.getId() : null;
    }

    @JsonProperty("nhomId")
    public String getNhomId() {
        return nhom != null ? nhom.getId() : null;
    }
}