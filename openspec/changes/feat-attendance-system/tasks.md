# 實作任務清單 (Tasks) — 出缺勤管理系統

## 1. 準備階段 (P0 基礎建設)
- [ ] 1.1 建立 MySQL 資料庫初始化腳本（schema.sql + data.sql）
- [ ] 1.2 驗證後端可正常啟動（mvn spring-boot:run）
- [ ] 1.3 安裝前端依賴（npm install）並驗證啟動
- [ ] 1.4 確認 Vite proxy 設定正確轉發 API
- [ ] 1.5 建立後端單元測試基礎框架
- [ ] 1.6 建立後端整合測試基礎框架

## 2. 權限管理 (P1 Auth + User + Department)
- [ ] 2.1 撰寫 AuthService 單元測試（login、refresh、changePassword）
- [ ] 2.2 撰寫 AuthController 整合測試
- [ ] 2.3 撰寫 UserService 單元測試（CRUD + Email 密碼通知）
- [ ] 2.4 撰寫 UserController 整合測試
- [ ] 2.5 撰寫 DepartmentService 單元測試
- [ ] 2.6 撰寫 DepartmentController 整合測試
- [ ] 2.7 驗證前端 Login/ChangePassword 頁面功能
- [ ] 2.8 驗證前端 Admin Users/Departments 頁面功能

## 3. 員工打卡 (P2 Clock In/Out)
- [ ] 3.1 撰寫 AttendanceService 單元測試（clockIn、clockOut、getToday、getMonthly）
- [ ] 3.2 撰寫 AttendanceController 整合測試
- [ ] 3.3 驗證遲到/早退判定邏輯
- [ ] 3.4 驗證前端 ClockIn/Monthly 頁面功能
- [ ] 3.5 驗證 ADMIN 的 Attendance 管理頁面

## 4. 請假/加班 (P3 Leave + Overtime)
- [ ] 4.1 撰寫 LeaveService 單元測試（apply、approve、reject、getBalance）
- [ ] 4.2 撰寫 LeaveController 整合測試
- [ ] 4.3 撰寫 OvertimeService 單元測試（apply、approve、reject）
- [ ] 4.4 實作補休自動產生邏輯（OvertimeService.approve 內）
- [ ] 4.5 撰寫補休自動產生的測試
- [ ] 4.6 撰寫 OvertimeController 整合測試
- [ ] 4.7 驗證前端 Leave Apply/My/Pending/Balance 頁面
- [ ] 4.8 驗證前端 Overtime Apply/My/Pending 頁面
- [ ] 4.9 驗證 Email 通知發送（新使用者、請假、加班）
- [ ] 4.10 驗證前端 Admin LeaveBalances 頁面

## 5. 代理人制度 (P4 Agent)
- [ ] 5.1 撰寫代理人指定的測試
- [ ] 5.2 驗證請假時代理人資訊正確顯示在 Email 中
- [ ] 5.3 驗證前端使用者管理中代理人欄位
- [ ] 5.4 驗證請假申請頁面代理人選擇功能

## 6. 端到端驗證
- [ ] 6.1 完整使用者流程測試（登入 → 打卡 → 請假 → 簽核）
- [ ] 6.2 角色權限驗證（EMPLOYEE/MANAGER/ADMIN 各自可用的功能）
- [ ] 6.3 邊界案例驗證（重複打卡、餘額不足、無效 Token）

## 7. 驗證與清理
- [ ] 7.1 執行全體測試（mvn test）
- [ ] 7.2 確認測試覆蓋率達標
- [ ] 7.3 文件同步更新
