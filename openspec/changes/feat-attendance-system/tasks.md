# 實作任務清單 (Tasks) — 出缺勤管理系統

## 1. 準備階段 (P0 基礎建設)
- [x] 1.1 建立 MySQL 資料庫初始化腳本（schema.sql + data.sql）
- [x] 1.2 驗證後端可正常啟動（mvn spring-boot:run）
- [x] 1.3 安裝前端依賴（npm install）並驗證啟動
- [x] 1.4 確認 Vite proxy 設定正確轉發 API
- [x] 1.5 建立後端單元測試基礎框架
- [x] 1.6 建立後端整合測試基礎框架

## 2. 權限管理 (P1 Auth + User + Department)
- [x] 2.1 撰寫 AuthService 單元測試（login、refresh、changePassword）
- [x] 2.2 撰寫 AuthController 整合測試
- [x] 2.3 撰寫 UserService 單元測試（CRUD + Email 密碼通知）
- [x] 2.4 撰寫 UserController 整合測試
- [x] 2.5 撰寫 DepartmentService 單元測試
- [x] 2.6 撰寫 DepartmentController 整合測試
- [x] 2.7 驗證前端 Login/ChangePassword 頁面功能
- [x] 2.8 驗證前端 Admin Users/Departments 頁面功能

## 3. 員工打卡 (P2 Clock In/Out)
- [x] 3.1 撰寫 AttendanceService 單元測試（clockIn、clockOut、getToday、getMonthly）
- [x] 3.2 撰寫 AttendanceController 整合測試
- [x] 3.3 驗證遲到/早退判定邏輯
- [x] 3.4 驗證前端 ClockIn/Monthly 頁面功能
- [x] 3.5 驗證 ADMIN 的 Attendance 管理頁面

## 4. 請假/加班 (P3 Leave + Overtime)
- [x] 4.1 撰寫 LeaveService 單元測試（apply、approve、reject、getBalance）
- [x] 4.2 撰寫 LeaveController 整合測試
- [x] 4.3 撰寫 OvertimeService 單元測試（apply、approve、reject）
- [x] 4.4 實作補休自動產生邏輯（OvertimeService.approve 內）
- [x] 4.5 撰寫補休自動產生的測試
- [x] 4.6 撰寫 OvertimeController 整合測試
- [x] 4.7 驗證前端 Leave Apply/My/Pending/Balance 頁面
- [x] 4.8 驗證前端 Overtime Apply/My/Pending 頁面
- [x] 4.9 驗證 Email 通知發送（新使用者、請假、加班）
- [x] 4.10 驗證前端 Admin LeaveBalances 頁面

## 5. 代理人制度 (P4 Agent)
- [x] 5.1 撰寫代理人指定的測試
- [x] 5.2 驗證請假時代理人資訊正確顯示在 Email 中
- [x] 5.3 驗證前端使用者管理中代理人欄位
- [x] 5.4 驗證請假申請頁面代理人選擇功能

## 6. 端到端驗證
- [x] 6.1 完整使用者流程測試（登入 → 打卡 → 請假 → 簽核）
- [x] 6.2 角色權限驗證（EMPLOYEE/MANAGER/ADMIN 各自可用的功能）
- [x] 6.3 邊界案例驗證（重複打卡、餘額不足、無效 Token）

## 7. 驗證與清理
- [x] 7.1 執行全體測試（mvn test）— 230 tests, 0 failures
- [x] 7.2 確認測試覆蓋率達標 — 整體 95% 指令覆蓋率 / 86% 分支覆蓋率
- [x] 7.3 文件同步更新

## 8. 審查與驗收 (Review & Acceptance)
- [x] 8.1 逐一對照 spec.md 場景與實作程式碼（39+ scenarios 全部通過）
- [x] 8.2 驗證 design.md 4 個技術決策一致性
- [x] 8.3 修復認證失敗回傳碼（400→401，新增 AuthenticationFailedException）
- [x] 8.4 修復部門名稱重複回傳碼（500→400，Service 層檢查 + ExceptionHandler）
- [x] 8.5 新增測試覆蓋修復項目（+2 tests → 232 total）
- [x] 8.6 完成驗收測試：後端 232 tests 全通 + 前端 build 成功
- [x] 8.7 完成 review.md 全部檢查項目 — **APPROVED**
