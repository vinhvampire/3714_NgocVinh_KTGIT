package com.Group117.hrm_system.Repository;

import com.Group117.hrm_system.entity.YeuCauTuyenDung;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface YeuCauTuyenDungRepository extends JpaRepository<YeuCauTuyenDung, String> {
    // Ông có thể thêm các hàm tìm kiếm theo trạng thái nếu cần
}