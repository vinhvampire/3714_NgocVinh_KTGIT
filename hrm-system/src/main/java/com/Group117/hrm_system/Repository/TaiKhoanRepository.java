package com.Group117.hrm_system.Repository;
import com.Group117.hrm_system.entity.TaiKhoan;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface TaiKhoanRepository extends JpaRepository<TaiKhoan, String> {
    TaiKhoan findByUsername(String username);
    TaiKhoan findByResetToken(String token);

    Optional<TaiKhoan> findByNhanVien_Id(String nhanVienId);

    long countByRoleIgnoreCase(String role);

    List<TaiKhoan> findByRoleIgnoreCase(String role);
}