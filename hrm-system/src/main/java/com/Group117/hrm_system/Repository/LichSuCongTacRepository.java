package com.Group117.hrm_system.Repository;

import com.Group117.hrm_system.entity.LichSuCongTac;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface LichSuCongTacRepository extends JpaRepository<LichSuCongTac, Long> {
    List<LichSuCongTac> findByNhanVienId(String nhanVienId);
}