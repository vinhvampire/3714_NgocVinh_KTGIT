package com.Group117.hrm_system.Repository;

import com.Group117.hrm_system.entity.LichPhongVan;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface LichPhongVanRepository extends JpaRepository<LichPhongVan, String> {
    List<LichPhongVan> findByUngVien_Id(Integer ungVienId);
}