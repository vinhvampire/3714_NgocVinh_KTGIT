package com.Group117.hrm_system.service;

import com.Group117.hrm_system.Repository.*;
import com.Group117.hrm_system.entity.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
public class OrganizationService {

    @Autowired private ChiNhanhRepository chiNhanhRepository;
    @Autowired private PhongBanRepository phongBanRepository;
    @Autowired private NhomRepository nhomRepository;
    @Autowired private ChucVuRepository chucVuRepository;

    // --- QUẢN LÝ CHI NHÁNH (Branch) ---
    public List<ChiNhanh> getAllChiNhanh() { return chiNhanhRepository.findAll(); }
    public ChiNhanh createChiNhanh(ChiNhanh cn) {
        if (cn.getId() == null || cn.getId().isEmpty()) cn.setId("CN-" + generateId());
        return chiNhanhRepository.save(cn);
    }
    public void deleteChiNhanh(String id) { chiNhanhRepository.deleteById(id); }

    public ChiNhanh updateChiNhanh(String id, ChiNhanh body) {
        ChiNhanh e = chiNhanhRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy chi nhánh: " + id));
        if (body.getTenChiNhanh() != null) e.setTenChiNhanh(body.getTenChiNhanh());
        if (body.getDiaChi() != null) e.setDiaChi(body.getDiaChi());
        return chiNhanhRepository.save(e);
    }

    // --- QUẢN LÝ PHÒNG BAN (Department) ---
    public List<PhongBan> getAllPhongBan() { return phongBanRepository.findAll(); }
    public PhongBan createPhongBan(PhongBan pb) {
        if (pb.getId() == null || pb.getId().isEmpty()) pb.setId("PB-" + generateId());
        return phongBanRepository.save(pb);
    }
    public void deletePhongBan(String id) { phongBanRepository.deleteById(id); }

    public PhongBan updatePhongBan(String id, PhongBan body) {
        PhongBan e = phongBanRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy phòng ban: " + id));
        if (body.getTenPhongBan() != null) e.setTenPhongBan(body.getTenPhongBan());
        if (body.getChiNhanh() != null && body.getChiNhanh().getId() != null && !body.getChiNhanh().getId().isEmpty()) {
            ChiNhanh cn = chiNhanhRepository.findById(body.getChiNhanh().getId())
                    .orElseThrow(() -> new IllegalArgumentException("Chi nhánh không tồn tại"));
            e.setChiNhanh(cn);
        }
        return phongBanRepository.save(e);
    }

    // --- QUẢN LÝ NHÓM (Group) ---
    public List<Nhom> getAllNhom() { return nhomRepository.findAll(); }
    public Nhom createNhom(Nhom nhom) {
        if (nhom.getId() == null || nhom.getId().isEmpty()) nhom.setId("NH-" + generateId());
        return nhomRepository.save(nhom);
    }

    public void deleteNhom(String id) { nhomRepository.deleteById(id); }

    public Nhom updateNhom(String id, Nhom body) {
        Nhom e = nhomRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy nhóm: " + id));
        if (body.getTenNhom() != null) e.setTenNhom(body.getTenNhom());
        if (body.getPhongBan() != null && body.getPhongBan().getId() != null && !body.getPhongBan().getId().isEmpty()) {
            PhongBan pb = phongBanRepository.findById(body.getPhongBan().getId())
                    .orElseThrow(() -> new IllegalArgumentException("Phòng ban không tồn tại"));
            e.setPhongBan(pb);
        }
        return nhomRepository.save(e);
    }

    // --- QUẢN LÝ CHỨC VỤ (Đã làm) ---
    public List<ChucVu> getAllChucVu() { return chucVuRepository.findAll(); }
    public ChucVu createChucVu(ChucVu cv) {
        if (cv.getId() == null || cv.getId().isEmpty()) cv.setId("CV-" + generateId());
        return chucVuRepository.save(cv);
    }

    public void deleteChucVu(String id) { chucVuRepository.deleteById(id); }

    public ChucVu updateChucVu(String id, ChucVu body) {
        ChucVu e = chucVuRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy chức vụ: " + id));
        if (body.getTenChucVu() != null) e.setTenChucVu(body.getTenChucVu());
        return chucVuRepository.save(e);
    }

    // Hàm bổ trợ tạo mã ngẫu nhiên 4 ký tự
    private String generateId() {
        return UUID.randomUUID().toString().substring(0, 4).toUpperCase();
    }
}