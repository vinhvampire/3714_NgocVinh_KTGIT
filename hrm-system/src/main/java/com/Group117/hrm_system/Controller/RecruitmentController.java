package com.Group117.hrm_system.Controller;

import com.Group117.hrm_system.entity.YeuCauTuyenDung;
import com.Group117.hrm_system.entity.HoSoUngVien;
import com.Group117.hrm_system.entity.LichPhongVan;
import com.Group117.hrm_system.entity.NhanVien;
import com.Group117.hrm_system.Repository.YeuCauTuyenDungRepository;
import com.Group117.hrm_system.Repository.HoSoUngVienRepository;
import com.Group117.hrm_system.Repository.LichPhongVanRepository;
import com.Group117.hrm_system.Repository.NhanVienRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.Collections;
import java.util.stream.Collectors;
import org.springframework.security.access.prepost.PreAuthorize;

@RestController
@RequestMapping("/api/recruitment")
@CrossOrigin(origins = "*")
public class RecruitmentController {

    @Autowired
    private YeuCauTuyenDungRepository yeuCauRepo;

    @Autowired
    private HoSoUngVienRepository candidateRepo;

    @Autowired
    private LichPhongVanRepository interviewRepo;

    @Autowired
    private NhanVienRepository nhanVienRepo;

    // --- GIỮ NGUYÊN CÁC API CŨ (1-6) ---

    @PostMapping("/hiring-request")
    public ResponseEntity<?> createHiringRequest(@RequestBody YeuCauTuyenDung request) {
        try {
            if (request.getIdYeuCau() == null || request.getIdYeuCau().isEmpty()) {
                request.setIdYeuCau("REQ_" + System.currentTimeMillis());
            }
            YeuCauTuyenDung saved = yeuCauRepo.save(request);
            return ResponseEntity.ok(saved);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Lỗi gửi yêu cầu: " + e.getMessage());
        }
    }

    @GetMapping("/all-requests")
    public ResponseEntity<List<YeuCauTuyenDung>> getAllRequests() {
        return ResponseEntity.ok(yeuCauRepo.findAll());
    }

    @PostMapping("/apply")
    public ResponseEntity<?> applyJob(@RequestBody HoSoUngVien candidate) {
        try {
            HoSoUngVien saved = candidateRepo.save(candidate);
            return ResponseEntity.ok("Nộp hồ sơ thành công! ID ứng viên: " + saved.getId());
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Lỗi nộp hồ sơ: " + e.getMessage());
        }
    }

    @GetMapping("/candidates")
    public ResponseEntity<?> getAllCandidates() {
        try {
            return ResponseEntity.ok(candidateRepo.findAll());
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body("Lỗi lấy danh sách ứng viên: " + e.getMessage());
        }
    }

    @PostMapping("/schedule-interview")
    public ResponseEntity<?> scheduleInterview(@RequestBody LichPhongVan schedule) {
        try {
            if (schedule.getIdLich() == null || schedule.getIdLich().isEmpty()) {
                schedule.setIdLich("IVW_" + System.currentTimeMillis());
            }
            LichPhongVan saved = interviewRepo.save(schedule);
            return ResponseEntity.ok("Lên lịch phỏng vấn thành công! ID Lịch: " + saved.getIdLich());
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Lỗi khi lập lịch phỏng vấn: " + e.getMessage());
        }
    }

    @PostMapping("/approve/{id}")
    @Transactional
    public ResponseEntity<?> approveCandidate(@PathVariable Integer id) {
        try {
            HoSoUngVien candidate = candidateRepo.findById(id)
                    .orElseThrow(() -> new RuntimeException("Không tìm thấy ứng viên ID: " + id));
            NhanVien nv = new NhanVien();
            nv.setId("NV" + (System.currentTimeMillis() / 1000));
            nv.setHoTen(candidate.getHoTen());
            nv.setEmailCongViec(candidate.getEmail());
            nv.setSoDienThoai(candidate.getSoDienThoai());
            nv.setNgayVaoLam(LocalDate.now());
            nv.setTrangThaiHoatDong("DANG_LAM_VIEC");
            nv.setHeSoLuong(1.0f);
            if (candidate.getYeuCauTuyenDung() != null) {
                nv.setPhongBan(candidate.getYeuCauTuyenDung().getPhongBan());
            }
            nhanVienRepo.save(nv);
            candidate.setTrangThai("TRUNG_TUYEN");
            candidateRepo.save(candidate);
            return ResponseEntity.ok("Đã phê duyệt Onboarding thành công cho: " + nv.getHoTen());
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Lỗi Onboarding: " + e.getMessage());
        }
    }

    // --- BỔ SUNG CÁC CHỨC NĂNG CÒN THIẾU (CRUD) ---

    // Cập nhật thông tin ứng viên
    @PutMapping("/candidate/{id}")
    public ResponseEntity<?> updateCandidate(@PathVariable Integer id, @RequestBody HoSoUngVien details) {
        return candidateRepo.findById(id).map(uv -> {
            uv.setHoTen(details.getHoTen());
            uv.setEmail(details.getEmail());
            uv.setSoDienThoai(details.getSoDienThoai());
            uv.setCvUrl(details.getCvUrl());
            uv.setTrangThai(details.getTrangThai());
            return ResponseEntity.ok(candidateRepo.save(uv));
        }).orElse(ResponseEntity.notFound().build());
    }

    // HR/Admin: Xem chi tiết hồ sơ ứng viên + lịch phỏng vấn liên quan
    @GetMapping("/candidate/{id}")
    @PreAuthorize("hasAnyRole('HR', 'ADMIN')")
    public ResponseEntity<?> getCandidateDetail(@PathVariable Integer id) {
        try {
            HoSoUngVien uv = candidateRepo.findById(id)
                    .orElseThrow(() -> new RuntimeException("Không tìm thấy ứng viên"));

            Map<String, Object> out = new HashMap<>();
            out.put("id", uv.getId());
            out.put("hoTen", uv.getHoTen());
            out.put("email", uv.getEmail());
            out.put("soDienThoai", uv.getSoDienThoai());
            out.put("cvUrl", uv.getCvUrl());
            out.put("trangThai", uv.getTrangThai());
            out.put("ngayNop", uv.getNgayNop());
            out.put("yeuCauTuyenDung", uv.getYeuCauTuyenDung());

            List<Map<String, Object>> interviews = interviewRepo.findByUngVien_Id(id).stream().map(iv -> {
                Map<String, Object> m = new HashMap<>();
                m.put("idLich", iv.getIdLich());
                m.put("thoiGian", iv.getThoiGian());
                m.put("diaDiem", iv.getDiaDiem());
                m.put("nguoiPhongVan", iv.getNguoiPhongVan());
                m.put("ghiChu", iv.getGhiChu());
                return m;
            }).collect(Collectors.toList());
            out.put("interviews", interviews);

            return ResponseEntity.ok(out);
        } catch (Exception e) {
            return ResponseEntity.status(404).body(Map.of("message", e.getMessage()));
        }
    }

    // Xóa hồ sơ ứng viên
    @DeleteMapping("/candidate/{id}")
    public ResponseEntity<?> deleteCandidate(@PathVariable Integer id) {
        try {
            candidateRepo.deleteById(id);
            return ResponseEntity.ok("Đã xóa ứng viên ID: " + id);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Lỗi khi xóa: " + e.getMessage());
        }
    }

    // Xóa yêu cầu tuyển dụng
    @DeleteMapping("/hiring-request/{id}")
    public ResponseEntity<?> deleteHiringRequest(@PathVariable String id) {
        try {
            yeuCauRepo.deleteById(id);
            return ResponseEntity.ok("Đã xóa yêu cầu: " + id);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Lỗi khi xóa: " + e.getMessage());
        }
    }

    // HR/Admin: Xem lịch phỏng vấn
    @GetMapping("/interviews")
    @PreAuthorize("hasAnyRole('HR', 'ADMIN')")
    public ResponseEntity<?> getInterviews(
            @RequestParam(required = false) String ungVienId) {
        try {
            List<LichPhongVan> interviews;
            if (ungVienId != null && !ungVienId.isBlank()) {
                Integer uvId;
                try {
                    uvId = Integer.parseInt(ungVienId.trim());
                } catch (Exception ex) {
                    return ResponseEntity.badRequest().body(Map.of("message", "ungVienId không hợp lệ"));
                }
                interviews = interviewRepo.findByUngVien_Id(uvId);
            } else {
                interviews = interviewRepo.findAll();
            }

            DateTimeFormatter fmt = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
            List<Map<String, Object>> response = interviews.stream().map(iv -> {
                Map<String, Object> m = new HashMap<>();
                m.put("idLich", iv.getIdLich());
                m.put("thoiGian", iv.getThoiGian() != null ? iv.getThoiGian().format(fmt) : null);
                m.put("diaDiem", iv.getDiaDiem());
                m.put("nguoiPhongVan", iv.getNguoiPhongVan());
                m.put("ghiChu", iv.getGhiChu());

                if (iv.getUngVien() != null) {
                    m.put("tenUngVien", iv.getUngVien().getHoTen());
                    m.put("trangThaiUngVien", iv.getUngVien().getTrangThai());
                    m.put("viTriUngTuyen",
                            iv.getUngVien().getYeuCauTuyenDung() != null
                                    ? iv.getUngVien().getYeuCauTuyenDung().getViTriCanTuyen()
                                    : null);
                } else {
                    m.put("tenUngVien", null);
                    m.put("viTriUngTuyen", null);
                    m.put("trangThaiUngVien", null);
                }
                return m;
            }).collect(Collectors.toList());

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("message", "Lỗi khi tải lịch phỏng vấn: " + e.getMessage()));
        }
    }
}