package com.Group117.hrm_system.Controller;

import com.Group117.hrm_system.entity.BangLuong;
import com.Group117.hrm_system.entity.PhieuLuong;
import com.Group117.hrm_system.entity.TaiKhoan;
import com.Group117.hrm_system.entity.NhanVien;
import com.Group117.hrm_system.Repository.NhanVienRepository;
import com.Group117.hrm_system.Repository.BangLuongRepository;
import com.Group117.hrm_system.Repository.TaiKhoanRepository;
import com.Group117.hrm_system.Repository.PhieuLuongRepository;
import com.Group117.hrm_system.service.PayrollService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/payroll")
public class PayrollController {

    @Autowired
    private TaiKhoanRepository taiKhoanRepository;

    @Autowired
    private PhieuLuongRepository phieuLuongRepository;

    @Autowired
    private BangLuongRepository bangLuongRepository;

    @Autowired
    private PayrollService payrollService;

    @Autowired
    private NhanVienRepository nhanVienRepository;

    // HR/Admin: Lấy danh sách bảng lương
    @GetMapping("/structure")
    @PreAuthorize("hasAnyRole('HR', 'ADMIN')")
    public ResponseEntity<?> getAllSalaryStructures() {
        return ResponseEntity.ok(bangLuongRepository.findAll());
    }

    // HR/Admin: Xóa bảng lương nếu không có nhân viên nào đang sử dụng
    @DeleteMapping("/structure/{id}")
    @PreAuthorize("hasAnyRole('HR', 'ADMIN')")
    public ResponseEntity<?> deleteSalaryStructure(@PathVariable String id) {
        List<NhanVien> employees = nhanVienRepository.findAll();
        boolean used = employees.stream().anyMatch(nv ->
                nv.getBangLuong() != null
                        && nv.getBangLuong().getId() != null
                        && nv.getBangLuong().getId().equals(id)
        );
        if (used) {
            return ResponseEntity.badRequest().body(Map.of(
                    "message", "Không thể xóa vì đang có nhân viên sử dụng bảng lương này"
            ));
        }
        bangLuongRepository.deleteById(id);
        return ResponseEntity.ok(Map.of("message", "Xóa bảng lương thành công"));
    }

    // 1. API dành cho Nhân viên: Xem danh sách phiếu lương của chính mình
    @GetMapping("/my-payslips")
    public ResponseEntity<?> getMyPayslips() {
        // Lấy username từ SecurityContext (tương tự ProfileController)
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        TaiKhoan tk = taiKhoanRepository.findByUsername(username);

        if (tk == null || tk.getNhanVien() == null) {
            return ResponseEntity.status(404).body("Không tìm thấy thông tin nhân viên");
        }

        // Lấy danh sách phiếu lương theo NhanVienId (dựa trên Repository đã viết)
        List<PhieuLuong> list = phieuLuongRepository.findByNhanVienId(tk.getNhanVien().getId())
                .stream()
                .peek(p -> p.setThangNam(PhieuLuong.normalizeThangNam(p.getThangNam())))
                .toList();
        return ResponseEntity.ok(list);
    }

    // 2. API dành cho Admin: Thiết lập cấu hình lương theo chức vụ
    @PostMapping("/structure")
    public ResponseEntity<?> setupSalaryStructure(@RequestBody BangLuong bangLuong) {
        BangLuong saved = bangLuongRepository.save(bangLuong);
        return ResponseEntity.ok(saved);
    }

    // 3. API dành cho Admin: Kích hoạt quét dữ liệu và tính lương cuối tháng
    @PostMapping("/generate")
    public ResponseEntity<?> generateMonthlyPayroll(@RequestParam String thangNam) {
        try {
            payrollService.scanAndCalculatePayroll(thangNam);
            return ResponseEntity.ok("Tổng hợp lương tháng " + thangNam + " thành công");
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body("Lỗi: " + e.getMessage());
        }
    }

    // 4. API dành cho Kế toán: Cập nhật trạng thái thanh toán phiếu lương
    @PatchMapping("/status/{id}")
    public ResponseEntity<?> updatePaymentStatus(@PathVariable String id, @RequestParam String status) {
        return phieuLuongRepository.findById(id)
                .map(pl -> {
                    pl.setTrangThaiThanhToan(status);
                    phieuLuongRepository.save(pl);
                    return ResponseEntity.ok("Cập nhật trạng thái thành công");
                })
                .orElse(ResponseEntity.status(404).body("Không tìm thấy phiếu lương"));
    }

    // HR/Admin: Lấy tất cả phiếu lương theo tháng (hoặc toàn bộ nếu không truyền thang)
    @GetMapping("/all")
    @PreAuthorize("hasAnyRole('HR', 'ADMIN')")
    public ResponseEntity<?> getAllPayslips(@RequestParam(required = false) String thang) {
        String normalized = PhieuLuong.normalizeThangNam(thang);
        List<PhieuLuong> list = (thang == null || thang.isBlank())
                ? phieuLuongRepository.findAll()
                : phieuLuongRepository.findByThangNamIn(monthAliases(normalized));
        list = list.stream()
                .peek(p -> p.setThangNam(PhieuLuong.normalizeThangNam(p.getThangNam())))
                .toList();
        return ResponseEntity.ok(list);
    }

    // HR/Admin: Phê duyệt phiếu lương
    @PutMapping("/{id}/approve")
    @PreAuthorize("hasAnyRole('HR', 'ADMIN')")
    public ResponseEntity<?> approvePayslip(@PathVariable String id) {
        return phieuLuongRepository.findById(id)
                .map(pl -> {
                    pl.setTrangThaiThanhToan("DA_THANH_TOAN");
                    phieuLuongRepository.save(pl);
                    return ResponseEntity.ok("Phê duyệt phiếu lương thành công");
                })
                .orElse(ResponseEntity.status(404).body("Không tìm thấy phiếu lương"));
    }

    // Director/Admin/HR: Tổng hợp quỹ lương theo tháng
    @GetMapping("/summary")
    @PreAuthorize("hasAnyRole('DIRECTOR', 'ADMIN', 'HR')")
    public ResponseEntity<?> getPayrollSummary(@RequestParam String thang) {
        String normalized = PhieuLuong.normalizeThangNam(thang);
        List<PhieuLuong> list = phieuLuongRepository.findByThangNamIn(monthAliases(normalized));
        double tongQuyLuong = list.stream()
                .map(PhieuLuong::getTongLuong)
                .filter(v -> v != null)
                .mapToDouble(Double::doubleValue)
                .sum();
        long daDuyet = list.stream()
                .filter(p -> "DA_THANH_TOAN".equalsIgnoreCase(p.getTrangThaiThanhToan()))
                .count();
        long chuaDuyet = list.stream()
                .filter(p -> "CHUA_THANH_TOAN".equalsIgnoreCase(p.getTrangThaiThanhToan()))
                .count();

        Map<String, Object> summary = new LinkedHashMap<>();
        summary.put("thang", normalized);
        summary.put("tongNhanVien", list.size());
        summary.put("tongQuyLuong", tongQuyLuong);
        summary.put("daDuyet", daDuyet);
        summary.put("chuaDuyet", chuaDuyet);
        return ResponseEntity.ok(summary);
    }

    private List<String> monthAliases(String normalized) {
        if (normalized == null || normalized.isBlank()) return List.of();
        if (!normalized.contains("-")) return List.of(normalized);
        String[] p = normalized.split("-");
        String legacy = p[1] + "/" + p[0];
        return List.of(normalized, legacy);
    }
}