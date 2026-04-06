package com.Group117.hrm_system.Controller;

import com.Group117.hrm_system.Repository.DonNghiPhepRepository;
import com.Group117.hrm_system.Repository.NhanVienRepository;
import com.Group117.hrm_system.Repository.TaiKhoanRepository;
import com.Group117.hrm_system.service.DashboardService;
import com.Group117.hrm_system.entity.DonNghiPhep;
import com.Group117.hrm_system.entity.NhanVien;
import com.Group117.hrm_system.entity.TaiKhoan;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.LocalDate;
import java.util.*;

@RestController
@RequestMapping("/api/dashboard/employee")
public class EmployeeDashboardController {

    private final TaiKhoanRepository taiKhoanRepository;
    private final NhanVienRepository nhanVienRepository;
    private final DonNghiPhepRepository donNghiPhepRepository;
    private final DashboardService dashboardService;

    @Value("${app.upload.avatar-dir:uploads/avatars}")
    private String avatarDir;

    public EmployeeDashboardController(TaiKhoanRepository taiKhoanRepository,
                                       NhanVienRepository nhanVienRepository,
                                       DonNghiPhepRepository donNghiPhepRepository,
                                       DashboardService dashboardService) {
        this.taiKhoanRepository = taiKhoanRepository;
        this.nhanVienRepository = nhanVienRepository;
        this.donNghiPhepRepository = donNghiPhepRepository;
        this.dashboardService = dashboardService;
    }

    @GetMapping("/profile")
    public ResponseEntity<?> getProfile() {
        NhanVien nv = currentNhanVien();
        if (nv == null) return unauthorized("Chưa xác thực");
        return ResponseEntity.ok(toProfileMap(nv));
    }

    @PutMapping("/profile")
    public ResponseEntity<?> updateProfile(@RequestBody Map<String, String> body) {
        NhanVien nv = currentNhanVien();
        if (nv == null) return unauthorized("Chưa xác thực");

        nv.setEmailCongViec(trimOrNull(body.get("email")));
        nv.setSoDienThoai(trimOrNull(body.get("phone")));
        nv.setDiaChiTamTru(trimOrNull(body.get("address")));

        String city = trimOrNull(body.get("city"));
        String notes = trimOrNull(body.get("notes"));
        if (city != null || notes != null) {
            String base = nv.getDiaChiTamTru() == null ? "" : nv.getDiaChiTamTru().trim();
            if (city != null && !city.isBlank()) base = base.isBlank() ? city : (base + ", " + city);
            if (notes != null && !notes.isBlank()) base = base.isBlank() ? ("Ghi chú: " + notes) : (base + "\nGhi chú: " + notes);
            nv.setDiaChiTamTru(base);
        }

        String startDate = trimOrNull(body.get("startDate"));
        if (startDate != null && !startDate.isBlank()) {
            try {
                nv.setNgayVaoLam(LocalDate.parse(startDate));
            } catch (Exception e) {
                return badRequest("startDate không hợp lệ, định dạng yyyy-MM-dd");
            }
        }

        nhanVienRepository.save(nv);
        return ResponseEntity.ok(toProfileMap(nv));
    }

    @PostMapping("/profile/avatar")
    public ResponseEntity<?> uploadAvatar(@RequestParam("avatar") MultipartFile avatar) {
        NhanVien nv = currentNhanVien();
        if (nv == null) return unauthorized("Chưa xác thực");
        if (avatar == null || avatar.isEmpty()) return badRequest("Chưa chọn file");

        String contentType = avatar.getContentType() == null ? "" : avatar.getContentType().toLowerCase();
        Set<String> allowed = Set.of("image/jpeg", "image/png", "image/gif", "image/webp");
        if (!allowed.contains(contentType)) return badRequest("Định dạng ảnh không hợp lệ");
        if (avatar.getSize() > 5L * 1024 * 1024) return badRequest("Kích thước file vượt quá 5MB");

        try {
            Path dir = Paths.get(avatarDir).toAbsolutePath().normalize();
            Files.createDirectories(dir);

            String ext = extensionFromContentType(contentType);
            String filename = nv.getId() + "_" + UUID.randomUUID().toString().replace("-", "") + "." + ext;
            Path target = dir.resolve(filename).normalize();
            Files.copy(avatar.getInputStream(), target, StandardCopyOption.REPLACE_EXISTING);

            String avatarUrl = "/uploads/avatars/" + filename;
            nv.setAnhDaiDienUrl(avatarUrl);
            nhanVienRepository.save(nv);
            return ResponseEntity.ok(Map.of("avatarUrl", avatarUrl));
        } catch (Exception e) {
            System.err.println(">>> uploadAvatar error: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("message", "Không thể upload ảnh"));
        }
    }

    @GetMapping("/stats")
    public ResponseEntity<?> stats() {
        NhanVien nv = currentNhanVien();
        if (nv == null) return unauthorized("Chưa xác thực");
        // Trả đúng cấu trúc stats mà dashboard/employee.html đang sử dụng
        return ResponseEntity.ok(dashboardService.getEmployeeStats(nv));
    }

    @GetMapping("/leaves")
    public ResponseEntity<?> leaves(@RequestParam(required = false) Integer limit) {
        NhanVien nv = currentNhanVien();
        if (nv == null) return unauthorized("Chưa xác thực");

        List<Map<String, Object>> out = dashboardService.getEmployeeLeaves(nv.getId(), limit).stream()
                .map(this::toLeaveMap)
                .toList();
        return ResponseEntity.ok(out);
    }

    @PostMapping("/leaves")
    public ResponseEntity<?> createLeave(@RequestBody Map<String, String> body) {
        NhanVien nv = currentNhanVien();
        if (nv == null) return unauthorized("Chưa xác thực");

        String loaiNghi = trimOrNull(body.get("loaiNghi"));
        String lyDo = trimOrNull(body.get("lyDo"));
        LocalDate tuNgay;
        LocalDate denNgay;

        if (loaiNghi == null || loaiNghi.isBlank()) return badRequest("loaiNghi là bắt buộc");
        try {
            tuNgay = LocalDate.parse(body.get("tuNgay"));
            denNgay = LocalDate.parse(body.get("denNgay"));
        } catch (Exception e) {
            return badRequest("tuNgay/denNgay phải theo định dạng yyyy-MM-dd");
        }
        if (tuNgay.isAfter(denNgay)) return badRequest("tuNgay phải nhỏ hơn hoặc bằng denNgay");
        if (tuNgay.isBefore(LocalDate.now())) return badRequest("tuNgay không được nhỏ hơn ngày hiện tại");

        DonNghiPhep d = new DonNghiPhep();
        d.setNhanVien(nv);
        d.setLoaiNghi(loaiNghi);
        d.setTuNgay(tuNgay);
        d.setDenNgay(denNgay);
        d.setLyDo(lyDo);
        d.setTrangThai("CHO_QL_DUYET");
        d = donNghiPhepRepository.save(d);

        return ResponseEntity.status(HttpStatus.CREATED).body(toLeaveMap(d));
    }

    @DeleteMapping("/leaves/{id}")
    public ResponseEntity<?> deleteLeave(@PathVariable Long id) {
        NhanVien nv = currentNhanVien();
        if (nv == null) return unauthorized("Chưa xác thực");

        DonNghiPhep d = donNghiPhepRepository.findById(id).orElse(null);
        if (d == null) return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("message", "Không tìm thấy đơn"));
        if (d.getNhanVien() == null || d.getNhanVien().getId() == null || !d.getNhanVien().getId().equals(nv.getId())) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("message", "Không có quyền thao tác đơn này"));
        }

        String st = d.getTrangThai() == null ? "" : d.getTrangThai().toUpperCase();
        if (!"CHO_QL_DUYET".equals(st) && !"CHO_HR_XAC_NHAN".equals(st)) {
            return badRequest("Không thể hủy đơn ở trạng thái hiện tại");
        }

        donNghiPhepRepository.delete(d);
        return ResponseEntity.ok(Map.of("message", "Đã hủy đơn"));
    }

    private NhanVien currentNhanVien() {
        try {
            String username = SecurityContextHolder.getContext().getAuthentication().getName();
            TaiKhoan tk = taiKhoanRepository.findByUsername(username);
            return tk != null ? tk.getNhanVien() : null;
        } catch (Exception e) {
            return null;
        }
    }

    private Map<String, Object> toProfileMap(NhanVien nv) {
        Map<String, Object> p = new LinkedHashMap<>();
        p.put("hoTen", nv.getHoTen());
        p.put("email", nv.getEmailCongViec());
        p.put("chucVu", nv.getChucVu() != null ? nv.getChucVu().getTenChucVu() : "---");
        p.put("phongBan", nv.getPhongBan() != null ? nv.getPhongBan().getTenPhongBan() : "---");
        p.put("phone", nv.getSoDienThoai());
        p.put("address", nv.getDiaChiTamTru());
        p.put("city", "");
        p.put("notes", "");
        p.put("ngayVaoLam", nv.getNgayVaoLam() != null ? nv.getNgayVaoLam().toString() : "");
        p.put("avatarUrl", nv.getAnhDaiDienUrl());
        return p;
    }

    private Map<String, Object> toLeaveMap(DonNghiPhep d) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", d.getId());
        m.put("loaiNghi", d.getLoaiNghi());
        m.put("tuNgay", d.getTuNgay() != null ? d.getTuNgay().toString() : null);
        m.put("denNgay", d.getDenNgay() != null ? d.getDenNgay().toString() : null);
        m.put("lyDo", d.getLyDo());
        m.put("trangThai", d.getTrangThai());
        return m;
    }

    private ResponseEntity<Map<String, String>> badRequest(String message) {
        return ResponseEntity.badRequest().body(Map.of("message", message));
    }

    private ResponseEntity<Map<String, String>> unauthorized(String message) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("message", message));
    }

    private String trimOrNull(String s) {
        if (s == null) return null;
        String t = s.trim();
        return t.isEmpty() ? null : t;
    }

    private String extensionFromContentType(String contentType) {
        return switch (contentType) {
            case "image/jpeg" -> "jpg";
            case "image/png" -> "png";
            case "image/gif" -> "gif";
            case "image/webp" -> "webp";
            default -> "png";
        };
    }
}
