package com.Group117.hrm_system.Controller;

import java.nio.charset.StandardCharsets;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
public class ViewController {

    @GetMapping("/quan-ly-chuc-vu")
    public String getChucVuPage() {
        return "redirect:/dashboard/hr/org/chuc-vu";
    }

    @GetMapping("/them-nhan-vien")
    public String getThemNhanVienPage() {
        return "redirect:/dashboard/hr/employees?new=1";
    }

    @GetMapping("/phong-ban")
    public String getPhongBanPage() {
        return "redirect:/dashboard/hr/org/phong-ban";
    }

    @GetMapping("/quan-ly-nhan-vien")
    public String getQuanLyNhanVienPage() {
        return "redirect:/dashboard/hr/employees";
    }

    @GetMapping("/chi-nhanh")
    public String getChiNhanhPage() {
        return "redirect:/dashboard/hr/org/chi-nhanh";
    }

    @GetMapping("/nhom")
    public String getNhomPage() {
        return "redirect:/dashboard/hr/org/nhom";
    }

    @GetMapping("/ho-so-nhan-vien")
    public String getHoSoNhanVienPage(@RequestParam(required = false) String id) {
        if (id == null || id.isBlank()) {
            return "redirect:/dashboard/hr/employees";
        }
        return "redirect:/dashboard/hr/employees/profile?id=" + java.net.URLEncoder.encode(id, StandardCharsets.UTF_8);
    }

    @GetMapping("/ban-hanh-quyet-dinh")
    public String getBanHanhQuyetDinhPage() {
        return "redirect:/dashboard/hr/decisions";
    }

    @GetMapping("/luong")
    public String getLuongPage() {
        return "redirect:/dashboard/hr/payroll";
    }
}
