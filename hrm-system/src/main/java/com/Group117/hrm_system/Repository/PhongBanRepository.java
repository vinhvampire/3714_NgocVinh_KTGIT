package com.Group117.hrm_system.Repository;

import com.Group117.hrm_system.entity.PhongBan;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface PhongBanRepository extends JpaRepository<PhongBan, String> {
    // Kế thừa sẵn các hàm findAll, save, deleteById...
}