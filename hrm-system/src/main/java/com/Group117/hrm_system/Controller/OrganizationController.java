package com.Group117.hrm_system.Controller;

import com.Group117.hrm_system.entity.*;
import com.Group117.hrm_system.service.OrganizationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/organizations")
public class OrganizationController {

    @Autowired
    private OrganizationService orgService;

    // --- CHI NHÁNH ---
    @GetMapping("/chi-nhanh")
    public List<ChiNhanh> getChiNhanh() { return orgService.getAllChiNhanh(); }

    @PreAuthorize("hasAnyRole('ADMIN','HR')")
    @PostMapping("/chi-nhanh")
    public ChiNhanh addChiNhanh(@RequestBody ChiNhanh cn) { return orgService.createChiNhanh(cn); }

    @PreAuthorize("hasAnyRole('ADMIN','HR')")
    @PutMapping("/chi-nhanh/{id}")
    public ResponseEntity<?> updateChiNhanh(@PathVariable String id, @RequestBody ChiNhanh body) {
        try {
            return ResponseEntity.ok(orgService.updateChiNhanh(id, body));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @PreAuthorize("hasAnyRole('ADMIN','HR')")
    @DeleteMapping("/chi-nhanh/{id}")
    public ResponseEntity<?> delChiNhanh(@PathVariable String id) {
        orgService.deleteChiNhanh(id);
        return ResponseEntity.ok("Đã xóa chi nhánh");
    }

    // --- PHÒNG BAN ---
    @GetMapping("/phong-ban")
    public List<PhongBan> getPhongBan() { return orgService.getAllPhongBan(); }

    @PreAuthorize("hasAnyRole('ADMIN','HR')")
    @PostMapping("/phong-ban")
    public PhongBan addPhongBan(@RequestBody PhongBan pb) { return orgService.createPhongBan(pb); }

    @PreAuthorize("hasAnyRole('ADMIN','HR')")
    @PutMapping("/phong-ban/{id}")
    public ResponseEntity<?> updatePhongBan(@PathVariable String id, @RequestBody PhongBan body) {
        try {
            return ResponseEntity.ok(orgService.updatePhongBan(id, body));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @PreAuthorize("hasAnyRole('ADMIN','HR')")
    @DeleteMapping("/phong-ban/{id}")
    public ResponseEntity<?> delPhongBan(@PathVariable String id) {
        orgService.deletePhongBan(id);
        return ResponseEntity.ok("Đã xóa phòng ban");
    }

    // --- NHÓM ---
    @GetMapping("/nhom")
    public List<Nhom> getNhom() { return orgService.getAllNhom(); }

    @PreAuthorize("hasAnyRole('ADMIN','HR')")
    @PostMapping("/nhom")
    public Nhom addNhom(@RequestBody Nhom nhom) { return orgService.createNhom(nhom); }

    @PreAuthorize("hasAnyRole('ADMIN','HR')")
    @PutMapping("/nhom/{id}")
    public ResponseEntity<?> updateNhom(@PathVariable String id, @RequestBody Nhom body) {
        try {
            return ResponseEntity.ok(orgService.updateNhom(id, body));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @PreAuthorize("hasAnyRole('ADMIN','HR')")
    @DeleteMapping("/nhom/{id}")
    public ResponseEntity<?> delNhom(@PathVariable String id) {
        orgService.deleteNhom(id);
        return ResponseEntity.ok("Đã xóa nhóm");
    }

    // --- CHỨC VỤ ---
    @GetMapping("/chuc-vu")
    public List<ChucVu> getChucVu() { return orgService.getAllChucVu(); }

    @PreAuthorize("hasAnyRole('ADMIN','HR')")
    @PostMapping("/chuc-vu")
    public ChucVu addChucVu(@RequestBody ChucVu cv) { return orgService.createChucVu(cv); }

    @PreAuthorize("hasAnyRole('ADMIN','HR')")
    @PutMapping("/chuc-vu/{id}")
    public ResponseEntity<?> updateChucVu(@PathVariable String id, @RequestBody ChucVu body) {
        try {
            return ResponseEntity.ok(orgService.updateChucVu(id, body));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @PreAuthorize("hasAnyRole('ADMIN','HR')")
    @DeleteMapping("/chuc-vu/{id}")
    public ResponseEntity<?> delChucVu(@PathVariable String id) {
        orgService.deleteChucVu(id);
        return ResponseEntity.ok("Đã xóa chức vụ");
    }
}