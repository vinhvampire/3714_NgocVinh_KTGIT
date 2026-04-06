package com.Group117.hrm_system.Controller;

import java.time.LocalDate;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.ArrayList;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import com.Group117.hrm_system.Repository.TaiKhoanRepository;
import com.Group117.hrm_system.config.JwtService;
import com.Group117.hrm_system.entity.DonNghiPhep;
import com.Group117.hrm_system.entity.NhanVien;
import com.Group117.hrm_system.entity.PhieuLuong;
import com.Group117.hrm_system.entity.TaiKhoan;
import com.Group117.hrm_system.service.DashboardService;
import com.Group117.hrm_system.service.DonNghiPhepService;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;

@Controller
public class DashboardController {

    @Autowired
    private JwtService jwtService;

    @Autowired
    private TaiKhoanRepository taiKhoanRepo;

    @Autowired
    private DashboardService dashboardService;

    @Autowired
    private DonNghiPhepService donNghiPhepService;

    // ─────────────────────────────────────────────────────────
    // Helper: Lấy NhanVien từ JWT trong request
    // ─────────────────────────────────────────────────────────
    private NhanVien getNhanVienFromRequest(HttpServletRequest request) {
        String token = getTokenFromCookie(request);
        if (token == null) return null;
        try {
            String username = jwtService.extractUsername(token);
            TaiKhoan tk = taiKhoanRepo.findByUsername(username);
            return (tk != null) ? tk.getNhanVien() : null;
        } catch (Exception e) {
            return null;
        }
    }

    // ─────────────────────────────────────────────────────────
    // Helper: đọc JWT từ cookie "jwt_token"
    // ─────────────────────────────────────────────────────────
    private String getTokenFromCookie(HttpServletRequest request) {
        // 1) Ưu tiên Authorization header (để các API call từ JS hoạt động độc lập theo từng tab)
        String authHeader = request.getHeader("Authorization");
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            String token = authHeader.substring(7).trim();
            if (token != null
                    && !token.isEmpty()
                    && !token.equalsIgnoreCase("null")
                    && !token.equalsIgnoreCase("undefined")) {
                return token;
            }
        }

        // 2) Fallback: access_token trên query param (để SSR trang dashboard đọc đúng token khi F5)
        String accessToken = request.getParameter("access_token");
        if (accessToken != null
                && !accessToken.isEmpty()
                && !accessToken.equalsIgnoreCase("null")
                && !accessToken.equalsIgnoreCase("undefined")) {
            return accessToken;
        }

        // 3) Cuối cùng mới fallback cookie (giữ tương thích nếu còn phần code cũ)
        if (request.getCookies() == null) return null;
        for (Cookie c : request.getCookies()) {
            if ("jwt_token".equals(c.getName())) {
                String value = c.getValue();
                if (value != null
                        && !value.isEmpty()
                        && !value.equalsIgnoreCase("null")
                        && !value.equalsIgnoreCase("undefined")) {
                    return value;
                }
            }
        }
        return null;
    }

    // ─────────────────────────────────────────────────────────
    // Helper: add common model attributes
    // ─────────────────────────────────────────────────────────
    private void addCommonAttributes(Model model, HttpServletRequest request,
                                     String username, String role, String breadcrumb) {
        LocalDate now = LocalDate.now();
        model.addAttribute("username",        username);
        model.addAttribute("role",            role);
        model.addAttribute("currentUri",      request.getRequestURI());
        model.addAttribute("pageBreadcrumb",  breadcrumb);
        model.addAttribute("currentMonth",    now.getMonthValue());
        model.addAttribute("currentYear",     now.getYear());

        // Thêm nhanVienId để dùng cho các API call (Checkin/Checkout)
        TaiKhoan tk = taiKhoanRepo.findByUsername(username);
        if (tk != null && tk.getNhanVien() != null) {
            model.addAttribute("nhanVienId", tk.getNhanVien().getId());
        }
    }

    // ─────────────────────────────────────────────────────────
    // GET /dashboard → Router tự động redirect theo role
    // ─────────────────────────────────────────────────────────
    @GetMapping("/dashboard")
    public String dashboardRouter(HttpServletRequest request) {
        String token = getTokenFromCookie(request);
        if (token == null) return "redirect:/login?error=unauthorized";

        try {
            String role = jwtService.extractClaim(token,
                    claims -> claims.get("role", String.class));
            System.out.println(">>> [Router] Role: " + role);
            return switch (role.toUpperCase()) {
                case "DIRECTOR" -> "redirect:/dashboard/director";
                case "ADMIN"    -> "redirect:/dashboard/admin";
                case "HR"       -> "redirect:/dashboard/hr";
                case "EMPLOYEE" -> "redirect:/dashboard/employee";
                default -> "redirect:/login?error=forbidden";
            };
        } catch (Exception e) {
            System.err.println(">>> [Router] Lỗi token: " + e.getMessage());
            return "redirect:/login?error=invalid_token";
        }
    }

    // ─────────────────────────────────────────────────────────
    // GET /dashboard/director
    // ─────────────────────────────────────────────────────────
    @GetMapping("/dashboard/director")
    public String directorPage(HttpServletRequest request, Model model) {
        return renderDirectorPage(request, model, "Dashboard");
    }

    @GetMapping("/dashboard/director/reports")
    public String directorReportsPage(HttpServletRequest request, Model model) {
        return renderDirectorPage(request, model, "Báo Cáo");
    }

    @GetMapping("/dashboard/director/kpi")
    public String directorKpiPage(HttpServletRequest request, Model model) {
        return renderDirectorPage(request, model, "KPI Kinh Doanh");
    }

    private String renderDirectorPage(HttpServletRequest request, Model model, String breadcrumb) {
        String token = getTokenFromCookie(request);
        if (token == null) return "redirect:/login?error=unauthorized";

        try {
            String role = jwtService.extractClaim(token,
                    claims -> claims.get("role", String.class));
            if (!"DIRECTOR".equalsIgnoreCase(role)) return "redirect:/login?error=forbidden";

            String username = jwtService.extractUsername(token);
            addCommonAttributes(model, request, username, role, breadcrumb);
            model.addAttribute("pageTitle", "Director Dashboard");

            System.out.println(">>> [Director] Render cho: " + username);
            return "dashboard/director";
        } catch (Exception e) {
            System.err.println(">>> [Director] Lỗi token: " + e.getMessage());
            return "redirect:/login?error=invalid_token";
        }
    }

    // ─────────────────────────────────────────────────────────
    // GET /dashboard/admin
    // ─────────────────────────────────────────────────────────
    @GetMapping("/dashboard/admin")
    public String adminOverviewPage(HttpServletRequest request, Model model) {
        String token = getTokenFromCookie(request);
        if (token == null) return "redirect:/login?error=unauthorized";

        try {
            String role = jwtService.extractClaim(token,
                    claims -> claims.get("role", String.class));
            if (!"ADMIN".equalsIgnoreCase(role)) return "redirect:/login?error=forbidden";

            String username = jwtService.extractUsername(token);
            addCommonAttributes(model, request, username, role, "Admin — Tổng quan");
            model.addAttribute("pageTitle", "Admin — Tổng quan");

            System.out.println(">>> [Admin] Render overview: " + username);
            return "dashboard/admin/overview";
        } catch (Exception e) {
            System.err.println(">>> [Admin] Lỗi token: " + e.getMessage());
            return "redirect:/login?error=invalid_token";
        }
    }

    @GetMapping("/dashboard/admin/accounts")
    public String adminAccountsPage(HttpServletRequest request, Model model) {
        String token = getTokenFromCookie(request);
        if (token == null) return "redirect:/login?error=unauthorized";
        try {
            String role = jwtService.extractClaim(token, claims -> claims.get("role", String.class));
            if (!"ADMIN".equalsIgnoreCase(role)) return "redirect:/login?error=forbidden";
            String username = jwtService.extractUsername(token);
            addCommonAttributes(model, request, username, role, "Admin — Tài khoản");
            model.addAttribute("pageTitle", "Admin — Quản lý tài khoản");
            return "dashboard/admin/accounts";
        } catch (Exception e) {
            return "redirect:/login?error=invalid_token";
        }
    }

    @GetMapping("/dashboard/admin/permissions")
    public String adminPermissionsPage(HttpServletRequest request, Model model) {
        String token = getTokenFromCookie(request);
        if (token == null) return "redirect:/login?error=unauthorized";
        try {
            String role = jwtService.extractClaim(token, claims -> claims.get("role", String.class));
            if (!"ADMIN".equalsIgnoreCase(role)) return "redirect:/login?error=forbidden";
            String username = jwtService.extractUsername(token);
            addCommonAttributes(model, request, username, role, "Admin — Phân quyền");
            model.addAttribute("pageTitle", "Admin — Phân quyền");
            return "dashboard/admin/permissions";
        } catch (Exception e) {
            return "redirect:/login?error=invalid_token";
        }
    }

    @GetMapping("/dashboard/admin/settings")
    public String adminSettingsPage(HttpServletRequest request, Model model) {
        String token = getTokenFromCookie(request);
        if (token == null) return "redirect:/login?error=unauthorized";
        try {
            String role = jwtService.extractClaim(token, claims -> claims.get("role", String.class));
            if (!"ADMIN".equalsIgnoreCase(role)) return "redirect:/login?error=forbidden";
            String username = jwtService.extractUsername(token);
            addCommonAttributes(model, request, username, role, "Admin — Cấu hình");
            model.addAttribute("pageTitle", "Admin — Cấu hình hệ thống");
            return "dashboard/admin/settings";
        } catch (Exception e) {
            return "redirect:/login?error=invalid_token";
        }
    }

    // ─────────────────────────────────────────────────────────
    // GET /dashboard/hr (Trang tổng quan)
    // ─────────────────────────────────────────────────────────
    @GetMapping("/dashboard/hr")
    public String hrIndexPage(HttpServletRequest request, Model model) {
        String token = getTokenFromCookie(request);
        if (token == null) return "redirect:/login?error=unauthorized";

        try {
            String role = jwtService.extractClaim(token, claims -> claims.get("role", String.class));
            if (!"HR".equalsIgnoreCase(role)) return "redirect:/login?error=forbidden";

            String username = jwtService.extractUsername(token);
            addCommonAttributes(model, request, username, role, "Dashboard");
            model.addAttribute("pageTitle", "HR Dashboard");

            System.out.println(">>> [HR] Render index cho: " + username);
            return "dashboard/hr/overview";
        } catch (Exception e) {
            System.err.println(">>> [HR] Lỗi token: " + e.getMessage());
            return "redirect:/login?error=invalid_token";
        }
    }

    // ─────────────────────────────────────────────────────────
    // GET /dashboard/hr/employees
    // ─────────────────────────────────────────────────────────
    @GetMapping("/dashboard/hr/employees")
    public String hrEmployeesPage(HttpServletRequest request, Model model) {
        String token = getTokenFromCookie(request);
        if (token == null) return "redirect:/login?error=unauthorized";

        try {
            String role = jwtService.extractClaim(token, claims -> claims.get("role", String.class));
            if (!"HR".equalsIgnoreCase(role)) return "redirect:/login?error=forbidden";

            String username = jwtService.extractUsername(token);
            addCommonAttributes(model, request, username, role, "Quản lý nhân viên");
            model.addAttribute("pageTitle", "HR - Nhân viên");

            return "dashboard/hr/employees";
        } catch (Exception e) {
            return "redirect:/login?error=invalid_token";
        }
    }

    // ─────────────────────────────────────────────────────────
    // GET /dashboard/hr/attendance
    // ─────────────────────────────────────────────────────────
    @GetMapping("/dashboard/hr/attendance")
    public String hrAttendancePage(HttpServletRequest request, Model model) {
        String token = getTokenFromCookie(request);
        if (token == null) return "redirect:/login?error=unauthorized";

        try {
            String role = jwtService.extractClaim(token, claims -> claims.get("role", String.class));
            if (!"HR".equalsIgnoreCase(role)) return "redirect:/login?error=forbidden";

            String username = jwtService.extractUsername(token);
            addCommonAttributes(model, request, username, role, "Chấm công");
            model.addAttribute("pageTitle", "HR - Chấm công");

            return "dashboard/hr/attendance";
        } catch (Exception e) {
            return "redirect:/login?error=invalid_token";
        }
    }

    // ─────────────────────────────────────────────────────────
    // GET /dashboard/hr/leaves
    // ─────────────────────────────────────────────────────────
    @GetMapping("/dashboard/hr/leaves")
    public String hrLeavesPage(HttpServletRequest request, Model model) {
        String token = getTokenFromCookie(request);
        if (token == null) return "redirect:/login?error=unauthorized";

        try {
            String role = jwtService.extractClaim(token, claims -> claims.get("role", String.class));
            if (!"HR".equalsIgnoreCase(role)) return "redirect:/login?error=forbidden";

            String username = jwtService.extractUsername(token);
            addCommonAttributes(model, request, username, role, "Đơn phép");
            model.addAttribute("pageTitle", "HR - Đơn phép");

            return "dashboard/hr/leaves";
        } catch (Exception e) {
            return "redirect:/login?error=invalid_token";
        }
    }

    // ─────────────────────────────────────────────────────────
    // GET /dashboard/hr/payroll
    // ─────────────────────────────────────────────────────────
    @GetMapping("/dashboard/hr/payroll")
    public String hrPayrollPage(HttpServletRequest request, Model model) {
        String token = getTokenFromCookie(request);
        if (token == null) return "redirect:/login?error=unauthorized";

        try {
            String role = jwtService.extractClaim(token, claims -> claims.get("role", String.class));
            if (!"HR".equalsIgnoreCase(role)) return "redirect:/login?error=forbidden";

            String username = jwtService.extractUsername(token);
            addCommonAttributes(model, request, username, role, "Lương");
            model.addAttribute("pageTitle", "HR - Lương");

            return "dashboard/hr/payroll";
        } catch (Exception e) {
            return "redirect:/login?error=invalid_token";
        }
    }

    // ─────────────────────────────────────────────────────────
    // GET /dashboard/hr/organization
    // ─────────────────────────────────────────────────────────
    @GetMapping("/dashboard/hr/organization")
    public String hrOrganizationPage(HttpServletRequest request, Model model) {
        String token = getTokenFromCookie(request);
        if (token == null) return "redirect:/login?error=unauthorized";

        try {
            String role = jwtService.extractClaim(token, claims -> claims.get("role", String.class));
            if (!"HR".equalsIgnoreCase(role)) return "redirect:/login?error=forbidden";

            String username = jwtService.extractUsername(token);
            addCommonAttributes(model, request, username, role, "Tổ chức");
            model.addAttribute("pageTitle", "HR - Tổ chức");

            return "dashboard/hr/organization";
        } catch (Exception e) {
            return "redirect:/login?error=invalid_token";
        }
    }

    // ─────────────────────────────────────────────────────────
    // GET /dashboard/hr/org/* (Chi nhánh, Phòng ban, Nhóm, Chức vụ)
    // ─────────────────────────────────────────────────────────
    @GetMapping("/dashboard/hr/org/chi-nhanh")
    public String hrOrgChiNhanh(HttpServletRequest request, Model model) {
        String token = getTokenFromCookie(request);
        if (token == null) return "redirect:/login?error=unauthorized";
        try {
            String role = jwtService.extractClaim(token, claims -> claims.get("role", String.class));
            if (!"HR".equalsIgnoreCase(role)) return "redirect:/login?error=forbidden";
            String username = jwtService.extractUsername(token);
            addCommonAttributes(model, request, username, role, "Chi nhánh");
            model.addAttribute("pageTitle", "HR - Chi nhánh");
            return "dashboard/hr/org/chi-nhanh";
        } catch (Exception e) {
            return "redirect:/login?error=invalid_token";
        }
    }

    @GetMapping("/dashboard/hr/org/phong-ban")
    public String hrOrgPhongBan(HttpServletRequest request, Model model) {
        String token = getTokenFromCookie(request);
        if (token == null) return "redirect:/login?error=unauthorized";
        try {
            String role = jwtService.extractClaim(token, claims -> claims.get("role", String.class));
            if (!"HR".equalsIgnoreCase(role)) return "redirect:/login?error=forbidden";
            String username = jwtService.extractUsername(token);
            addCommonAttributes(model, request, username, role, "Phòng ban");
            model.addAttribute("pageTitle", "HR - Phòng ban");
            return "dashboard/hr/org/phong-ban";
        } catch (Exception e) {
            return "redirect:/login?error=invalid_token";
        }
    }

    @GetMapping("/dashboard/hr/org/nhom")
    public String hrOrgNhom(HttpServletRequest request, Model model) {
        String token = getTokenFromCookie(request);
        if (token == null) return "redirect:/login?error=unauthorized";
        try {
            String role = jwtService.extractClaim(token, claims -> claims.get("role", String.class));
            if (!"HR".equalsIgnoreCase(role)) return "redirect:/login?error=forbidden";
            String username = jwtService.extractUsername(token);
            addCommonAttributes(model, request, username, role, "Nhóm");
            model.addAttribute("pageTitle", "HR - Nhóm");
            return "dashboard/hr/org/nhom";
        } catch (Exception e) {
            return "redirect:/login?error=invalid_token";
        }
    }

    @GetMapping("/dashboard/hr/org/chuc-vu")
    public String hrOrgChucVu(HttpServletRequest request, Model model) {
        String token = getTokenFromCookie(request);
        if (token == null) return "redirect:/login?error=unauthorized";
        try {
            String role = jwtService.extractClaim(token, claims -> claims.get("role", String.class));
            if (!"HR".equalsIgnoreCase(role)) return "redirect:/login?error=forbidden";
            String username = jwtService.extractUsername(token);
            addCommonAttributes(model, request, username, role, "Chức vụ");
            model.addAttribute("pageTitle", "HR - Chức vụ");
            return "dashboard/hr/org/chuc-vu";
        } catch (Exception e) {
            return "redirect:/login?error=invalid_token";
        }
    }

    // ─────────────────────────────────────────────────────────
    // GET /dashboard/hr/employees/profile
    // ─────────────────────────────────────────────────────────
    @GetMapping("/dashboard/hr/employees/profile")
    public String hrEmployeeProfilePage(HttpServletRequest request, Model model) {
        String token = getTokenFromCookie(request);
        if (token == null) return "redirect:/login?error=unauthorized";
        try {
            String role = jwtService.extractClaim(token, claims -> claims.get("role", String.class));
            if (!"HR".equalsIgnoreCase(role)) return "redirect:/login?error=forbidden";
            String username = jwtService.extractUsername(token);
            addCommonAttributes(model, request, username, role, "Hồ sơ nhân viên");
            model.addAttribute("pageTitle", "HR - Hồ sơ nhân viên");
            return "dashboard/hr/employees-profile";
        } catch (Exception e) {
            return "redirect:/login?error=invalid_token";
        }
    }

    // ─────────────────────────────────────────────────────────
    // GET /dashboard/hr/decisions
    // ─────────────────────────────────────────────────────────
    @GetMapping("/dashboard/hr/decisions")
    public String hrDecisionsPage(HttpServletRequest request, Model model) {
        String token = getTokenFromCookie(request);
        if (token == null) return "redirect:/login?error=unauthorized";
        try {
            String role = jwtService.extractClaim(token, claims -> claims.get("role", String.class));
            if (!"HR".equalsIgnoreCase(role)) return "redirect:/login?error=forbidden";
            String username = jwtService.extractUsername(token);
            addCommonAttributes(model, request, username, role, "Quyết định");
            model.addAttribute("pageTitle", "HR - Ban hành quyết định");
            return "dashboard/hr/decisions";
        } catch (Exception e) {
            return "redirect:/login?error=invalid_token";
        }
    }

    // ─────────────────────────────────────────────────────────
    // GET /dashboard/hr/recruitment
    // ─────────────────────────────────────────────────────────
    @GetMapping("/dashboard/hr/recruitment")
    public String hrRecruitmentPage(HttpServletRequest request, Model model) {
        String token = getTokenFromCookie(request);
        if (token == null) return "redirect:/login?error=unauthorized";

        try {
            String role = jwtService.extractClaim(token, claims -> claims.get("role", String.class));
            if (!"HR".equalsIgnoreCase(role)) return "redirect:/login?error=forbidden";

            String username = jwtService.extractUsername(token);
            addCommonAttributes(model, request, username, role, "Tuyển dụng");
            model.addAttribute("pageTitle", "HR - Tuyển dụng");

            return "dashboard/hr/recruitment";
        } catch (Exception e) {
            return "redirect:/login?error=invalid_token";
        }
    }

    // ─────────────────────────────────────────────────────────
    // GET /dashboard/employee
    // ─────────────────────────────────────────────────────────
    @GetMapping("/dashboard/employee")
    public String employeePage(HttpServletRequest request, Model model) {
        String token = getTokenFromCookie(request);
        if (token == null) return "redirect:/login?error=unauthorized";

        try {
            String role = jwtService.extractClaim(token,
                    claims -> claims.get("role", String.class));
            if (!"EMPLOYEE".equalsIgnoreCase(role)) return "redirect:/login?error=forbidden";

            String username = jwtService.extractUsername(token);
            addCommonAttributes(model, request, username, role, "Dashboard");
            model.addAttribute("pageTitle", "Employee Dashboard");

            System.out.println(">>> [Employee] Render cho: " + username);
            return "dashboard/employee";
        } catch (Exception e) {
            System.err.println(">>> [Employee] Lỗi token: " + e.getMessage());
            return "redirect:/login?error=invalid_token";
        }
    }

    // ─────────────────────────────────────────────────────────
    // GET /dashboard/employee/profile
    // ─────────────────────────────────────────────────────────
    @GetMapping("/dashboard/employee/profile")
    public String employeeProfile(HttpServletRequest request, Model model) {
        String token = getTokenFromCookie(request);
        if (token == null) return "redirect:/login?error=unauthorized";

        try {
            String role = jwtService.extractClaim(token,
                    claims -> claims.get("role", String.class));
            if (!"EMPLOYEE".equalsIgnoreCase(role)) return "redirect:/login?error=forbidden";

            String username = jwtService.extractUsername(token);
            addCommonAttributes(model, request, username, role, "Hồ Sơ Cá Nhân");
            model.addAttribute("pageTitle", "Employee Profile");

            System.out.println(">>> [Employee] Profile cho: " + username);
            return "dashboard/employee/profile";
        } catch (Exception e) {
            return "redirect:/login?error=invalid_token";
        }
    }

    // ─────────────────────────────────────────────────────────
    // GET /dashboard/employee/attendance
    // ─────────────────────────────────────────────────────────
    @GetMapping("/dashboard/employee/attendance")
    public String employeeAttendance(HttpServletRequest request, Model model) {
        String token = getTokenFromCookie(request);
        if (token == null) return "redirect:/login?error=unauthorized";

        try {
            String role = jwtService.extractClaim(token,
                    claims -> claims.get("role", String.class));
            if (!"EMPLOYEE".equalsIgnoreCase(role)) return "redirect:/login?error=forbidden";

            String username = jwtService.extractUsername(token);
            addCommonAttributes(model, request, username, role, "Chấm Công Của Tôi");
            model.addAttribute("pageTitle", "Employee Attendance");

            System.out.println(">>> [Employee] Attendance cho: " + username);
            return "dashboard/employee/attendance";
        } catch (Exception e) {
            return "redirect:/login?error=invalid_token";
        }
    }

    // ─────────────────────────────────────────────────────────
    // GET /dashboard/employee/leaves
    // ─────────────────────────────────────────────────────────
    @GetMapping("/dashboard/employee/leaves")
    public String employeeLeaves(HttpServletRequest request, Model model) {
        String token = getTokenFromCookie(request);
        if (token == null) return "redirect:/login?error=unauthorized";

        try {
            String role = jwtService.extractClaim(token,
                    claims -> claims.get("role", String.class));
            if (!"EMPLOYEE".equalsIgnoreCase(role)) return "redirect:/login?error=forbidden";

            String username = jwtService.extractUsername(token);
            addCommonAttributes(model, request, username, role, "Đơn Phép");
            model.addAttribute("pageTitle", "Employee Leaves");

            System.out.println(">>> [Employee] Leaves cho: " + username);
            return "dashboard/employee/leaves";
        } catch (Exception e) {
            return "redirect:/login?error=invalid_token";
        }
    }

    // ─────────────────────────────────────────────────────────
    // GET /dashboard/employee/payslip
    // ─────────────────────────────────────────────────────────
    @GetMapping("/dashboard/employee/payslip")
    public String employeePayslip(HttpServletRequest request, Model model) {
        String token = getTokenFromCookie(request);
        if (token == null) return "redirect:/login?error=unauthorized";

        try {
            String role = jwtService.extractClaim(token,
                    claims -> claims.get("role", String.class));
            if (!"EMPLOYEE".equalsIgnoreCase(role)) return "redirect:/login?error=forbidden";

            String username = jwtService.extractUsername(token);
            addCommonAttributes(model, request, username, role, "Lương Của Tôi");
            model.addAttribute("pageTitle", "Employee Payslip");

            System.out.println(">>> [Employee] Payslip cho: " + username);
            return "dashboard/employee/payslip";
        } catch (Exception e) {
            return "redirect:/login?error=invalid_token";
        }
    }

    // ─────────────────────────────────────────────────────────
    // REST API FOR EMPLOYEE DASHBOARD
    // ─────────────────────────────────────────────────────────


    @GetMapping("/api/dashboard/employee/attendance-summary")
    @ResponseBody
    public ResponseEntity<?> getAttendanceSummary(HttpServletRequest request) {
        NhanVien nv = getNhanVienFromRequest(request);
        if (nv == null) return ResponseEntity.status(401).body("Unauthorized");

        Map<String, Object> summary = dashboardService.getAttendanceSummary(nv);
        return ResponseEntity.ok(summary);
    }

    @GetMapping("/api/dashboard/employee/payslips")
    @ResponseBody
    public ResponseEntity<?> getEmployeePayslips(HttpServletRequest request,
            @RequestParam(required = false) Integer limit,
            @RequestParam(required = false) String year,
            @RequestParam(required = false) String month) {
        NhanVien nv = getNhanVienFromRequest(request);
        if (nv == null) return ResponseEntity.status(401).body("Unauthorized");

        List<PhieuLuong> payslips = dashboardService.getEmployeePayslips(nv.getId(), limit, year, month);
        return ResponseEntity.ok(payslips);
    }

    @GetMapping("/api/dashboard/employee/payslips/{id}")
    @ResponseBody
    public ResponseEntity<?> getEmployeePayslipDetail(HttpServletRequest request, @PathVariable String id) {
        NhanVien nv = getNhanVienFromRequest(request);
        if (nv == null) return ResponseEntity.status(401).body("Unauthorized");
        Map<String, Object> detail = dashboardService.getEmployeePayslipDetail(id, nv);
        if (detail == null) return ResponseEntity.status(404).body("Not found");
        return ResponseEntity.ok(detail);
    }

    @GetMapping(value = "/api/dashboard/employee/payslips/{id}/download", produces = MediaType.APPLICATION_PDF_VALUE)
    @ResponseBody
    public ResponseEntity<byte[]> downloadEmployeePayslip(HttpServletRequest request, @PathVariable String id) {
        NhanVien nv = getNhanVienFromRequest(request);
        if (nv == null) return ResponseEntity.status(401).build();
        Map<String, Object> detail = dashboardService.getEmployeePayslipDetail(id, nv);
        if (detail == null) return ResponseEntity.notFound().build();
        byte[] body = buildPayslipPdf(detail);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=payslip-" + id + ".pdf")
                .body(body);
    }

    private byte[] buildPayslipPdf(Map<String, Object> detail) {
        List<String> lines = new ArrayList<>();
        lines.add("PHIEU LUONG - HRM Group 117");
        lines.add("Ma phieu: " + safe(detail.get("id")));
        lines.add("Ky luong: " + safe(detail.get("thangNam")));
        // PDF builder hiện tại chỉ dùng Helvetica + ASCII, nên loại bỏ dấu để tránh lỗi hiển thị.
        lines.add("Nhan vien: " + asciiSafe(detail.get("hoTen")) + " (" + safe(detail.get("maNhanVien")) + ")");
        lines.add("Phat muon: " + safe(detail.get("phatMuon")));
        lines.add("Nghi khong phep: " + safe(detail.get("nghiKhongPhep")));
        lines.add("Tong luong: " + safe(detail.get("tongLuong")));
        lines.add("Trang thai thanh toan: " + safe(detail.get("trangThaiThanhToan")));

        StringBuilder stream = new StringBuilder();
        // Set leading để mỗi dòng xuống hàng đúng, tránh chồng chữ (lỗi hiện tại).
        stream.append("BT\n/F1 12 Tf\n12 TL\n50 800 Td\n");
        for (int i = 0; i < lines.size(); i++) {
            if (i > 0) stream.append("T*\n");
            stream.append("(").append(pdfEscape(lines.get(i))).append(") Tj\n");
        }
        stream.append("ET\n");
        byte[] contentBytes = stream.toString().getBytes(StandardCharsets.US_ASCII);

        String obj1 = "1 0 obj\n<< /Type /Catalog /Pages 2 0 R >>\nendobj\n";
        String obj2 = "2 0 obj\n<< /Type /Pages /Kids [3 0 R] /Count 1 >>\nendobj\n";
        String obj3 = "3 0 obj\n<< /Type /Page /Parent 2 0 R /MediaBox [0 0 595 842] /Resources << /Font << /F1 4 0 R >> >> /Contents 5 0 R >>\nendobj\n";
        String obj4 = "4 0 obj\n<< /Type /Font /Subtype /Type1 /BaseFont /Helvetica >>\nendobj\n";
        String obj5Header = "5 0 obj\n<< /Length " + contentBytes.length + " >>\nstream\n";
        String obj5Footer = "endstream\nendobj\n";

        StringBuilder pdf = new StringBuilder();
        pdf.append("%PDF-1.4\n");
        int off1 = pdf.length(); pdf.append(obj1);
        int off2 = pdf.length(); pdf.append(obj2);
        int off3 = pdf.length(); pdf.append(obj3);
        int off4 = pdf.length(); pdf.append(obj4);
        int off5 = pdf.length(); pdf.append(obj5Header);
        String headerPart = pdf.toString();
        byte[] headerBytes = headerPart.getBytes(StandardCharsets.US_ASCII);
        byte[] footerBytes = obj5Footer.getBytes(StandardCharsets.US_ASCII);

        int xrefStart = headerBytes.length + contentBytes.length + footerBytes.length;
        String xref = "xref\n0 6\n"
                + "0000000000 65535 f \n"
                + String.format("%010d 00000 n \n", off1)
                + String.format("%010d 00000 n \n", off2)
                + String.format("%010d 00000 n \n", off3)
                + String.format("%010d 00000 n \n", off4)
                + String.format("%010d 00000 n \n", off5)
                + "trailer\n<< /Size 6 /Root 1 0 R >>\nstartxref\n"
                + xrefStart + "\n%%EOF";
        byte[] xrefBytes = xref.getBytes(StandardCharsets.US_ASCII);

        byte[] out = new byte[headerBytes.length + contentBytes.length + footerBytes.length + xrefBytes.length];
        System.arraycopy(headerBytes, 0, out, 0, headerBytes.length);
        System.arraycopy(contentBytes, 0, out, headerBytes.length, contentBytes.length);
        System.arraycopy(footerBytes, 0, out, headerBytes.length + contentBytes.length, footerBytes.length);
        System.arraycopy(xrefBytes, 0, out, headerBytes.length + contentBytes.length + footerBytes.length, xrefBytes.length);
        return out;
    }

    private String pdfEscape(String s) {
        return s.replace("\\", "\\\\").replace("(", "\\(").replace(")", "\\)");
    }

    private String safe(Object v) {
        return v == null ? "" : String.valueOf(v);
    }

    private String asciiSafe(Object v) {
        String s = safe(v);
        if (s.isBlank()) return s;
        try {
            String norm = java.text.Normalizer.normalize(s, java.text.Normalizer.Form.NFD);
            return norm.replaceAll("\\p{InCombiningDiacriticalMarks}+", "").replace("đ", "d").replace("Đ", "D");
        } catch (Exception e) {
            return s;
        }
    }

    @GetMapping("/api/dashboard/employee/attendance-history")
    @ResponseBody
    public ResponseEntity<?> getAttendanceHistory(HttpServletRequest request,
            @RequestParam(required = false) String month) {
        NhanVien nv = getNhanVienFromRequest(request);
        if (nv == null) return ResponseEntity.status(401).body("Unauthorized");
        return ResponseEntity.ok(dashboardService.getAttendanceHistory(nv, month));
    }

    @GetMapping("/api/dashboard/employee/attendance-trend")
    @ResponseBody
    public ResponseEntity<?> getEmployeeAttendanceTrend(HttpServletRequest request) {
        NhanVien nv = getNhanVienFromRequest(request);
        if (nv == null) return ResponseEntity.status(401).body("Unauthorized");
        return ResponseEntity.ok(dashboardService.getEmployeeAttendanceTrend(nv));
    }


    // ─────────────────────────────────────────────────────────
    // REST API FOR DIRECTOR/HR/ADMIN
    // ─────────────────────────────────────────────────────────

    @GetMapping("/api/dashboard/director/kpi")
    @ResponseBody
    public ResponseEntity<?> getDirectorKpi(HttpServletRequest request) {
        String role = getRoleFromRequest(request); if (role == null || !"DIRECTOR".equalsIgnoreCase(role)) return ResponseEntity.status(403).body("Forbidden");
        NhanVien director = getNhanVienFromRequest(request);
        if (director == null) return ResponseEntity.status(401).body("Unauthorized");
        return ResponseEntity.ok(dashboardService.getDirectorKpi(director));
    }

    @GetMapping("/api/dashboard/director/headcount-trend")
    @ResponseBody
    public ResponseEntity<?> getDirectorHeadcountTrend(HttpServletRequest request) {
        String role = getRoleFromRequest(request); if (role == null || !"DIRECTOR".equalsIgnoreCase(role)) return ResponseEntity.status(403).body("Forbidden");
        return ResponseEntity.ok(dashboardService.getDirectorHeadcountTrend());
    }

    @GetMapping("/api/dashboard/director/attendance-by-dept")
    @ResponseBody
    public ResponseEntity<?> getDirectorAttendanceByDept(HttpServletRequest request,
            @RequestParam(required = false) Integer month,
            @RequestParam(required = false) Integer year) {
        String role = getRoleFromRequest(request); if (role == null || !"DIRECTOR".equalsIgnoreCase(role)) return ResponseEntity.status(403).body("Forbidden");
        return ResponseEntity.ok(dashboardService.getDirectorAttendanceByDept(month, year));
    }

    @GetMapping("/api/dashboard/director/salary-trend")
    @ResponseBody
    public ResponseEntity<?> getDirectorSalaryTrend(HttpServletRequest request) {
        String role = getRoleFromRequest(request); if (role == null || !"DIRECTOR".equalsIgnoreCase(role)) return ResponseEntity.status(403).body("Forbidden");
        return ResponseEntity.ok(dashboardService.getDirectorSalaryTrend());
    }

    // Director workflow: đơn nghỉ phép chờ duyệt
    @GetMapping("/api/dashboard/director/leaves/pending")
    @ResponseBody
    public ResponseEntity<?> getDirectorPendingLeaves(HttpServletRequest request) {
        NhanVien manager = getNhanVienFromRequest(request);
        if (manager == null) return ResponseEntity.status(401).body("Unauthorized");
        return ResponseEntity.ok(donNghiPhepService.getManagerPendingLeaves(manager));
    }

    @PutMapping("/api/dashboard/director/leaves/{id}/approve")
    @ResponseBody
    public ResponseEntity<?> directorApproveLeave(HttpServletRequest request, @PathVariable Long id) {
        NhanVien manager = getNhanVienFromRequest(request);
        if (manager == null) return ResponseEntity.status(401).body("Unauthorized");
        try {
            DonNghiPhep updated = donNghiPhepService.approveByManager(id, manager);
            return ResponseEntity.ok(updated);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @PutMapping("/api/dashboard/director/leaves/{id}/reject")
    @ResponseBody
    public ResponseEntity<?> directorRejectLeave(HttpServletRequest request, @PathVariable Long id,
            @RequestBody Map<String, String> payload) {
        NhanVien manager = getNhanVienFromRequest(request);
        if (manager == null) return ResponseEntity.status(401).body("Unauthorized");

        String reason = payload != null ? payload.getOrDefault("reason", "") : "";
        try {
            DonNghiPhep updated = donNghiPhepService.rejectLeave(id, reason, manager, true);
            return ResponseEntity.ok(updated);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @GetMapping("/api/dashboard/hr/stats")
    @ResponseBody
    public ResponseEntity<?> getHrStats(HttpServletRequest request) {
        requireRole(request, "HR");
        return ResponseEntity.ok(dashboardService.getHrStats());
    }

    // HR employee CRUD
    @GetMapping("/api/dashboard/hr/employees")
    @ResponseBody
    public ResponseEntity<?> listEmployees(HttpServletRequest request) {
        requireRole(request, "HR");
        return ResponseEntity.ok(dashboardService.getAllEmployees());
    }

    @GetMapping("/api/dashboard/hr/employees/{id}")
    @ResponseBody
    public ResponseEntity<?> getEmployee(HttpServletRequest request, @PathVariable String id) {
        requireRole(request, "HR");
        NhanVien nv = dashboardService.getEmployeeById(id);
        return nv == null ? ResponseEntity.status(404).body("Employee not found") : ResponseEntity.ok(nv);
    }

    @PostMapping("/api/dashboard/hr/employees")
    @ResponseBody
    public ResponseEntity<?> createEmployee(HttpServletRequest request, @RequestBody Map<String,Object> payload) {
        requireRole(request, "HR");
        try {
            NhanVien created = dashboardService.createEmployeeFromMap(payload);
            return ResponseEntity.ok(created);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Failed to create employee: " + e.getMessage());
        }
    }

    @PutMapping("/api/dashboard/hr/employees/{id}")
    @ResponseBody
    public ResponseEntity<?> updateEmployee(HttpServletRequest request, @PathVariable String id, @RequestBody Map<String,Object> payload) {
        requireRole(request, "HR");
        try {
            NhanVien updated = dashboardService.updateEmployee(id, payload);
            return updated == null ? ResponseEntity.status(404).body("Employee not found") : ResponseEntity.ok(updated);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Failed to update employee: " + e.getMessage());
        }
    }

    @DeleteMapping("/api/dashboard/hr/employees/{id}")
    @ResponseBody
    public ResponseEntity<?> deleteEmployee(HttpServletRequest request, @PathVariable String id) {
        requireRole(request, "HR");
        boolean removed = dashboardService.deleteEmployee(id);
        return removed ? ResponseEntity.ok("Deleted") : ResponseEntity.status(404).body("Employee not found");
    }

    // HR leave workflow
    @GetMapping("/api/dashboard/hr/leaves/pending")
    @ResponseBody
    public ResponseEntity<?> getHrPendingLeaves(HttpServletRequest request) {
        requireRole(request, "HR");
        return ResponseEntity.ok(donNghiPhepService.getHrPendingLeaves());
    }

    @GetMapping("/api/dashboard/hr/leaves")
    @ResponseBody
    public ResponseEntity<?> getHrAllLeaves(HttpServletRequest request) {
        requireRole(request, "HR");
        return ResponseEntity.ok(dashboardService.getAllLeaves());
    }

    @PutMapping("/api/dashboard/hr/leaves/{id}/approve")
    @ResponseBody
    public ResponseEntity<?> hrApproveLeave(HttpServletRequest request, @PathVariable Long id) {
        requireRole(request, "HR");
        try {
            DonNghiPhep updated = donNghiPhepService.confirmByHr(id);
            return ResponseEntity.ok(updated);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @PutMapping("/api/dashboard/hr/leaves/{id}/reject")
    @ResponseBody
    public ResponseEntity<?> hrRejectLeave(HttpServletRequest request, @PathVariable Long id, @RequestBody Map<String, String> payload) {
        requireRole(request, "HR");
        String reason = payload.getOrDefault("reason", "");
        try {
            NhanVien hr = getNhanVienFromRequest(request);
            DonNghiPhep updated = donNghiPhepService.rejectLeave(id, reason, hr, false);
            return ResponseEntity.ok(updated);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @DeleteMapping("/api/dashboard/hr/leaves/{id}")
    @ResponseBody
    public ResponseEntity<?> hrDeleteLeave(HttpServletRequest request, @PathVariable Long id) {
        requireRole(request, "HR");
        boolean deleted = dashboardService.deleteLeaveRequest(id);
        if (!deleted) return ResponseEntity.status(404).body("Leave not found");
        return ResponseEntity.ok("Deleted");
    }

    @GetMapping("/api/dashboard/hr/payslips")
    @ResponseBody
    public ResponseEntity<?> getHrPayslips(HttpServletRequest request, @RequestParam(required = false) String month) {
        requireRole(request, "HR");
        return ResponseEntity.ok(dashboardService.getAllPayslips(month));
    }

    @PutMapping("/api/dashboard/hr/payslips/{id}/confirm")
    @ResponseBody
    public ResponseEntity<?> confirmHrPayslip(HttpServletRequest request, @PathVariable String id) {
        requireRole(request, "HR");
        PhieuLuong pl = dashboardService.confirmPayslip(id);
        return pl == null ? ResponseEntity.status(404).body("Payslip not found") : ResponseEntity.ok(pl);
    }

    @GetMapping("/api/dashboard/hr/attendance/summary")
    @ResponseBody
    public ResponseEntity<?> getHrAttendanceSummary(HttpServletRequest request, @RequestParam(required = false) String month) {
        requireRole(request, "HR");
        return ResponseEntity.ok(dashboardService.getAttendanceSummary(month));
    }

    @GetMapping("/api/dashboard/hr/attendance/top-late")
    @ResponseBody
    public ResponseEntity<?> getHrTopLate(HttpServletRequest request, @RequestParam(required = false) String month, @RequestParam(defaultValue = "5") int limit) {
        requireRole(request, "HR");
        return ResponseEntity.ok(dashboardService.getTopLateEmployees(month, limit));
    }

    @GetMapping("/api/dashboard/hr/recruitment/candidates")
    @ResponseBody
    public ResponseEntity<?> getHrRecruitmentCandidates(HttpServletRequest request, @RequestParam(required = false) String month) {
        requireRole(request, "HR");
        return ResponseEntity.ok(dashboardService.getRecruitmentCandidates(month));
    }

    @GetMapping("/api/dashboard/hr/recruitment/requests")
    @ResponseBody
    public ResponseEntity<?> getHrRecruitmentRequests(HttpServletRequest request, @RequestParam(required = false) String month) {
        requireRole(request, "HR");
        return ResponseEntity.ok(dashboardService.getRecruitmentRequests(month));
    }

    private String getRoleFromRequest(HttpServletRequest request) {
        String token = getTokenFromCookie(request);
        if (token == null) return null;
        try {
            return jwtService.extractClaim(token, claims -> claims.get("role", String.class));
        } catch (Exception e) {
            return null;
        }
    }

    // ─────────────────────────────────────────────────────────
    // Helper: Kiểm tra role và throw exception nếu không đúng
    // ─────────────────────────────────────────────────────────
    private void requireRole(HttpServletRequest request, String requiredRole) {
        String role = getRoleFromRequest(request);
        if (role == null || !requiredRole.equalsIgnoreCase(role)) {
            throw new SecurityException("Forbidden: " + requiredRole + " role required");
        }
    }
}
