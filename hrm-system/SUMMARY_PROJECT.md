# Tóm Tắt Dự Án HRM System

## 📋 Thông Tin Chung

- **Tên Project:** hrm-system (Hệ Thống Quản Lý Nhân Sự)
- **Framework:** Spring Boot 4.0.3
- **Language:** Java 25
- **Database:** MySQL
- **Template Engine:** Thymeleaf
- **Authentication:** Spring Security + JWT
- **Version:** 0.0.1-SNAPSHOT

---

## 🎯 Mục Đích Dự Án

Hệ thống quản lý nhân sự toàn diện dành cho các doanh nghiệp, hỗ trợ:
- Quản lý thông tin nhân viên
- Quản lý bộ phận và chi nhánh
- Quản lý lương và phiếu lương
- Quản lý tuyển dụng và ứng viên
- Quản lý chấm công và nghỉ phép
- Lịch sử công tác nhân viên
- Ban hành quyết định

---

## 🏗️ Kiến Trúc Ứng Dụng

```
src/main/java/com/Group117/hrm_system/
├── Controller/           # 11 Controllers xử lý API
├── entity/              # 15 Entity models (JPA)
├── service/             # Business logic services
├── Repository/          # Data access layer (JPA Repositories)
├── config/              # Configuration files
└── HrmSystemApplication.java  # Main application class
```

---

## 🎮 Controllers (11 Controllers)

| Controller | Chức Năng |
|-----------|----------|
| **AuthController** | Xác thực, đăng nhập, quản lý token JWT |
| **LoginController** | Trang đăng nhập |
| **EmployeeController** | Quản lý thông tin nhân viên |
| **OrganizationController** | Quản lý bộ phận, chi nhánh, chức vụ, nhóm |
| **PayrollController** | Quản lý lương, phiếu lương, bảng lương |
| **AttendanceController** | Quản lý chấm công |
| **HistoryController** | Lịch sử công tác nhân viên |
| **RecruitmentController** | Quản lý tuyển dụng, ứng viên, phỏng vấn |
| **ProfileController** | Thông tin hồ sơ nhân viên |
| **DashboardController** | Trang dashboard theo vai trò người dùng |
| **ViewController** | Điều hướng view (template pages) |

---

## 📊 Entity Models (15 Models)

### Nhân Viên & Tổ Chức
- **NhanVien** - Thông tin nhân viên
- **TaiKhoan** - Tài khoản người dùng
- **PhongBan** - Bộ phận
- **ChiNhanh** - Chi nhánh công ty
- **ChucVu** - Chức vụ
- **Nhom** - Nhóm công tác

### Lương & Tài Chính
- **BangLuong** - Bảng lương
- **PhieuLuong** - Phiếu lương
- **QuyetDinh** - Quyết định hành chính

### Nhân Sự & Tuyển Dụng
- **HoSoUngVien** - Hồ sơ ứng viên
- **YeuCauTuyenDung** - Yêu cầu tuyển dụng
- **LichPhongVan** - Lịch phỏng vấn

### Công Tác & Tính Công
- **ChamCong** - Chấm công
- **DonNghiPhep** - Đơn xin nghỉ phép
- **LichSuCongTac** - Lịch sử công tác

---

## 🔧 Services (Business Logic)

| Service | Chức Năng |
|---------|----------|
| **EmployeeService** | Xử lý logic quản lý nhân viên |
| **OrganizationService** | Xử lý logic tổ chức (bộ phận, chi nhánh, chức vụ) |
| **PayrollService** | Xử lý logic tính lương, phiếu lương |
| **HistoryService** | Xử lý logic lịch sử công tác |
| **CustomUserDetailsService** | Xác thực người dùng (Spring Security) |
| **ResetPasswordService** | Xử lý đặt lại mật khẩu |

---

## 🖥️ Frontend Templates (Thymeleaf)

| Template | Mục Đích |
|----------|---------|
| **login.html** | Trang đăng nhập |
| **index.html** | Trang chủ |
| **quan-ly-nhan-vien.html** | Danh sách quản lý nhân viên |
| **them-nhan-vien.html** | Form thêm nhân viên |
| **ho-so-nhan-vien.html** | Hồ sơ nhân viên |
| **phong-ban.html** | Quản lý bộ phận |
| **chi-nhanh.html** | Quản lý chi nhánh |
| **chuc-vu.html** | Quản lý chức vụ |
| **nhom.html** | Quản lý nhóm |
| **luong.html** | Quản lý lương |
| **ban-hanh-quyet-dinh.html** | Ban hành quyết định |
| **dashboard/** | Dashboard cho các vai trò (admin, director, hr, employee) |

---

## 🔐 Công Nghệ & Thư Viện Chính

### Core Frameworks
- **Spring Boot Starter Web** - REST API
- **Spring Boot Starter Security** - Bảo mật ứng dụng
- **Spring Boot Starter Data JPA** - ORM & Database access
- **Spring Boot Starter Thymeleaf** - Template engine
- **Spring Boot Starter Mail** - Gửi email
- **Spring Boot Starter Batch** - Xử lý batch jobs

### Authentication
- **JWT (JJWT 0.11.5)** - Token-based authentication

### Database
- **MySQL Connector/J** - Driver MySQL

### Utilities
- **Lombok** - Code generation
- **Validation** - Input validation

### Frontend
- **Bootstrap 5.3** - CSS Framework
- **jQuery 4.0** - JavaScript library

---

## 🗄️ Database Configuration

```properties
# MySQL Configuration
Database: hrm_system
Host: localhost:3306
DDL-Auto: update (auto schema creation)

# Email Configuration
SMTP: smtp.gmail.com:587
Authentication: Enabled
```

---

## 📝 Các Tính Năng Chính

1. ✅ **Quản lý nhân viên** - CRUD nhân viên, hồ sơ, thông tin cá nhân
2. ✅ **Quản lý tổ chức** - Bộ phận, chi nhánh, chức vụ, nhóm
3. ✅ **Lương & Payroll** - Tính lương, phiếu lương, bảng lương
4. ✅ **Tuyển dụng** - Quản lý ứng viên, yêu cầu, lịch phỏng vấn
5. ✅ **Chấm công & Nghỉ phép** - Quản lý chấm công, đơn xin nghỉ
6. ✅ **Lịch sử công tác** - Theo dõi quá trình công tác nhân viên
7. ✅ **Quyết định hành chính** - Ban hành & quản lý quyết định
8. ✅ **Xác thực & Phân quyền** - Spring Security + JWT
9. ✅ **Dashboard** - Trang tổng quan theo vai trò (Manager, HR, Director, Employee)
10. ✅ **Gửi email** - Thông báo qua email

---

## 🚀 Công Nghệ Stack

```
Frontend:  HTML5 + CSS3 (Bootstrap 5) + JavaScript (jQuery)
Backend:   Java 25 + Spring Boot 4.0.3
Database:  MySQL
Security:  Spring Security + JWT
ORM:       JPA/Hibernate
ViewEngine: Thymeleaf
Build:     Maven
```

---

## 📦 Maven Dependencies Chính

- spring-boot-starter-web
- spring-boot-starter-security
- spring-boot-starter-data-jpa
- spring-boot-starter-mail
- spring-boot-starter-thymeleaf
- spring-boot-starter-validation
- spring-boot-starter-batch
- io.jsonwebtoken (JWT)
- mysql-connector-j
- lombok
- spring-boot-devtools

---

## 🔄 Luồng Xác Thực

1. User login → AuthController
2. Validate credentials → CustomUserDetailsService
3. Generate JWT Token → JJWT
4. Return token to client
5. Client include token in Authorization header
6. System validates token for each request

---

## 📂 Cấu Trúc Thư Mục

```
hrm-system/
├── src/
│   ├── main/
│   │   ├── java/com/Group117/hrm_system/
│   │   │   ├── Controller/
│   │   │   ├── entity/
│   │   │   ├── service/
│   │   │   ├── Repository/
│   │   │   └── config/
│   │   └── resources/
│   │       ├── application.properties
│   │       ├── static/ (CSS, JS, Images)
│   │       └── templates/ (Thymeleaf HTML)
│   └── test/
├── target/ (Build output)
├── pom.xml (Maven configuration)
└── README.md
```

---

## ✨ Ghi Chú Kỹ Thuật

- **Java Version:** 25 (Latest)
- **Spring Boot Version:** 4.0.3
- **ORM:** Hibernate (via Spring Data JPA)
- **Security:** Spring Security 6 + JWT
- **Database:** MySQL with Hibernate DDL-auto: update
- **Email:** Gmail SMTP with TLS
- **Logging:** Enabled in application.properties (show-sql: true)

---

## Conversation summary (ngắn)

Các hướng chỉnh đã thống nhất trong phiên làm việc gần đây:

1. **Chuyển dashboard HR:** URL cũ (`/chi-nhanh`, `/phong-ban`, …) → `ViewController` chỉ `redirect` sang `/dashboard/hr/...`; màn tổ chức nằm trong `templates/dashboard/hr/org/*`, dùng `dashboard-layout` + cookie JWT. Template HTML độc lập cũ đã loại bỏ; `module.md` có bảng URL chuẩn HR.

2. **Phân quyền:** `OrganizationController` (ghi) và `HistoryController` (ban hành quyết định) dùng `ADMIN` + `HR`.

3. **HR — thao tác nghiệp vụ trên UI:** Trang Lương có thêm tính lương tháng + lưu cấu trúc lương; Tuyển dụng có thêm ứng viên, PV, trạng thái, onboard, xóa; Tổ chức (tổng quan) có lọc tháng, tạo/xóa **yêu cầu tuyển dụng** (đầu vào nhu cầu theo phòng ban, nối với hồ sơ ứng viên).

4. **Tổ chức chi tiết:** Chi nhánh / Phòng ban / Nhóm / Chức vụ có đủ **Sửa / Xóa** (REST `PUT`/`DELETE` bổ sung cho nhóm & chức vụ; UI modal + cột thao tác).

5. Module 6 — Notifications (WebSocket): đã tích hợp STOMP + SockJS; lưu DB thong_bao trước, push sau khi commit; frontend subscribe /topic/notifications (broadcast) và /user/queue/notifications (private), đồng bộ lịch sử/đếm chưa đọc qua REST /api/notifications/*.

6. Dashboard ADMIN: thay dashboard/admin.html bằng các trang con dashboard/admin/overview|accounts|permissions|settings; API quản lý tài khoản nằm dưới /api/dashboard/admin/* (tạo/khóa-mở/đổi role/gán NV/reset MK).

7 Dashboard director: done 
----
