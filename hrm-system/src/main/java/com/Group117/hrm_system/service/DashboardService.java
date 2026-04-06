package com.Group117.hrm_system.service;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.YearMonth;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.Group117.hrm_system.Repository.ChamCongRepository;
import com.Group117.hrm_system.Repository.ChucVuRepository;
import com.Group117.hrm_system.Repository.DonNghiPhepRepository;
import com.Group117.hrm_system.Repository.HoSoUngVienRepository;
import com.Group117.hrm_system.Repository.NhanVienRepository;
import com.Group117.hrm_system.Repository.NhomRepository;
import com.Group117.hrm_system.Repository.PhieuLuongRepository;
import com.Group117.hrm_system.Repository.PhongBanRepository;
import com.Group117.hrm_system.Repository.TaiKhoanRepository;
import com.Group117.hrm_system.Repository.YeuCauTuyenDungRepository;
import com.Group117.hrm_system.dto.HrLeaveDto;
import com.Group117.hrm_system.entity.ChamCong;
import com.Group117.hrm_system.entity.ChucVu;
import com.Group117.hrm_system.entity.DonNghiPhep;
import com.Group117.hrm_system.entity.HoSoUngVien;
import com.Group117.hrm_system.entity.NhanVien;
import com.Group117.hrm_system.entity.Nhom;
import com.Group117.hrm_system.entity.PhieuLuong;
import com.Group117.hrm_system.entity.TaiKhoan;
import com.Group117.hrm_system.entity.YeuCauTuyenDung;

@Service
public class DashboardService {
    @Value("${app.upload.avatar-dir:uploads/avatars}")
    private String avatarDir;

    @Autowired
    private ChamCongRepository chamCongRepo;

    @Autowired
    private DonNghiPhepRepository donNghiPhepRepo;

    @Autowired
    private NhanVienRepository nhanVienRepo;

    @Autowired
    private PhieuLuongRepository phieuLuongRepo;

    @Autowired
    private TaiKhoanRepository taiKhoanRepo;

    @Autowired
    private AdminService adminService;

    @Autowired
    private NotificationService notificationService;

    @Autowired
    private PhongBanRepository phongBanRepo;

    @Autowired
    private ChucVuRepository chucVuRepo;

    @Autowired
    private NhomRepository nhomRepo;

    @Autowired
    private HoSoUngVienRepository hoSoUngVienRepo;

    @Autowired
    private YeuCauTuyenDungRepository yeuCauTuyenDungRepo;

    // ─────────────────────────────────────────────────────────
    // API cho Employee Dashboard
    // ─────────────────────────────────────────────────────────

    public Map<String, Object> getEmployeeStats(NhanVien nv) {
        LocalDate today = LocalDate.now();
        YearMonth yearMonth = YearMonth.from(today);
        LocalDate startOfMonth = yearMonth.atDay(1);
        LocalDate endOfMonth = yearMonth.atEndOfMonth();

        // 1. Chấm công hôm nay
        ChamCong ccToday = chamCongRepo.findByNhanVienIdAndNgay(nv.getId(), today).orElse(null);
        Map<String, Object> checkinInfo = new HashMap<>();
        if (ccToday != null) {
            checkinInfo.put("status", ccToday.getTrangThai());
            checkinInfo.put("gioVao", ccToday.getGioVao());
            checkinInfo.put("gioRa", ccToday.getGioRa());
        } else {
            checkinInfo.put("status", "CHUA_CHECKIN");
        }

        // 2. Ngày có mặt & Đi muộn tháng này
        long ngayCoMat = chamCongRepo.countByNhanVienIdAndNgayBetweenAndTrangThai(nv.getId(), startOfMonth, endOfMonth, "DI_LAM")
                + chamCongRepo.countByNhanVienIdAndNgayBetweenAndTrangThai(nv.getId(), startOfMonth, endOfMonth, "DI_MUON");
        long diMuon = chamCongRepo.countByNhanVienIdAndNgayBetweenAndTrangThai(nv.getId(), startOfMonth, endOfMonth, "DI_MUON");

        // Tổng ngày công dự kiến (tạm tính các ngày từ thứ 2-6)
        int tongNgayCong = 0;
        for (int i = 1; i <= yearMonth.lengthOfMonth(); i++) {
            LocalDate d = yearMonth.atDay(i);
            if (d.getDayOfWeek().getValue() >= 1 && d.getDayOfWeek().getValue() <= 5) {
                tongNgayCong++;
            }
        }

        // 3. Lương tháng trước
        String lastMonthStr = String.format("%04d-%02d",
            today.minusMonths(1).getYear(),
            today.minusMonths(1).getMonthValue());
        List<PhieuLuong> plLastMonth = phieuLuongRepo.findByNhanVienIdAndThangNamIn(nv.getId(), monthAliases(lastMonthStr));
        Double luongThangTruoc = 0.0;
        if (!plLastMonth.isEmpty()) {
            luongThangTruoc = plLastMonth.get(0).getTongLuong();
        }

        Map<String, Object> stats = new HashMap<>();
        stats.put("checkinToday", checkinInfo);
        stats.put("ngayCoMat", ngayCoMat);
        stats.put("tongNgayCong", tongNgayCong);
        stats.put("diMuon", diMuon);
        stats.put("phepConLai", nv.getSoNgayPhepConLai());
        stats.put("maxPhep", 12);
        stats.put("luongThangTruoc", luongThangTruoc);

        return stats;
    }

    public List<DonNghiPhep> getEmployeeLeaves(String nhanVienId, Integer limit) {
        List<DonNghiPhep> list = donNghiPhepRepo.findByNhanVienIdOrderByTuNgayDesc(nhanVienId);
        if (limit != null && limit > 0 && list.size() > limit) {
            return new ArrayList<>(list.subList(0, limit));
        }
        return list;
    }

    public Map<String, Object> getAttendanceSummary(NhanVien nv) {
        LocalDate today = LocalDate.now();
        YearMonth yearMonth = YearMonth.from(today);
        LocalDate startOfMonth = yearMonth.atDay(1);
        LocalDate endOfMonth = yearMonth.atEndOfMonth();

        long dungGio = chamCongRepo.countByNhanVienIdAndNgayBetweenAndTrangThai(nv.getId(), startOfMonth, endOfMonth, "DI_LAM");
        long diMuon = chamCongRepo.countByNhanVienIdAndNgayBetweenAndTrangThai(nv.getId(), startOfMonth, endOfMonth, "DI_MUON");

        // Tính số ngày đã qua trong tháng (tới hôm nay) trừ cuối tuần
        int ngayDaQua = 0;
        for (int i = 1; i <= today.getDayOfMonth(); i++) {
            LocalDate d = yearMonth.atDay(i);
            if (d.getDayOfWeek().getValue() >= 1 && d.getDayOfWeek().getValue() <= 5) {
                ngayDaQua++;
            }
        }

        long vangMat = Math.max(0, ngayDaQua - dungGio - diMuon);

        Map<String, Object> summary = new HashMap<>();
        if (ngayDaQua == 0) {
            summary.put("pctDungGio", 0);
            summary.put("pctDiMuon", 0);
            summary.put("pctVangMat", 0);
        } else {
            summary.put("pctDungGio", (int) (dungGio * 100 / ngayDaQua));
            summary.put("pctDiMuon", (int) (diMuon * 100 / ngayDaQua));
            summary.put("pctVangMat", (int) (vangMat * 100 / ngayDaQua));
        }

        return summary;
    }

    public List<PhieuLuong> getEmployeePayslips(String nhanVienId, Integer limit, String year, String month) {
        List<PhieuLuong> payslips = phieuLuongRepo.findByNhanVienId(nhanVienId).stream()
                .peek(p -> p.setThangNam(PhieuLuong.normalizeThangNam(p.getThangNam())))
                .filter(p -> matchesYearMonthFilter(p.getThangNam(), year, month))
                .sorted(Comparator.comparing(this::parseThangNamToYm, Comparator.nullsFirst(Comparator.naturalOrder())).reversed())
                .collect(Collectors.toList());
        if (limit != null && limit > 0 && payslips.size() > limit) {
            return new ArrayList<>(payslips.subList(0, limit));
        }
        return payslips;
    }

    public Map<String, Object> getEmployeePayslipDetail(String id, NhanVien nv) {
        return phieuLuongRepo.findById(id)
                .filter(p -> p.getNhanVien() != null && nv.getId().equals(p.getNhanVien().getId()))
                .map(p -> {
                    Map<String, Object> m = new HashMap<>();
                    m.put("id", p.getId());
                    m.put("thangNam", p.getThangNam());
                    m.put("tongLuong", p.getTongLuong());
                    m.put("trangThaiThanhToan", p.getTrangThaiThanhToan());
                    m.put("phatMuon", p.getPhatMuon());
                    m.put("nghiKhongPhep", p.getNghiKhongPhep());
                    m.put("hoTen", nv.getHoTen());
                    m.put("maNhanVien", nv.getId());
                    m.put("chucVu", nv.getChucVu() != null ? nv.getChucVu().getTenChucVu() : "");
                    m.put("phongBan", nv.getPhongBan() != null ? nv.getPhongBan().getTenPhongBan() : "");
                    return m;
                }).orElse(null);
    }

    private boolean matchesYearMonthFilter(String thangNam, String year, String month) {
        boolean noY = year == null || year.isBlank();
        boolean noM = month == null || month.isBlank();
        if (noY && noM) return true;
        YearMonth ym = parseThangNamString(thangNam);
        if (ym == null) return false;
        String m = String.format("%02d", ym.getMonthValue());
        String y = String.valueOf(ym.getYear());
        if (!noY && !y.equals(year.trim())) return false;
        if (!noM) {
            String mm = month.trim();
            if (mm.length() == 1) mm = "0" + mm;
            if (!m.equals(mm)) return false;
        }
        return true;
    }

    private YearMonth parseThangNamToYm(PhieuLuong p) {
        return parseThangNamString(p != null ? p.getThangNam() : null);
    }

    private YearMonth parseThangNamString(String thangNam) {
        if (thangNam == null || thangNam.isBlank()) return null;
        String normalized = thangNam.trim().replace("-", "/");
        if (!normalized.contains("/")) return null;
        String[] p = normalized.split("/");
        if (p.length != 2) return null;
        try {
            int a = Integer.parseInt(p[0].trim());
            int b = Integer.parseInt(p[1].trim());
            int mo;
            int yr;
            // Hỗ trợ cả MM/YYYY và YYYY/MM (hoặc YYYY-MM)
            if (a > 1000) {
                yr = a;
                mo = b;
            } else {
                mo = a;
                yr = b;
            }
            return YearMonth.of(yr, mo);
        } catch (Exception e) {
            return null;
        }
    }

    private List<String> monthAliases(String monthYear) {
        String canonical = PhieuLuong.normalizeThangNam(monthYear);
        if (canonical == null || canonical.isBlank()) return List.of();
        if (!canonical.contains("-")) return List.of(canonical);
        String[] p = canonical.split("-");
        String legacy = p[1] + "/" + p[0];
        return List.of(canonical, legacy);
    }

    public List<ChamCong> getAttendanceHistory(NhanVien nv, String monthYear) {
        YearMonth ym = (monthYear != null && !monthYear.isBlank())
                ? YearMonth.parse(monthYear)
                : YearMonth.from(LocalDate.now());
        LocalDate start = ym.atDay(1);
        LocalDate end = ym.atEndOfMonth();
        return chamCongRepo.findByNhanVienIdAndNgayBetween(nv.getId(), start, end);
    }

    public Map<String, Object> getEmployeeAttendanceTrend(NhanVien nv) {
        LocalDate today = LocalDate.now();
        List<String> labels = new ArrayList<>();
        List<Long> onTime = new ArrayList<>();
        List<Long> late = new ArrayList<>();
        List<Long> absent = new ArrayList<>();
        for (int i = 5; i >= 0; i--) {
            YearMonth ym = YearMonth.from(today.minusMonths(i));
            labels.add(String.format("T%d/%d", ym.getMonthValue(), ym.getYear()));
            LocalDate start = ym.atDay(1);
            LocalDate end = ym.atEndOfMonth();
            long o = chamCongRepo.countByNhanVienIdAndNgayBetweenAndTrangThai(nv.getId(), start, end, "DI_LAM");
            long l = chamCongRepo.countByNhanVienIdAndNgayBetweenAndTrangThai(nv.getId(), start, end, "DI_MUON");
            int wd = countWeekdaysBetween(start, end);
            long ab = Math.max(0, wd - o - l);
            onTime.add(o);
            late.add(l);
            absent.add(ab);
        }
        Map<String, Object> out = new HashMap<>();
        out.put("labels", labels);
        out.put("onTime", onTime);
        out.put("late", late);
        out.put("absent", absent);
        return out;
    }

    private static int countWeekdaysBetween(LocalDate from, LocalDate to) {
        int n = 0;
        for (LocalDate d = from; !d.isAfter(to); d = d.plusDays(1)) {
            DayOfWeek w = d.getDayOfWeek();
            if (w != DayOfWeek.SATURDAY && w != DayOfWeek.SUNDAY) n++;
        }
        return n;
    }

    public Map<String, Object> getEmployeeProfile(NhanVien nv) {
        Map<String, Object> profile = new HashMap<>();
        profile.put("id", nv.getId());
        profile.put("hoTen", nv.getHoTen());
        profile.put("email", nv.getEmailCongViec());
        profile.put("phone", nv.getSoDienThoai());

        // `diaChiTamTru` có thể chứa cả "địa chỉ + thành phố + ghi chú" (để tránh phải đổi schema ngay).
        AddressParts parts = parseDiaChiTamTru(nv.getDiaChiTamTru());
        profile.put("address", parts.address);
        profile.put("city", parts.city);
        profile.put("notes", parts.notes);

        profile.put("avatarUrl", nv.getAnhDaiDienUrl());
        profile.put("ngayVaoLam", nv.getNgayVaoLam() != null ? nv.getNgayVaoLam().toString() : "");
        profile.put("chucVu", (nv.getChucVu() != null) ? nv.getChucVu().getTenChucVu() : "");
        profile.put("phongBan", (nv.getPhongBan() != null) ? nv.getPhongBan().getTenPhongBan() : "");
        return profile;
    }

    public NhanVien updateEmployeeProfile(String nhanVienId, Map<String, String> updates) {
        NhanVien nv = nhanVienRepo.findById(nhanVienId).orElse(null);
        if (nv == null) return null;

        if (updates.containsKey("email")) nv.setEmailCongViec(updates.get("email"));
        if (updates.containsKey("phone")) nv.setSoDienThoai(updates.get("phone"));

        // Compose address/city/notes into `diaChiTamTru`
        String address = updates.getOrDefault("address", "");
        String city = updates.getOrDefault("city", "");
        String notes = updates.getOrDefault("notes", "");
        String startDate = updates.getOrDefault("startDate", "");

        if (updates.containsKey("address") || updates.containsKey("city") || updates.containsKey("notes")) {
            String base = address != null ? address.trim() : "";
            String c = city != null ? city.trim() : "";
            String n = notes != null ? notes.trim() : "";

            if (!c.isEmpty()) {
                base = base.isEmpty() ? c : (base + ", " + c);
            }
            if (!n.isEmpty()) {
                base = base.isEmpty() ? ("Ghi chú: " + n) : (base + "\nGhi chú: " + n);
            }
            nv.setDiaChiTamTru(base);
        }

        if (startDate != null && !startDate.isBlank()) {
            try {
                nv.setNgayVaoLam(LocalDate.parse(startDate));
            } catch (Exception ignored) { /* ignore parse error */ }
        }

        nv = nhanVienRepo.save(nv);
        return nv;
    }

    public NhanVien updateEmployeeAvatar(String nhanVienId, MultipartFile avatarFile) {
        try {
            NhanVien nv = nhanVienRepo.findById(nhanVienId).orElse(null);
            if (nv == null) return null;
            if (avatarFile == null || avatarFile.isEmpty()) {
                throw new IllegalArgumentException("Avatar file is empty");
            }

            Path uploadDir = Paths.get(avatarDir).toAbsolutePath().normalize();
            Files.createDirectories(uploadDir);

            String originalName = avatarFile.getOriginalFilename();
            String ext = ".png";
            if (originalName != null && originalName.contains(".")) {
                ext = originalName.substring(originalName.lastIndexOf('.'));
                if (ext.length() > 10) ext = ".png";
            }

            String filename = nhanVienId + "_" + UUID.randomUUID().toString().replace("-", "") + ext;
            Path target = uploadDir.resolve(filename).normalize();
            Files.copy(avatarFile.getInputStream(), target, StandardCopyOption.REPLACE_EXISTING);

            nv.setAnhDaiDienUrl("/uploads/avatars/" + filename);
            return nhanVienRepo.save(nv);
        } catch (Exception e) {
            System.err.println(">>> updateEmployeeAvatar error: " + e.getMessage());
            e.printStackTrace();
            throw new RuntimeException("Không thể lưu ảnh avatar", e);
        }
    }

    private static class AddressParts {
        private final String address;
        private final String city;
        private final String notes;

        private AddressParts(String address, String city, String notes) {
            this.address = address;
            this.city = city;
            this.notes = notes;
        }
    }

    private static AddressParts parseDiaChiTamTru(String diaChiTamTru) {
        if (diaChiTamTru == null || diaChiTamTru.isBlank()) {
            return new AddressParts("", "", "");
        }

        String full = diaChiTamTru.trim();
        String lower = full.toLowerCase();

        String notesPrefix = "ghi chú:";
        String notesPrefixAlt = "ghi chu:";

        int idx = lower.indexOf(notesPrefix);
        int idxAlt = idx < 0 ? lower.indexOf(notesPrefixAlt) : -1;
        int noteIdx = idx >= 0 ? idx : idxAlt;

        String addressPart = full;
        String notesPart = "";
        if (noteIdx >= 0) {
            addressPart = full.substring(0, noteIdx).trim();
            int prefixLen = (idx >= 0) ? notesPrefix.length() : notesPrefixAlt.length();
            notesPart = full.substring(noteIdx + prefixLen).trim();
        }

        // Parse city as last comma-separated segment in addressPart
        String ap = addressPart.trim();
        int lastComma = ap.lastIndexOf(',');
        if (lastComma >= 0) {
            String addrOnly = ap.substring(0, lastComma).trim();
            String city = ap.substring(lastComma + 1).trim();
            return new AddressParts(addrOnly, city, notesPart);
        }

        return new AddressParts(ap, "", notesPart);
    }

    public DonNghiPhep createLeaveRequest(NhanVien nv, DonNghiPhep request) {
        request.setNhanVien(nv);
        request.setTrangThai("CHO_DUYET");
        DonNghiPhep saved = donNghiPhepRepo.save(request);
        String ten = nv.getHoTen() != null ? nv.getHoTen() : nv.getId();
        notificationService.notifyAllHr("LEAVE_PENDING", "Đơn nghỉ phép chờ duyệt",
                ten + " (" + nv.getId() + ") vừa gửi đơn nghỉ phép.",
                "{\"leaveId\":" + saved.getId() + "}");
        return saved;
    }

    public boolean cancelLeaveRequest(Long requestId, NhanVien nv) {
        return donNghiPhepRepo.findById(requestId)
                .filter(leave -> leave.getNhanVien() != null && leave.getNhanVien().getId().equals(nv.getId()))
                .map(leave -> {
                    leave.setTrangThai("TU_CHOI");
                    donNghiPhepRepo.save(leave);
                    return true;
                }).orElse(false);
    }

    // ─────────────────────────────────────────────────────────
    // API cho Director / HR / Admin
    // ─────────────────────────────────────────────────────────

    public Map<String, Object> getDirectorKpi(NhanVien director) {
        LocalDate today = LocalDate.now();
        YearMonth ym = YearMonth.from(today);
        LocalDate startOfMonth = ym.atDay(1);
        LocalDate endOfMonth = ym.atEndOfMonth();

        long totalEmployees = nhanVienRepo.count(); // chỉ gồm NV đang hoạt động do @SQLRestriction

        // NV mới vào trong tháng hiện tại
        long newThisMonth = nhanVienRepo.findAll().stream()
                .filter(nv -> nv.getNgayVaoLam() != null
                        && !nv.getNgayVaoLam().isBefore(startOfMonth)
                        && !nv.getNgayVaoLam().isAfter(endOfMonth))
                .count();

        // Turnover rate: hiện tại entity `NhanVien` chặn truy vấn NV đã nghỉ,
        // nên dữ liệu "rời đi" không lấy được chính xác. Trả về 0 để UI vẫn hoạt động.
        double turnoverRate = 0d;

        // Vị trí tuyển dụng đang mở (theo trạng thái request)
        long openPositions = yeuCauTuyenDungRepo.findAll().stream()
                .filter(r -> r.getTrangThai() != null && "PENDING".equalsIgnoreCase(r.getTrangThai()))
                .count();

        // Đơn nghỉ phép chờ duyệt theo workflow
        List<DonNghiPhep> pendingAll = donNghiPhepRepo.findByTrangThai("CHO_QL_DUYET");
        long pendingLeaves;
        if (director != null) {
            pendingLeaves = pendingAll.stream()
                    .filter(don -> don.getNguoiDuyet() != null && director.getId().equals(don.getNguoiDuyet().getId()))
                    .count();
        } else {
            pendingLeaves = pendingAll.size();
        }

        long unclosedPayroll = phieuLuongRepo.findByTrangThaiThanhToan("CHUA_THANH_TOAN").size();

        Map<String, Object> data = new HashMap<>();
        data.put("totalActive", totalEmployees);
        data.put("newThisMonth", newThisMonth);
        data.put("turnoverRate", turnoverRate);
        data.put("openPositions", openPositions);
        data.put("pendingLeaves", pendingLeaves);
        data.put("unclosedPayroll", unclosedPayroll);
        return data;
    }

    public List<Map<String, Object>> getDirectorHeadcountTrend() {
        LocalDate today = LocalDate.now();
        List<Map<String, Object>> trend = new java.util.ArrayList<>();

        // Do entity `NhanVien` loại NV đã nghỉ, nên chỉ mô tả "quy mô tồn tại theo thời điểm" + "ngày vào"
        // trên tập NV đang hoạt động hiện tại.
        List<NhanVien> actives = nhanVienRepo.findAll();

        for (int i = 5; i >= 0; i--) {
            YearMonth ym = YearMonth.from(today.minusMonths(i));
            LocalDate endOfMonth = ym.atEndOfMonth();

            long totalAsOfMonth = actives.stream()
                    .filter(nv -> nv.getNgayVaoLam() != null && !nv.getNgayVaoLam().isAfter(endOfMonth))
                    .count();

            long newJoined = actives.stream()
                    .filter(nv -> nv.getNgayVaoLam() != null && YearMonth.from(nv.getNgayVaoLam()).equals(ym))
                    .count();

            Map<String, Object> item = new HashMap<>();
            item.put("label", String.format("%02d/%d", ym.getMonthValue(), ym.getYear()));
            item.put("totalActive", totalAsOfMonth);
            item.put("newJoined", newJoined);
            item.put("left", 0); // chưa có dữ liệu rời đi do SQLRestriction
            trend.add(item);
        }
        return trend;
    }

    public List<Map<String, Object>> getDirectorAttendanceByDept() {
        return getDirectorAttendanceByDept(null, null);
    }

    public List<Map<String, Object>> getDirectorAttendanceByDept(Integer month, Integer year) {
        LocalDate now = LocalDate.now();
        YearMonth targetYm;
        if (month != null && year != null) {
            targetYm = YearMonth.of(year, month);
        } else {
            targetYm = YearMonth.from(now);
        }

        LocalDate start = targetYm.atDay(1);
        LocalDate end = targetYm.atEndOfMonth();

        // Kỳ vọng: "% ngày có mặt / tổng ngày làm chuẩn".
        // Công thức: attendanceRate = (số (nhân viên, ngày) có mặt) / (số NV của phòng * số ngày làm việc T2-T6 trong tháng) * 100
        int weekdays = countWeekdaysBetween(start, end);

        // Nhóm NV theo phòng ban để biết quy mô (denominator)
        Map<String, Set<String>> employeeIdsByDept = nhanVienRepo.findAll().stream()
                .collect(Collectors.groupingBy(
                        nv -> (nv.getPhongBan() != null && nv.getPhongBan().getTenPhongBan() != null)
                                ? nv.getPhongBan().getTenPhongBan()
                                : "—",
                        Collectors.mapping(NhanVien::getId, Collectors.toSet())
                ));

        // Đếm "có mặt" theo (nhân viên, ngày) để tránh trường hợp trùng record làm phồng số liệu
        Set<String> presentPairs = chamCongRepo.findAll().stream()
                .filter(cc -> cc.getNgay() != null && !cc.getNgay().isBefore(start) && !cc.getNgay().isAfter(end))
                .filter(cc -> cc.getNhanVien() != null && cc.getNhanVien().getId() != null)
                .filter(cc -> {
                    String st = cc.getTrangThai();
                    return st != null && (
                            "DI_LAM".equalsIgnoreCase(st)
                                    || "DI_MUON".equalsIgnoreCase(st)
                                    || "PHEP".equalsIgnoreCase(st)
                                    || "NGHI_PHEP".equalsIgnoreCase(st)
                    );
                })
                .map(cc -> {
                    String deptName = (cc.getNhanVien().getPhongBan() != null && cc.getNhanVien().getPhongBan().getTenPhongBan() != null)
                            ? cc.getNhanVien().getPhongBan().getTenPhongBan()
                            : "—";
                    return deptName + "|" + cc.getNhanVien().getId() + "|" + cc.getNgay();
                })
                .collect(Collectors.toSet());

        Map<String, Long> presentByDept = new HashMap<>();
        for (String k : presentPairs) {
            String dept = k.split("\\|", 2)[0];
            presentByDept.merge(dept, 1L, Long::sum);
        }

        // Output cho tất cả phòng có NV (kể cả không có record chấm công => 0%)
        return employeeIdsByDept.entrySet().stream()
                .map(e -> {
                    String dept = e.getKey();
                    int empCount = e.getValue() == null ? 0 : e.getValue().size();
                    long expected = (long) empCount * (long) weekdays;
                    long present = presentByDept.getOrDefault(dept, 0L);
                    int rate = expected == 0 ? 0 : (int) Math.round((present * 100.0) / expected);
                    // clamp về 0..100 để UI ổn định nếu dữ liệu bẩn
                    if (rate < 0) rate = 0;
                    if (rate > 100) rate = 100;
                    return Map.<String, Object>of(
                            "departmentName", dept,
                            "attendanceRate", rate
                    );
                })
                .sorted(Comparator.comparing((Map<String, Object> m) -> String.valueOf(m.get("departmentName"))))
                .collect(Collectors.toList());
    }

    public List<Map<String, Object>> getDirectorSalaryTrend() {
        LocalDate today = LocalDate.now();
        List<Map<String, Object>> trend = new java.util.ArrayList<>();
        for (int i = 5; i >= 0; i--) {
            LocalDate m = today.minusMonths(i);
            String thangNam = String.format("%04d-%02d", m.getYear(), m.getMonthValue());
            List<PhieuLuong> payslips = phieuLuongRepo.findByThangNamIn(monthAliases(thangNam));
            double total = 0.0;
            for (PhieuLuong p : payslips) {
                if (p.getTongLuong() != null) {
                    total += p.getTongLuong();
                }
            }
            Map<String, Object> entry = new HashMap<>();
            entry.put("label", thangNam);
            entry.put("total", total);
            entry.put("averagePerEmployee", payslips.isEmpty() ? 0 : total / payslips.size());
            trend.add(entry);
        }
        return trend;
    }

    // ─────────────────────────────────────────────────────────
    // Director workflow: duyệt đơn nghỉ phép chờ duyệt
    // ─────────────────────────────────────────────────────────

    public List<HrLeaveDto> getPendingLeavesForDirector(NhanVien director) {
        if (director == null || director.getId() == null) return List.of();

        return donNghiPhepRepo.findByTrangThai("CHO_DUYET").stream()
                .filter(don -> don.getNguoiDuyet() != null && director.getId().equals(don.getNguoiDuyet().getId()))
                .map(this::toHrLeaveDto)
                .collect(Collectors.toList());
    }

    public DonNghiPhep approveLeaveAsDirector(Long leaveId, NhanVien director) {
        if (director == null || director.getId() == null) {
            throw new SecurityException("Forbidden");
        }
        return donNghiPhepRepo.findById(leaveId).map(don -> {
            if (!"CHO_DUYET".equalsIgnoreCase(don.getTrangThai())) {
                throw new IllegalStateException("Leave request already processed");
            }
            if (don.getNguoiDuyet() == null || !director.getId().equals(don.getNguoiDuyet().getId())) {
                throw new SecurityException("Forbidden");
            }

            long soNgayNghi = java.time.temporal.ChronoUnit.DAYS.between(don.getTuNgay(), don.getDenNgay()) + 1;

            // Leave Balance Logic: Trừ quỹ phép năm
            if ("PH_NAM".equalsIgnoreCase(don.getLoaiNghi())) {
                NhanVien nv = don.getNhanVien();
                int cap = nv.getSoNgayPhepConLai() == null ? 0 : nv.getSoNgayPhepConLai();
                if (cap < soNgayNghi) {
                    throw new IllegalArgumentException("Nhân viên không đủ ngày phép");
                }
                nv.setSoNgayPhepConLai(cap - (int) soNgayNghi);
                nhanVienRepo.save(nv);
            }

            don.setTrangThai("DA_DUYET");
            DonNghiPhep saved = donNghiPhepRepo.save(don);
            if (saved.getNhanVien() != null) {
                notificationService.findUsernameByNhanVienId(saved.getNhanVien().getId()).ifPresent(u ->
                        notificationService.notifyPrivate(u, "LEAVE_APPROVED", "Đơn nghỉ phép đã được duyệt",
                                "Đơn từ " + saved.getTuNgay() + " đến " + saved.getDenNgay() + " đã được Director duyệt.",
                                "{\"leaveId\":" + saved.getId() + "}"));
            }
            return saved;
        }).orElseThrow(() -> new RuntimeException("Leave request not found"));
    }

    public DonNghiPhep rejectLeaveAsDirector(Long leaveId, String reason, NhanVien director) {
        if (director == null || director.getId() == null) {
            throw new SecurityException("Forbidden");
        }
        return donNghiPhepRepo.findById(leaveId).map(don -> {
            if (!"CHO_DUYET".equalsIgnoreCase(don.getTrangThai())) {
                throw new IllegalStateException("Leave request already processed");
            }
            if (don.getNguoiDuyet() == null || !director.getId().equals(don.getNguoiDuyet().getId())) {
                throw new SecurityException("Forbidden");
            }

            don.setTrangThai("TU_CHOI");
            if (reason != null && !reason.isBlank()) {
                don.setLyDo((don.getLyDo() == null ? "" : don.getLyDo() + " \n") + "Director từ chối: " + reason);
            }

            DonNghiPhep saved = donNghiPhepRepo.save(don);
            if (saved.getNhanVien() != null) {
                String detail = reason != null && !reason.isBlank() ? (" Lý do: " + reason) : "";
                notificationService.findUsernameByNhanVienId(saved.getNhanVien().getId()).ifPresent(u ->
                        notificationService.notifyPrivate(u, "LEAVE_REJECTED", "Đơn nghỉ phép bị từ chối",
                                "Đơn từ " + saved.getTuNgay() + " đến " + saved.getDenNgay() + " không được duyệt." + detail,
                                "{\"leaveId\":" + saved.getId() + "}"));
            }
            return saved;
        }).orElseThrow(() -> new RuntimeException("Leave request not found"));
    }

    public Map<String, Object> getHrStats() {
        long totalEmployees = nhanVienRepo.count();
        long totalDepartments = phongBanRepo.count();
        long pendingLeaves = donNghiPhepRepo.findByTrangThai("CHO_HR_XAC_NHAN").size();

        // Chấm công thiếu: giả định có trangThai không là DI_LAM/DI_MUON/DI_VANG
        long missingAttendance = chamCongRepo.findAll().stream()
                .filter(cc -> cc.getTrangThai() == null || "DI_VANG".equalsIgnoreCase(cc.getTrangThai()))
                .count();

        long unconfirmedSalary = phieuLuongRepo.findByTrangThaiThanhToan("CHUA_THANH_TOAN").size();

        Map<String, Object> data = new HashMap<>();
        data.put("totalEmployees", totalEmployees);
        data.put("totalDepartments", totalDepartments);
        data.put("pendingLeaves", pendingLeaves);
        data.put("missingAttendance", missingAttendance);
        data.put("unconfirmedSalary", unconfirmedSalary);
        return data;
    }

    // hr CRUD + workflow
    public List<NhanVien> getAllEmployees() {
        return nhanVienRepo.findAll();
    }

    public NhanVien getEmployeeById(String id) {
        return nhanVienRepo.findById(id).orElse(null);
    }

    public NhanVien createEmployee(NhanVien nv) {
        if (nv.getId() == null || nv.getId().isBlank()) {
            throw new IllegalArgumentException("Employee ID is required");
        }
        if (nhanVienRepo.existsById(nv.getId())) {
            throw new IllegalArgumentException("Employee already exists");
        }
        nv.setTrangThaiHoatDong("DANG_LAM_VIEC");
        nv.setSoNgayPhepConLai(nv.getSoNgayPhepConLai() == null ? 12 : nv.getSoNgayPhepConLai());
        return nhanVienRepo.save(nv);
    }

    /** Tạo nhân viên mới từ Map payload (FR HR Dashboard) — nhất quán với updateEmployee() */
    public NhanVien createEmployeeFromMap(Map<String, Object> payload) {
        String id = (String) payload.get("id");
        if (id == null || id.isBlank()) throw new IllegalArgumentException("Employee ID is required");

        NhanVien nv = new NhanVien();
        nv.setId(id.trim());

        String hoTen = (String) payload.get("hoTen");
        if (hoTen == null || hoTen.isBlank()) throw new IllegalArgumentException("Họ tên là bắt buộc");
        nv.setHoTen(hoTen);

        // UNIQUE fields — chỉ set khi có giá trị
        String soCccd = (String) payload.get("soCccd");
        if (soCccd != null && !soCccd.isBlank()) nv.setSoCccd(soCccd);

        String email = (String) payload.get("emailCongViec");
        if (email != null && !email.isBlank()) nv.setEmailCongViec(email);

        if (payload.containsKey("soDienThoai")) nv.setSoDienThoai((String) payload.get("soDienThoai"));
        if (payload.containsKey("gioiTinh"))    nv.setGioiTinh((String) payload.get("gioiTinh"));

        Object leave = payload.get("soNgayPhepConLai");
        nv.setSoNgayPhepConLai(leave instanceof Number ? ((Number) leave).intValue() : 12);

        String trangThai = (String) payload.get("trangThaiHoatDong");
        nv.setTrangThaiHoatDong(trangThai != null ? trangThai : "DANG_LAM_VIEC");

        // Vị trí tổ chức
        String pbId = (String) payload.get("phongBanId");
        if (pbId != null && !pbId.isBlank()) nv.setPhongBan(phongBanRepo.findById(pbId).orElse(null));

        String cvId = (String) payload.get("chucVuId");
        if (cvId != null && !cvId.isBlank()) nv.setChucVu(chucVuRepo.findById(cvId).orElse(null));

        String nhId = (String) payload.get("nhomId");
        if (nhId != null && !nhId.isBlank()) nv.setNhom(nhomRepo.findById(nhId).orElse(null));

        return createEmployee(nv);
    }

    public NhanVien updateEmployee(String id, Map<String, Object> payload) {
        return nhanVienRepo.findById(id).map(nv -> {
            // Chỉ update nếu giá trị hợp lệ (tránh ghi đè bằng blank)
            String hoTen = (String) payload.get("hoTen");
            if (hoTen != null && !hoTen.isBlank()) nv.setHoTen(hoTen);

            if (payload.containsKey("gioiTinh")) nv.setGioiTinh((String) payload.get("gioiTinh"));

            // UNIQUE fields: chỉ update khi có giá trị thực — để trống = giữ nguyên
            String soCccd = (String) payload.get("soCccd");
            if (soCccd != null && !soCccd.isBlank()) nv.setSoCccd(soCccd);

            String email = (String) payload.get("emailCongViec");
            if (email != null && !email.isBlank()) nv.setEmailCongViec(email);

            if (payload.containsKey("soDienThoai")) nv.setSoDienThoai((String) payload.get("soDienThoai"));
            if (payload.containsKey("diaChiTamTru")) nv.setDiaChiTamTru((String) payload.get("diaChiTamTru"));
            if (payload.containsKey("trangThaiHoatDong")) nv.setTrangThaiHoatDong((String) payload.get("trangThaiHoatDong"));

            if (payload.containsKey("soNgayPhepConLai")) {
                Object val = payload.get("soNgayPhepConLai");
                if (val instanceof Number) nv.setSoNgayPhepConLai(((Number) val).intValue());
            }

            if (payload.containsKey("ngayVaoLam")) {
                String ngayVaoLam = (String) payload.get("ngayVaoLam");
                if (ngayVaoLam != null && !ngayVaoLam.isBlank()) {
                    try { nv.setNgayVaoLam(java.time.LocalDate.parse(ngayVaoLam)); } catch (Exception ignored) {}
                }
            }

            // --- Gán Phòng ban, Chức vụ, Nhóm ---
            if (payload.containsKey("phongBanId")) {
                String pbId = (String) payload.get("phongBanId");
                nv.setPhongBan((pbId == null || pbId.isBlank()) ? null : phongBanRepo.findById(pbId).orElse(null));
            }
            if (payload.containsKey("chucVuId")) {
                String cvId = (String) payload.get("chucVuId");
                nv.setChucVu((cvId == null || cvId.isBlank()) ? null : chucVuRepo.findById(cvId).orElse(null));
            }
            if (payload.containsKey("nhomId")) {
                String nhId = (String) payload.get("nhomId");
                nv.setNhom((nhId == null || nhId.isBlank()) ? null : nhomRepo.findById(nhId).orElse(null));
            }
            return nhanVienRepo.save(nv);
        }).orElse(null);
    }

    public boolean deleteEmployee(String id) {
        if (!nhanVienRepo.existsById(id)) return false;
        nhanVienRepo.deleteById(id);
        return true;
    }

    public List<HrLeaveDto> getPendingLeaves() {
        return donNghiPhepRepo.findByTrangThai("CHO_DUYET").stream()
                .map(this::toHrLeaveDto)
                .collect(Collectors.toList());
    }

    public List<HrLeaveDto> getAllLeaves() {
        return donNghiPhepRepo.findAll().stream()
                .map(this::toHrLeaveDto)
                .collect(Collectors.toList());
    }

    public DonNghiPhep approveLeaveAsHr(Long leaveId) {
        return donNghiPhepRepo.findById(leaveId).map(don -> {
            if (!"CHO_DUYET".equalsIgnoreCase(don.getTrangThai())) {
                throw new IllegalStateException("Leave request already processed");
            }
            long soNgayNghi = java.time.temporal.ChronoUnit.DAYS.between(don.getTuNgay(), don.getDenNgay()) + 1;
            if ("PH_NAM".equalsIgnoreCase(don.getLoaiNghi())) {
                NhanVien nv = don.getNhanVien();
                int cap = nv.getSoNgayPhepConLai() == null ? 0 : nv.getSoNgayPhepConLai();
                if (cap < soNgayNghi) {
                    throw new IllegalArgumentException("Nhân viên không đủ ngày phép");
                }
                nv.setSoNgayPhepConLai(cap - (int) soNgayNghi);
                nhanVienRepo.save(nv);
            }
            don.setTrangThai("DA_DUYET");
            DonNghiPhep saved = donNghiPhepRepo.save(don);
            if (saved.getNhanVien() != null) {
                notificationService.findUsernameByNhanVienId(saved.getNhanVien().getId()).ifPresent(u ->
                        notificationService.notifyPrivate(u, "LEAVE_APPROVED", "Đơn nghỉ phép đã được duyệt",
                                "Đơn từ " + saved.getTuNgay() + " đến " + saved.getDenNgay() + " đã được HR duyệt.",
                                "{\"leaveId\":" + saved.getId() + "}"));
            }
            return saved;
        }).orElseThrow(() -> new RuntimeException("Leave request not found"));
    }

    public DonNghiPhep rejectLeaveAsHr(Long leaveId, String reason) {
        return donNghiPhepRepo.findById(leaveId).map(don -> {
            if (!"CHO_DUYET".equalsIgnoreCase(don.getTrangThai())) {
                throw new IllegalStateException("Leave request already processed");
            }
            don.setTrangThai("TU_CHOI");
            // optional: reason saved to lyDo or another field
            if (reason != null && !reason.isBlank()) {
                don.setLyDo((don.getLyDo() == null ? "" : don.getLyDo() + " \n") + "HR từ chối: " + reason);
            }
            DonNghiPhep saved = donNghiPhepRepo.save(don);
            if (saved.getNhanVien() != null) {
                String detail = reason != null && !reason.isBlank() ? (" Lý do: " + reason) : "";
                notificationService.findUsernameByNhanVienId(saved.getNhanVien().getId()).ifPresent(u ->
                        notificationService.notifyPrivate(u, "LEAVE_REJECTED", "Đơn nghỉ phép bị từ chối",
                                "Đơn từ " + saved.getTuNgay() + " đến " + saved.getDenNgay() + " không được duyệt." + detail,
                                "{\"leaveId\":" + saved.getId() + "}"));
            }
            return saved;
        }).orElseThrow(() -> new RuntimeException("Leave request not found"));
    }

    public boolean deleteLeaveRequest(Long id) {
        if (!donNghiPhepRepo.existsById(id)) return false;
        donNghiPhepRepo.deleteById(id);
        return true;
    }

    public List<PhieuLuong> getAllPayslips(String monthYear) {
        if (monthYear == null || monthYear.isBlank()) {
            return phieuLuongRepo.findAll().stream()
                    .peek(p -> p.setThangNam(PhieuLuong.normalizeThangNam(p.getThangNam())))
                    .collect(Collectors.toList());
        }
        String normalized = PhieuLuong.normalizeThangNam(monthYear);
        return phieuLuongRepo.findByThangNamIn(monthAliases(normalized)).stream()
                .peek(p -> p.setThangNam(PhieuLuong.normalizeThangNam(p.getThangNam())))
                .collect(Collectors.toList());
    }

    public PhieuLuong confirmPayslip(String payslipId) {
        return phieuLuongRepo.findById(payslipId).map(p -> {
            p.setTrangThaiThanhToan("DA_THANH_TOAN");
            PhieuLuong saved = phieuLuongRepo.save(p);
            if (saved.getNhanVien() != null) {
                notificationService.findUsernameByNhanVienId(saved.getNhanVien().getId()).ifPresent(u ->
                        notificationService.notifyPrivate(u, "PAYSLIP_READY", "Phiếu lương đã chốt",
                                "Phiếu lương tháng " + saved.getThangNam() + " đã được xác nhận thanh toán.",
                                "{\"payslipId\":\"" + saved.getId() + "\"}"));
            }
            return saved;
        }).orElse(null);
    }

    public Map<String, Object> getAttendanceSummary(String monthYear) {
        // monthYear format: YYYY-MM
        LocalDate now = LocalDate.now();
        YearMonth target = monthYear != null && !monthYear.isBlank() ? YearMonth.parse(monthYear) : YearMonth.from(now);
        LocalDate from = target.atDay(1);
        LocalDate to = target.atEndOfMonth();

        long total = 0;
        long onTime = 0;
        long late = 0;
        long permitted = 0;
        long absent = 0;

        List<ChamCong> all = chamCongRepo.findAll();
        Map<String, List<ChamCong>> grouped = all.stream()
                .filter(cc -> !cc.getNgay().isBefore(from) && !cc.getNgay().isAfter(to))
                .collect(Collectors.groupingBy(cc -> cc.getNhanVien().getId()));

        for (var entry : grouped.entrySet()) {
            for (ChamCong cc : entry.getValue()) {
                total++;
                String status = cc.getTrangThai();
                if ("DI_LAM".equalsIgnoreCase(status)) onTime++;
                else if ("DI_MUON".equalsIgnoreCase(status)) late++;
                else if ("PHEP".equalsIgnoreCase(status) || "NGHI_PHEP".equalsIgnoreCase(status)) permitted++;
                else absent++;
            }
        }

        Map<String, Object> summary = new HashMap<>();
        summary.put("presentPct", total == 0 ? 0 : Math.round((onTime * 100.0) / total));
        summary.put("latePct", total == 0 ? 0 : Math.round((late * 100.0) / total));
        summary.put("permittedPct", total == 0 ? 0 : Math.round((permitted * 100.0) / total));
        summary.put("absentPct", total == 0 ? 0 : Math.round((absent * 100.0) / total));
        return summary;
    }

    public List<Map<String, Object>> getTopLateEmployees(String monthYear, int limit) {
        LocalDate now = LocalDate.now();
        YearMonth target = monthYear != null && !monthYear.isBlank() ? YearMonth.parse(monthYear) : YearMonth.from(now);
        LocalDate from = target.atDay(1);
        LocalDate to = target.atEndOfMonth();

        List<ChamCong> records = chamCongRepo.findAll().stream()
                .filter(cc -> !cc.getNgay().isBefore(from) && !cc.getNgay().isAfter(to))
                .filter(cc -> "DI_MUON".equalsIgnoreCase(cc.getTrangThai()))
                .collect(Collectors.toList());

        Map<String, Map<String, Object>> aggregate = new java.util.HashMap<>();
        for (ChamCong cc : records) {
            String id = cc.getNhanVien().getId();
            String ten = cc.getNhanVien().getHoTen();
            String phong = cc.getNhanVien().getPhongBan() != null ? cc.getNhanVien().getPhongBan().getTenPhongBan() : "—";
            String chucVu = cc.getNhanVien().getChucVu() != null ? cc.getNhanVien().getChucVu().getTenChucVu() : "—";
            int muonPhut = calculateLateMinutes(cc);
            int tienPhat = (int) Math.round(muonPhut * (50000.0 / 60.0)); // 50k / giờ

            aggregate.compute(id, (k,v)->{
                if (v==null) {
                    Map<String,Object> m=new HashMap<>();
                    m.put("nhanVienId", id);
                    m.put("tenNV", ten);
                    m.put("tenPhong", phong);
                    m.put("chucVu", chucVu);
                    m.put("soLanMuon", 1);
                    m.put("tongPhutMuon", muonPhut);
                    m.put("tienPhat", tienPhat);
                    return m;
                }
                v.put("soLanMuon", ((int)v.get("soLanMuon")) + 1);
                v.put("tongPhutMuon", ((int)v.get("tongPhutMuon")) + muonPhut);
                v.put("tienPhat", ((int)v.get("tienPhat")) + tienPhat);
                return v;
            });
        }

        List<Map<String,Object>> list = new java.util.ArrayList<>(aggregate.values());
        list.sort((a,b) -> Integer.compare((int)b.get("soLanMuon"), (int)a.get("soLanMuon")));
        if (list.size() > limit) return list.subList(0, limit);
        return list;
    }

    public List<Map<String, Object>> getRecruitmentCandidates(String monthYear) {
        YearMonth ym = (monthYear != null && !monthYear.isBlank()) ? YearMonth.parse(monthYear) : null;
        return hoSoUngVienRepo.findAll().stream()
                .filter(c -> {
                    if (ym == null) return true;
                    if (c.getNgayNop() == null) return false;
                    return YearMonth.from(c.getNgayNop()).equals(ym);
                })
                .map(c -> {
                    Map<String, Object> row = new HashMap<>();
                    row.put("id", c.getId());
                    row.put("hoTen", c.getHoTen());
                    row.put("email", c.getEmail());
                    row.put("soDienThoai", c.getSoDienThoai());
                    row.put("cvUrl", c.getCvUrl());
                    row.put("trangThai", c.getTrangThai());
                    row.put("ngayNop", c.getNgayNop() != null ? c.getNgayNop().toLocalDate().toString() : "");
                    row.put("viTri", c.getYeuCauTuyenDung() != null ? c.getYeuCauTuyenDung().getViTriCanTuyen() : "");
                    row.put("phongBan", c.getYeuCauTuyenDung() != null && c.getYeuCauTuyenDung().getPhongBan() != null
                            ? c.getYeuCauTuyenDung().getPhongBan().getTenPhongBan() : "");
                    return row;
                }).collect(Collectors.toList());
    }

    public List<Map<String, Object>> getRecruitmentRequests(String monthYear) {
        YearMonth ym = (monthYear != null && !monthYear.isBlank()) ? YearMonth.parse(monthYear) : null;
        return yeuCauTuyenDungRepo.findAll().stream()
                .filter(r -> {
                    if (ym == null) return true;
                    if (r.getNgayYeuCau() == null) return false;
                    return YearMonth.from(r.getNgayYeuCau()).equals(ym);
                })
                .map(r -> {
                    Map<String, Object> row = new HashMap<>();
                    row.put("idYeuCau", r.getIdYeuCau());
                    row.put("viTriCanTuyen", r.getViTriCanTuyen());
                    row.put("soLuong", r.getSoLuong());
                    row.put("trangThai", r.getTrangThai());
                    row.put("ngayYeuCau", r.getNgayYeuCau() != null ? r.getNgayYeuCau().toString() : "");
                    row.put("phongBan", r.getPhongBan() != null ? r.getPhongBan().getTenPhongBan() : "");
                    return row;
                }).collect(Collectors.toList());
    }

    private HrLeaveDto toHrLeaveDto(DonNghiPhep don) {
        NhanVien nv = don.getNhanVien();
        String tenNV = nv != null ? nv.getHoTen() : "";
        String tenPhong = (nv != null && nv.getPhongBan() != null) ? nv.getPhongBan().getTenPhongBan() : "—";
        String chucVu = (nv != null && nv.getChucVu() != null) ? nv.getChucVu().getTenChucVu() : "—";
        long soNgay = (don.getTuNgay() != null && don.getDenNgay() != null)
                ? java.time.temporal.ChronoUnit.DAYS.between(don.getTuNgay(), don.getDenNgay()) + 1
                : 0;
        return new HrLeaveDto(
                don.getId(),
                tenNV,
                tenPhong,
                chucVu,
                mapLeaveType(don.getLoaiNghi()),
                don.getTuNgay() != null ? don.getTuNgay().toString() : "",
                don.getDenNgay() != null ? don.getDenNgay().toString() : "",
                soNgay,
                don.getTuNgay() != null ? don.getTuNgay().toString() : "",
                "APPROVED",
                don.getTrangThai()
        );
    }

    private String mapLeaveType(String loaiNghi) {
        if (loaiNghi == null) return "KHAC";
        return switch (loaiNghi.toUpperCase()) {
            case "PH_NAM", "PHEP_NAM" -> "PHEP_NAM";
            case "NGHI_OM", "PHEP_OM" -> "PHEP_OM";
            case "NGHI_LE" -> "NGHI_LE";
            default -> "KHAC";
        };
    }

    private int calculateLateMinutes(ChamCong cc) {
        if (cc.getGioVao() == null) return 0;
        LocalTime threshold = LocalTime.of(8, 0);
        if (!cc.getGioVao().isAfter(threshold)) return 0;
        return (int) java.time.Duration.between(threshold, cc.getGioVao()).toMinutes();
    }

    public Map<String, Object> getAdminStats() {
        long totalAccounts = taiKhoanRepo.count();
        long activeAccounts = taiKhoanRepo.findAll().stream().filter(TaiKhoan::isTrangThaiTaiKhoan).count();
        long lockedAccounts = totalAccounts - activeAccounts;
        long unassignedAccounts = taiKhoanRepo.findAll().stream().filter(t -> t.getNhanVien() == null).count();

        Map<String, Object> data = new HashMap<>();
        data.put("totalAccounts", totalAccounts);
        data.put("activeAccounts", activeAccounts);
        data.put("lockedAccounts", lockedAccounts);
        data.put("unassignedAccounts", unassignedAccounts);
        data.put("newThisMonth", adminService.countNewAccountsThisMonth());
        return data;
    }
}
