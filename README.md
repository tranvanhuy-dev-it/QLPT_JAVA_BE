# Nhà Trọ Thông Minh - Backend (Spring Boot)

Đây là mã nguồn Backend của hệ thống **Nhà Trọ Thông Minh** - giải pháp SaaS quản lý dãy trọ và phòng trọ chuyên nghiệp dành cho Chủ trọ và Khách thuê.

---

## 1. Công Nghệ Sử Dụng (Tech Stack)

* **Ngôn ngữ**: Java 17
* **Framework**: Spring Boot 3.x
* **Security & Auth**: Spring Security + JWT (JSON Web Token)
* **ORM & Database Access**: Spring Data JPA + Hibernate
* **Cơ sở dữ liệu**: PostgreSQL
* **Build tool**: Maven
* **Thư viện phụ trợ**: Lombok, Mapstruct / DTO Mapping, Jackson JSON

---

## 2. Các Tính Năng Core

* **Xác thực & Phân quyền (Security & Auth)**:
  * Đăng ký, đăng nhập nhận token JWT thời hạn 24 giờ.
  * Phân quyền vai trò hệ thống (`ADMIN`, `LANDLORD`, `TENANT`).
* **Cơ chế SaaS Subscriptions**:
  * Đăng ký mới tự động kích hoạt **45 ngày dùng thử miễn phí**.
  * Quản lý gói cước gia hạn (3, 6, 12 tháng) kết hợp sinh nội dung chuyển khoản ngẫu nhiên.
  * Bộ lọc `JwtAuthenticationFilter` tự động chặn tài khoản hết hạn (trả về lỗi `402 Payment Required`), chỉ ngoại lệ cho các API thiết yếu (Profile, Đổi mật khẩu, Gia hạn, Thông báo).
* **Quản lý Dãy Trọ & Phòng Trọ**:
  * Thêm/sửa dãy trọ, đơn giá điện nước mặc định, cách tính tiền nước (`BY_INDEX` / `FIXED_PER_PERSON`).
  * Tích hợp tài khoản ngân hàng nhận thanh toán (Ngân hàng, STK, Tên chủ tài khoản) để hiển thị VietQR.
  * Thiết lập nội quy, quy định chi tiết cho từng dãy trọ.
  * Quản lý phòng trọ (Số phòng, sức chứa, chỉ số điện nước công tơ ban đầu).
* **Quản lý Hợp Đồng & Khách Thuê**:
  * Tạo hợp đồng thuê, liên kết phòng với khách thuê đại diện.
  * Ghi nhận tiền đặt cọc, đơn giá thỏa thuận, ngày bắt đầu và kết thúc.
  * Quản lý phụ lục hợp đồng, lịch sử phụ lục và thanh lý hợp đồng.
* **Hóa Đơn & Ghi Nhận Thanh Toán**:
  * Lập hóa đơn hàng tháng dựa trên chênh lệch chỉ số điện/nước cũ và mới.
  * Tính hợp phụ phí dịch vụ định kỳ (Wifi, vệ sinh, quản lý...).
  * Cho phép thêm giảm giá (discount) và tự động tính tổng tiền cần thu.
  * Ghi nhận thanh toán hóa đơn (`PENDING` -> `PAID`), đối soát công nợ chi tiết.
* **Hệ Thống Thông Báo**:
  * Tự động gửi thông báo thời gian thực khi có hóa đơn mới, phê duyệt thanh toán, kích hoạt hợp đồng hoặc duyệt gói cước dịch vụ.

---

## 3. Cấu Trúc Mã Nguồn (Project Structure)

```text
com.qlpt.backend
├── config                      # Cấu hình Spring Security, JWT Filter, CORS
│   ├── JwtAuthenticationFilter.java
│   └── SecurityConfig.java
├── controller                  # Khai báo các REST API endpoints
│   ├── AuthController.java
│   ├── AdminController.java
│   ├── UserController.java
│   ├── RoomController.java
│   ├── ContractController.java
│   └── SubscriptionController.java
├── dto                         # Định nghĩa các Data Transfer Objects (Requests/Responses)
├── entity                      # Định nghĩa các thực thể ánh xạ JPA Hibernate
│   ├── User.java
│   ├── BoardingHouse.java
│   ├── Room.java
│   ├── Contract.java
│   ├── Invoice.java
│   └── UpgradeRequest.java
├── repository                  # Các Interfaces Spring Data JPA Repository
├── service                     # Triển khai tầng Business Logic
│   └── impl
└── exception                   # Xử lý ngoại lệ tập trung (GlobalExceptionHandler)
```

---

## 4. Cài Đặt & Chạy Ứng Dụng

### 4.1. Yêu cầu Hệ thống
* Java Development Kit (JDK) 17 hoặc mới hơn.
* Apache Maven 3.x.
* Cơ sở dữ liệu PostgreSQL.

### 4.2. Cấu hình Cơ sở dữ liệu
Chỉnh sửa thông tin kết nối cơ sở dữ liệu trong file `src/main/resources/application.yml`:
```yaml
spring:
  datasource:
    url: jdbc:postgresql://<host>:<port>/<db_name>
    username: <username>
    password: <password>
    driver-class-name: org.postgresql.Driver
```

### 4.3. Lệnh chạy ứng dụng
1. Tải và cài đặt các dependencies:
   ```bash
   mvn clean install
   ```
2. Chạy ứng dụng Spring Boot ở môi trường phát triển:
   ```bash
   mvn spring-boot:run
   ```
3. Đóng gói ứng dụng thành file `.jar`:
   ```bash
   mvn clean package -DskipTests
   ```
   File `.jar` sẽ được tạo ra tại thư mục `target/`. Bạn có thể khởi chạy bằng lệnh:
   ```bash
   java -jar target/backend-0.0.1-SNAPSHOT.jar
   ```
