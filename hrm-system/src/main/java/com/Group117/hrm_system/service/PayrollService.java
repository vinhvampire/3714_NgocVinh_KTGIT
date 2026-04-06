package com.Group117.hrm_system.service;

import com.Group117.hrm_system.Repository.NhanVienRepository;
import com.Group117.hrm_system.Repository.PhieuLuongRepository;
import com.Group117.hrm_system.Repository.ChamCongRepository;
import com.Group117.hrm_system.Repository.DonNghiPhepRepository;
import com.Group117.hrm_system.entity.ChamCong;
import com.Group117.hrm_system.entity.DonNghiPhep;
import com.Group117.hrm_system.entity.NhanVien;
import com.Group117.hrm_system.entity.PhieuLuong;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.YearMonth;
import java.time.temporal.ChronoUnit;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class PayrollService {

    @Autowired
    private PhieuLuongRepository phieuLuongRepository;

    @Autowired
    private NhanVienRepository nhanVienRepository;

    @Autowired
    private ChamCongRepository chamCongRepository;

    @Autowired
    private DonNghiPhepRepository donNghiPhepRepository;

    // 1. Quét dữ liệu và tổng hợp lương cuối tháng cho toàn bộ nhân viên
    public void scanAndCalculatePayroll(String thangNam) {
        String normalizedThangNam = PhieuLuong.normalizeThangNam(thangNam);
        String[] parts = normalizedThangNam.split("-");
        int year = Integer.parseInt(parts[0]);
        int month = Integer.parseInt(parts[1]);
        LocalDate startOfMonth = LocalDate.of(year, month, 1);
        LocalDate endOfMonth = startOfMonth.withDayOfMonth(startOfMonth.lengthOfMonth());
        LocalDate today = LocalDate.now();
        // Nếu đang tổng hợp cho tháng hiện tại, chỉ tính tới ngày hiện tại để tránh phạt các ngày tương lai.
        LocalDate effectiveEnd = YearMonth.from(startOfMonth).equals(YearMonth.from(today))
                ? (today.isBefore(endOfMonth) ? today : endOfMonth)
                : endOfMonth;
        LocalTime GIO_CHUAN = LocalTime.of(8, 0);

        List<NhanVien> allEmployees = nhanVienRepository.findAll();

        for (NhanVien nv : allEmployees) {
            if (nv.getBangLuong() == null) {
                continue;
            }
            // Không tạo/tính phiếu lương cho các tháng trước khi nhân viên bắt đầu làm.
            // Rule: nếu ngày vào làm nằm SAU tháng đang tính (YYYY-MM) thì bỏ qua.
            if (nv.getNgayVaoLam() != null
                    && YearMonth.from(nv.getNgayVaoLam()).isAfter(YearMonth.from(startOfMonth))) {
                continue;
            }

            List<PhieuLuong> existing = phieuLuongRepository.findByNhanVienIdAndThangNamIn(
                    nv.getId(),
                    monthAliases(normalizedThangNam)
            );
            // Nếu đã có phiếu lương tháng này thì cập nhật lại theo logic mới (để sửa dữ liệu sai).
            // Nếu chưa có thì tạo mới như bình thường.
            PhieuLuong phieuLuong = existing.isEmpty() ? new PhieuLuong() : existing.get(0);

            List<ChamCong> chamCongs = chamCongRepository
                    .findByNhanVienIdAndNgayBetween(nv.getId(), startOfMonth, effectiveEnd);

            double tongPhutMuon = chamCongs.stream()
                    .filter(c -> "DI_MUON".equals(c.getTrangThai()))
                    .filter(c -> c.getGioVao() != null && c.getGioVao().isAfter(GIO_CHUAN))
                    .mapToDouble(c -> ChronoUnit.MINUTES.between(GIO_CHUAN, c.getGioVao()))
                    .sum();

            // Bước 1: Danh sách ngày làm việc trong khoảng tính lương (T2-T6)
            List<LocalDate> ngayLamViecTrongThang = startOfMonth.datesUntil(effectiveEnd.plusDays(1))
                    .filter(d -> d.getDayOfWeek().getValue() <= 5)
                    .collect(Collectors.toList());

            // Bước 2: Tập ngày đã chấm công hợp lệ (DI_LAM / DI_MUON), ép lại range
            Set<LocalDate> ngayDaChamCong = chamCongs.stream()
                    .filter(c -> {
                        String st = c.getTrangThai() == null ? "" : c.getTrangThai().toUpperCase();
                        return "DI_LAM".equals(st) || "DI_MUON".equals(st);
                    })
                    .map(ChamCong::getNgay)
                    .filter(d -> d != null && !d.isBefore(startOfMonth) && !d.isAfter(effectiveEnd))
                    .collect(Collectors.toSet());

            // Bước 3: Tập ngày nghỉ phép hợp lệ đã duyệt (giao với tháng, chỉ T2-T6)
            Set<LocalDate> ngayPhepSet = new HashSet<>();
            donNghiPhepRepository.findByNhanVien(nv).stream()
                    .filter(d -> "DA_DUYET".equals(d.getTrangThai()))
                    .filter(d -> d.getTuNgay() != null)
                    .forEach(don -> {
                        LocalDate from = don.getTuNgay();
                        LocalDate to = don.getDenNgay() != null ? don.getDenNgay() : don.getTuNgay();
                        if (from == null || to == null) return;
                        if (to.isBefore(startOfMonth) || from.isAfter(effectiveEnd)) return;
                        LocalDate overlapStart = from.isBefore(startOfMonth) ? startOfMonth : from;
                        LocalDate overlapEnd = to.isAfter(effectiveEnd) ? effectiveEnd : to;
                        overlapStart.datesUntil(overlapEnd.plusDays(1))
                                .filter(day -> day.getDayOfWeek().getValue() <= 5)
                                .forEach(ngayPhepSet::add);
                    });

            // Bước 4: Đếm số ngày vắng không phép theo tập ngày làm việc
            long soNgayVangKhongPhep = ngayLamViecTrongThang.stream()
                    .filter(day -> !ngayDaChamCong.contains(day) && !ngayPhepSet.contains(day))
                    .count();

            double luongCB = normalizeMoneyScale(nv.getBangLuong().getLuongCoBan());
            double phuCap = normalizeMoneyScale(nv.getBangLuong().getPhuCapDinhMuc());
            double luongNgay = luongCB / 26.0;

            double tienPhatMuon = tongPhutMuon * 2000;
            // Bước 5: Giới hạn tiền phạt nghỉ tối đa bằng lương cơ bản
            double tienPhatNghi = Math.min(soNgayVangKhongPhep * luongNgay, luongCB);
            double tongLuong = Math.max(0d, (luongCB + phuCap) - tienPhatMuon - tienPhatNghi);

            if (phieuLuong.getId() == null || phieuLuong.getId().isBlank()) {
                phieuLuong.setId("PL_" + nv.getId() + "_" + String.format("%02d", month) + year);
            }
            phieuLuong.setNhanVien(nv);
            phieuLuong.setThangNam(normalizedThangNam);
            phieuLuong.setLuongCoBan(luongCB);
            phieuLuong.setPhuCap(phuCap);
            phieuLuong.setPhatMuon(roundVnd(tienPhatMuon));
            phieuLuong.setNghiKhongPhep(roundVnd(tienPhatNghi));
            phieuLuong.setTongLuong(roundVnd(tongLuong));
            if (phieuLuong.getTrangThaiThanhToan() == null || phieuLuong.getTrangThaiThanhToan().isBlank()) {
                phieuLuong.setTrangThaiThanhToan("CHUA_THANH_TOAN");
            }

            phieuLuongRepository.save(phieuLuong);
        }
    }

    /**
     * Fix trường hợp dữ liệu lương/phụ cấp bị lệch scale (thường do lưu nhầm đơn vị).
     * Nếu giá trị > 1 tỷ VNĐ thì nhiều khả năng đã bị nhân 1000 (ví dụ: 25.000.000 → 25.000.000.000).
     */
    private double normalizeMoneyScale(Double raw) {
        if (raw == null) return 0d;
        double v = raw;
        if (Double.isNaN(v) || Double.isInfinite(v)) return 0d;
        if (v < 0d) return 0d;
        if (v > 1_000_000_000d) {
            v = v / 1000d;
        }
        return v;
    }

    private double roundVnd(double v) {
        return Math.round(v);
    }

    private List<String> monthAliases(String normalized) {
        String canonical = PhieuLuong.normalizeThangNam(normalized);
        if (canonical == null || !canonical.contains("-")) return List.of(normalized);
        String[] p = canonical.split("-");
        String legacy = p[1] + "/" + p[0];
        return List.of(canonical, legacy);
    }

    // 2. Cập nhật trạng thái thanh toán (Payslip Management)
    public PhieuLuong updatePaymentStatus(String id, String status) {
        PhieuLuong existingPhieu = phieuLuongRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy phiếu lương với mã: " + id));

        existingPhieu.setTrangThaiThanhToan(status);
        return phieuLuongRepository.save(existingPhieu);
    }

    // 3. Lấy phiếu lương theo ID
    public PhieuLuong getPayrollById(String id) {
        return phieuLuongRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy phiếu lương: " + id));
    }

    // 4. Lấy danh sách phiếu lương của một nhân viên
    public List<PhieuLuong> getPayrollByEmployee(String employeeId) {
        return phieuLuongRepository.findByNhanVienId(employeeId);
    }
}
