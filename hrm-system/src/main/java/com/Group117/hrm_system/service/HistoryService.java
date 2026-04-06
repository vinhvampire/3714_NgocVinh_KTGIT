package com.Group117.hrm_system.service;

import com.Group117.hrm_system.Repository.*;
import com.Group117.hrm_system.entity.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class HistoryService {

    @Autowired
    private QuyetDinhRepository quyetDinhRepository;
    @Autowired
    private LichSuCongTacRepository lichSuCongTacRepository;
    @Autowired
    private NhanVienRepository nhanVienRepository;
    @Autowired
    private PhongBanRepository phongBanRepository; // Thêm vào để sửa lỗi static
    @Autowired
    private ChucVuRepository chucVuRepository;     // Thêm vào để sửa lỗi static

    @Transactional
    public QuyetDinh banHanhQuyetDinh(QuyetDinh qd, String phongBanMoiId, String chucVuMoiId) {
        // 1. Kiểm tra nhân viên có tồn tại không
        NhanVien nv = nhanVienRepository.findById(qd.getNhanVien().getId())
                .orElseThrow(() -> new RuntimeException("Nhân viên không tồn tại"));

        // 2. Xử lý theo loại quyết định
        String loaiQd = qd.getLoaiQuyetDinh();

        if ("DIEU_CHUYEN".equalsIgnoreCase(loaiQd)) {
            // Ghi nhận lịch sử công tác
            LichSuCongTac ls = new LichSuCongTac();
            ls.setNhanVien(nv);
            ls.setTuNgay(qd.getNgayKy() != null ? qd.getNgayKy() : new Date());

            // Lưu thông tin cũ (Tên phòng ban/chức vụ)
            ls.setPhongBanCu(nv.getPhongBan() != null ? nv.getPhongBan().getTenPhongBan() : "Chưa có");
            ls.setChucVuCu(nv.getChucVu() != null ? nv.getChucVu().getTenChucVu() : "Chưa có");

            // Cập nhật Phòng ban mới cho Nhân viên và Lịch sử
            if (phongBanMoiId != null && !phongBanMoiId.isEmpty()) {
                PhongBan pbMoi = phongBanRepository.findById(phongBanMoiId).orElse(null);
                if (pbMoi != null) {
                    nv.setPhongBan(pbMoi);
                    ls.setPhongBanMoi(pbMoi.getTenPhongBan());
                }
            }

            // Cập nhật Chức vụ mới cho Nhân viên và Lịch sử
            if (chucVuMoiId != null && !chucVuMoiId.isEmpty()) {
                ChucVu cvMoi = chucVuRepository.findById(chucVuMoiId).orElse(null);
                if (cvMoi != null) {
                    nv.setChucVu(cvMoi);
                    ls.setChucVuMoi(cvMoi.getTenChucVu());
                }
            }

            ls.setGhiChu("Theo quyết định: " + qd.getSoQuyetDinh());
            lichSuCongTacRepository.save(ls);
            nhanVienRepository.save(nv); // Cập nhật thông tin mới vào hồ sơ nhân viên

        } else if ("THOI_VIEC".equalsIgnoreCase(loaiQd)) {
            // Soft Delete: Hibernate tự động chuyển trạng thái thành 'DA_NGHI_VIEC'
            nhanVienRepository.delete(nv);
        }
        else if ("KHEN_THUONG".equalsIgnoreCase(loaiQd) || "KY_LUAT".equalsIgnoreCase(loaiQd)) {
            LichSuCongTac ls = new LichSuCongTac();
            ls.setNhanVien(nv);
            ls.setTuNgay(qd.getNgayKy() != null ? qd.getNgayKy() : new java.util.Date());
            // Ghi nội dung khen thưởng/kỷ luật vào cột Ghi chú
            ls.setGhiChu("[" + loaiQd + "] " + qd.getNoiDungQuyetDinh());
            lichSuCongTacRepository.save(ls);
        }

        // 3. Lưu quyết định
        return quyetDinhRepository.save(qd);
    }

    public List<LichSuCongTac> getLichSuByNhanVien(String nhanVienId) {
        return lichSuCongTacRepository.findByNhanVienId(nhanVienId);
    }

    public List<Map<String, Object>> getAllQuyetDinh(String nhanVienId) {
        List<QuyetDinh> list = (nhanVienId == null || nhanVienId.isBlank())
                ? quyetDinhRepository.findAll()
                : quyetDinhRepository.findByNhanVienId(nhanVienId);

        return list.stream()
                .sorted(Comparator.comparing(QuyetDinh::getNgayKy, Comparator.nullsLast(Comparator.naturalOrder())).reversed())
                .map(qd -> {
                    Map<String, Object> m = new LinkedHashMap<>();
                    m.put("soQuyetDinh", qd.getSoQuyetDinh());
                    m.put("loaiQuyetDinh", qd.getLoaiQuyetDinh());
                    m.put("ngayKy", qd.getNgayKy());
                    m.put("noiDungQuyetDinh", qd.getNoiDungQuyetDinh());
                    m.put("nguoiKy", qd.getNguoiKy());
                    m.put("noiDung", qd.getNoiDung());
                    m.put("soTien", qd.getSoTien());
                    if (qd.getNhanVien() != null) {
                        m.put("nhanVienId", qd.getNhanVien().getId());
                        m.put("tenNhanVien", qd.getNhanVien().getHoTen());
                    } else {
                        m.put("nhanVienId", null);
                        m.put("tenNhanVien", null);
                    }
                    return m;
                })
                .toList();
    }
}