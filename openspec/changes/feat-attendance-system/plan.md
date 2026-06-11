# 執行計畫 (Plan) — 出缺勤管理系統

## 1. 初始化狀態
- **分支名稱**: `main`
- **GSD 檢查點**: 已完成基礎骨架（Phase 1-8）
- **現有程式碼**: 後端 60+ Java 檔案、前端 35+ TS/Vue 檔案

## 2. 實作微步驟 (Micro-tasks)

### Phase 0: 基礎建設
1. [GSD] 確認當前分支狀態乾淨
2. [Code] 建立 `schema.sql`：定義所有資料表的 DDL（含 foreign keys、unique constraints）
3. [Code] 建立 `data.sql`：種子資料（ADMIN 帳號、預設假別、測試部門）
4. [Verify] 啟動後端 `mvn spring-boot:run` 確認 JPA auto-ddl 正確
5. [Code] 安裝前端依賴 `cd apps/frontend && npm install`
6. [Code] 確認 `vite.config.ts` 的 proxy 設定正確
7. [Verify] 前端 `npm run dev` 可啟動，API proxy 正常
8. [TDD] 建立測試基礎框架：`src/test/java/com/attendance/` 目錄結構

### Phase 1: 權限管理
1. [TDD] 撰寫 `AuthServiceTest`：login 成功/失敗、refresh、changePassword
2. [TDD] 撰寫 `AuthControllerIntegrationTest`：API 端到端驗證
3. [TDD] 撰寫 `UserServiceTest`：createUser（含 Email 驗證）、updateUser、deactivateUser
4. [TDD] 撰寫 `UserControllerIntegrationTest`：CRUD + 權限驗證
5. [TDD] 撰寫 `DepartmentServiceTest`：create、update、list
6. [TDD] 撰寫 `DepartmentControllerIntegrationTest`
7. [GSD] 執行 sync 穩定上下文
8. [Verify] 前端 Login、ChangePassword、Admin Users、Admin Departments 功能正常

### Phase 2: 員工打卡
1. [TDD] 撰寫 `AttendanceServiceTest`：clockIn 正常/遲到/重複、clockOut 正常/早退/未打卡、getToday、getMonthly
2. [TDD] 撰寫 `AttendanceControllerIntegrationTest`
3. [GSD] 執行 sync
4. [Verify] 前端 ClockIn、Monthly 頁面功能正常

### Phase 3: 請假/加班
1. [TDD] 撰寫 `LeaveServiceTest`：apply、approve（含餘額扣減）、reject、getBalance
2. [TDD] 撰寫 `LeaveControllerIntegrationTest`
3. [TDD] 撰寫 `OvertimeServiceTest`：apply、approve、reject
4. [Code] **實作補休自動產生邏輯**：
   - 在 `OvertimeService.approve()` 中計算加班時數
   - 查找 `LeaveType` 中 code 為 `COMPENSATORY` 的假別
   - 查找或建立對應的 `LeaveBalance`
   - 將加班天數累加至 `totalDays`
5. [TDD] 撰寫補休自動產生的測試案例
6. [TDD] 撰寫 `OvertimeControllerIntegrationTest`
7. [GSD] 執行 sync
8. [Verify] 前端 Leave、Overtime 相關頁面全部功能正常
9. [Verify] Email 發送正常（新使用者、請假通知/結果、加班通知/結果）

### Phase 4: 代理人制度
1. [TDD] 撰寫代理人指定相關測試
2. [Verify] 請假時代理人資訊正確顯示在 Email 通知
3. [Verify] 前端使用者管理的代理人欄位、請假申請的代理人選擇
4. [GSD] 執行 sync

### Phase 5: 端到端驗證
1. [Verify] 完整使用者流程：ADMIN 建立使用者 → 使用者登入改密碼 → 打卡 → 請假 → 主管簽核
2. [Verify] 角色權限隔離：EMPLOYEE 不能存取 ADMIN/MANAGER 功能
3. [Verify] 邊界案例：重複打卡、餘額不足、無效 Token、過期 Refresh
4. [Code] 執行全體測試 `mvn test`，確認全部通過

## 3. 提交紀錄 (Commits)
- `test: add database initialization scripts (schema + seed data)`
- `test: add Auth/User/Department service and controller tests`
- `test: add Attendance service and controller tests`
- `test: add Leave/Overtime service and controller tests`
- `feat: implement compensatory leave auto-generation on overtime approval`
- `test: add agent system tests`
- `chore: verify E2E user flows and role permissions`
