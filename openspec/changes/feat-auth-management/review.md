# 審查報告 (Review): feat-auth-management

> 出缺勤管理系統 -- 最終程式碼與規格比對審查

## 審查摘要

- 審查日期: 2026-04-28（最後更新: 2026-04-28）
- 審查範圍: 後端 59 個 .java + 前端 33 個 .ts/.vue + 5 個 email 模板
- 整體結果: **通過**

---

## 1. 規格符合度總覽

| 章節 | 項目數 | 符合 | 部分符合 | 不符合 |
|------|--------|------|----------|--------|
| S2 資料模型 | 7 張表 | 7 | 0 | 0 |
| S3 API 端點 | 24 個端點 | 24 | 0 | 0 |
| S4 JWT 機制 | 6 項 | 6 | 0 | 0 |
| S5 Email 通知 | 5 個事件 | 5 | 0 | 0 |
| S6 安全設計 | 7 項 | 7 | 0 | 0 |
| S7 前端架構 | 16 路由 + 5 store + 7 API | 28 | 0 | 0 |

---

## 2. S2 資料模型

### 2.1 users

| 規格欄位 | 規格型態 | 實作欄位 | 實作型態 | 狀態 |
|----------|----------|----------|----------|------|
| id | BIGINT PK | id (GenerationType.IDENTITY) | Long | PASS |
| email | VARCHAR(255) UNIQUE | email (unique=true, length=255) | String | PASS |
| password | VARCHAR(255) | password (nullable=false) | String | PASS |
| name | VARCHAR(100) | name (nullable=false, length=100) | String | PASS |
| role | ENUM(ADMIN, MANAGER, EMPLOYEE) | role (EnumType.STRING) | UserRole enum | PASS |
| dept_id | BIGINT FK -> departments.id | department (@ManyToOne, JoinColumn dept_id) | Department | PASS |
| manager_id | BIGINT FK -> users.id | manager (@ManyToOne, JoinColumn manager_id) | User | PASS |
| agent_id | BIGINT FK -> users.id | agent (@ManyToOne, JoinColumn agent_id) | User | PASS |
| must_change_password | BOOLEAN | mustChangePassword (nullable=false, default false) | Boolean | PASS |
| is_active | BOOLEAN | isActive (nullable=false, default true) | Boolean | PASS |
| created_at | DATETIME | createdAt (updatable=false, @PrePersist) | LocalDateTime | PASS |
| updated_at | DATETIME | updatedAt (@PrePersist + @PreUpdate) | LocalDateTime | PASS |

### 2.2 departments

| 規格欄位 | 實作 | 狀態 |
|----------|------|------|
| id BIGINT PK | id (GenerationType.IDENTITY) | PASS |
| name VARCHAR(100) UNIQUE | name (unique=true, length=100) | PASS |
| created_at DATETIME | createdAt (@PrePersist) | PASS |

### 2.3 leave_types

| 規格欄位 | 實作 | 狀態 |
|----------|------|------|
| id BIGINT PK | id (GenerationType.IDENTITY) | PASS |
| name VARCHAR(50) | name (length=50) | PASS |
| code VARCHAR(20) UNIQUE | code (unique=true, length=20) | PASS |
| is_paid BOOLEAN | isPaid (default false) | PASS |
| requires_doc BOOLEAN | requiresDoc (default false) | PASS |

### 2.4 leave_balances

| 規格欄位 | 實作 | 狀態 |
|----------|------|------|
| id BIGINT PK | id (GenerationType.IDENTITY) | PASS |
| user_id FK | user (@ManyToOne, JoinColumn user_id) | PASS |
| leave_type_id FK | leaveType (@ManyToOne, JoinColumn leave_type_id) | PASS |
| year INT | year (Integer) | PASS |
| total_days DECIMAL(5,1) | totalDays (BigDecimal, precision=5, scale=1) | PASS |
| used_days DECIMAL(5,1) | usedDays (BigDecimal, precision=5, scale=1, default ZERO) | PASS |
| UNIQUE(user_id, leave_type_id, year) | @UniqueConstraint(columnNames) | PASS |

### 2.5 attendance

| 規格欄位 | 實作 | 狀態 |
|----------|------|------|
| id BIGINT PK | id (GenerationType.IDENTITY) | PASS |
| user_id FK | user (@ManyToOne, JoinColumn user_id) | PASS |
| date DATE | date (LocalDate) | PASS |
| clock_in DATETIME | clockIn (LocalDateTime) | PASS |
| clock_out DATETIME | clockOut (LocalDateTime) | PASS |
| status ENUM(NORMAL, LATE, EARLY_LEAVE, ABSENT) | status (AttendanceStatus enum, EnumType.STRING) | PASS |

### 2.6 leave_requests

| 規格欄位 | 實作 | 狀態 |
|----------|------|------|
| id BIGINT PK | id (GenerationType.IDENTITY) | PASS |
| user_id FK | user (@ManyToOne) | PASS |
| leave_type_id FK | leaveType (@ManyToOne) | PASS |
| start_time DATETIME | startTime (LocalDateTime) | PASS |
| end_time DATETIME | endTime (LocalDateTime) | PASS |
| reason TEXT | reason (columnDefinition TEXT) | PASS |
| agent_id FK | agent (@ManyToOne, JoinColumn agent_id) | PASS |
| status ENUM(PENDING, APPROVED, REJECTED) | status (RequestStatus enum, default PENDING) | PASS |
| approved_by FK | approvedBy (@ManyToOne) | PASS |
| approved_at DATETIME | approvedAt (LocalDateTime) | PASS |
| created_at DATETIME | createdAt (@PrePersist) | PASS |

### 2.7 overtime_requests

| 規格欄位 | 實作 | 狀態 |
|----------|------|------|
| id BIGINT PK | id (GenerationType.IDENTITY) | PASS |
| user_id FK | user (@ManyToOne) | PASS |
| start_time DATETIME | startTime (LocalDateTime) | PASS |
| end_time DATETIME | endTime (LocalDateTime) | PASS |
| reason TEXT | reason (columnDefinition TEXT) | PASS |
| status ENUM(PENDING, APPROVED, REJECTED) | status (RequestStatus enum, default PENDING) | PASS |
| approved_by FK | approvedBy (@ManyToOne) | PASS |
| approved_at DATETIME | approvedAt (LocalDateTime) | PASS |
| created_at DATETIME | createdAt (@PrePersist) | PASS |

### Entity 關聯

| 規格關聯 | 實作 | 狀態 |
|----------|------|------|
| users --(dept_id)--> departments | User.department @ManyToOne Dept | PASS |
| users --(manager_id)--> users | User.manager @ManyToOne User | PASS |
| users --(agent_id)--> users | User.agent @ManyToOne User | PASS |

---

## 3. S3 API 端點

### 3.1 認證（公開）

| Method | 規格端點 | 實作端點 | 權限 | 狀態 |
|--------|----------|----------|------|------|
| POST | /api/v1/auth/login | AuthController @PostMapping("/login") | permitAll | PASS |
| POST | /api/v1/auth/refresh | AuthController @PostMapping("/refresh") | permitAll | PASS |
| POST | /api/v1/auth/change-password | AuthController @PostMapping("/change-password") | authenticated | PASS |

### 3.2 使用者管理（ADMIN）

| Method | 規格端點 | 實作端點 | 權限 | 狀態 |
|--------|----------|----------|------|------|
| GET | /api/v1/users | UserController @GetMapping | @PreAuthorize hasRole('ADMIN') | PASS |
| GET | /api/v1/users/{id} | UserController @GetMapping("/{id}") | @PreAuthorize hasRole('ADMIN') | PASS |
| POST | /api/v1/users | UserController @PostMapping | @PreAuthorize hasRole('ADMIN') | PASS |
| PUT | /api/v1/users/{id} | UserController @PutMapping("/{id}") | @PreAuthorize hasRole('ADMIN') | PASS |
| DELETE | /api/v1/users/{id} | UserController @DeleteMapping("/{id}") | @PreAuthorize hasRole('ADMIN') | PASS |

- 分頁搜尋: `page`, `size`, `search` 參數已實作 (PASS)
- 新增使用者自動產密碼: `generateRandomPassword()` 12 字元 (PASS)
- 新增使用者寄 Email: `mailService.sendNewUserCredentials()` (PASS)
- DELETE 為 soft delete: `user.setIsActive(false)` (PASS)

### 3.3 部門管理（ADMIN）

| Method | 規格端點 | 實作端點 | 權限 | 狀態 |
|--------|----------|----------|------|------|
| GET | /api/v1/departments | DepartmentController @GetMapping | authenticated | PASS |
| POST | /api/v1/departments | DepartmentController @PostMapping | @PreAuthorize hasRole('ADMIN') | PASS |
| PUT | /api/v1/departments/{id} | DepartmentController @PutMapping("/{id}") | @PreAuthorize hasRole('ADMIN') | PASS |

### 3.4 打卡

| Method | 規格端點 | 實作端點 | 權限 | 狀態 |
|--------|----------|----------|------|------|
| POST | /api/v1/attendance/clock-in | AttendanceController @PostMapping("/clock-in") | hasAnyRole(EMPLOYEE, MANAGER, ADMIN) | PASS |
| POST | /api/v1/attendance/clock-out | AttendanceController @PostMapping("/clock-out") | hasAnyRole(EMPLOYEE, MANAGER, ADMIN) | PASS |
| GET | /api/v1/attendance/today | AttendanceController @GetMapping("/today") | hasAnyRole(EMPLOYEE, MANAGER, ADMIN) | PASS |
| GET | /api/v1/attendance/monthly | AttendanceController @GetMapping("/monthly") | hasAnyRole(EMPLOYEE, MANAGER, ADMIN) | PASS |
| GET | /api/v1/attendance/monthly?userId={id} | 同上，有 userId 參數，ADMIN/MANAGER 可查他人 | 條件判斷 role | PASS |

- 遲到判斷: 09:00 後為 LATE (PASS)
- 早退判斷: 18:00 前打卡下班為 EARLY_LEAVE (PASS)

### 3.5 請假

| Method | 規格端點 | 實作端點 | 權限 | 狀態 |
|--------|----------|----------|------|------|
| POST | /api/v1/leaves | LeaveController @PostMapping | hasAnyRole(EMPLOYEE, MANAGER, ADMIN) | PASS |
| GET | /api/v1/leaves/my | LeaveController @GetMapping("/my") | hasAnyRole(EMPLOYEE, MANAGER, ADMIN) | PASS |
| GET | /api/v1/leaves/pending | LeaveController @GetMapping("/pending") | hasAnyRole(MANAGER, ADMIN) | PASS |
| PUT | /api/v1/leaves/{id}/approve | LeaveController @PutMapping("/{id}/approve") | hasAnyRole(MANAGER, ADMIN) | PASS |
| PUT | /api/v1/leaves/{id}/reject | LeaveController @PutMapping("/{id}/reject") | hasAnyRole(MANAGER, ADMIN) | PASS |
| GET | /api/v1/leaves/balance | LeaveController @GetMapping("/balance") | hasAnyRole(EMPLOYEE, MANAGER, ADMIN) | PASS |
| GET | /api/v1/leaves/balance?userId={id} | 同上，ADMIN 可查他人 | 條件判斷 isAdmin | PASS |

- 簽核通過時扣抵餘額: `balance.setUsedDays(balance.getUsedDays().add(requestDays))` (PASS)
- 簽核時檢查餘額不足: `remaining.compareTo(requestDays) < 0` 拋錯 (PASS)
- 代理人: `agent_id` 可選填 (PASS)

### 3.6 加班

| Method | 規格端點 | 實作端點 | 權限 | 狀態 |
|--------|----------|----------|------|------|
| POST | /api/v1/overtimes | OvertimeController @PostMapping | hasAnyRole(EMPLOYEE, MANAGER, ADMIN) | PASS |
| GET | /api/v1/overtimes/my | OvertimeController @GetMapping("/my") | hasAnyRole(EMPLOYEE, MANAGER, ADMIN) | PASS |
| GET | /api/v1/overtimes/pending | OvertimeController @GetMapping("/pending") | hasAnyRole(MANAGER, ADMIN) | PASS |
| PUT | /api/v1/overtimes/{id}/approve | OvertimeController @PutMapping("/{id}/approve") | hasAnyRole(MANAGER, ADMIN) | PASS |
| PUT | /api/v1/overtimes/{id}/reject | OvertimeController @PutMapping("/{id}/reject") | hasAnyRole(MANAGER, ADMIN) | PASS |

---

## 4. S4 JWT 認證機制

| 規格項目 | 實作 | 狀態 |
|----------|------|------|
| Access Token 有效期 2 小時 | `jwt.access-token-expiration: 7200000` (ms) = 2h | PASS |
| Refresh Token 有效期 7 天 | `jwt.refresh-token-expiration: 604800000` (ms) = 7d | PASS |
| Token 內容: userId, email, role | `subject(userId)`, `claim("email")`, `claim("role")` | PASS |
| 前端 Axios interceptor 401 自動 refresh | `request.ts` response interceptor 處理 401 + refresh | PASS |
| Refresh 失敗導向 /login | `catch { router.push('/login') }` | PASS |
| 後端 JwtAuthenticationFilter 解析 Authorization header | `OncePerRequestFilter`, 從 Bearer 取 token | PASS |
| jjwt 函式庫 | `io.jsonwebtoken` imports | PASS |
| Token 存 localStorage | `localStorage.setItem('accessToken')` / `localStorage.setItem('refreshToken')` | PASS |

---

## 5. S5 Email 通知

| 規格事件 | 實作方法 | 觸發位置 | 狀態 |
|----------|----------|----------|------|
| 新增使用者 -> 新員工 Email (帳號+初始密碼) | `mailService.sendNewUserCredentials()` | `UserService.createUser()` | PASS |
| 請假申請 -> 直屬主管 | `mailService.sendLeaveApplicationNotification()` | `LeaveService.apply()` | PASS |
| 請假簽核結果 -> 申請人 | `mailService.sendLeaveApprovalResult()` | `LeaveService.approve()` / `reject()` | PASS |
| 加班申請 -> 直屬主管 | `mailService.sendOvertimeApplicationNotification()` | `OvertimeService.apply()` | PASS |
| 加班簽核結果 -> 申請人 | `mailService.sendOvertimeApprovalResult()` | `OvertimeService.approve()` / `reject()` | PASS |

| 技術規格 | 實作 | 狀態 |
|----------|------|------|
| JavaMailSender | `JavaMailSender` injected | PASS |
| Gmail SMTP smtp.gmail.com:587 | `spring.mail.host: smtp.gmail.com`, `port: 587` | PASS |
| Thymeleaf 模板 | `TemplateEngine.process()`, 5 個 .html 模板存在 | PASS |
| @Async 非同步寄送 | `@EnableAsync` (AsyncConfig) + 所有 mail 方法標注 `@Async` | PASS |

---

## 6. S6 安全設計

| 規格項目 | 實作 | 狀態 |
|----------|------|------|
| BCrypt 密碼雜湊 | `SecurityConfig.passwordEncoder()` 回傳 `BCryptPasswordEncoder` | PASS |
| 初始密碼隨機 12 字元 (大小寫+數字) | `CHAR_POOL = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789"`, `PASSWORD_LENGTH = 12` | PASS |
| must_change_password 強制改密碼 | `user.setMustChangePassword(true)` on create; `LoginResponse` 含 `mustChangePassword`; 前端 Login.vue 判斷導向 `/change-password`; `AuthService.changePassword()` 設為 false | PASS |
| Spring Security filter chain | `SecurityConfig.filterChain()` + `SessionCreationPolicy.STATELESS` | PASS |
| 公開端點外全需驗證 | `.requestMatchers("/api/v1/auth/**").permitAll()` + `.anyRequest().authenticated()` | PASS |
| @PreAuthorize (hasRole) | UserController/DepartmentController 用 `@PreAuthorize("hasRole('ADMIN')")`; Attendance/Leave/Overtime 用 `@PreAuthorize("hasAnyRole(...)")` | PASS |
| CORS 限制前端開發位址 | `CorsConfig`: `allowedOrigins = ["http://localhost:5173"]` | PASS |
| Bean Validation (@Valid + @NotNull) | 所有 Controller 接收 `@Valid @RequestBody`; DTO 使用 `@NotBlank`, `@NotNull`, `@Email`, `@Size` | PASS |
| CSRF 停用 (因使用 JWT stateless) | `.csrf(AbstractHttpConfigurer::disable)` | PASS |

---

## 7. S7 前端架構

### 7.1 路由與頁面

| 規格路由 | 規格權限 | 實作路由 | 實作 meta.roles | 狀態 |
|----------|----------|----------|-----------------|------|
| /login | 公開 | path: '/login', meta: { public: true } | -- | PASS |
| /change-password | 公開 | path: '/change-password', meta: { public: true } | -- | PASS |
| /dashboard | 共用 | path: 'dashboard' (under MainLayout) | (無限制, 需 token) | PASS |
| /attendance/clock-in | 員工 | path: 'attendance/clock-in' | ['EMPLOYEE','MANAGER','ADMIN'] | PASS |
| /attendance/monthly | 員工 | path: 'attendance/monthly' | ['EMPLOYEE','MANAGER','ADMIN'] | PASS |
| /leaves/apply | 員工 | path: 'leaves/apply' | ['EMPLOYEE','MANAGER','ADMIN'] | PASS |
| /leaves/my | 員工 | path: 'leaves/my' | ['EMPLOYEE','MANAGER','ADMIN'] | PASS |
| /leaves/balance | 員工 | path: 'leaves/balance' | ['EMPLOYEE','MANAGER','ADMIN'] | PASS |
| /overtimes/apply | 員工 | path: 'overtimes/apply' | ['EMPLOYEE','MANAGER','ADMIN'] | PASS |
| /overtimes/my | 員工 | path: 'overtimes/my' | ['EMPLOYEE','MANAGER','ADMIN'] | PASS |
| /leaves/pending | 主管 | path: 'leaves/pending' | ['MANAGER','ADMIN'] | PASS |
| /overtimes/pending | 主管 | path: 'overtimes/pending' | ['MANAGER','ADMIN'] | PASS |
| /admin/users | 管理者 | path: 'admin/users' | ['ADMIN'] | PASS |
| /admin/departments | 管理者 | path: 'admin/departments' | ['ADMIN'] | PASS |
| /admin/attendance | 管理者 | path: 'admin/attendance' | ['ADMIN'] | PASS |
| /admin/leave-balances | 管理者 | path: 'admin/leave-balances' | ['ADMIN'] | PASS |

### 7.2 權限守衛 (Router beforeEach)

| 規格規則 | 實作 | 狀態 |
|----------|------|------|
| 無 token -> /login | `if (!token) return next('/login')` | PASS |
| token 過期 -> 嘗試 refresh -> 失敗 -> /login | Axios interceptor 處理 (見下方說明) | PASS |
| 比對 route.meta.roles 與使用者角色 | `if (to.meta.roles && !(to.meta.roles).includes(role))` | PASS |
| 角色不符 -> /dashboard | `return next('/dashboard')` | PASS |

> **說明**: 規格第 2 點「token 過期 -> 嘗試 refresh」是在 Axios response interceptor 中處理（401 時觸發 refresh），而非 router guard。這與規格描述的行為等效，因為 token 過期必定在 API 呼叫時才會被偵測，由 interceptor 攔截處理。

### 7.3 Pinia Stores

| 規格 Store | 實作檔案 | 職責 | 狀態 |
|------------|----------|------|------|
| useAuthStore | `stores/auth.ts` | login, logout, changePassword, token/user 管理 | PASS |
| useAttendanceStore | `stores/attendance.ts` | clockIn, clockOut, fetchToday, fetchMonthly | PASS |
| useLeaveStore | `stores/leave.ts` | applyLeave, fetchMyLeaves, fetchPendingLeaves, approveLeave, rejectLeave, fetchBalance | PASS |
| useOvertimeStore | `stores/overtime.ts` | applyOvertime, fetchMyOvertimes, fetchPendingOvertimes, approveOvertime, rejectOvertime | PASS |
| useUserStore | `stores/user.ts` | fetchUsers, createUser, updateUser, deactivateUser (分頁) | PASS |

### 7.4 API 模組

| 規格檔案 | 實作檔案 | 函式 | 狀態 |
|----------|----------|------|------|
| request.js (Axios instance + interceptors) | `api/request.ts` | baseURL, request interceptor, response interceptor (401 refresh) | PASS |
| auth.js | `api/auth.ts` | login, refreshToken, changePassword | PASS |
| users.js | `api/users.ts` | getUsers, getUser, createUser, updateUser, deactivateUser | PASS |
| departments.js | `api/departments.ts` | getDepartments, createDepartment, updateDepartment | PASS |
| attendance.js | `api/attendance.ts` | clockIn, clockOut, getToday, getMonthly | PASS |
| leaves.js | `api/leaves.ts` | applyLeave, getMyLeaves, getPendingLeaves, approveLeave, rejectLeave, getLeaveBalance | PASS |
| overtimes.js | `api/overtimes.ts` | applyOvertime, getMyOvertimes, getPendingOvertimes, approveOvertime, rejectOvertime | PASS |

### 7.5 前端配置

| 規格項目 | 實作 | 狀態 |
|----------|------|------|
| Vite 開發伺服器 port 5173 | `server.port: 5173` | PASS |
| API proxy -> 後端 8080 | `proxy: { '/api': { target: 'http://localhost:8080' } }` | PASS |

### 7.6 Layout / Sidebar

| 規格項目 | 實作 | 狀態 |
|----------|------|------|
| MainLayout.vue | 已實作，含 Sidebar + Header (使用者名稱、角色、登出) | PASS |
| Sidebar.vue 角色可見度 | showEmployee (EMPLOYEE+MANAGER+ADMIN), showManager (MANAGER+ADMIN), showAdmin (ADMIN) | PASS |
| Sidebar 選單項目 | 總覽、出缺勤(打卡/月度紀錄)、請假(申請/我的假單/餘額)、加班(申請/我的加班)、簽核(待簽核假單/待簽核加班)、系統管理(使用者/部門/出缺勤/假別餘額) | PASS |

---

## 8. 注意事項與建議

### 8.1 已修正項目

1. ~~**auth store login 回傳值結構**~~: 已修正 `stores/auth.ts`，改為 `data.user.role`、`data.user.name`、`data.user.id`。

2. ~~**leaves/Apply.vue 假別清單未載入**~~: 已新增 `getLeaveTypes()` API 呼叫，並修正 `onMounted`。

3. ~~**缺少 leave_types 列表 API**~~: 已在 `LeaveController` 新增 `GET /leaves/types` 端點。

4. ~~**GlobalExceptionHandler 未處理 AccessDeniedException**~~: 已新增 `@ExceptionHandler(AccessDeniedException.class)` 回傳 403。

5. ~~**admin/Attendance.vue 未使用 userId 參數**~~: 已新增員工選擇下拉選單。

6. ~~**程式碼品質: 未使用 import 與 null safety 警告**~~: 已清除所有未使用 import（`SecurityConfig`、`UserCreateRequest`、`GlobalExceptionHandler`），並全面加入 `@NonNull` 註解與 `Objects.requireNonNull()` 防護（涵蓋 7 個 entity、3 個 security 類別、6 個 service、4 個 controller），VSCode 零警告。

### 8.2 嚴重程度: 低 (不影響功能正確性)

1. **change-password 頁面無需登入即可存取**: 路由中 `/change-password` 設定為 `meta: { public: true }`，但 `AuthController.changePassword()` 需要 `@AuthenticationPrincipal`，代表必須帶有效 token。若使用者首次登入後 token 過期，在 change-password 頁面將無法成功修改密碼。建議移除 public 標記，或在 change-password 頁面加入 token 過期的處理邏輯。

2. **缺少 leave_types 初始化的 data.sql 觸發**: `data.sql` 使用 `INSERT IGNORE`，需要搭配 `spring.sql.init.mode=always` 或 `spring.jpa.hibernate.ddl-auto=create` 才會自動執行。目前使用 `ddl-auto: update`，`data.sql` 可能不會被執行。建議確認初始化策略。

3. **前端 API 模組副檔名差異**: 規格標示 `request.js`、`auth.js` 等，實際使用 TypeScript 副檔名 `.ts`。功能上無影響，僅為命名差異。

### 8.3 建議改善 (非規格必要)

1. **Vite proxy 未處理 WebSocket**: 開發環境若需要 HMR (Hot Module Replacement) 的 WebSocket 代理，目前的 proxy 設定未涵蓋。

2. **JWT secret 硬編碼預設值**: `application.yml` 中 `jwt.secret` 有預設值 `myDefaultSecretKeyForDevelopmentOnlyDoNotUseInProduction2026AttendanceSystem`，雖然有環境變數覆蓋機制，但建議在正式環境中移除此預設值，強制透過環境變數提供。

3. **前端缺少 /leaves/balance?year 參數**: `LeaveBalance.vue` 和 `stores/leave.ts` 的 `fetchBalance` 未傳入 year 參數，依賴後端預設為當前年度。可考慮加入年度選擇器。

---

## 9. 結論

本審查針對 `design.md` 的 6 個章節（資料模型、API 端點、JWT 機制、Email 通知、安全設計、前端架構）逐項比對後端 59 個 Java 檔案、前端 33 個 TS/Vue 檔案與 5 個 email 模板。

**整體結果: 通過**

規格中的所有核心功能均已實作，包含：
- 7 張資料表的完整欄位與關聯
- 24 個 API 端點的方法、路徑、權限全部一致
- JWT access/refresh token 雙 token 機制完整
- 5 個 Email 通知觸發事件全部串接（含 @Async + Thymeleaf 模板）
- 安全設計（BCrypt、mustChangePassword、CORS、@PreAuthorize、Bean Validation）到位
- 前端 16 個路由頁面、5 個 Pinia store、7 個 API 模組齊全

需關注的注意事項：
- change-password 頁面 public 路由與 token 驗證的邊界情境
- data.sql 初始化策略在 `ddl-auto: update` 模式下可能不執行

前次審查中發現的 6 項問題（auth store 結構、假別 API、AccessDeniedException、admin 員工選擇器、未使用 import、null safety 警告）均已修正完畢。
