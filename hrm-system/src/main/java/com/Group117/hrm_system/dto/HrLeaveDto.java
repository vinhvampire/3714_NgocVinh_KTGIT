package com.Group117.hrm_system.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class HrLeaveDto {
    private Long id;
    private String tenNV;
    private String tenPhong;
    private String chucVu;
    private String loaiPhep;
    private String tuNgay;
    private String denNgay;
    private long soNgay;
    private String ngayNop;
    private String deptHeadStatus;
    private String trangThai;
}

