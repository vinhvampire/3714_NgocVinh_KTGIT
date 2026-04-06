package com.Group117.hrm_system.service;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.Group117.hrm_system.dto.HrLeaveDto;
import com.Group117.hrm_system.entity.DonNghiPhep;
import com.Group117.hrm_system.entity.NhanVien;
import com.Group117.hrm_system.Repository.DonNghiPhepRepository;
import com.Group117.hrm_system.Repository.NhanVienRepository;

@Service
public class DonNghiPhepService {

    private static final String CHO_QL_DUYET = "CHO_QL_DUYET";
    private static final String CHO_HR_XAC_NHAN = "CHO_HR_XAC_NHAN";
    private static final String LEGACY_CHO_DUYET = "CHO_DUYET"; // workflow cũ
    private static final String DA_DUYET = "DA_DUYET";
    private static final String TU_CHOI = "TU_CHOI";

    @Autowired
    private DonNghiPhepRepository donNghiPhepRepo;

    @Autowired
    private NhanVienRepository nhanVienRepo;

    @Autowired
    private NotificationService notificationService;

    @Transactional
    public DonNghiPhep createLeaveRequest(NhanVien employee, DonNghiPhep request) {
        if (employee == null || employee.getId() == null || employee.getId().isBlank()) {
            throw new IllegalArgumentException("Employee is required");
        }
        if (request == null) {
            throw new IllegalArgumentException("Leave request is required");
        }

        request.setNhanVien(employee);

        NhanVien manager = employee.getNguoiQuanLyTruocTiep();
        String empName = employee.getHoTen() != null ? employee.getHoTen() : employee.getId();

        // Ensure status routing by hierarchy
        if (manager != null && manager.getId() != null && !manager.getId().isBlank()) {
            request.setNguoiDuyet(manager);
            request.setTrangThai(CHO_QL_DUYET);

            DonNghiPhep created = donNghiPhepRepo.save(request);
            Long leaveId = created.getId();

            notificationService.findUsernameByNhanVienId(manager.getId())
                    .ifPresent(username -> notificationService.notifyPrivate(
                            username,
                            "LEAVE_PENDING_MANAGER",
                            "Đơn nghỉ phép chờ quản lý duyệt",
                            empName + " vừa gửi đơn nghỉ phép chờ bạn duyệt.",
                            "{\"leaveId\":" + leaveId + "}"
                    ));
            return created;
        }

        request.setNguoiDuyet(null);
        request.setTrangThai(CHO_HR_XAC_NHAN);
        DonNghiPhep created = donNghiPhepRepo.save(request);
        Long leaveId = created.getId();

        notificationService.notifyAllHr(
                "LEAVE_PENDING_HR",
                "Đơn nghỉ phép chờ HR xác nhận",
                empName + " vừa gửi đơn nghỉ phép (không có quản lý trực tiếp).",
                "{\"leaveId\":" + leaveId + "}"
        );

        return created;
    }

    @Transactional
    public DonNghiPhep approveByManager(Long leaveId, NhanVien manager) {
        if (leaveId == null) throw new IllegalArgumentException("leaveId is required");
        if (manager == null || manager.getId() == null || manager.getId().isBlank()) {
            throw new SecurityException("Forbidden");
        }

        DonNghiPhep don = donNghiPhepRepo.findById(leaveId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy đơn nghỉ phép"));

        if (!CHO_QL_DUYET.equalsIgnoreCase(don.getTrangThai())) {
            throw new IllegalStateException("Leave request is not pending for manager");
        }
        if (don.getNguoiDuyet() == null || don.getNguoiDuyet().getId() == null) {
            throw new SecurityException("Forbidden");
        }
        if (!manager.getId().equals(don.getNguoiDuyet().getId())) {
            throw new SecurityException("Forbidden");
        }

        don.setTrangThai(CHO_HR_XAC_NHAN);
        DonNghiPhep saved = donNghiPhepRepo.save(don);

        NhanVien nv = saved.getNhanVien();
        String empName = nv != null && nv.getHoTen() != null ? nv.getHoTen() : (nv != null ? nv.getId() : "NV");
        notificationService.notifyAllHr(
                "LEAVE_PENDING_HR",
                "Đơn nghỉ phép chờ HR xác nhận",
                empName + " vừa được quản lý duyệt. Chờ HR xác nhận.",
                "{\"leaveId\":" + saved.getId() + "}"
        );

        return saved;
    }

    @Transactional
    public DonNghiPhep confirmByHr(Long leaveId) {
        if (leaveId == null) throw new IllegalArgumentException("leaveId is required");

        DonNghiPhep don = donNghiPhepRepo.findById(leaveId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy đơn nghỉ phép"));

        if (!(CHO_HR_XAC_NHAN.equalsIgnoreCase(don.getTrangThai())
                || LEGACY_CHO_DUYET.equalsIgnoreCase(don.getTrangThai()))) {
            throw new IllegalStateException("Leave request is not pending for HR");
        }

        NhanVien nv = don.getNhanVien();
        if (nv == null || nv.getId() == null) {
            throw new IllegalStateException("Leave request missing employee");
        }

        long soNgayNghi = calculateLeaveDays(don);
        if ("PH_NAM".equalsIgnoreCase(don.getLoaiNghi())) {
            int cap = nv.getSoNgayPhepConLai() != null ? nv.getSoNgayPhepConLai() : 0;
            if (cap < soNgayNghi) {
                throw new IllegalArgumentException("Nhân viên không đủ ngày phép");
            }
            nv.setSoNgayPhepConLai(cap - (int) soNgayNghi);
            nhanVienRepo.save(nv);
        }

        don.setTrangThai(DA_DUYET);
        DonNghiPhep saved = donNghiPhepRepo.save(don);

        String empName = nv.getHoTen() != null ? nv.getHoTen() : nv.getId();
        notificationService.findUsernameByNhanVienId(nv.getId())
                .ifPresent(username -> notificationService.notifyPrivate(
                        username,
                        "LEAVE_APPROVED",
                        "Đơn nghỉ phép đã được duyệt",
                        "Đơn từ " + safeToString(don.getTuNgay()) + " đến " + safeToString(don.getDenNgay()) + " đã được HR xác nhận.",
                        "{\"leaveId\":" + saved.getId() + "}"
                ));

        // Thông báo cho Director: nhân viên đã được phê duyệt nghỉ phép
        notificationService.notifyAllDirectors(
                "LEAVE_APPROVED_SUMMARY",
                "Nhân viên nghỉ phép đã xác nhận",
                empName + " đã được duyệt nghỉ phép từ " + safeToString(don.getTuNgay()) + " đến " + safeToString(don.getDenNgay()) + ".",
                "{\"leaveId\":" + saved.getId() + "}"
        );

        return saved;
    }

    @Transactional
    public DonNghiPhep rejectLeave(Long leaveId, String reason, NhanVien actor, boolean rejectedByManager) {
        if (leaveId == null) throw new IllegalArgumentException("leaveId is required");
        String rs = reason != null ? reason.trim() : "";

        DonNghiPhep don = donNghiPhepRepo.findById(leaveId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy đơn nghỉ phép"));

        if (rejectedByManager) {
            if (!CHO_QL_DUYET.equalsIgnoreCase(don.getTrangThai())) {
                throw new IllegalStateException("Leave request is not pending for manager");
            }
            if (actor == null || actor.getId() == null || actor.getId().isBlank()) {
                throw new SecurityException("Forbidden");
            }
            if (don.getNguoiDuyet() == null || don.getNguoiDuyet().getId() == null) {
                throw new SecurityException("Forbidden");
            }
            if (!actor.getId().equals(don.getNguoiDuyet().getId())) {
                throw new SecurityException("Forbidden");
            }
            don.setTrangThai(TU_CHOI);
            don.setLyDo(buildLyDo(don.getLyDo(), "Manager từ chối: " + rs));
        } else {
            if (!(CHO_HR_XAC_NHAN.equalsIgnoreCase(don.getTrangThai())
                    || LEGACY_CHO_DUYET.equalsIgnoreCase(don.getTrangThai()))) {
                throw new IllegalStateException("Leave request is not pending for HR");
            }
            don.setTrangThai(TU_CHOI);
            don.setLyDo(buildLyDo(don.getLyDo(), "HR từ chối: " + rs));
        }

        DonNghiPhep saved = donNghiPhepRepo.save(don);

        NhanVien nv = saved.getNhanVien();
        if (nv != null && nv.getId() != null) {
            String empName = nv.getHoTen() != null ? nv.getHoTen() : nv.getId();
            notificationService.findUsernameByNhanVienId(nv.getId())
                    .ifPresent(username -> notificationService.notifyPrivate(
                            username,
                            "LEAVE_REJECTED",
                            "Đơn nghỉ phép bị từ chối",
                            "Đơn của " + empName + " đã bị từ chối. " + (rs.isBlank() ? "" : ("Lý do: " + rs)),
                            "{\"leaveId\":" + saved.getId() + "}"
                    ));
        }

        return saved;
    }

    // ─────────────────────────────────────────────────────────
    // Query helpers for UI
    // ─────────────────────────────────────────────────────────

    public List<HrLeaveDto> getManagerPendingLeaves(NhanVien manager) {
        if (manager == null || manager.getId() == null || manager.getId().isBlank()) {
            return List.of();
        }
        return donNghiPhepRepo.findByNguoiDuyet_IdAndTrangThai(manager.getId(), CHO_QL_DUYET).stream()
                .map(this::toHrLeaveDto)
                .toList();
    }

    public List<HrLeaveDto> getHrPendingLeaves() {
        List<DonNghiPhep> list1 = donNghiPhepRepo.findByTrangThaiOrderByTuNgayDesc(CHO_HR_XAC_NHAN);
        List<DonNghiPhep> list2 = donNghiPhepRepo.findByTrangThaiOrderByTuNgayDesc(LEGACY_CHO_DUYET);

        // Merge while preserving order by tuNgay (roughly)
        list1.addAll(list2);
        return list1.stream().map(this::toHrLeaveDto).toList();
    }

    // ─────────────────────────────────────────────────────────
    // Mapping / utils
    // ─────────────────────────────────────────────────────────

    private HrLeaveDto toHrLeaveDto(DonNghiPhep don) {
        NhanVien nv = don.getNhanVien();
        String tenNV = nv != null ? nv.getHoTen() : "";
        String tenPhong = (nv != null && nv.getPhongBan() != null) ? nv.getPhongBan().getTenPhongBan() : "—";
        String chucVu = (nv != null && nv.getChucVu() != null) ? nv.getChucVu().getTenChucVu() : "—";

        long soNgay = calculateLeaveDays(don);
        String ngayTu = don.getTuNgay() != null ? don.getTuNgay().toString() : "";
        String ngayDen = don.getDenNgay() != null ? don.getDenNgay().toString() : "";

        boolean hrPending = CHO_HR_XAC_NHAN.equalsIgnoreCase(don.getTrangThai()) || LEGACY_CHO_DUYET.equalsIgnoreCase(don.getTrangThai());
        String deptHeadStatus = hrPending ? "APPROVED" : "PENDING";
        return new HrLeaveDto(
                don.getId(),
                tenNV,
                tenPhong,
                chucVu,
                mapLeaveType(don.getLoaiNghi()),
                ngayTu,
                ngayDen,
                soNgay,
                ngayTu,
                deptHeadStatus,
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

    private long calculateLeaveDays(DonNghiPhep don) {
        if (don == null || don.getTuNgay() == null || don.getDenNgay() == null) return 0;
        return ChronoUnit.DAYS.between(don.getTuNgay(), don.getDenNgay()) + 1;
    }

    private String safeToString(LocalDate d) {
        return d != null ? d.toString() : "";
    }

    private String buildLyDo(String existing, String append) {
        String ex = existing != null ? existing.trim() : "";
        if (ex.isBlank()) return append;
        if (append == null || append.isBlank()) return ex;
        return ex + "\n" + append;
    }
}

