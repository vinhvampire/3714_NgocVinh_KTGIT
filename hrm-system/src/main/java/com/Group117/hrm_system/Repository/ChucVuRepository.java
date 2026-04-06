package com.Group117.hrm_system.Repository;

import com.Group117.hrm_system.entity.ChucVu;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ChucVuRepository extends JpaRepository<ChucVu, String> {
    // Kế thừa sẵn các hàm findAll, save, deleteById...
}