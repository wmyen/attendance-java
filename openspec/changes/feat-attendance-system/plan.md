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
4. [Code] 執行全體測試 `mvn test`，確認全部通過（230 tests）

### Phase 6: 驗證與清理
1. [Verify] 執行全體測試 `mvn clean test` — 230 tests, 0 failures, 0 errors
2. [Code] 加入 JaCoCo 覆蓋率插件，產生覆蓋率報告
3. [Verify] 覆蓋率達標 — 整體 95% 指令覆蓋率 / 86% 分支覆蓋率
   - Service 層：99% 指令 / 95% 分支
   - Controller 層：98% 指令 / 77% 分支
   - Security 層：93% 指令 / 87% 分支
4. [Code] 文件同步更新 — tasks.md 全部標記完成、plan.md 更新

### Phase 7: 前端驗證
1. [Verify] 前端 TypeScript 編譯 + Vite 建置成功（32 個原始碼檔案）
2. [Verify] API 層 7 個模組端點路徑 + DTO 欄位與後端 Controller 完全一致
3. [Verify] Router 路由守衛：公開頁面、角色權限、未登入導向正確
4. [Verify] 5 個 Pinia Stores 狀態管理正確
5. [Verify] 17 個 Vue 頁面元件完整性
6. [Fix] LeaveResponse/OvertimeResponse 新增 userId、userName 欄位
7. [Fix] 新增 GET /api/v1/users/brief 端點（所有已認證使用者可用）
8. [Fix] leaves/Apply.vue 改用 /users/brief 載入代理人下拉選單
9. [Fix] admin/Users.vue 新增主管/代理人選擇欄位
10. [Verify] 後端 230 測試全通 + 前端 build 成功

### Phase 8: 審查與驗收 (Review & Acceptance)
1. [Verify] 逐一對照 spec.md 39+ 場景與後端實作程式碼
2. [Verify] design.md 4 個技術決策實作一致性
3. [Fix] 認證失敗回傳碼 400→401（新增 AuthenticationFailedException）
4. [Fix] 部門名稱重複 500→400（Service 層檢查 + DataIntegrityViolation handler）
5. [Test] 新增部門名稱重複單元測試 + 整合測試（+2 tests）
6. [Verify] 後端 232 tests, 0 failures ✅
7. [Verify] 前端 TypeScript + Vite build 成功 ✅
8. [Docs] 完成 review.md 全部檢查項目 — **APPROVED**

## 3. 提交紀錄 (Commits)
- `test: add database initialization scripts (schema + seed data)`
- `test: add Auth/User/Department service and controller tests`
- `test: add Attendance service and controller tests`
- `test: add Leave/Overtime service and controller tests`
- `feat: implement compensatory leave auto-generation on overtime approval`
- `test: add agent system tests`
- `chore: verify E2E user flows and role permissions`
- `chore: add JaCoCo coverage report, verify 95% coverage (Phase 6)`
- `fix: add userName to leave/overtime responses, add brief users endpoint for agent selection (Phase 7)`
- `fix: auth failures return 401, dept duplicate name returns 400 (Phase 8 review)`
