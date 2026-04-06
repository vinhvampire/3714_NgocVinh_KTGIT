package com.Group117.hrm_system.Repository;

import com.Group117.hrm_system.entity.ChamCong;
import org.springframework.data.jpa.repository.JpaRepository;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface ChamCongRepository extends JpaRepository<ChamCong, Long> {
    Optional<ChamCong> findByNhanVienIdAndNgay(String nhanVienId, LocalDate ngay);
    
    List<ChamCong> findByNhanVienIdAndNgayBetween(String nhanVienId, LocalDate start, LocalDate end);
    
    long countByNhanVienIdAndNgayBetweenAndTrangThai(String nhanVienId, LocalDate start, LocalDate end, String trangThai);
}