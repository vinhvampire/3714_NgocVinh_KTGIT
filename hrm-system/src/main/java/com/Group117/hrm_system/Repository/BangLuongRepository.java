package com.Group117.hrm_system.Repository;

import com.Group117.hrm_system.entity.BangLuong;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface BangLuongRepository extends JpaRepository<BangLuong, String> {
    // Tìm kiếm cấu hình lương theo tên chức vụ (Dùng cho API thiết lập)
    Optional<BangLuong> findByTenChucVu(String tenChucVu);
}