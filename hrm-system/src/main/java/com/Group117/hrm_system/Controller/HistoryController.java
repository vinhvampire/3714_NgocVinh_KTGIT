package com.Group117.hrm_system.Controller;

import com.Group117.hrm_system.entity.LichSuCongTac;
import com.Group117.hrm_system.entity.QuyetDinh;
import com.Group117.hrm_system.service.HistoryService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/history")
public class HistoryController {

    @Autowired
    private HistoryService historyService;

    // API Ban hành Quyết định (Điều chuyển, Thôi việc...)
    @PreAuthorize("hasAnyRole('ADMIN','HR')")
    @PostMapping("/quyet-dinh")
    public ResponseEntity<QuyetDinh> banHanhQuyetDinh(
            @RequestBody QuyetDinh quyetDinh,
            @RequestParam(required = false) String phongBanMoiId,
            @RequestParam(required = false) String chucVuMoiId) {

        // Gọi hàm banHanhQuyetDinh để hưởng trọn logic:
        // Lưu Quyết định + Cập nhật NV + Ghi Lịch sử
        return ResponseEntity.ok(historyService.banHanhQuyetDinh(quyetDinh, phongBanMoiId, chucVuMoiId));
    }

    // API Lấy lịch sử của 1 nhân viên
    @GetMapping("/cong-tac/{nhanVienId}")
    public ResponseEntity<List<LichSuCongTac>> getLichSuCongTac(@PathVariable String nhanVienId) {
        return ResponseEntity.ok(historyService.getLichSuByNhanVien(nhanVienId));
    }

    // API danh sách quyết định cho HR/Admin
    @PreAuthorize("hasAnyRole('ADMIN','HR')")
    @GetMapping("/quyet-dinh")
    public ResponseEntity<List<Map<String, Object>>> getQuyetDinhs(
            @RequestParam(required = false) String nhanVienId) {
        return ResponseEntity.ok(historyService.getAllQuyetDinh(nhanVienId));
    }
}