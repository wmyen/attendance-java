# 實作計畫 (Plan): feat-auth-management

> 出缺勤管理系統 — 認證、權限、使用者管理、部門管理、Email 通知

## 總覽

依據 design.md 的技術棧與 API 規格，將實作拆分為 **8 個 Phase**，每個 Phase 產出可獨立驗證的增量。

| Phase | 名稱 | 主要產出 | 估計檔案數 |
|-------|------|----------|-----------|
| 1 | Spring Boot 鷹架與基礎配置 | 專案骨架、pom.xml、application.yml | ~8 |
| 2 | 資料庫 Entity 與 Repository | 7 張表的 JPA Entity + Repository | ~14 |
| 3 | JWT 認證與 Spring Security | Security 配置、JWT Filter、Auth API | ~10 |
| 4 | 部門與使用者 CRUD | Department / User 的 Service + Controller | ~12 |
| 5 | 出缺勤打卡 | Attendance Service + Controller | ~8 |
| 6 | 請假與加班申請簽核 | Leave / Overtime 的 Service + Controller | ~14 |
| 7 | Email 通知 | Mail Service + Thymeleaf 模板 | ~6 |
| 8 | 前端 Vue 3 應用 | Vite 專案、路由、頁面、API 模組、Pinia Stores | ~30+ |

---

## Phase 1: Spring Boot 鷹架與基礎配置

**目標**: 建立 `apps/backend/` 可啟動的 Spring Boot 3.x 專案骨架。

### 1.1 Maven 專案結構

```
apps/backend/
├── pom.xml
└── src/
    ├── main/
    │   ├── java/com/attendance/
    │   │   ├── AttendanceApplication.java
    │   │   ├── config/
    │   │   ├── controller/
    │   │   ├── service/
    │   │   ├── repository/
    │   │   ├── entity/
    │   │   ├── dto/
    │   │   ├── security/
    │   │   └── exception/
    │   └── resources/
    │       ├── application.yml
    │       └── templates/          # Thymeleaf email templates
    └── test/
        └── java/com/attendance/
```

### 1.2 pom.xml 依賴

| 依賴 | 用途 |
|------|------|
| spring-boot-starter-web | REST API |
| spring-boot-starter-data-jpa | JPA / Hibernate |
| spring-boot-starter-security | Spring Security |
| spring-boot-starter-validation | Bean Validation |
| spring-boot-starter-mail | Email 寄送 |
| spring-boot-starter-thymeleaf | Email 模板 |
| mysql-connector-j | MySQL 驅動 |
| jjwt-api + jjwt-impl + jjwt-jackson | JWT (0.12.x) |
| lombok | 簡化 Boilerplate |
| spring-boot-starter-test | 測試 |

### 1.3 application.yml

- DataSource: MySQL 連線（`jdbc:mysql://localhost:3306/attendance_db`）
- JPA: `ddl-auto: update`（開發期）、`show-sql: true`
- Mail: Gmail SMTP `smtp.gmail.com:587`
- JWT secret + token 有效期設定
- Server port: 8080

### 1.3.1 環境變數設定（`.env`）

所有敏感設定透過環境變數注入，預設值定義於 `application.yml`。專案根目錄的 `.env` 檔案（已在 `.gitignore` 中）用於本地開發：

```properties
# --- MySQL 資料庫 ---
DB_USERNAME=root
DB_PASSWORD=root

# --- Email (Gmail SMTP) ---
MAIL_USERNAME=
MAIL_PASSWORD=

# --- JWT ---
JWT_SECRET=...
```

> **MySQL 注意事項**
> - 需先建立資料庫：`CREATE DATABASE attendance_db;`
> - `application.yml` 預設帳密為 `root/root`，依本地環境調整

> **Email 注意事項（Gmail SMTP）**
> - `MAIL_USERNAME` 填 Gmail 地址
> - `MAIL_PASSWORD` 需使用 **Google 應用程式密碼**（非 Gmail 登入密碼）
> - 取得方式：Google 帳號 → 安全性 → 啟用兩步驟驗證 → 應用程式密碼 → 產生一組密碼
> - SMTP 設定：`smtp.gmail.com:587`（STARTTLS）

| 環境變數 | 對應 application.yml | 預設值 | 說明 |
|---------|---------------------|--------|------|
| `DB_USERNAME` | `spring.datasource.username` | `root` | MySQL 帳號 |
| `DB_PASSWORD` | `spring.datasource.password` | `root` | MySQL 密碼 |
| `MAIL_USERNAME` | `spring.mail.username` | 空 | Gmail 地址 |
| `MAIL_PASSWORD` | `spring.mail.password` | 空 | Google 應用程式密碼 |
| `JWT_SECRET` | `jwt.secret` | 開發用預設值 | JWT 簽章金鑰 |

### 1.4 CORS 配置類別

- `config/CorsConfig.java` — 允許前端 `localhost:5173`（Vite 預設）

### 1.5 全域例外處理

- `exception/GlobalExceptionHandler.java` — `@RestControllerAdvice`
- 統一錯誤回應格式：`{ code, message, timestamp }`

### 1.6 驗證方式

- `mvn compile` 成功
- Application 啟動不報錯（資料庫連線正常）

---

## Phase 2: 資料庫 Entity 與 Repository

**目標**: 對應 design.md 的 7 張資料表，建立 JPA Entity 與 Spring Data Repository。

### 2.1 Entity 清單

| Entity | 對應資料表 | 重點欄位 |
|--------|-----------|---------|
| `User` | users | 自關聯 managerId / agentId；role 為 enum |
| `Department` | departments | name UNIQUE |
| `LeaveType` | leave_types | code UNIQUE；系統初始化植入 |
| `LeaveBalance` | leave_balances | UNIQUE(user, leaveType, year) |
| `Attendance` | attendance | ENUM status |
| `LeaveRequest` | leave_requests | ENUM status；FK 至 user/leaveType/agent/approver |
| `OvertimeRequest` | overtime_requests | ENUM status；FK 至 user/approver |

### 2.2 Enum 定義

- `UserRole`: ADMIN, MANAGER, EMPLOYEE
- `AttendanceStatus`: NORMAL, LATE, EARLY_LEAVE, ABSENT
- `RequestStatus`: PENDING, APPROVED, REJECTED

### 2.3 Repository 清單

| Repository | 自訂方法 |
|------------|---------|
| `UserRepository` | `findByEmail()`, `findByDeptId()`, `findByManagerId()` |
| `DepartmentRepository` | — |
| `LeaveTypeRepository` | `findByCode()` |
| `LeaveBalanceRepository` | `findByUserIdAndYear()`, `findByUserIdAndLeaveTypeIdAndYear()` |
| `AttendanceRepository` | `findByUserIdAndDate()`, `findByUserIdAndDateBetween()` |
| `LeaveRequestRepository` | `findByUserId()`, `findByStatusAndApproverId()` |
| `OvertimeRequestRepository` | `findByUserId()`, `findByStatusAndApproverId()` |

### 2.4 資料庫初始化

- `data.sql` — 植入 5 筆 LeaveType（ANNUAL, PERSONAL, SICK, BEREAVEMENT, COMPENSATORY）
- 植入 1 個 ADMIN 預設帳號（可選）

### 2.5 驗證方式

- Application 啟動後，MySQL 中 7 張表自動建立
- `data.sql` 執行後 LeaveType 有 5 筆資料

---

## Phase 3: JWT 認證與 Spring Security

**目標**: 完成 JWT 發行/驗證機制，實作 3 個 Auth API。

### 3.1 JWT 工具類別

- `security/JwtTokenProvider.java`
  - `generateAccessToken(userId, email, role)` — 2 小時有效
  - `generateRefreshToken(userId, email, role)` — 7 天有效
  - `parseToken(token)` → Claims
  - `validateToken(token)` → boolean

### 3.2 Spring Security 配置

- `config/SecurityConfig.java`
  - Stateless session（`SessionCreationPolicy.STATELESS`）
  - 公開端點：`/api/v1/auth/**`
  - 其餘端點需認證
  - 註冊 `JwtAuthenticationFilter`

- `security/JwtAuthenticationFilter.java`（extends `OncePerRequestFilter`）
  - 從 `Authorization: Bearer <token>` 取得 JWT
  - 解析並設定 `SecurityContextHolder`

### 3.3 UserDetailsService 實作

- `security/CustomUserDetailsService.java`
  - `loadUserByUsername(email)` → `UserDetails`
  - 回傳 `CustomUserDetails`（含 id, email, role）

### 3.4 Auth API

| Endpoint | 處理邏輯 |
|----------|---------|
| `POST /api/v1/auth/login` | 驗證 email/password → 產生 access + refresh token → 回傳 |
| `POST /api/v1/auth/refresh` | 驗證 refresh token → 產生新 access token → 回傳 |
| `POST /api/v1/auth/change-password` | 驗證舊密碼 → BCrypt 新密碼 → 更新 → 若 mustChangePassword 為 true 則改為 false |

### 3.5 Auth DTO

- `dto/auth/LoginRequest.java` — email, password
- `dto/auth/LoginResponse.java` — accessToken, refreshToken, user info
- `dto/auth/RefreshRequest.java` — refreshToken
- `dto/auth/ChangePasswordRequest.java` — oldPassword, newPassword

### 3.6 驗證方式

- 以 Postman/curl 測試 login → 取得 JWT
- 攜帶 JWT 存取受保護端點 → 200
- 無 JWT 存取受保護端點 → 401
- Refresh token 換新 access token → 成功

---

## Phase 4: 部門與使用者 CRUD

**目標**: 實作 Department 與 User 的完整 CRUD（對應 ADMIN 權限）。

### 4.1 部門管理

- `service/DepartmentService.java`
  - `listAll()`, `create()`, `update()`
- `controller/DepartmentController.java`
  - `GET /api/v1/departments`
  - `POST /api/v1/departments` — `@PreAuthorize("hasRole('ADMIN')")`
  - `PUT /api/v1/departments/{id}` — `@PreAuthorize("hasRole('ADMIN')")`
- `dto/department/DepartmentRequest.java` — name
- `dto/department/DepartmentResponse.java`

### 4.2 使用者管理

- `service/UserService.java`
  - `listUsers(pageable, search)` — 分頁 + 搜尋
  - `getUser(id)`, `createUser()`, `updateUser()`, `deactivateUser(id)`（soft delete：`isActive = false`）
  - `createUser` 內部流程：
    1. 隨機產生 12 字元密碼（大小寫 + 數字）
    2. BCrypt 雜湊存入
    3. 設定 `mustChangePassword = true`
    4. 非同步寄 Email（Phase 7 觸發）
- `controller/UserController.java`
  - `GET /api/v1/users` — `@PreAuthorize("hasRole('ADMIN')")`
  - `GET /api/v1/users/{id}` — `@PreAuthorize("hasRole('ADMIN')")`
  - `POST /api/v1/users` — `@PreAuthorize("hasRole('ADMIN')")`
  - `PUT /api/v1/users/{id}` — `@PreAuthorize("hasRole('ADMIN')")`
  - `DELETE /api/v1/users/{id}` — `@PreAuthorize("hasRole('ADMIN')")`（soft delete）
- `dto/user/UserCreateRequest.java` — email, name, role, deptId, managerId, agentId
- `dto/user/UserUpdateRequest.java`
- `dto/user/UserResponse.java`
- `dto/user/UserListResponse.java` — 含分頁資訊

### 4.3 驗證方式

- ADMIN 登入後可 CRUD 部門
- ADMIN 可新增/修改/停用使用者
- 非 ADMIN 呼叫 → 403 Forbidden
- 新增使用者時密碼自動產生且 BCrypt 雜湊

---

## Phase 5: 出缺勤打卡

**目標**: 實作員工打卡與出缺勤紀錄查詢。

### 5.1 Attendance Service

- `service/AttendanceService.java`
  - `clockIn(userId)` — 今日若已打上班卡則拒絕；建立紀錄，計算 status（依據上班時間判斷 LATE）
  - `clockOut(userId)` — 更新下班時間，判斷 EARLY_LEAVE
  - `getToday(userId)` — 查詢今日紀錄
  - `getMonthly(userId, year, month)` — 該月所有紀錄
  - `getMonthlyByUser(targetUserId)` — ADMIN/MANAGER 查特定員工

### 5.2 Attendance Controller

| Endpoint | 權限 |
|----------|------|
| `POST /api/v1/attendance/clock-in` | EMPLOYEE, MANAGER, ADMIN |
| `POST /api/v1/attendance/clock-out` | EMPLOYEE, MANAGER, ADMIN |
| `GET /api/v1/attendance/today` | EMPLOYEE, MANAGER, ADMIN |
| `GET /api/v1/attendance/monthly` | EMPLOYEE, MANAGER, ADMIN |
| `GET /api/v1/attendance/monthly?userId={id}` | ADMIN, MANAGER |

### 5.3 DTO

- `dto/attendance/ClockResponse.java` — id, clockIn, clockOut, status
- `dto/attendance/MonthlyResponse.java` — List of daily records
- `dto/attendance/TodayResponse.java`

### 5.4 驗證方式

- 員工上班打卡 → 回傳紀錄（status: NORMAL 或 LATE）
- 同日重複打卡 → 400 錯誤
- 下班打卡 → 更新 clockOut
- 月度查詢回傳該月所有紀錄

---

## Phase 6: 請假與加班申請簽核

**目標**: 實作請假、加班的申請/簽核流程，含假別餘額管理。

### 6.1 請假（Leave）

- `service/LeaveService.java`
  - `apply(userId, request)` — 建立假單、扣減 leave_balances.usedDays（PENDING 階段先不扣，簽核通過再扣）
  - `getMyLeaves(userId)` — 自己的假單
  - `getPendingLeaves(managerId)` — 直屬下屬待簽核假單
  - `approve(requestId, managerId)` — 更新 status=APPROVED、記錄 approver、扣減餘額
  - `reject(requestId, managerId)` — 更新 status=REJECTED
  - `getBalance(userId, year)` — 該年度各假別餘額
  - `getBalanceByUser(targetUserId, year)` — ADMIN 查特定員工

- `controller/LeaveController.java`

| Endpoint | 權限 |
|----------|------|
| `POST /api/v1/leaves` | EMPLOYEE, MANAGER, ADMIN |
| `GET /api/v1/leaves/my` | EMPLOYEE, MANAGER, ADMIN |
| `GET /api/v1/leaves/pending` | MANAGER, ADMIN |
| `PUT /api/v1/leaves/{id}/approve` | MANAGER, ADMIN |
| `PUT /api/v1/leaves/{id}/reject` | MANAGER, ADMIN |
| `GET /api/v1/leaves/balance` | EMPLOYEE, MANAGER, ADMIN |
| `GET /api/v1/leaves/balance?userId={id}` | ADMIN |

### 6.2 加班（Overtime）

- `service/OvertimeService.java`
  - `apply(userId, request)` — 建立加班單
  - `getMyOvertimes(userId)`
  - `getPendingOvertimes(managerId)` — 直屬下屬待簽核
  - `approve(requestId, managerId)`
  - `reject(requestId, managerId)`

- `controller/OvertimeController.java`

| Endpoint | 權限 |
|----------|------|
| `POST /api/v1/overtimes` | EMPLOYEE, MANAGER, ADMIN |
| `GET /api/v1/overtimes/my` | EMPLOYEE, MANAGER, ADMIN |
| `GET /api/v1/overtimes/pending` | MANAGER, ADMIN |
| `PUT /api/v1/overtimes/{id}/approve` | MANAGER, ADMIN |
| `PUT /api/v1/overtimes/{id}/reject` | MANAGER, ADMIN |

### 6.3 DTO

- `dto/leave/LeaveApplyRequest.java` — leaveTypeId, startTime, endTime, reason, agentId
- `dto/leave/LeaveResponse.java`
- `dto/leave/LeaveBalanceResponse.java`
- `dto/overtime/OvertimeApplyRequest.java` — startTime, endTime, reason
- `dto/overtime/OvertimeResponse.java`

### 6.4 驗證方式

- 員工提交請假 → PENDING 假單建立
- 主管簽核通過 → 餘額扣減、狀態變 APPROVED
- 主管簽核駁回 → 狀態變 REJECTED
- 餘額不足請假 → 400 錯誤
- 加班流程同請假邏輯

---

## Phase 7: Email 通知

**目標**: 依 design.md 5 個觸發事件，實作非同步 Email 寄送。

### 7.1 Mail 配置

- `application.yml` 已於 Phase 1 配置 Gmail SMTP
- 確認 `spring.mail` 設定正確

### 7.2 Mail Service

- `service/MailService.java`
  - `@Async` 非同步寄送
  - 方法：
    - `sendNewUserCredentials(toEmail, name, password)`
    - `sendLeaveApplicationNotification(managerEmail, applicantName, leaveTypeName, startTime, endTime, agentName)`
    - `sendLeaveApprovalResult(applicantEmail, approved, leaveTypeName)`
    - `sendOvertimeApplicationNotification(managerEmail, applicantName, startTime, endTime)`
    - `sendOvertimeApprovalResult(applicantEmail, approved)`

### 7.3 Thymeleaf Email 模板

```
resources/templates/email/
├── new-user.html
├── leave-application.html
├── leave-result.html
├── overtime-application.html
└── overtime-result.html
```

### 7.4 觸發點串接

| 觸發事件 | 串接位置 |
|---------|---------|
| 新增使用者 | `UserService.createUser()` 呼叫 `mailService.sendNewUserCredentials()` |
| 請假申請 | `LeaveService.apply()` 呼叫 `mailService.sendLeaveApplicationNotification()` |
| 請假簽核結果 | `LeaveService.approve()/reject()` 呼叫 `mailService.sendLeaveApprovalResult()` |
| 加班申請 | `OvertimeService.apply()` 呼叫 `mailService.sendOvertimeApplicationNotification()` |
| 加班簽核結果 | `OvertimeService.approve()/reject()` 呼叫 `mailService.sendOvertimeApprovalResult()` |

### 7.5 驗證方式

- 新增使用者後，該使用者 Email 收到帳密信
- 提交請假後，直屬主管 Email 收到通知信
- 簽核後，申請人 Email 收到結果信

---

## Phase 8: 前端 Vue 3 應用

**目標**: 建立 `apps/frontend/` 完整的 Vite + Vue 3 + Element Plus 前端應用。

### 8.1 專案初始化

- Vite + Vue 3 + TypeScript
- 安裝：Element Plus, Vue Router, Pinia, Axios
- 目錄結構如 design.md 定義

### 8.2 Axios 基礎配置

- `src/api/request.js`
  - Axios instance（baseURL: `/api/v1`）
  - Request interceptor：自動帶 Authorization header
  - Response interceptor：401 時嘗試 refresh → 失敗導向 /login

### 8.3 認證頁面

- `src/views/Login.vue` — 登入表單
- `src/views/ChangePassword.vue` — 修改密碼（首次登入強制導入）

### 8.4 路由與權限守衛

- `src/router/index.js`
  - 路由表如 design.md §7 定義（17 條路由）
  - `beforeEach` 守衛：token 驗證 → refresh → 角色比對

### 8.5 Pinia Stores

| Store | 檔案 | 職責 |
|-------|------|------|
| useAuthStore | `stores/auth.js` | login/logout, token 管理, 使用者資訊 |
| useAttendanceStore | `stores/attendance.js` | 打卡操作、月度紀錄 |
| useLeaveStore | `stores/leave.js` | 請假申請、假單列表、餘額 |
| useOvertimeStore | `stores/overtime.js` | 加班申請、列表 |
| useUserStore | `stores/user.js` | 使用者 CRUD（管理者） |

### 8.6 API 模組

```
src/api/
├── request.js
├── auth.js
├── users.js
├── departments.js
├── attendance.js
├── leaves.js
└── overtimes.js
```

### 8.7 頁面組件（依角色）

**共用**
- `views/Dashboard.vue` — 總覽儀表板

**員工**
- `views/attendance/ClockIn.vue` — 打卡頁面
- `views/attendance/Monthly.vue` — 月度出缺勤
- `views/leaves/Apply.vue` — 請假申請
- `views/leaves/My.vue` — 我的假單
- `views/leaves/Balance.vue` — 假別餘額
- `views/overtimes/Apply.vue` — 加班申請
- `views/overtimes/My.vue` — 我的加班

**主管**
- `views/leaves/Pending.vue` — 待簽核假單
- `views/overtimes/Pending.vue` — 待簽核加班

**管理者**
- `views/admin/Users.vue` — 使用者管理 CRUD
- `views/admin/Departments.vue` — 部門管理
- `views/admin/Attendance.vue` — 全公司出缺勤
- `views/admin/LeaveBalances.vue` — 假別餘額管理

### 8.8 驗證方式

- 登入 → JWT 存入 localStorage → 導向 Dashboard
- 無 token → 導向 /login
- EMPLOYEE 看不到 /admin/* 路由
- 管理者可執行使用者 CRUD
- 打卡 → 即時更新 UI
- 請假 → 送出後出現在主管待簽核列表

---

## 執行順序與相依關係

```
Phase 1 (鷹架)
  └─► Phase 2 (Entity/Repo)
       └─► Phase 3 (JWT/Auth) ──► 可獨立驗證：登入取 token
            └─► Phase 4 (Dept/User CRUD) ──► 可獨立驗證：管理使用者
                 ├─► Phase 5 (Attendance)
                 ├─► Phase 6 (Leave + Overtime)
                 └─► Phase 7 (Email) ──► 依附 Phase 4/6 觸發點
Phase 8 (Frontend) ──► 可在 Phase 3 完成後平行啟動
```

## 交付檢查清單

- [ ] Phase 1: `mvn compile` 成功，Application 可啟動
- [ ] Phase 2: 7 張表建立，LeaveType 初始資料植入
- [ ] Phase 3: Login/Refresh/ChangePassword API 可用
- [ ] Phase 4: 部門 CRUD + 使用者 CRUD（含 soft delete）
- [ ] Phase 5: 打卡 + 月度查詢
- [ ] Phase 6: 請假/加班申請簽核 + 餘額管理
- [ ] Phase 7: 5 種 Email 通知寄送正常
- [ ] Phase 8: 前端所有頁面可操作，權限守衛正確
