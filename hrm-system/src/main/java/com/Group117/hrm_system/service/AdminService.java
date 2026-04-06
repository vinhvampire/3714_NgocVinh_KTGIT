package com.Group117.hrm_system.service;

import com.Group117.hrm_system.Repository.NhanVienRepository;
import com.Group117.hrm_system.Repository.TaiKhoanRepository;
import com.Group117.hrm_system.dto.AdminAccountResponse;
import com.Group117.hrm_system.entity.NhanVien;
import com.Group117.hrm_system.entity.TaiKhoan;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.HashMap;
import java.util.stream.Collectors;

@Service
public class AdminService {

    private static final Set<String> ROLES = Set.of("DIRECTOR", "ADMIN", "HR", "EMPLOYEE");

    @Autowired
    private TaiKhoanRepository taiKhoanRepository;

    @Autowired
    private NhanVienRepository nhanVienRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private NotificationService notificationService;

    public List<AdminAccountResponse> listAccounts(String q, String roleFilter, Boolean activeOnly) {
        String qn = q == null || q.isBlank() ? null : q.trim().toLowerCase();
        String rfn = roleFilter == null || roleFilter.isBlank() ? null : roleFilter.trim().toUpperCase();

        return taiKhoanRepository.findAll().stream()
                .filter(t -> qn == null || t.getUsername().toLowerCase().contains(qn))
                .filter(t -> rfn == null || (t.getRole() != null && t.getRole().equalsIgnoreCase(rfn)))
                .filter(t -> activeOnly == null || t.isTrangThaiTaiKhoan() == activeOnly)
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    public List<AdminAccountResponse> listTaiKhoanChuaGanNhanVien() {
        return taiKhoanRepository.findAll().stream()
                .filter(t -> t.getNhanVien() == null)
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    public List<Map<String, String>> listNhanVienChuaCoTaiKhoanBrief() {
        return nhanVienRepository.findNhanVienChuaCoTaiKhoan().stream()
                .map(n -> {
                    Map<String, String> m = new HashMap<>();
                    m.put("id", n.getId());
                    m.put("hoTen", n.getHoTen() != null ? n.getHoTen() : "");
                    m.put("emailCongViec", n.getEmailCongViec() != null ? n.getEmailCongViec() : "");
                    return m;
                })
                .collect(Collectors.toList());
    }

    @Transactional
    public AdminAccountResponse createAccount(String username, String rawPassword, String role,
                                             String nhanVienId, String actorUsername) {
        requireRoleValue(role);
        boolean isAdminRole = "ADMIN".equalsIgnoreCase(role);
        if (username == null || username.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "username bắt buộc");
        }
        if (rawPassword == null || rawPassword.length() < 6) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Mật khẩu tối thiểu 6 ký tự");
        }
        if (taiKhoanRepository.findByUsername(username.trim()) != null) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Username đã tồn tại");
        }

        TaiKhoan tk = new TaiKhoan();
        tk.setMaTaiKhoan("TK_" + UUID.randomUUID().toString().replace("-", "").substring(0, 20));
        tk.setUsername(username.trim());
        tk.setPassword(passwordEncoder.encode(rawPassword));
        tk.setRole(role.toUpperCase());
        tk.setTrangThaiTaiKhoan(true);
        tk.setNgayTao(LocalDateTime.now());

        // Role != ADMIN bắt buộc phải gán nhân viên (mỗi nhân viên chỉ có 1 tài khoản)
        if (!isAdminRole && (nhanVienId == null || nhanVienId.isBlank())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Gán nhân viên là bắt buộc với role không phải ADMIN");
        }

        if (nhanVienId != null && !nhanVienId.isBlank()) {
            NhanVien nv = nhanVienRepository.findById(nhanVienId.trim())
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Không có nhân viên"));
            if (nv.getTaiKhoan() != null) {
                throw new ResponseStatusException(HttpStatus.CONFLICT, "Nhân viên đã có tài khoản");
            }
            taiKhoanRepository.findByNhanVien_Id(nv.getId()).ifPresent(existing -> {
                throw new ResponseStatusException(HttpStatus.CONFLICT, "Nhân viên đã được gán tài khoản khác");
            });
            tk.setNhanVien(nv);
        }

        taiKhoanRepository.save(tk);
        notificationService.notifyPrivate(tk.getUsername(), "ACCOUNT_CREATED", "Tài khoản đã được tạo",
                "Tài khoản " + tk.getUsername() + " (" + tk.getRole() + ") đã sẵn sàng đăng nhập.", null);
        notificationService.notifyAllHr("ACCOUNT_CREATED", "Tài khoản mới trong hệ thống",
                "Admin vừa tạo tài khoản: " + tk.getUsername() + " (" + tk.getRole() + ").", null);
        return toResponse(tk);
    }

    @Transactional
    public AdminAccountResponse toggleStatus(String maTaiKhoan, String actorUsername) {
        TaiKhoan tk = getTaiKhoanOr404(maTaiKhoan);
        forbidSelf(tk.getUsername(), actorUsername, "Không thể khóa/mở chính tài khoản đang đăng nhập");
        tk.setTrangThaiTaiKhoan(!tk.isTrangThaiTaiKhoan());
        taiKhoanRepository.save(tk);
        return toResponse(tk);
    }

    @Transactional
    public AdminAccountResponse updateRole(String maTaiKhoan, String newRole, String actorUsername) {
        requireRoleValue(newRole);
        TaiKhoan tk = getTaiKhoanOr404(maTaiKhoan);
        String nr = newRole.toUpperCase();
        String old = tk.getRole() != null ? tk.getRole().toUpperCase() : "";

        if ("ADMIN".equals(old) && !"ADMIN".equals(nr)) {
            long admins = taiKhoanRepository.countByRoleIgnoreCase("ADMIN");
            if (admins <= 1) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Không thể đổi role — còn duy nhất một tài khoản ADMIN");
            }
        }
        forbidSelf(tk.getUsername(), actorUsername, "Không thể đổi role chính mình từ giao diện này");

        tk.setRole(nr);
        taiKhoanRepository.save(tk);
        return toResponse(tk);
    }

    @Transactional
    public AdminAccountResponse updateRoleAndDirectManager(String maTaiKhoan, String newRole, String directManagerId, String actorUsername) {
        AdminAccountResponse updated = updateRole(maTaiKhoan, newRole, actorUsername);
        if (directManagerId == null) return updated;

        // Nếu tài khoản chưa gán nhân viên thì không thể gán manager
        if (updated.getNhanVienId() == null || updated.getNhanVienId().isBlank()) {
            return updated;
        }

        String subId = updated.getNhanVienId();
        if (directManagerId.isBlank() || "null".equalsIgnoreCase(directManagerId)) {
            return updated;
        }

        if (directManagerId.trim().equalsIgnoreCase(subId.trim())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Không thể chọn chính mình làm người quản lý trực tiếp");
        }

        NhanVien sub = nhanVienRepository.findById(subId.trim())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Không có nhân viên"));

        NhanVien manager = nhanVienRepository.findById(directManagerId.trim())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Không có người quản lý trực tiếp"));

        // Manager được yêu cầu có tài khoản vai trò EMPLOYEE để có thể đăng nhập duyệt theo màn hình employee
        if (manager.getTaiKhoan() == null || manager.getTaiKhoan().getRole() == null
                || !"EMPLOYEE".equalsIgnoreCase(manager.getTaiKhoan().getRole())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Người quản lý trực tiếp phải là tài khoản vai trò EMPLOYEE");
        }

        sub.setNguoiQuanLyTruocTiep(manager);
        nhanVienRepository.save(sub);
        return toResponse(sub.getTaiKhoan());
    }

    @Transactional
    public AdminAccountResponse assignNhanVien(String maTaiKhoan, String nhanVienId, String actorUsername) {
        TaiKhoan tk = getTaiKhoanOr404(maTaiKhoan);

        // Với role không phải ADMIN thì không cho phép bỏ gán nhân viên
        if (nhanVienId == null || nhanVienId.isBlank()) {
            if (!"ADMIN".equalsIgnoreCase(tk.getRole())) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Bắt buộc gán nhân viên cho role không phải ADMIN");
            }
            tk.setNhanVien(null);
            taiKhoanRepository.save(tk);
            return toResponse(tk);
        }

        NhanVien nv = nhanVienRepository.findById(nhanVienId.trim())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Không có nhân viên"));

        taiKhoanRepository.findByNhanVien_Id(nv.getId()).ifPresent(other -> {
            if (!other.getMaTaiKhoan().equals(tk.getMaTaiKhoan())) {
                throw new ResponseStatusException(HttpStatus.CONFLICT, "Nhân viên đã gán cho tài khoản khác");
            }
        });

        if (nv.getTaiKhoan() != null && !nv.getTaiKhoan().getMaTaiKhoan().equals(tk.getMaTaiKhoan())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Nhân viên đã có tài khoản");
        }

        tk.setNhanVien(nv);
        taiKhoanRepository.save(tk);
        return toResponse(tk);
    }

    @Transactional
    public void deleteAccount(String maTaiKhoan, String actorUsername) {
        TaiKhoan tk = getTaiKhoanOr404(maTaiKhoan);
        forbidSelf(tk.getUsername(), actorUsername, "Không thể xóa chính tài khoản đang đăng nhập");

        if ("ADMIN".equalsIgnoreCase(tk.getRole())) {
            long admins = taiKhoanRepository.countByRoleIgnoreCase("ADMIN");
            if (admins <= 1) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Không thể xóa — còn duy nhất một tài khoản ADMIN");
            }
        }

        tk.setNhanVien(null);
        taiKhoanRepository.save(tk);
        taiKhoanRepository.deleteById(tk.getMaTaiKhoan());
    }

    @Transactional
    public String resetPassword(String maTaiKhoan, String optionalNewPassword) {
        TaiKhoan tk = getTaiKhoanOr404(maTaiKhoan);
        String raw = (optionalNewPassword != null && !optionalNewPassword.isBlank())
                ? optionalNewPassword
                : randomPassword(10);
        if (raw.length() < 6) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Mật khẩu tối thiểu 6 ký tự");
        }
        tk.setPassword(passwordEncoder.encode(raw));
        tk.setResetToken(null);
        tk.setResetTokenExpiry(null);
        taiKhoanRepository.save(tk);
        return raw;
    }

    public long countNewAccountsThisMonth() {
        YearMonth ym = YearMonth.now();
        LocalDateTime start = ym.atDay(1).atStartOfDay();
        LocalDateTime end = ym.atEndOfMonth().plusDays(1).atStartOfDay();

        return taiKhoanRepository.findAll().stream()
                .filter(t -> t.getNgayTao() != null)
                .filter(t -> !t.getNgayTao().isBefore(start) && t.getNgayTao().isBefore(end))
                .count();
    }

    private void forbidSelf(String targetUsername, String actorUsername, String msg) {
        if (actorUsername != null && actorUsername.equalsIgnoreCase(targetUsername)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, msg);
        }
    }

    private TaiKhoan getTaiKhoanOr404(String maTaiKhoan) {
        return taiKhoanRepository.findById(maTaiKhoan)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy tài khoản"));
    }

    private void requireRoleValue(String role) {
        if (role == null || !ROLES.contains(role.trim().toUpperCase())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Role không hợp lệ");
        }
    }

    private AdminAccountResponse toResponse(TaiKhoan t) {
        NhanVien nv = t.getNhanVien();
        return AdminAccountResponse.builder()
                .maTaiKhoan(t.getMaTaiKhoan())
                .username(t.getUsername())
                .role(t.getRole())
                .trangThaiTaiKhoan(t.isTrangThaiTaiKhoan())
                .nhanVienId(nv != null ? nv.getId() : null)
                .hoTenNhanVien(nv != null ? nv.getHoTen() : null)
                .directManagerId(nv != null && nv.getNguoiQuanLyTruocTiep() != null ? nv.getNguoiQuanLyTruocTiep().getId() : null)
                .emailCongViec(nv != null ? nv.getEmailCongViec() : null)
                .ngayTao(t.getNgayTao())
                .build();
    }

    public List<Map<String, String>> listDirectManagerCandidates() {
        // Người quản lý trực tiếp: chỉ lấy nhân viên có tài khoản vai trò EMPLOYEE
        return nhanVienRepository.findNhanVienCoTaiKhoanRoleEmployee().stream()
                .map(n -> {
                    Map<String, String> m = new HashMap<>();
                    m.put("id", n.getId());
                    m.put("hoTen", n.getHoTen() != null ? n.getHoTen() : "");
                    return m;
                })
                .collect(Collectors.toList());
    }

    public List<Map<String, String>> listEmployeesWithAccountsBrief() {
        return nhanVienRepository.findNhanVienCoTaiKhoan().stream()
                .map(n -> {
                    Map<String, String> m = new HashMap<>();
                    m.put("id", n.getId());
                    m.put("hoTen", n.getHoTen() != null ? n.getHoTen() : "");
                    return m;
                })
                .collect(Collectors.toList());
    }

    public List<Map<String, String>> listSubordinatesBrief(String managerNhanVienId) {
        if (managerNhanVienId == null || managerNhanVienId.isBlank()) return List.of();
        return nhanVienRepository.findNhanVienByNguoiQuanLyTruocTiepId(managerNhanVienId.trim()).stream()
                .map(n -> {
                    Map<String, String> m = new HashMap<>();
                    m.put("id", n.getId());
                    m.put("hoTen", n.getHoTen() != null ? n.getHoTen() : "");
                    return m;
                })
                .collect(Collectors.toList());
    }

    public List<Map<String, String>> listUngVienSubordinateBrief(String managerNhanVienId) {
        if (managerNhanVienId == null || managerNhanVienId.isBlank()) return List.of();
        return nhanVienRepository.findUngVienGiaoQuanLyTrucTiep(managerNhanVienId.trim()).stream()
                .map(n -> {
                    Map<String, String> m = new HashMap<>();
                    m.put("id", n.getId());
                    m.put("hoTen", n.getHoTen() != null ? n.getHoTen() : "");
                    return m;
                })
                .collect(Collectors.toList());
    }

    @Transactional
    public void updateManagedSubordinates(String managerAccountId, List<String> subordinateIds, String actorUsername) {
        TaiKhoan managerTk = getTaiKhoanOr404(managerAccountId);
        NhanVien managerNv = managerTk.getNhanVien();
        if (managerNv == null || managerNv.getId() == null || managerNv.getId().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Tài khoản này chưa được gán nhân viên");
        }

        java.util.Set<String> wanted = new java.util.HashSet<>();
        if (subordinateIds != null) {
            for (String id : subordinateIds) {
                if (id == null || id.isBlank()) continue;
                wanted.add(id.trim());
            }
        }

        if (wanted.contains(managerNv.getId())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Không thể chọn chính mình làm nhân viên quản lý");
        }

        // Gán manager cho các nhân viên được chọn
        for (String subId : wanted) {
            NhanVien sub = nhanVienRepository.findById(subId)
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Không có nhân viên: " + subId));
            if (sub.getTaiKhoan() == null) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Nhân viên " + subId + " chưa có tài khoản");
            }
            sub.setNguoiQuanLyTruocTiep(managerNv);
            nhanVienRepository.save(sub);
        }

        // Nếu muốn đồng bộ đúng theo UI: nhân viên đang được manager này quản lý nhưng không còn được chọn thì bỏ gán
        var currentSubs = nhanVienRepository.findNhanVienByNguoiQuanLyTruocTiepId(managerNv.getId());
        for (NhanVien cur : currentSubs) {
            if (cur == null || cur.getId() == null) continue;
            if (!wanted.contains(cur.getId())) {
                cur.setNguoiQuanLyTruocTiep(null);
                nhanVienRepository.save(cur);
            }
        }
    }

    private static String randomPassword(int len) {
        byte[] buf = new byte[len];
        new SecureRandom().nextBytes(buf);
        String s = Base64.getUrlEncoder().withoutPadding().encodeToString(buf);
        return s.substring(0, Math.min(len, s.length()));
    }
}
