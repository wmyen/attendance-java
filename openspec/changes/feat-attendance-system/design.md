# 技術設計文件 (Design) — 出缺勤管理系統

## 1. 系統架構

### 1.1 整體架構
```
┌─────────────────────────────────────────────────────┐
│                   Frontend (Vue 3)                   │
│  Vite + TypeScript + Element Plus + Pinia + Router   │
│                    Axios (HTTP)                       │
└────────────────────┬────────────────────────────────┘
                     │ REST API (JSON)
                     ▼
┌─────────────────────────────────────────────────────┐
│                 Backend (Spring Boot 3)               │
│  ┌─────────┐  ┌──────────┐  ┌───────────────────┐  │
│  │Controller│→│  Service  │→│    Repository      │  │
│  │  (REST)  │  │(Business) │  │  (JPA/Hibernate)  │  │
│  └─────────┘  └──────────┘  └───────────────────┘  │
│       │            │                                  │
│  ┌─────────┐  ┌──────────┐                           │
│  │ Security│  │   Mail   │                           │
│  │  (JWT)  │  │(Thymeleaf)│                           │
│  └─────────┘  └──────────┘                           │
└────────────────────┬────────────────────────────────┘
                     │ JDBC
                     ▼
              ┌─────────────┐
              │MySQL (Local) │
              └─────────────┘
```

### 1.2 組件設計
| 層級 | 組件 | 職責 |
|------|------|------|
| **Controller** | AuthController | 登入、Token 刷新、修改密碼 |
| | UserController | 使用者 CRUD（ADMIN only） |
| | DepartmentController | 部門 CRUD（ADMIN only） |
| | AttendanceController | 打卡、今日記錄、月度報表 |
| | LeaveController | 請假申請、簽核、假別餘額 |
| | OvertimeController | 加班申請、簽核 |
| **Service** | AuthService | JWT 產生/驗證、密碼變更 |
| | UserService | 使用者管理、隨機密碼產生 |
| | AttendanceService | 打卡邏輯、遲到/早退判定 |
| | LeaveService | 請假申請、餘額扣減、簽核 |
| | OvertimeService | 加班申請、簽核、**補休產生** |
| | MailService | 非同步 Email 發送 |
| | DepartmentService | 部門管理 |
| **Entity** | User | 使用者（含 manager、agent 關聯） |
| | Department | 部門 |
| | Attendance | 出勤記錄（date, clockIn, clockOut, status） |
| | LeaveRequest | 請假單（含 agent、approvedBy） |
| | LeaveBalance | 假別餘額（user + leaveType + year） |
| | LeaveType | 假別定義（年假、病假、事假、補休等） |
| | OvertimeRequest | 加班申請 |

### 1.3 資料流向
```
打卡流程:
  使用者 → POST /api/v1/attendance/clock-in
  → AttendanceService.clockIn()
  → 判定遲到(NORMAL/LATE) → 存入 DB → 回傳 ClockResponse

請假流程:
  員工 → POST /api/v1/leaves (含 agentId)
  → LeaveService.apply()
  → 存入 DB + 非同步寄 Email 給主管
  → 主管 PUT /api/v1/leaves/{id}/approve
  → LeaveService.approve()
  → 扣減假別餘額 + 寄 Email 給員工

加班流程:
  員工 → POST /api/v1/overtimes
  → OvertimeService.apply()
  → 存入 DB + 非同步寄 Email 給主管
  → 主管 PUT /api/v1/overtimes/{id}/approve
  → OvertimeService.approve()
  → **自動產生補休餘額** + 寄 Email 給員工
```

## 2. 關鍵技術決策

### 2.1 補休自動產生
- **決策內容**: 加班核准時，自動計算加班時數並產生/累加對應年度的補休 LeaveBalance
- **Rationale**: 補休是加班的法定權益，手動管理容易遺漏。自動化確保員工權益不漏。

### 2.2 打卡狀態判定
- **決策內容**: 
  - 上班打卡時間 > 09:00 → `LATE`
  - 下班打卡時間 < 18:00 → `EARLY_LEAVE`
  - 遲到又早退 → 保持 `LATE`（已遲到優先）
  - 當天未打卡 → 由排程任務標記 `ABSENT`
- **Rationale**: 符合台灣勞基法常規工時 09:00-18:00（含午休 1 小時）。

### 2.3 三層角色設計
- **決策內容**: ADMIN / MANAGER / EMPLOYEE 三級角色
- **Rationale**: 
  - ADMIN: 系統管理（使用者/部門/假別設定）
  - MANAGER: 部門管理（簽核部屬的假單/加班單）
  - EMPLOYEE: 一般操作（打卡/申請）

### 2.4 JWT 無狀態認證
- **決策內容**: Access Token + Refresh Token 雙 Token 機制
- **Rationale**: 前後端分離架構標準做法，避免 Session 在分散式環境的問題。

## 3. 介面定義

### 3.1 API Endpoints（現有已實作）

| Method | Path | 角色 | 說明 |
|--------|------|------|------|
| POST | `/api/v1/auth/login` | Public | 登入 |
| POST | `/api/v1/auth/refresh` | Auth | Token 刷新 |
| POST | `/api/v1/auth/change-password` | Auth | 修改密碼 |
| GET | `/api/v1/users` | ADMIN | 使用者列表（分頁+搜尋） |
| GET | `/api/v1/users/{id}` | ADMIN | 取得使用者 |
| POST | `/api/v1/users` | ADMIN | 建立使用者 |
| PUT | `/api/v1/users/{id}` | ADMIN | 更新使用者 |
| DELETE | `/api/v1/users/{id}` | ADMIN | 停用使用者 |
| GET | `/api/v1/departments` | Auth | 部門列表 |
| POST | `/api/v1/departments` | ADMIN | 建立部門 |
| PUT | `/api/v1/departments/{id}` | ADMIN | 更新部門 |
| POST | `/api/v1/attendance/clock-in` | EMPLOYEE+ | 上班打卡 |
| POST | `/api/v1/attendance/clock-out` | EMPLOYEE+ | 下班打卡 |
| GET | `/api/v1/attendance/today` | EMPLOYEE+ | 今日打卡 |
| GET | `/api/v1/attendance/monthly` | EMPLOYEE+ | 月度報表 |
| GET | `/api/v1/leaves/types` | EMPLOYEE+ | 假別列表 |
| POST | `/api/v1/leaves` | EMPLOYEE+ | 請假申請 |
| GET | `/api/v1/leaves/my` | EMPLOYEE+ | 我的請假 |
| GET | `/api/v1/leaves/pending` | MANAGER+ | 待簽核假單 |
| PUT | `/api/v1/leaves/{id}/approve` | MANAGER+ | 核准假單 |
| PUT | `/api/v1/leaves/{id}/reject` | MANAGER+ | 駁回假單 |
| GET | `/api/v1/leaves/balance` | EMPLOYEE+ | 假別餘額 |
| POST | `/api/v1/overtimes` | EMPLOYEE+ | 加班申請 |
| GET | `/api/v1/overtimes/my` | EMPLOYEE+ | 我的加班 |
| GET | `/api/v1/overtimes/pending` | MANAGER+ | 待簽核加班 |
| PUT | `/api/v1/overtimes/{id}/approve` | MANAGER+ | 核准加班 |
| PUT | `/api/v1/overtimes/{id}/reject` | MANAGER+ | 駁回加班 |

### 3.2 資料庫 Schema

```sql
-- 使用者表
users (id, email, password, name, role, dept_id, manager_id, agent_id,
       must_change_password, is_active, created_at, updated_at)

-- 部門表
departments (id, name, created_at)

-- 出勤表
attendance (id, user_id, date, clock_in, clock_out, status)

-- 假別表
leave_types (id, name, code, is_paid, requires_doc)

-- 假別餘額表
leave_balances (id, user_id, leave_type_id, year, total_days, used_days)
  UNIQUE(user_id, leave_type_id, year)

-- 請假單表
leave_requests (id, user_id, leave_type_id, start_time, end_time, reason,
                agent_id, status, approved_by, approved_at, created_at)

-- 加班申請表
overtime_requests (id, user_id, start_time, end_time, reason,
                   status, approved_by, approved_at, created_at)
```

## 4. 風險與緩解 (Risks & Mitigations)

| 風險 | 說明 | 緩解方案 |
|------|------|----------|
| 補休計算精度 | 加班時數轉補休天數的四捨五入爭議 | 使用 BigDecimal，精度到小數點後 1 位，HALF_UP 捨入 |
| 併發打卡 | 同一使用者重複打卡 | DB unique constraint + 應用層檢查 clockIn != null |
| 併發簽核 | 多人同時簽核同一假單 | 檢查 status == PENDING，乐观锁保护 |
| Email 發送失敗 | Mail server 不可用 | @Async + try-catch，記錄日誌但不影響主流程 |
| JWT 過期 | 使用者操作中 Token 過期 | 前端 Axios interceptor 自動 refresh |

## 5. 遷移與部署計畫
- **部署步驟**: 
  1. 初始化 MySQL 資料庫（建立 schema + seed data）
  2. 啟動後端 `mvn spring-boot:run`
  3. 安裝前端依賴 `npm install`
  4. 啟動前端 `npm run dev`
  5. Vite proxy 轉發 API 至後端
- **回滾策略**: Git revert 至前一個 stable commit
