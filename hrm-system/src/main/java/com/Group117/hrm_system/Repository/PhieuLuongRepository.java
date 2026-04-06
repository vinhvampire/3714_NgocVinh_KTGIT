package com.Group117.hrm_system.Repository;

import com.Group117.hrm_system.entity.PhieuLuong;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PhieuLuongRepository extends JpaRepository<PhieuLuong, String> {
    
    // Tìm danh sách phiếu lương của một nhân viên cụ thể (Giống mẫu findByNhanVienId)
    List<PhieuLuong> findByNhanVienId(String nhanVienId);

    // Tìm tất cả phiếu lương của một tháng cụ thể để tổng hợp báo cáo
    List<PhieuLuong> findByThangNam(String thangNam);
    List<PhieuLuong> findByThangNamIn(List<String> thangNamList);

    // Tìm phiếu lương dựa trên nhân viên và tháng (Để tránh tạo trùng phiếu lương)
    List<PhieuLuong> findByNhanVienIdAndThangNam(String nhanVienId, String thangNam);
    List<PhieuLuong> findByNhanVienIdAndThangNamIn(String nhanVienId, List<String> thangNamList);

    // Tìm phiếu lương theo trạng thái thanh toán
    List<PhieuLuong> findByTrangThaiThanhToan(String trangThaiThanhToan);
}