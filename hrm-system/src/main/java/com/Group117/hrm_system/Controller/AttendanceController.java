package com.Group117.hrm_system.Controller;

import com.Group117.hrm_system.entity.ChamCong;
import com.Group117.hrm_system.entity.NhanVien;
import com.Group117.hrm_system.entity.DonNghiPhep;
import com.Group117.hrm_system.Repository.ChamCongRepository;
import com.Group117.hrm_system.Repository.NhanVienRepository;
import com.Group117.hrm_system.Repository.DonNghiPhepRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/attendance")
@CrossOrigin(origins = "*")
public class AttendanceController {

    @Autowired
    private ChamCongRepository chamCongRepo;

    @Autowired
    private NhanVienRepository nhanVienRepo;

    @Autowired
    private DonNghiPhepRepository nghiPhepRepo;

    // --- 1. CHẤM CÔNG (Check-in/out) ---

    @PostMapping("/check-in/{nhanVienId}")
    public ResponseEntity<?> checkIn(@PathVariable String nhanVienId) {
        try {
            if (chamCongRepo.findByNhanVienIdAndNgay(nhanVienId, LocalDate.now()).isPresent()) {
                return ResponseEntity.badRequest().body("Bạn đã check-in ngày hôm nay rồi!");
            }

            NhanVien nv = nhanVienRepo.findById(nhanVienId)
                    .orElseThrow(() -> new RuntimeException("Không tìm thấy nhân viên"));

            ChamCong cc = new ChamCong();
            cc.setNhanVien(nv);
            cc.setGioVao(LocalTime.now());

            // Logic: Sau 08:15 là đi muộn
            if (cc.getGioVao().isAfter(LocalTime.of(8, 15))) {
                cc.setTrangThai("DI_MUON");
            } else {
                cc.setTrangThai("DI_LAM");
            }

            chamCongRepo.save(cc);
            return ResponseEntity.ok("Check-in thành công! Trạng thái: " + cc.getTrangThai());
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Lỗi: " + e.getMessage());
        }
    }

    @PostMapping("/check-out/{nhanVienId}")
    public ResponseEntity<?> checkOut(@PathVariable String nhanVienId) {
        try {
            ChamCong cc = chamCongRepo.findByNhanVienIdAndNgay(nhanVienId, LocalDate.now())
                    .orElseThrow(() -> new RuntimeException("Chưa có bản ghi check-in hôm nay!"));

            cc.setGioRa(LocalTime.now());
            chamCongRepo.save(cc);
            return ResponseEntity.ok("Check-out thành công lúc " + cc.getGioRa());
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Lỗi: " + e.getMessage());
        }
    }

    // --- BỔ SUNG: SỬA BẢN GHI CHẤM CÔNG (CRUD) ---
    @PutMapping("/record/{id}")
    public ResponseEntity<?> updateAttendance(@PathVariable Long id, @RequestBody ChamCong details) {
        return chamCongRepo.findById(id).map(cc -> {
            cc.setGioVao(details.getGioVao());
            cc.setGioRa(details.getGioRa());
            cc.setTrangThai(details.getTrangThai());
            return ResponseEntity.ok(chamCongRepo.save(cc));
        }).orElse(ResponseEntity.notFound().build());
    }

    // --- 2. NGHỈ PHÉP (Workflow & Leave Balance) ---

    @PostMapping("/leave-request")
    public ResponseEntity<?> createLeaveRequest(@RequestBody DonNghiPhep request) {
        try {
            NhanVien nv = nhanVienRepo.findById(request.getNhanVien().getId())
                    .orElseThrow(() -> new RuntimeException("Nhân viên không tồn tại"));

            // Workflow: Tìm người quản lý trực tiếp
            NhanVien manager = nv.getNguoiQuanLyTruocTiep();
            if (manager == null) {
                manager = nhanVienRepo.findById("GD001").orElse(null);
            }

            request.setNhanVien(nv);
            request.setNguoiDuyet(manager);
            request.setTrangThai("CHO_DUYET");

            nghiPhepRepo.save(request);
            return ResponseEntity.ok("Gửi đơn thành công! Chờ " + (manager != null ? manager.getHoTen() : "Ban Giám Đốc") + " duyệt.");
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Lỗi: " + e.getMessage());
        }
    }

    @PostMapping("/approve-leave/{donId}")
    @Transactional
    public ResponseEntity<?> approveLeave(@PathVariable Long donId) {
        try {
            DonNghiPhep don = nghiPhepRepo.findById(donId)
                    .orElseThrow(() -> new RuntimeException("Không tìm thấy đơn nghỉ phép"));

            if (!"CHO_DUYET".equals(don.getTrangThai())) {
                return ResponseEntity.badRequest().body("Đơn này đã được xử lý.");
            }

            long soNgayNghi = ChronoUnit.DAYS.between(don.getTuNgay(), don.getDenNgay()) + 1;

            // Leave Balance Logic: Trừ quỹ phép năm
            if ("PH_NAM".equals(don.getLoaiNghi())) {
                NhanVien nv = don.getNhanVien();
                // Phòng thủ lỗi null quỹ phép
                int quyPhapHienTai = (nv.getSoNgayPhepConLai() != null) ? nv.getSoNgayPhepConLai() : 0;

                if (quyPhapHienTai < soNgayNghi) {
                    return ResponseEntity.badRequest().body("Nhân viên không đủ ngày phép!");
                }
                nv.setSoNgayPhepConLai(quyPhapHienTai - (int) soNgayNghi);
                nhanVienRepo.save(nv);
            }

            don.setTrangThai("DA_DUYET");
            nghiPhepRepo.save(don);

            return ResponseEntity.ok("Đã duyệt đơn và trừ " + soNgayNghi + " ngày phép của " + don.getNhanVien().getHoTen());
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Lỗi: " + e.getMessage());
        }
    }

    // --- BỔ SUNG: XÓA ĐƠN NGHỈ PHÉP (CRUD) ---
    @DeleteMapping("/leave-request/{id}")
    public ResponseEntity<?> deleteLeaveRequest(@PathVariable Long id) {
        try {
            DonNghiPhep don = nghiPhepRepo.findById(id)
                    .orElseThrow(() -> new RuntimeException("Không tìm thấy đơn"));

            // Chỉ cho phép xóa nếu đơn chưa được duyệt
            if (!"CHO_DUYET".equals(don.getTrangThai())) {
                return ResponseEntity.badRequest().body("Không thể xóa đơn đã được phê duyệt!");
            }

            nghiPhepRepo.deleteById(id);
            return ResponseEntity.ok("Đã xóa đơn nghỉ phép ID: " + id);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Lỗi: " + e.getMessage());
        }
    }

    // --- 3. BÁO CÁO CÁ NHÂN (Personal Report) ---

    @GetMapping("/report/{nhanVienId}")
    public ResponseEntity<?> getPersonalReport(@PathVariable String nhanVienId) {
        try {
            LocalDate start = LocalDate.now().withDayOfMonth(1);
            LocalDate end = LocalDate.now().withDayOfMonth(LocalDate.now().lengthOfMonth());

            List<ChamCong> records = chamCongRepo.findAll().stream()
                    .filter(cc -> cc.getNhanVien().getId().equals(nhanVienId))
                    .filter(cc -> !cc.getNgay().isBefore(start) && !cc.getNgay().isAfter(end))
                    .collect(Collectors.toList());

            long diMuon = records.stream().filter(r -> "DI_MUON".equals(r.getTrangThai())).count();
            long dungGio = records.stream().filter(r -> "DI_LAM".equals(r.getTrangThai())).count();

            Map<String, Object> report = new HashMap<>();
            report.put("nhanVienId", nhanVienId);
            report.put("thang", LocalDate.now().getMonthValue());
            report.put("tongBuoiChamCong", records.size());
            report.put("diMuon", diMuon);
            report.put("dungGio", dungGio);
            report.put("lichSuChiTiet", records);

            return ResponseEntity.ok(report);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Lỗi báo cáo: " + e.getMessage());
        }
    }
}