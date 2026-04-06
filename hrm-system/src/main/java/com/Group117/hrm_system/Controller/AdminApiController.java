package com.Group117.hrm_system.Controller;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.Group117.hrm_system.dto.AdminAccountResponse;
import com.Group117.hrm_system.service.AdminService;
import com.Group117.hrm_system.service.DashboardService;

@RestController
@RequestMapping("/api/dashboard/admin")
@PreAuthorize("hasRole('ADMIN')")
public class AdminApiController {

    @Autowired
    private AdminService adminService;

    @Autowired
    private DashboardService dashboardService;

    @GetMapping("/stats")
    public ResponseEntity<?> stats() {
        return ResponseEntity.ok(dashboardService.getAdminStats());
    }

    @GetMapping("/accounts")
    public ResponseEntity<List<AdminAccountResponse>> listAccounts(
            @RequestParam(required = false) String q,
            @RequestParam(required = false) String role,
            @RequestParam(required = false) Boolean active) {
        return ResponseEntity.ok(adminService.listAccounts(q, role, active));
    }

    @GetMapping("/accounts/unassigned")
    public ResponseEntity<List<AdminAccountResponse>> unassignedTaiKhoan() {
        return ResponseEntity.ok(adminService.listTaiKhoanChuaGanNhanVien());
    }

    @GetMapping("/employees/unassigned")
    public ResponseEntity<List<Map<String, String>>> unassignedNhanVien() {
        return ResponseEntity.ok(adminService.listNhanVienChuaCoTaiKhoanBrief());
    }

    /** Chưa có bảng audit — trả về mảng rỗng, giữ endpoint cho frontend */
    @GetMapping("/login-logs")
    public ResponseEntity<List<Map<String, Object>>> loginLogs() {
        return ResponseEntity.ok(List.of());
    }

    @PostMapping("/accounts")
    public ResponseEntity<AdminAccountResponse> create(@RequestBody Map<String, String> body) {
        String actor = currentUsername();
        String u = body.get("username");
        String p = body.get("password");
        String role = body.get("role");
        String nvid = body.get("nhanVienId");
        return ResponseEntity.ok(adminService.createAccount(u, p, role, nvid, actor));
    }

    @PutMapping("/accounts/{maTaiKhoan}/toggle-status")
    public ResponseEntity<AdminAccountResponse> toggle(@PathVariable String maTaiKhoan) {
        return ResponseEntity.ok(adminService.toggleStatus(maTaiKhoan, currentUsername()));
    }

    @PutMapping("/accounts/{maTaiKhoan}/role")
    public ResponseEntity<AdminAccountResponse> role(
            @PathVariable String maTaiKhoan,
            @RequestBody Map<String, String> body) {
        String newRole = body != null ? body.get("role") : null;
        // Cho phép UI truyền thêm directManagerId để set quan hệ duyệt theo workflow
        String directManagerId = body != null ? body.get("directManagerId") : null;
        return ResponseEntity.ok(adminService.updateRoleAndDirectManager(maTaiKhoan, newRole, directManagerId, currentUsername()));
    }

    @GetMapping("/employees/direct-managers")
    public ResponseEntity<List<Map<String, String>>> directManagerCandidates() {
        return ResponseEntity.ok(adminService.listDirectManagerCandidates());
    }

    @GetMapping("/employees/with-accounts")
    public ResponseEntity<List<Map<String, String>>> employeesWithAccounts() {
        return ResponseEntity.ok(adminService.listEmployeesWithAccountsBrief());
    }

    @GetMapping("/employees/subordinates")
    public ResponseEntity<List<Map<String, String>>> subordinates(
            @RequestParam("managerId") String managerId) {
        return ResponseEntity.ok(adminService.listSubordinatesBrief(managerId));
    }

    @GetMapping("/employees/subordinate-candidates")
    public ResponseEntity<List<Map<String, String>>> subordinateCandidates(
            @RequestParam("managerId") String managerId) {
        return ResponseEntity.ok(adminService.listUngVienSubordinateBrief(managerId));
    }

    @PutMapping("/accounts/{maTaiKhoan}/manage-subordinates")
    public ResponseEntity<Void> manageSubordinates(
            @PathVariable String maTaiKhoan,
            @RequestBody Map<String, Object> body) {

        Object raw = body != null ? body.get("employeeIds") : null;
        List<String> employeeIds = null;
        if (raw instanceof List<?> list) {
            employeeIds = list.stream()
                    .map(x -> x != null ? String.valueOf(x) : null)
                    .filter(x -> x != null && !x.isBlank())
                    .collect(Collectors.toList());
        }

        adminService.updateManagedSubordinates(maTaiKhoan, employeeIds, currentUsername());
        return ResponseEntity.ok().build();
    }

    @PutMapping("/accounts/{maTaiKhoan}/assign")
    public ResponseEntity<AdminAccountResponse> assign(
            @PathVariable String maTaiKhoan,
            @RequestBody Map<String, String> body) {
        String nvid = body != null ? body.get("nhanVienId") : null;
        return ResponseEntity.ok(adminService.assignNhanVien(maTaiKhoan, nvid, currentUsername()));
    }

    @DeleteMapping("/accounts/{maTaiKhoan}")
    public ResponseEntity<Void> delete(@PathVariable String maTaiKhoan) {
        adminService.deleteAccount(maTaiKhoan, currentUsername());
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/accounts/{maTaiKhoan}/reset-password")
    public ResponseEntity<Map<String, String>> resetPassword(
            @PathVariable String maTaiKhoan,
            @RequestBody(required = false) Map<String, String> body) {
        String np = body != null ? body.get("newPassword") : null;
        String raw = adminService.resetPassword(maTaiKhoan, np);
        return ResponseEntity.ok(Map.of("message", "Đã đặt mật khẩu mới", "temporaryPassword", raw));
    }

    private static String currentUsername() {
        var auth = SecurityContextHolder.getContext().getAuthentication();
        return auth != null ? auth.getName() : null;
    }
}
