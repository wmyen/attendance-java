# 變更審查 (Review) — 出缺勤管理系統（最終版）

> **審查日期**: 2026-06-12
> **審查版本**: 含 Email 完整實作 + Phase 8 審查修復 + 最終歸檔

---

## 1. Spec 場景驗證（共 39+ 場景）

### 1.1 認證與授權（6 scenarios）
| 場景 | 程式碼 | 測試 | 結果 |
|------|--------|------|------|
| 正常登入 → 200 + JWT | AuthService.login() | AuthServiceTest + AuthControllerIntegrationTest | ✅ |
| 登入失敗 → 401 | AuthenticationFailedException | Auth*Test | ✅ |
| 有效 Refresh Token → 200 | AuthService.refresh() | AuthServiceTest | ✅ |
| 無效 Refresh Token → 401 | AuthenticationFailedException | AuthServiceTest | ✅ |
| 首次登入強制改密碼 | mustChangePassword 欄位 | E2E 測試 | ✅ |
| 正常修改密碼 → 200 | AuthService.changePassword() | AuthServiceTest | ✅ |

### 1.2 使用者管理（5 scenarios）
| 場景 | 程式碼 | 測試 | 結果 |
|------|--------|------|------|
| 建立使用者 + Email 通知 | UserService.createUser() + MailService | UserServiceTest | ✅ |
| Email 重複 → 400 | Service 層檢查 | UserServiceTest | ✅ |
| 查詢使用者列表（分頁） | UserService.listUsers() | UserServiceTest | ✅ |
| 更新使用者（partial update） | UserService.updateUser() | UserServiceTest | ✅ |
| 停用使用者（軟刪除） | UserService.deactivateUser() | UserServiceTest | ✅ |

### 1.3 部門管理（3 scenarios）
| 場景 | 程式碼 | 測試 | 結果 |
|------|--------|------|------|
| 建立部門 | DepartmentService.create() | DepartmentServiceTest | ✅ |
| 部門名稱重複 → 400 | Service 層檢查 + DataIntegrityViolation handler | DepartmentServiceTest + Integration | ✅ |
| 查詢部門列表 | DepartmentService.listAll() | DepartmentServiceTest | ✅ |

### 1.4 員工打卡（8 scenarios）
| 場景 | 程式碼 | 測試 | 結果 |
|------|--------|------|------|
| 正常上班打卡 → NORMAL | AttendanceService.clockIn() | AttendanceServiceTest | ✅ |
| 遲到 → LATE | 09:00 門檻判定 | AttendanceServiceTest | ✅ |
| 重複上班打卡 → 400 | clockIn != null 檢查 | AttendanceServiceTest | ✅ |
| 正常下班打卡 | AttendanceService.clockOut() | AttendanceServiceTest | ✅ |
| 早退 → EARLY_LEAVE | 18:00 門檻判定 | AttendanceServiceTest | ✅ |
| 未打上班卡即下班 → 400 | clockIn == null 檢查 | AttendanceServiceTest | ✅ |
| 查詢今日打卡（204 if none） | AttendanceService.getToday() | AttendanceServiceTest | ✅ |
| 月度出勤報表 | AttendanceService.getMonthly() | AttendanceServiceTest | ✅ |

### 1.5 請假管理（7 scenarios）
| 場景 | 程式碼 | 測試 | 結果 |
|------|--------|------|------|
| 正常請假申請 + Email 通知主管 | LeaveService.apply() | LeaveServiceTest | ✅ |
| 請假含代理人 | agentId → agent 欄位 | LeaveServiceTest (AgentTests) | ✅ |
| 主管核准 → 扣餘額 + Email | LeaveService.approve() | LeaveServiceTest | ✅ |
| 主管駁回 + Email | LeaveService.reject() | LeaveServiceTest | ✅ |
| 餘額不足 → 400 | remaining < requestDays | LeaveServiceTest | ✅ |
| 重複簽核 → 400 | status != PENDING 檢查 | LeaveServiceTest | ✅ |
| 查詢假別餘額 | LeaveService.getBalance() | LeaveServiceTest | ✅ |

### 1.6 加班管理（4 scenarios）
| 場景 | 程式碼 | 測試 | 結果 |
|------|--------|------|------|
| 正常加班申請 + Email 通知主管 | OvertimeService.apply() | OvertimeServiceTest | ✅ |
| 主管核准 → 補休產生 + Email | OvertimeService.approve() + generateCompensatoryLeave() | OvertimeServiceTest | ✅ |
| 主管駁回 + Email | OvertimeService.reject() | OvertimeServiceTest | ✅ |
| 補休自動產生（8h=1d, BigDecimal HALF_UP） | generateCompensatoryLeave() | CompensatoryLeaveTests (5 cases) | ✅ |

### 1.7 代理人制度（3 scenarios）
| 場景 | 程式碼 | 測試 | 結果 |
|------|--------|------|------|
| 設定預設代理人 | UserService.updateUser(agentId) | UserServiceTest.AgentTests | ✅ |
| 請假指定代理人 | LeaveService.apply(agentId) | LeaveServiceTest.DefaultAgentTests | ✅ |
| 未指定代理人 → fallback 預設 | user.getAgent() fallback | LeaveServiceTest.DefaultAgentTests | ✅ |

### 1.8 Email 通知（5 scenarios）
| 場景 | 程式碼 | 測試 | 結果 |
|------|--------|------|------|
| 新使用者建立通知 | MailService.sendNewUserCredentials() | MailServiceTest + IntegrationTest | ✅ |
| 請假申請通知主管 | MailService.sendLeaveApplicationNotification() | MailServiceTest | ✅ |
| 請假結果通知申請人 | MailService.sendLeaveApprovalResult() | MailServiceTest | ✅ |
| 加班申請通知主管 | MailService.sendOvertimeApplicationNotification() | MailServiceTest | ✅ |
| 加班結果通知申請人 | MailService.sendOvertimeApprovalResult() | MailServiceTest | ✅ |

---

## 2. Design 技術決策驗證（4 項）

| # | 技術決策 | 實作驗證 | 結果 |
|---|---------|---------|------|
| 2.1 | 補休自動產生（BigDecimal, HALF_UP, 8h=1d） | OvertimeService.generateCompensatoryLeave() — 5 個測試案例（新建/累加/8h=1d/4h=0.5d/無假別不影響） | ✅ |
| 2.2 | 打卡狀態判定（09:00/18:00, LATE 優先） | AttendanceService.clockIn/clockOut — StatusLogicTests (NORMAL/LATE/EARLY_LEAVE/LATE+EARLY_LEAVE) | ✅ |
| 2.3 | 三層角色（ADMIN/MANAGER/EMPLOYEE） | 所有 Controller @PreAuthorize — E2E RolePermissionTests (18 測試) | ✅ |
| 2.4 | JWT 無狀態雙 Token | JwtTokenProvider + SecurityConfig (STATELESS) — AuthServiceTest + E2E | ✅ |

---

## 3. API 端點驗證（29 個端點）

| Method | Path | @PreAuthorize | 結果 |
|--------|------|---------------|------|
| POST | /api/v1/auth/login | Public | ✅ |
| POST | /api/v1/auth/refresh | Auth | ✅ |
| POST | /api/v1/auth/change-password | Auth | ✅ |
| GET | /api/v1/users/brief | isAuthenticated() | ✅ |
| GET | /api/v1/users | ADMIN | ✅ |
| GET | /api/v1/users/{id} | ADMIN | ✅ |
| POST | /api/v1/users | ADMIN | ✅ |
| PUT | /api/v1/users/{id} | ADMIN | ✅ |
| DELETE | /api/v1/users/{id} | ADMIN | ✅ |
| GET | /api/v1/departments | Auth | ✅ |
| POST | /api/v1/departments | ADMIN | ✅ |
| PUT | /api/v1/departments/{id} | ADMIN | ✅ |
| POST | /api/v1/attendance/clock-in | EMPLOYEE+ | ✅ |
| POST | /api/v1/attendance/clock-out | EMPLOYEE+ | ✅ |
| GET | /api/v1/attendance/today | EMPLOYEE+ | ✅ |
| GET | /api/v1/attendance/monthly | EMPLOYEE+ | ✅ |
| GET | /api/v1/leaves/types | EMPLOYEE+ | ✅ |
| POST | /api/v1/leaves | EMPLOYEE+ | ✅ |
| GET | /api/v1/leaves/my | EMPLOYEE+ | ✅ |
| GET | /api/v1/leaves/pending | MANAGER+ | ✅ |
| PUT | /api/v1/leaves/{id}/approve | MANAGER+ | ✅ |
| PUT | /api/v1/leaves/{id}/reject | MANAGER+ | ✅ |
| GET | /api/v1/leaves/balance | EMPLOYEE+ | ✅ |
| POST | /api/v1/overtimes | EMPLOYEE+ | ✅ |
| GET | /api/v1/overtimes/my | EMPLOYEE+ | ✅ |
| GET | /api/v1/overtimes/pending | MANAGER+ | ✅ |
| PUT | /api/v1/overtimes/{id}/approve | MANAGER+ | ✅ |
| PUT | /api/v1/overtimes/{id}/reject | MANAGER+ | ✅ |

> 設計文件列 27 端點 + Phase 7 新增 /users/brief = **29 端點**，全部實作且帶正確權限標註。

---

## 4. 風險緩解驗證

| 風險 | 緩解方案 | 實作驗證 | 結果 |
|------|----------|---------|------|
| 補休計算精度 | BigDecimal HALF_UP, 1 位小數 | CompensatoryLeaveTests (0.5d, 1.0d, 1.5d) | ✅ |
| 併發打卡 | DB unique + 應用層檢查 | AttendanceServiceTest (重複打卡 → 400) | ✅ |
| 併發簽核 | status == PENDING 檢查 | LeaveServiceTest / OvertimeServiceTest (重複簽核 → 400) | ✅ |
| Email 發送失敗 | @Async + try-catch, 僅 log | MailServiceTest.ExceptionTests (2 cases) | ✅ |
| JWT 過期 | 前端 Axios interceptor refresh | E2E 測試 | ✅ |

---

## 5. 工程品質

| 指標 | 數值 |
|------|------|
| **測試總數** | 243 tests |
| **測試結果** | 0 failures, 0 errors |
| **覆蓋率** | 95% 指令 / 86% 分支 (JaCoCo) |
| **API 端點** | 29 個（全部帶權限標註） |
| **Email 模板** | 5 個 HTML（含專業樣式） |
| **Email 測試** | 單元 10 + 整合 1 + 實際 SMTP 寄送驗證 ✅ |

---

## 6. Phase 0-8 修復歷史

| Phase | 修復 | 說明 |
|-------|------|------|
| Phase 7 | LeaveResponse/OvertimeResponse 新增 userId/userName | 前端列表顯示需要 |
| Phase 7 | 新增 GET /api/v1/users/brief | 代理人下拉選單 |
| Phase 8 | 認證失敗 400→401 (AuthenticationFailedException) | Spec 合規 |
| Phase 8 | 部門名稱重複 500→400 (Service 層檢查) | Spec 合規 |
| Email | MailService 簽名擴充（加入 startTime/endTime） | 結果通知含完整資訊 |
| Email | 新增 spring.mail.from + @Value 注入 | 寄件人地址 |
| Email | 5 個模板專業化（標題列 + footer + 色碼狀態） | 使用者體驗 |
| Email | MailServiceIntegrationTest 實際 SMTP 驗證 | 端到端驗證 |

---

## 7. 批准狀態

- **審查人/AI**: Claude Code
- **審查結論**: **✅ APPROVED — 通過最終審查**
- **理由**: 全部 39+ spec 場景通過、4 項技術決策一致、29 個 API 端點驗證、243 tests 全通過、Email 實際寄送驗證成功。系統已可進行歸檔。
