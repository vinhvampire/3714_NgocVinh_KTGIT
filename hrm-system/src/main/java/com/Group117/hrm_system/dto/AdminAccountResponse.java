package com.Group117.hrm_system.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AdminAccountResponse {
    private String maTaiKhoan;
    private String username;
    private String role;
    private boolean trangThaiTaiKhoan;
    private String nhanVienId;
    private String hoTenNhanVien;
    private String directManagerId;
    private String emailCongViec;
    private LocalDateTime ngayTao;
}
