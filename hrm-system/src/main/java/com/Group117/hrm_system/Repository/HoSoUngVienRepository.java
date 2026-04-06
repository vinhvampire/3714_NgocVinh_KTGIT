package com.Group117.hrm_system.Repository;

import com.Group117.hrm_system.entity.HoSoUngVien;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface HoSoUngVienRepository extends JpaRepository<HoSoUngVien, Integer> {
}