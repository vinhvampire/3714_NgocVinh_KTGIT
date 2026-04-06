package com.Group117.hrm_system.Repository;

import com.Group117.hrm_system.entity.DonNghiPhep;
import com.Group117.hrm_system.entity.NhanVien;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface DonNghiPhepRepository extends JpaRepository<DonNghiPhep, Long> {
    List<DonNghiPhep> findByNhanVien(NhanVien nv);

    List<DonNghiPhep> findTop5ByNhanVienIdOrderByTuNgayDesc(String nhanVienId);

    List<DonNghiPhep> findByNhanVienIdOrderByTuNgayDesc(String nhanVienId);

    List<DonNghiPhep> findByTrangThai(String trangThai);

    List<DonNghiPhep> findByNhanVienIdAndTrangThai(String nhanVienId, String trangThai);

    // Workflow: CHO_QL_DUYET (đợi quản lý duyệt) theo đúng người quản lý
    List<DonNghiPhep> findByNguoiDuyet_IdAndTrangThai(String nguoiDuyetId, String trangThai);

    // Workflow: CHO_HR_XAC_NHAN (đợi HR xác nhận)
    List<DonNghiPhep> findByTrangThaiOrderByTuNgayDesc(String trangThai);
}