package com.Group117.hrm_system.Repository;

import com.Group117.hrm_system.entity.ThongBao;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface ThongBaoRepository extends JpaRepository<ThongBao, Long> {

    @Query("SELECT t FROM ThongBao t WHERE (t.nguoiNhan IS NULL OR t.nguoiNhan = :username) ORDER BY t.ngayTao DESC")
    List<ThongBao> findVisibleForUser(@Param("username") String username, Pageable pageable);

    /** Chỉ đếm tin riêng chưa đọc (broadcast không dùng chung cờ daDoc trên một dòng) */
    @Query("SELECT COUNT(t) FROM ThongBao t WHERE t.nguoiNhan = :username AND t.daDoc = false")
    long countUnreadPrivate(@Param("username") String username);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("UPDATE ThongBao t SET t.daDoc = true WHERE t.id = :id AND t.nguoiNhan = :username")
    int markReadIfOwned(@Param("id") Long id, @Param("username") String username);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("UPDATE ThongBao t SET t.daDoc = true WHERE t.nguoiNhan = :username AND t.daDoc = false")
    int markAllReadPrivateForUser(@Param("username") String username);
}
