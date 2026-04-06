package com.Group117.hrm_system.Repository;

import com.Group117.hrm_system.entity.NhanVien;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface NhanVienRepository extends JpaRepository<NhanVien, String> {
    // Tìm nhân viên dựa vào email công việc
    NhanVien findByEmailCongViec(String email);

    @Query("SELECT n FROM NhanVien n WHERE n.taiKhoan IS NULL")
    List<NhanVien> findNhanVienChuaCoTaiKhoan();

    // Nhân viên có tài khoản và vai trò EMPLOYEE (dùng làm người quản lý trực tiếp)
    @Query("SELECT n FROM NhanVien n WHERE n.taiKhoan IS NOT NULL AND UPPER(n.taiKhoan.role) = 'EMPLOYEE'")
    List<NhanVien> findNhanVienCoTaiKhoanRoleEmployee();

    // Dùng cho UI gán quan hệ quản lý trực tiếp (manager quản nhiều nhân viên)
    @Query("SELECT n FROM NhanVien n WHERE n.taiKhoan IS NOT NULL")
    List<NhanVien> findNhanVienCoTaiKhoan();

    @Query("SELECT n FROM NhanVien n WHERE n.nguoiQuanLyTruocTiep.id = :managerId")
    List<NhanVien> findNhanVienByNguoiQuanLyTruocTiepId(@org.springframework.data.repository.query.Param("managerId") String managerId);

    // Ứng viên để gán quản lý trực tiếp:
    // - Nhân viên (ROLE EMPLOYEE) chưa có quản lý trực tiếp
    // - hoặc đang do chính manager này quản lý (để UI giữ lựa chọn hiện tại)
    @Query("""
            SELECT n
            FROM NhanVien n
            WHERE n.taiKhoan IS NOT NULL
              AND UPPER(n.taiKhoan.role) = 'EMPLOYEE'
              AND n.id <> :managerId
              AND (n.nguoiQuanLyTruocTiep IS NULL OR n.nguoiQuanLyTruocTiep.id = :managerId)
            """)
    List<NhanVien> findUngVienGiaoQuanLyTrucTiep(@org.springframework.data.repository.query.Param("managerId") String managerId);
}