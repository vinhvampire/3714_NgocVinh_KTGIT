package com.Group117.hrm_system.entity;

import jakarta.persistence.*;
import lombok.Data;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Entity
@Table(name = "phieu_luong")
@Data
public class PhieuLuong {
    @Id
    @Column(name = "id")
    private String id; // VD: PL_NV001_032026

    @Column(name = "thang_nam")
    private String thangNam;

    @Column(name = "luong_co_ban")
    private Double luongCoBan;

    @Column(name = "phu_cap")
    private Double phuCap;

    @Column(name = "phat_muon")
    private Double phatMuon;

    @Column(name = "nghi_khong_phep")
    private Double nghiKhongPhep;

    @Column(name = "tong_luong")
    private Double tongLuong;

    @Column(name = "trang_thai_thanh_toan")
    private String trangThaiThanhToan; // "CHUA_THANH_TOAN", "DA_THANH_TOAN"

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "nhan_vien_id")
    @com.fasterxml.jackson.annotation.JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
    private NhanVien nhanVien;

    // Chuẩn hóa thangNam về định dạng duy nhất YYYY-MM
    public void setThangNam(String thangNam) {
        this.thangNam = normalizeThangNam(thangNam);
    }

    @PrePersist
    @PreUpdate
    private void normalizeBeforeSave() {
        this.thangNam = normalizeThangNam(this.thangNam);
    }

    public static String normalizeThangNam(String value) {
        if (value == null) return null;
        String raw = value.trim();
        if (raw.isEmpty()) return raw;

        Matcher iso = Pattern.compile("^(\\d{4})[-/](\\d{1,2})$").matcher(raw);
        if (iso.matches()) {
            int year = Integer.parseInt(iso.group(1));
            int month = Integer.parseInt(iso.group(2));
            if (month >= 1 && month <= 12) return String.format("%04d-%02d", year, month);
        }

        Matcher legacy = Pattern.compile("^(\\d{1,2})[-/](\\d{4})$").matcher(raw);
        if (legacy.matches()) {
            int month = Integer.parseInt(legacy.group(1));
            int year = Integer.parseInt(legacy.group(2));
            if (month >= 1 && month <= 12) return String.format("%04d-%02d", year, month);
        }

        return raw;
    }
}