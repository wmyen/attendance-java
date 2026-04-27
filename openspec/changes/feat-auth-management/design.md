# 技術設計文件 (Design): feat-auth-management

> 出缺勤管理系統 — 認證、權限、使用者管理、部門管理、Email 通知

## 1. 系統架構

```
┌─────────────────────────────────────────────────┐
│  Frontend (Vite + Vue 3 + Element Plus)         │
│  apps/frontend/                                 │
│  - JWT 存 localStorage                          │
│  - Axios interceptor 處理 token refresh          │
│  - Vue Router guards 控制角色權限                │
└────────────────────┬────────────────────────────┘
                     │ REST API (JSON)
                     ▼
┌─────────────────────────────────────────────────┐
│  Backend (Spring Boot 3.x)                      │
│  apps/backend/                                  │
│  - Spring Security + JWT (stateless)            │
│  - Spring Data JPA + MySQL                      │
│  - Spring Boot Mail + Gmail SMTP                │
│  - Bean Validation (input 驗證)                 │
│                                                 │
│  分層: Controller → Service → Repository → Entity│
└─────────────────────────────────────────────────┘
```

### Monorepo 結構

```
apps/
├── frontend/           # Vite + Vue 3 + Element Plus
│   ├── src/
│   │   ├── views/      # 頁面組件
│   │   ├── components/ # 共用組件
│   │   ├── api/        # API 呼叫模組
│   │   ├── stores/     # Pinia 狀態管理
│   │   ├── router/     # 路由與權限守衛
│   │   └── utils/      # 工具函式
│   └── ...
└── backend/            # Spring Boot 3.x
    └── src/main/java/com/attendance/
        ├── config/     # Security, Mail, CORS 配置
        ├── controller/ # REST API endpoints
        ├── service/    # 商業邏輯
        ├── repository/ # JPA Repositories
        ├── entity/     # JPA Entities
        ├── dto/        # Request/Response DTOs
        ├── security/   # JWT filter, UserDetails
        └── exception/  # 全域例外處理
```

## 2. 資料模型

### Entity 關聯

```
users ──(dept_id)──► departments
users ──(manager_id)──► users (自關聯，直屬主管)
users ──(agent_id)──► users (自關聯，職務代理人)
```

### users

| 欄位 | 型態 | 說明 |
|------|------|------|
| id | BIGINT PK | 自增主鍵 |
| email | VARCHAR(255) UNIQUE | 登入帳號 |
| password | VARCHAR(255) | BCrypt 雜湊 |
| name | VARCHAR(100) | 員工姓名 |
| role | ENUM(ADMIN, MANAGER, EMPLOYEE) | 角色 |
| dept_id | BIGINT FK → departments.id | 所屬部門 |
| manager_id | BIGINT FK → users.id | 直屬主管（MANAGER/EMPLOYEE 有值） |
| agent_id | BIGINT FK → users.id | 職務代理人（可 null） |
| must_change_password | BOOLEAN | 首次登入需改密碼 |
| is_active | BOOLEAN | 帳號啟用狀態（soft delete） |
| created_at | DATETIME | 建立時間 |
| updated_at | DATETIME | 更新時間 |

### departments

| 欄位 | 型態 | 說明 |
|------|------|------|
| id | BIGINT PK | 自增主鍵 |
| name | VARCHAR(100) UNIQUE | 部門名稱 |
| created_at | DATETIME | 建立時間 |

### leave_types（固定假別，系統初始化時植入）

| 欄位 | 型態 | 說明 |
|------|------|------|
| id | BIGINT PK | 自增主鍵 |
| name | VARCHAR(50) | 假別名稱（特休、事假、病假、喪假、補休...） |
| code | VARCHAR(20) UNIQUE | 代碼（ANNUAL, PERSONAL, SICK, BEREAVEMENT, COMPENSATORY） |
| is_paid | BOOLEAN | 是否有薪 |
| requires_doc | BOOLEAN | 是否需要證明文件 |

### leave_balances（每年度每假別餘額）

| 欄位 | 型態 | 說明 |
|------|------|------|
| id | BIGINT PK | 自增主鍵 |
| user_id | BIGINT FK → users.id | 員工 |
| leave_type_id | BIGINT FK → leave_types.id | 假別 |
| year | INT | 年度 |
| total_days | DECIMAL(5,1) | 總天數 |
| used_days | DECIMAL(5,1) | 已用天數 |

> UNIQUE 約束：`(user_id, leave_type_id, year)`

### attendance

| 欄位 | 型態 | 說明 |
|------|------|------|
| id | BIGINT PK | 自增主鍵 |
| user_id | BIGINT FK → users.id | 員工 |
| date | DATE | 打卡日期 |
| clock_in | DATETIME | 上班打卡時間 |
| clock_out | DATETIME | 下班打卡時間 |
| status | ENUM(NORMAL, LATE, EARLY_LEAVE, ABSENT) | 出缺勤狀態 |

### leave_requests

| 欄位 | 型態 | 說明 |
|------|------|------|
| id | BIGINT PK | 自增主鍵 |
| user_id | BIGINT FK → users.id | 申請人 |
| leave_type_id | BIGINT FK → leave_types.id | 假別 |
| start_time | DATETIME | 開始時間 |
| end_time | DATETIME | 結束時間 |
| reason | TEXT | 事由 |
| agent_id | BIGINT FK → users.id | 職務代理人 |
| status | ENUM(PENDING, APPROVED, REJECTED) | 簽核狀態 |
| approved_by | BIGINT FK → users.id | 簽核主管 |
| approved_at | DATETIME | 簽核時間 |
| created_at | DATETIME | 申請時間 |

### overtime_requests

| 欄位 | 型態 | 說明 |
|------|------|------|
| id | BIGINT PK | 自增主鍵 |
| user_id | BIGINT FK → users.id | 申請人 |
| start_time | DATETIME | 加班開始 |
| end_time | DATETIME | 加班結束 |
| reason | TEXT | 事由 |
| status | ENUM(PENDING, APPROVED, REJECTED) | 簽核狀態 |
| approved_by | BIGINT FK → users.id | 簽核主管 |
| approved_at | DATETIME | 簽核時間 |
| created_at | DATETIME | 申請時間 |

## 3. API 設計

基礎路徑：`/api/v1`

### 認證（公開）

| Method | Endpoint | 說明 |
|--------|----------|------|
| POST | `/auth/login` | 登入，回傳 JWT access + refresh token |
| POST | `/auth/refresh` | 用 refresh token 換新 access token |
| POST | `/auth/change-password` | 修改密碼 |

### 使用者管理（ADMIN）

| Method | Endpoint | 說明 |
|--------|----------|------|
| GET | `/users` | 列出使用者（分頁、搜尋） |
| GET | `/users/{id}` | 取得單一使用者 |
| POST | `/users` | 新增使用者（自動產密碼、寄 Email） |
| PUT | `/users/{id}` | 修改使用者 |
| DELETE | `/users/{id}` | 停用使用者（soft delete） |

### 部門管理（ADMIN）

| Method | Endpoint | 說明 |
|--------|----------|------|
| GET | `/departments` | 列出部門 |
| POST | `/departments` | 新增部門 |
| PUT | `/departments/{id}` | 修改部門 |

### 打卡

| Method | Endpoint | 說明 |
|--------|----------|------|
| POST | `/attendance/clock-in` | 上班打卡 |
| POST | `/attendance/clock-out` | 下班打卡 |
| GET | `/attendance/today` | 今日打卡紀錄 |
| GET | `/attendance/monthly` | 月度出缺勤 |
| GET | `/attendance/monthly?userId={id}` | 查詢特定員工（ADMIN/MANAGER） |

### 請假

| Method | Endpoint | 說明 |
|--------|----------|------|
| POST | `/leaves` | 提交請假申請 |
| GET | `/leaves/my` | 自己的假單 |
| GET | `/leaves/pending` | 待簽核假單（MANAGER） |
| PUT | `/leaves/{id}/approve` | 簽核通過 |
| PUT | `/leaves/{id}/reject` | 簽核駁回 |
| GET | `/leaves/balance` | 自己假別餘額 |
| GET | `/leaves/balance?userId={id}` | 特定員工餘額（ADMIN） |

### 加班

| Method | Endpoint | 說明 |
|--------|----------|------|
| POST | `/overtimes` | 提交加班申請 |
| GET | `/overtimes/my` | 自己的加班申請 |
| GET | `/overtimes/pending` | 待簽核加班（MANAGER） |
| PUT | `/overtimes/{id}/approve` | 簽核通過 |
| PUT | `/overtimes/{id}/reject` | 簽核駁回 |

## 4. JWT 認證機制

- **Access Token**: 有效期 2 小時，存 localStorage
- **Refresh Token**: 有效期 7 天，存 localStorage
- **Token 內容**: userId, email, role
- **前端**: Axios interceptor 在 401 時自動 refresh，失敗導向 /login
- **後端**: `JwtAuthenticationFilter` 從 Authorization header 解析驗證
- **函式庫**: `jjwt` (io.jsonwebtoken)

## 5. Email 通知

### 觸發點

| 事件 | 收件人 | 內容 |
|------|--------|------|
| 新增使用者 | 新員工 Email | 帳號 + 初始密碼 |
| 請假申請 | 直屬主管 | 申請人、假別、時間、代理人 |
| 請假簽核結果 | 申請人 | 核准/駁回 |
| 加班申請 | 直屬主管 | 申請人、加班時段 |
| 加班簽核結果 | 申請人 | 核准/駁回 |

### 實作

- Spring Boot Mail + `JavaMailSender`
- Gmail SMTP (`smtp.gmail.com:587`)，application.yml 配置
- Thymeleaf 模板產生 HTML Email
- `@Async` 非同步寄送，不阻塞 API 回應

## 6. 安全設計

- **密碼**: BCrypt 雜湊，Spring Security 預設強度
- **初始密碼**: 隨機 12 字元（大小寫 + 數字）
- **首次登入**: `must_change_password = true` 強制改密碼
- **API 安全**: Spring Security filter chain，公開端點外全需驗證
- **角色控管**: `@PreAuthorize` (hasRole)
- **CORS**: 限制為前端開發位址
- **輸入驗證**: Bean Validation (`@Valid` + `@NotNull` 等)

## 7. 前端架構

### 路由與頁面

```
/login                          公開
/change-password                公開
/dashboard                      共用
/attendance/clock-in            員工
/attendance/monthly             員工
/leaves/apply                   員工
/leaves/my                      員工
/leaves/balance                 員工
/overtimes/apply                員工
/overtimes/my                   員工
/leaves/pending                 主管
/overtimes/pending              主管
/admin/users                    管理者
/admin/departments              管理者
/admin/attendance               管理者
/admin/leave-balances           管理者
```

### 權限守衛 (Router beforeEach)

1. 無 token → /login
2. token 過期 → 嘗試 refresh → 失敗 → /login
3. 比對 route.meta.roles 與使用者角色
4. 角色不符 → /dashboard

### Pinia Stores

| Store | 職責 |
|-------|------|
| useAuthStore | 登入/登出、token、使用者資訊 |
| useAttendanceStore | 打卡、月度紀錄 |
| useLeaveStore | 請假申請、假單、餘額 |
| useOvertimeStore | 加班申請、列表 |
| useUserStore | 管理者使用者 CRUD |

### API 模組

```
src/api/
├── request.js       # Axios instance + interceptors
├── auth.js
├── users.js
├── departments.js
├── attendance.js
├── leaves.js
└── overtimes.js
```
