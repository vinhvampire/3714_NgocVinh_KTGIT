package com.Group117.hrm_system.service;

import com.Group117.hrm_system.Repository.NhanVienRepository;
import com.Group117.hrm_system.Repository.BangLuongRepository;
import com.Group117.hrm_system.entity.NhanVien;
import com.Group117.hrm_system.entity.BangLuong;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
public class EmployeeService {

    @Autowired
    private NhanVienRepository nhanVienRepository;

    @Autowired
    private BangLuongRepository bangLuongRepository;

    // 1. Thêm mới nhân viên (Onboarding - Không tự tạo tài khoản)
    public NhanVien onboardEmployee(NhanVien nhanVien) {
        if (nhanVien.getId() == null || nhanVien.getId().isEmpty()) {
            nhanVien.setId("NV-" + UUID.randomUUID().toString().substring(0, 5).toUpperCase());
        }
        // Lưu thẳng thông tin hồ sơ vào Database
        return nhanVienRepository.save(nhanVien);
    }

    // 2. Cập nhật hồ sơ cá nhân
    public NhanVien updateProfile(String id, NhanVien updatedInfo) {
        NhanVien existingEmployee = nhanVienRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy nhân viên"));

        // Cập nhật các trường cho phép sửa
        if (updatedInfo.getSoDienThoai() != null) existingEmployee.setSoDienThoai(updatedInfo.getSoDienThoai());
        if (updatedInfo.getEmailCongViec() != null) existingEmployee.setEmailCongViec(updatedInfo.getEmailCongViec());
        if (updatedInfo.getDiaChiTamTru() != null) existingEmployee.setDiaChiTamTru(updatedInfo.getDiaChiTamTru());
        if (updatedInfo.getAnhDaiDienUrl() != null) existingEmployee.setAnhDaiDienUrl(updatedInfo.getAnhDaiDienUrl());
        if (updatedInfo.getSoCccd() != null) existingEmployee.setSoCccd(updatedInfo.getSoCccd());

        return nhanVienRepository.save(existingEmployee);
    }

    public NhanVien getEmployeeById(String id) {
        return nhanVienRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy nhân viên với mã: " + id));
    }
    // Lấy danh sách tất cả nhân viên đang làm việc
    public List<NhanVien> getAllEmployees() {
        return nhanVienRepository.findAll();
    }

    // Gán/huỷ gán bảng lương cho nhân viên
    public void setBangLuongForEmployee(String employeeId, String bangLuongId) {
        NhanVien nv = nhanVienRepository.findById(employeeId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy nhân viên"));

        if (bangLuongId == null || bangLuongId.isBlank()) {
            nv.setBangLuong(null);
            nhanVienRepository.save(nv);
            return;
        }

        BangLuong bl = bangLuongRepository.findById(bangLuongId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy bảng lương"));

        nv.setBangLuong(bl);
        nhanVienRepository.save(nv);
    }
}