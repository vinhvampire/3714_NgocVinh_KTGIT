package com.Group117.hrm_system.Repository;

import com.Group117.hrm_system.entity.QuyetDinh;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface QuyetDinhRepository extends JpaRepository<QuyetDinh, String> {
    List<QuyetDinh> findByNhanVienId(String nhanVienId);
}