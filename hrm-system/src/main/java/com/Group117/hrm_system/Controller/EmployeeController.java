package com.Group117.hrm_system.Controller;

import com.Group117.hrm_system.entity.NhanVien;
import org.springframework.security.access.prepost.PreAuthorize;
import com.Group117.hrm_system.service.EmployeeService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/employees")
public class EmployeeController {

    @Autowired
    private EmployeeService employeeService;

    // 1. Lấy danh sách nhân viên (Dùng cho bảng danh sách nhân viên)
    @GetMapping
    public ResponseEntity<List<NhanVien>> getAllEmployees() {
        return ResponseEntity.ok(employeeService.getAllEmployees());
    }

    // 2. Thêm mới nhân viên (Onboarding) - Chỉ Admin
    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/onboard")
    public ResponseEntity<NhanVien> onboardEmployee(@RequestBody NhanVien nhanVien) {
        return ResponseEntity.ok(employeeService.onboardEmployee(nhanVien));
    }

    // 3. Cập nhật hồ sơ cá nhân (CCCD, SĐT, Email...)
    @PutMapping("/{id}/update-profile")
    public ResponseEntity<NhanVien> updateProfile(
            @PathVariable String id,
            @RequestBody NhanVien updatedInfo) {
        // Gọi thẳng vào Service để xử lý, không gọi trực tiếp Repository ở đây
        return ResponseEntity.ok(employeeService.updateProfile(id, updatedInfo));
    }
    // Lấy chi tiết 1 nhân viên theo ID
    @GetMapping("/{id}")
    public ResponseEntity<NhanVien> getEmployeeById(@PathVariable String id) {
        return ResponseEntity.ok(employeeService.getEmployeeById(id));
    }

    // HR/Admin: Gán hoặc hủy gán bảng lương cho nhân viên
    @PutMapping("/{id}/bang-luong")
    @PreAuthorize("hasAnyRole('HR', 'ADMIN')")
    public ResponseEntity<?> setEmployeeBangLuong(
            @PathVariable String id,
            @RequestBody(required = false) Map<String, String> payload) {

        String bangLuongId = payload != null ? payload.get("bangLuongId") : null;

        try {
            employeeService.setBangLuongForEmployee(id, bangLuongId);
            return ResponseEntity.ok(Map.of("message", "Gán bảng lương thành công"));
        } catch (RuntimeException ex) {
            String msg = ex.getMessage() != null ? ex.getMessage() : "Lỗi";
            // Không dùng entity trực tiếp nên trả JSON message theo yêu cầu
            if (msg.toLowerCase().contains("nhân viên")) {
                return ResponseEntity.status(404).body(Map.of("message", "Không tìm thấy nhân viên"));
            }
            if (msg.toLowerCase().contains("bảng lương")) {
                return ResponseEntity.status(404).body(Map.of("message", "Không tìm thấy bảng lương"));
            }
            return ResponseEntity.status(400).body(Map.of("message", msg));
        }
    }
}