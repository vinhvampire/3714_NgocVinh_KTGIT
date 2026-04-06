# Dữ liệu mẫu MySQL (Laragon) — `hrm_system`

Script theo **entity JPA** trong `hrm-system/src/main/java/com/Group117/hrm_system/entity/`.  
Mỗi bảng **10 dòng** (khi có thể). Chạy **theo thứ tự** trong khối SQL.

**Lưu ý**

- Trùng `id` / `username` / `email_cong_viec` / `so_cccd` → đổi prefix hoặc xóa dữ liệu cũ trước.
- Cột `` `so_dien-thoai` `` ở `ho_so_ung_vien` (theo `@Column` trong code) — **bắt buộc backtick** trong MySQL.
- `cham_cong.ngay` trong entity map cột **`ngay_cham_cong`**.
- BCrypt dưới đây tương ứng mật khẩu: **`password`** (đổi nếu cần).

```sql
USE hrm_system;
SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- ========== 1. chi_nhanh (10) ==========
INSERT INTO chi_nhanh (id, ten_chi_nhanh, dia_chi) VALUES
('CN001', 'Chi nhánh Hà Nội', '123 Phố Huế, Hai Bà Trưng'),
('CN002', 'Chi nhánh TP.HCM', '456 Nguyễn Huệ, Q1'),
('CN003', 'Chi nhánh Đà Nẵng', '789 Trần Phú'),
('CN004', 'Chi nhánh Cần Thơ', '12 Nguyễn Văn Cừ'),
('CN005', 'Chi nhánh Hải Phòng', '34 Lê Hồng Phong'),
('CN006', 'Chi nhánh Nha Trang', '56 Trần Hưng Đạo'),
('CN007', 'Chi nhánh Vũng Tàu', '78 Lê Lợi'),
('CN008', 'Chi nhánh Bình Dương', '90 Đại lộ Bình Dương'),
('CN009', 'Chi nhánh Biên Hòa', '11 Amata'),
('CN010', 'Chi nhánh Quy Nhon', '22 Nguyễn Tất Thành');

-- ========== 2. phong_ban (10) ==========
INSERT INTO phong_ban (id, ten_phong_ban, chi_nhanh_id) VALUES
('PB001', 'Phòng Kỹ thuật', 'CN001'),
('PB002', 'Phòng Nhân sự', 'CN002'),
('PB003', 'Phòng Kinh doanh', 'CN003'),
('PB004', 'Phòng Kế toán', 'CN004'),
('PB005', 'Phòng Marketing', 'CN005'),
('PB006', 'Phòng CSKH', 'CN006'),
('PB007', 'Phòng QA', 'CN007'),
('PB008', 'Phòng R&D', 'CN008'),
('PB009', 'Phòng Hành chính', 'CN009'),
('PB010', 'Phòng Pháp chế', 'CN010');

-- ========== 3. chuc_vu (10) ==========
INSERT INTO chuc_vu (id, ten_chuc_vu) VALUES
('CV001', 'Trưởng phòng'),
('CV002', 'Phó phòng'),
('CV003', 'Nhân viên'),
('CV004', 'Chuyên viên'),
('CV005', 'Thực tập sinh'),
('CV006', 'Giám đốc'),
('CV007', 'Kế toán trưởng'),
('CV008', 'Trưởng nhóm'),
('CV009', 'Cộng tác viên'),
('CV010', 'Quản lý dự án');

-- ========== 4. nhom (10) ==========
INSERT INTO nhom (id, ten_nhom, phong_ban_id) VALUES
('NH001', 'Nhóm Frontend', 'PB001'),
('NH002', 'Nhóm Backend', 'PB001'),
('NH003', 'Nhóm Tuyển dụng', 'PB002'),
('NH004', 'Nhóm B2B', 'PB003'),
('NH005', 'Nhóm Kế toán tổng hợp', 'PB004'),
('NH006', 'Nhóm Content', 'PB005'),
('NH007', 'Nhóm Hotline', 'PB006'),
('NH008', 'Nhóm Test', 'PB007'),
('NH009', 'Nhóm Lab', 'PB008'),
('NH010', 'Nhóm Hợp đồng', 'PB010');

-- ========== 5. bang_luong (10) ==========
INSERT INTO bang_luong (id, ten_chuc_vu, luong_co_ban, phu_cap_dinh_muc) VALUES
('BL001', 'Frontend Developer', 15000000, 1000000),
('BL002', 'Backend Developer', 16000000, 1200000),
('BL003', 'HR Executive', 12000000, 800000),
('BL004', 'Sales', 11000000, 2000000),
('BL005', 'Accountant', 13000000, 500000),
('BL006', 'Marketing', 11500000, 700000),
('BL007', 'CSKH', 10000000, 400000),
('BL008', 'QA Engineer', 14000000, 900000),
('BL009', 'R&D', 17000000, 1500000),
('BL010', 'Legal', 18000000, 1000000);

-- ========== 6. nhan_vien (10) ==========
INSERT INTO nhan_vien (
  id, ho_ten, gioi_tinh, so_cccd, ngay_cap, noi_cap, ngay_sinh,
  so_dien_thoai, email_cong_viec, dia_chi_tam_tru, anh_dai_dien_url,
  ngay_vao_lam, so_ngay_phep_con_lai, trang_thai_hoat_dong, he_so_luong,
  phong_ban_id, nhom_id, chuc_vu_id, bang_luong_id, nguoi_quan_ly_truoc_tiep_id
) VALUES
('NV001', 'Nguyễn Văn A', 'Nam', '034001000001', '2015-01-10', 'CA Hà Nội', '1990-05-01',
 '0901000001', 'nv001@demo.local', 'Hà Nội', NULL,
 '2026-04-01', 12, 'DANG_LAM_VIEC', 1.0,
 'PB001', 'NH001', 'CV001', 'BL001', NULL),
('NV002', 'Trần Thị B', 'Nữ', '034001000002', '2016-02-11', 'CA TP.HCM', '1992-06-02',
 '0901000002', 'nv002@demo.local', 'TP.HCM', NULL,
 '2026-04-02', 12, 'DANG_LAM_VIEC', 1.0,
 'PB002', 'NH003', 'CV003', 'BL003', 'NV001'),
('NV003', 'Lê Văn C', 'Nam', '034001000003', '2017-03-12', 'CA Đà Nẵng', '1993-07-03',
 '0901000003', 'nv003@demo.local', 'Đà Nẵng', NULL,
 '2026-04-03', 11, 'DANG_LAM_VIEC', 1.0,
 'PB003', 'NH004', 'CV004', 'BL004', 'NV001'),
('NV004', 'Phạm Thị D', 'Nữ', '034001000004', '2018-04-13', 'CA Cần Thơ', '1994-08-04',
 '0901000004', 'nv004@demo.local', 'Cần Thơ', NULL,
 '2026-04-04', 10, 'DANG_LAM_VIEC', 1.0,
 'PB004', 'NH005', 'CV004', 'BL005', 'NV001'),
('NV005', 'Hoàng Văn E', 'Nam', '034001000005', '2019-05-14', 'CA Hải Phòng', '1995-09-05',
 '0901000005', 'nv005@demo.local', 'Hải Phòng', NULL,
 '2026-04-05', 12, 'DANG_LAM_VIEC', 1.0,
 'PB005', 'NH006', 'CV003', 'BL006', 'NV001'),
('NV006', 'Võ Thị F', 'Nữ', '034001000006', '2020-06-15', 'CA Khánh Hòa', '1996-10-06',
 '0901000006', 'nv006@demo.local', 'Nha Trang', NULL,
 '2026-04-06', 9, 'DANG_LAM_VIEC', 1.0,
 'PB006', 'NH007', 'CV003', 'BL007', 'NV001'),
('NV007', 'Đặng Văn G', 'Nam', '034001000007', '2021-07-16', 'CA BR-VT', '1997-11-07',
 '0901000007', 'nv007@demo.local', 'Vũng Tàu', NULL,
 '2026-04-07', 8, 'DANG_LAM_VIEC', 1.0,
 'PB007', 'NH008', 'CV004', 'BL008', 'NV001'),
('NV008', 'Bùi Thị H', 'Nữ', '034001000008', '2022-08-17', 'CA Bình Dương', '1998-12-08',
 '0901000008', 'nv008@demo.local', 'Bình Dương', NULL,
 '2026-04-08', 12, 'DANG_LAM_VIEC', 1.0,
 'PB008', 'NH009', 'CV004', 'BL009', 'NV001'),
('NV009', 'Đinh Văn I', 'Nam', '034001000009', '2023-09-18', 'CA Đồng Nai', '1999-01-09',
 '0901000009', 'nv009@demo.local', 'Biên Hòa', NULL,
 '2026-04-09', 7, 'DANG_LAM_VIEC', 1.0,
 'PB009', 'NH010', 'CV003', 'BL002', 'NV001'),
('NV010', 'Ngô Thị K', 'Nữ', '034001000010', '2024-10-19', 'CA Bình Định', '2000-02-10',
 '0901000010', 'nv010@demo.local', 'Quy Nhon', NULL,
 '2026-04-10', 6, 'DANG_LAM_VIEC', 1.0,
 'PB010', 'NH010', 'CV005', 'BL010', 'NV001');

-- ========== 7. taikhoan (10) — BCrypt: password ==========
-- Hash: $2a$10$dXJ3SW6G7P50lGmMkkmwe.20cQQubK3.HZWzG3YB1tlRy.fqvM/BG
INSERT INTO taikhoan (ma_tai_khoan, username, password, role, reset_token, reset_token_expiry, trang_thai_tai_khoan, ngay_tao, id_nhan_vien) VALUES
('TK001', 'user_nv001', '$2a$10$dXJ3SW6G7P50lGmMkkmwe.20cQQubK3.HZWzG3YB1tlRy.fqvM/BG', 'EMPLOYEE', NULL, NULL, 1, '2026-04-01 08:00:00', 'NV001'),
('TK002', 'user_nv002', '$2a$10$dXJ3SW6G7P50lGmMkkmwe.20cQQubK3.HZWzG3YB1tlRy.fqvM/BG', 'EMPLOYEE', NULL, NULL, 1, '2026-04-01 08:00:00', 'NV002'),
('TK003', 'user_nv003', '$2a$10$dXJ3SW6G7P50lGmMkkmwe.20cQQubK3.HZWzG3YB1tlRy.fqvM/BG', 'EMPLOYEE', NULL, NULL, 1, '2026-04-01 08:00:00', 'NV003'),
('TK004', 'user_nv004', '$2a$10$dXJ3SW6G7P50lGmMkkmwe.20cQQubK3.HZWzG3YB1tlRy.fqvM/BG', 'EMPLOYEE', NULL, NULL, 1, '2026-04-01 08:00:00', 'NV004'),
('TK005', 'user_nv005', '$2a$10$dXJ3SW6G7P50lGmMkkmwe.20cQQubK3.HZWzG3YB1tlRy.fqvM/BG', 'EMPLOYEE', NULL, NULL, 1, '2026-04-01 08:00:00', 'NV005'),
('TK006', 'user_nv006', '$2a$10$dXJ3SW6G7P50lGmMkkmwe.20cQQubK3.HZWzG3YB1tlRy.fqvM/BG', 'EMPLOYEE', NULL, NULL, 1, '2026-04-01 08:00:00', 'NV006'),
('TK007', 'user_nv007', '$2a$10$dXJ3SW6G7P50lGmMkkmwe.20cQQubK3.HZWzG3YB1tlRy.fqvM/BG', 'EMPLOYEE', NULL, NULL, 1, '2026-04-01 08:00:00', 'NV007'),
('TK008', 'user_nv008', '$2a$10$dXJ3SW6G7P50lGmMkkmwe.20cQQubK3.HZWzG3YB1tlRy.fqvM/BG', 'EMPLOYEE', NULL, NULL, 1, '2026-04-01 08:00:00', 'NV008'),
('TK009', 'user_nv009', '$2a$10$dXJ3SW6G7P50lGmMkkmwe.20cQQubK3.HZWzG3YB1tlRy.fqvM/BG', 'EMPLOYEE', NULL, NULL, 1, '2026-04-01 08:00:00', 'NV009'),
('TK010', 'user_nv010', '$2a$10$dXJ3SW6G7P50lGmMkkmwe.20cQQubK3.HZWzG3YB1tlRy.fqvM/BG', 'EMPLOYEE', NULL, NULL, 1, '2026-04-01 08:00:00', 'NV010');

-- ========== 8. cham_cong (10) — cột ngày: ngay_cham_cong ==========
INSERT INTO cham_cong (nhan_vien_id, ngay_cham_cong, gio_vao, gio_ra, trang_thai) VALUES
('NV001', '2026-04-01', '08:00:00', '17:30:00', 'DI_LAM'),
('NV002', '2026-04-02', '08:15:00', '17:30:00', 'DI_MUON'),
('NV003', '2026-04-03', '07:55:00', '17:00:00', 'DI_LAM'),
('NV004', '2026-04-04', '08:00:00', NULL, 'DI_LAM'),
('NV005', '2026-04-05', '08:30:00', '17:30:00', 'DI_MUON'),
('NV006', '2026-04-06', '08:00:00', '17:00:00', 'DI_LAM'),
('NV007', '2026-04-07', '08:05:00', '17:30:00', 'DI_LAM'),
('NV008', '2026-04-08', '08:00:00', '17:30:00', 'DI_LAM'),
('NV009', '2026-04-09', '09:00:00', '17:30:00', 'DI_MUON'),
('NV010', '2026-04-10', '08:00:00', '17:30:00', 'DI_LAM');

-- ========== 9. don_nghi_phep (10) ==========
INSERT INTO don_nghi_phep (nhan_vien_id, loai_nghi, tu_ngay, den_ngay, ly_do, trang_thai, nguoi_duyet_id) VALUES
('NV002', 'PH_NAM', '2026-04-12', '2026-04-12', 'Nghỉ phép năm', 'DA_DUYET', 'NV001'),
('NV003', 'NGHI_OM', '2026-04-15', '2026-04-16', 'Ốm', 'DA_DUYET', 'NV001'),
('NV004', 'PH_NAM', '2026-04-20', '2026-04-20', 'Việc riêng', 'CHO_QL_DUYET', NULL),
('NV005', 'PH_NAM', '2026-04-22', '2026-04-22', 'Phép', 'CHO_HR_XAC_NHAN', NULL),
('NV006', 'NGHI_LE', '2026-04-30', '2026-04-30', 'Lễ', 'DA_DUYET', 'NV001'),
('NV007', 'KHONG_LUONG', '2026-04-18', '2026-04-18', 'Không lương', 'TU_CHOI', 'NV001'),
('NV008', 'PH_NAM', '2026-04-25', '2026-04-25', 'Phép', 'DA_DUYET', 'NV001'),
('NV009', 'NGHI_OM', '2026-04-11', '2026-04-11', 'Ốm', 'DA_DUYET', 'NV001'),
('NV010', 'PH_NAM', '2026-04-14', '2026-04-14', 'Phép', 'DA_DUYET', 'NV001'),
('NV002', 'PH_NAM', '2026-05-02', '2026-05-02', 'Dự phòng', 'CHO_DUYET', NULL);

-- ========== 10. phieu_luong (10) ==========
INSERT INTO phieu_luong (id, thang_nam, luong_co_ban, phu_cap, phat_muon, nghi_khong_phep, tong_luong, trang_thai_thanh_toan, nhan_vien_id) VALUES
('PL_NV001_042026', '2026-04', 15000000, 1000000, 0, 0, 16000000, 'CHUA_THANH_TOAN', 'NV001'),
('PL_NV002_042026', '2026-04', 12000000, 800000, 50000, 0, 12750000, 'CHUA_THANH_TOAN', 'NV002'),
('PL_NV003_042026', '2026-04', 11000000, 2000000, 0, 100000, 12900000, 'DA_THANH_TOAN', 'NV003'),
('PL_NV004_042026', '2026-04', 13000000, 500000, 0, 0, 13500000, 'CHUA_THANH_TOAN', 'NV004'),
('PL_NV005_042026', '2026-04', 11500000, 700000, 200000, 0, 12000000, 'CHUA_THANH_TOAN', 'NV005'),
('PL_NV006_042026', '2026-04', 10000000, 400000, 0, 0, 10400000, 'DA_THANH_TOAN', 'NV006'),
('PL_NV007_042026', '2026-04', 14000000, 900000, 100000, 0, 14800000, 'CHUA_THANH_TOAN', 'NV007'),
('PL_NV008_042026', '2026-04', 17000000, 1500000, 0, 500000, 18000000, 'CHUA_THANH_TOAN', 'NV008'),
('PL_NV009_042026', '2026-04', 16000000, 1200000, 0, 0, 17200000, 'CHUA_THANH_TOAN', 'NV009'),
('PL_NV010_042026', '2026-04', 18000000, 1000000, 0, 0, 19000000, 'CHUA_THANH_TOAN', 'NV010');

-- ========== 11. yeu_cau_tuyen_dung (10) ==========
INSERT INTO yeu_cau_tuyen_dung (id_yeu_cau, vi_tri_can_tuyen, so_luong, trinh_do_yeu_cau, trang_thai, mo_ta, ngay_yeu_cau, phong_ban_id) VALUES
('REQ001', 'Lập trình viên Java', 2, 'Đại học CNTT', 'PENDING', 'Spring Boot', '2026-04-01', 'PB001'),
('REQ002', 'Nhân sự tổng hợp', 1, 'Đại học', 'PENDING', 'HR', '2026-04-02', 'PB002'),
('REQ003', 'Nhân viên kinh doanh', 3, 'Cao đẳng+', 'OPEN', 'B2B', '2026-04-03', 'PB003'),
('REQ004', 'Kế toán viên', 2, 'Có chứng chỉ', 'PENDING', 'Kế toán', '2026-04-04', 'PB004'),
('REQ005', 'Content Marketing', 1, 'Không yêu cầu', 'PENDING', 'Viết bài', '2026-04-05', 'PB005'),
('REQ006', 'CSKH', 4, 'Giao tiếp tốt', 'OPEN', 'Call center', '2026-04-06', 'PB006'),
('REQ007', 'Tester', 2, 'ISTQB', 'PENDING', 'Manual + auto', '2026-04-07', 'PB007'),
('REQ008', 'Data Analyst', 1, 'SQL/Python', 'PENDING', 'Phân tích', '2026-04-08', 'PB008'),
('REQ009', 'Hành chính', 1, 'THPT+', 'PENDING', 'Văn phòng', '2026-04-09', 'PB009'),
('REQ010', 'Pháp chế', 1, 'Luật', 'PENDING', 'Hợp đồng', '2026-04-10', 'PB010');

-- ========== 12. ho_so_ung_vien (10) — cột số điện thoại: `so_dien-thoai` ==========
INSERT INTO ho_so_ung_vien (ho_ten, email, `so_dien-thoai`, cv_url, trang_thai, ngay_nop, id_yeu_cau) VALUES
('Ứng viên 01', 'uv01@mail.com', '0911110001', '/cv/uv01.pdf', 'CHO_DUYET', '2026-04-01 10:00:00', 'REQ001'),
('Ứng viên 02', 'uv02@mail.com', '0911110002', '/cv/uv02.pdf', 'CHO_DUYET', '2026-04-01 11:00:00', 'REQ001'),
('Ứng viên 03', 'uv03@mail.com', '0911110003', NULL, 'DAT', '2026-04-02 09:00:00', 'REQ002'),
('Ứng viên 04', 'uv04@mail.com', '0911110004', '/cv/uv04.pdf', 'LOAI', '2026-04-03 14:00:00', 'REQ003'),
('Ứng viên 05', 'uv05@mail.com', '0911110005', '/cv/uv05.pdf', 'CHO_DUYET', '2026-04-04 08:30:00', 'REQ004'),
('Ứng viên 06', 'uv06@mail.com', '0911110006', NULL, 'CHO_DUYET', '2026-04-05 16:00:00', 'REQ005'),
('Ứng viên 07', 'uv07@mail.com', '0911110007', '/cv/uv07.pdf', 'CHO_DUYET', '2026-04-06 10:15:00', 'REQ006'),
('Ứng viên 08', 'uv08@mail.com', '0911110008', '/cv/uv08.pdf', 'CHO_DUYET', '2026-04-07 11:20:00', 'REQ007'),
('Ứng viên 09', 'uv09@mail.com', '0911110009', NULL, 'CHO_DUYET', '2026-04-08 13:45:00', 'REQ008'),
('Ứng viên 10', 'uv10@mail.com', '0911110010', '/cv/uv10.pdf', 'CHO_DUYET', '2026-04-09 09:00:00', 'REQ010');

-- ========== 13. lich_phong_van (10) — FK id_ung_vien = ho_so_ung_vien.id (1..10) ==========
INSERT INTO lich_phong_van (id_lich, thoi_gian, dia_diem, ghi_chu, id_ung_vien, nguoi_phong_van) VALUES
('LPV001', '2026-04-15 09:00:00', 'Phòng A1', 'Vòng 1', 1, 'NV001'),
('LPV002', '2026-04-15 10:00:00', 'Phòng A1', 'Vòng 1', 2, 'NV001'),
('LPV003', '2026-04-16 14:00:00', 'Online', 'Google Meet', 3, 'NV002'),
('LPV004', '2026-04-17 08:30:00', 'Phòng B2', NULL, 4, 'NV002'),
('LPV005', '2026-04-18 15:00:00', 'Phòng C3', NULL, 5, 'NV003'),
('LPV006', '2026-04-19 09:30:00', 'Online', NULL, 6, 'NV003'),
('LPV007', '2026-04-20 10:00:00', 'Phòng A1', NULL, 7, 'NV004'),
('LPV008', '2026-04-21 11:00:00', 'Phòng A1', NULL, 8, 'NV004'),
('LPV009', '2026-04-22 13:00:00', 'Phòng D4', NULL, 9, 'NV005'),
('LPV010', '2026-04-23 14:30:00', 'Online', NULL, 10, 'NV005');

-- ========== 14. lich_su_cong_tac (10) ==========
INSERT INTO lich_su_cong_tac (nhan_vien_id, tu_ngay, den_ngay, phong_ban_cu, phong_ban_moi, chuc_vu_cu, chuc_vu_moi, ghi_chu) VALUES
('NV001', '2024-01-01', '2025-12-31', 'PB002', 'PB001', 'CV003', 'CV001', 'Thăng chức'),
('NV002', '2025-06-01', '2026-03-31', 'PB001', 'PB002', 'CV005', 'CV003', 'Chuyển phòng'),
('NV003', '2025-01-01', '2025-12-31', 'PB003', 'PB003', 'CV004', 'CV004', 'Giữ nguyên'),
('NV004', '2024-09-01', '2026-04-01', 'PB004', 'PB004', 'CV004', 'CV004', NULL),
('NV005', '2023-05-01', '2026-04-01', 'PB005', 'PB005', 'CV003', 'CV003', NULL),
('NV006', '2026-01-01', '2026-04-01', 'PB006', 'PB006', 'CV003', 'CV003', NULL),
('NV007', '2025-11-01', '2026-04-01', 'PB007', 'PB007', 'CV004', 'CV004', NULL),
('NV008', '2025-08-01', '2026-04-01', 'PB008', 'PB008', 'CV004', 'CV004', NULL),
('NV009', '2025-12-01', '2026-04-01', 'PB009', 'PB009', 'CV003', 'CV003', NULL),
('NV010', '2026-02-01', '2026-04-01', 'PB010', 'PB010', 'CV005', 'CV005', NULL);

-- ========== 15. quyet_dinh (10) ==========
INSERT INTO quyet_dinh (so_quyet_dinh, nhan_vien_id, loai_quyet_dinh, ngay_ky, noi_dung_quyet_dinh, nguoi_ky, noi_dung, so_tien) VALUES
('QD-001/2026', 'NV001', 'KHEN_THUONG', '2026-04-01', 'Khen thưởng thành tích', 'Ban TGĐ', 'Hoàn thành tốt', 5000000),
('QD-002/2026', 'NV002', 'KY_LUAT', '2026-04-02', 'Nhắc nhở', 'Trưởng phòng', 'Đi muộn', 0),
('QD-003/2026', 'NV003', 'DIEU_CHUYEN', '2026-04-03', 'Điều chuyển nội bộ', 'HR', 'Chuyển bộ phận', NULL),
('QD-004/2026', 'NV004', 'KHEN_THUONG', '2026-04-04', 'Thưởng quý', 'Kế toán', NULL, 2000000),
('QD-005/2026', 'NV005', 'KY_LUAT', '2026-04-05', 'Phạt nhẹ', 'Trưởng phòng', NULL, 500000),
('QD-006/2026', 'NV006', 'KHEN_THUONG', '2026-04-06', 'CSKH tốt', 'QL', NULL, 1000000),
('QD-007/2026', 'NV007', 'DIEU_CHUYEN', '2026-04-07', 'Luân chuyển', 'HR', NULL, NULL),
('QD-008/2026', 'NV008', 'KHEN_THUONG', '2026-04-08', 'Sáng kiến', 'R&D', NULL, 3000000),
('QD-009/2026', 'NV009', 'KY_LUAT', '2026-04-09', 'Vi phạm nhỏ', 'HC', NULL, 200000),
('QD-010/2026', 'NV010', 'THOI_VIEC', '2026-04-10', 'Không áp dụng', 'Pháp chế', 'Mẫu quyết định', NULL);

-- ========== 16. thong_bao (10) ==========
INSERT INTO thong_bao (nguoi_nhan, tieu_de, noi_dung, loai, da_doc, ngay_tao, ref_payload) VALUES
('user_nv001', 'Chào mừng', 'Tài khoản đã kích hoạt', 'SYSTEM', 0, '2026-04-01 08:00:00', NULL),
('user_nv002', 'Đơn phép', 'Có đơn chờ duyệt', 'LEAVE_PENDING', 0, '2026-04-02 09:00:00', '{"leaveId":1}'),
(NULL, 'Thông báo chung', 'Họp toàn công ty', 'BROADCAST', 0, '2026-04-03 10:00:00', NULL),
('user_nv003', 'Lương', 'Phiếu lương tháng 4', 'PAYSLIP_READY', 1, '2026-04-04 11:00:00', '{"payslipId":"PL_NV003_042026"}'),
('user_nv004', 'Cập nhật', 'Cập nhật hồ sơ', 'PROFILE', 0, '2026-04-05 14:00:00', NULL),
('user_nv005', 'Nhắc nhở', 'Hoàn thành timesheet', 'REMINDER', 0, '2026-04-06 08:30:00', NULL),
('user_nv006', 'Phỏng vấn', 'Lịch PV ứng viên', 'INTERVIEW', 0, '2026-04-07 15:00:00', NULL),
('user_nv007', 'Bảo trì', 'Hệ thống bảo trì tối', 'MAINT', 0, '2026-04-08 17:00:00', NULL),
('user_nv008', 'Khen thưởng', 'Quyết định khen thưởng', 'DECISION', 0, '2026-04-09 10:00:00', NULL),
('user_nv009', 'Khảo sát', 'Khảo sát nội bộ', 'SURVEY', 0, '2026-04-10 16:00:00', NULL);

SET FOREIGN_KEY_CHECKS = 1;
```

## Bảng không tách script (hoặc cần chỉnh tay)

| Bảng | Ghi chú |
|------|--------|
| *(tất cả 16 bảng entity)* | Đã có 10 dòng trong script trên nếu chạy được trên schema khớp entity. |

Nếu MySQL của bạn **tên cột khác** (ví dụ Hibernate đổi `so_dien-thoai` → `so_dien_thoai`), hãy sửa cho khớp `SHOW CREATE TABLE ho_so_ung_vien;`.
