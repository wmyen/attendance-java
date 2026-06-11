# 變更提案 (Proposal) — 出缺勤管理系統完整規劃

## 1. 變更動機 (Why)
現有系統已完成基礎骨架（Phase 1-8），包含後端 Spring Boot API、前端 Vue 3 頁面、JWT 認證等。但系統缺乏測試覆蓋、資料庫初始化腳本、補休自動產生機制，且前後端尚未實際整合驗證。需要系統性地完善四大核心模組，使其達到生產可用狀態。

## 2. 變更範圍 (What)
- [ ] **P0 基礎建設**: 資料庫 schema 初始化、前後端依賴安裝、測試框架建置
- [ ] **P1 權限管理**: 登入/登出、JWT Refresh、使用者 CRUD、部門管理、Email 密碼通知、強制改密碼
- [ ] **P2 員工打卡**: 上班打卡、下班打卡、今日狀態查詢、月度出勤報表、遲到/早退偵測
- [ ] **P3 請假/加班**: 請假申請（含假別選擇）、加班申請、主管簽核、假別餘額查詢、Email 通知
- [ ] **P4 代理人制度**: 代理人指定、請假時代理人通知、代理人出勤代理

## 3. 能力契約 (Capabilities)
> 此部分將決定後續 Spec 檔案的生成。

- **新增能力**:
    - `db-initialization` — 資料庫自動初始化 (schema + seed data)
    - `compensatory-leave` — 加班核准後自動產生補休餘額
    - `attendance-export` — 出勤資料匯出
- **修改能力**:
    - `auth-management` — 登入/登出/JWT/改密碼（已實作，需補測試）
    - `user-management` — 使用者 CRUD（已實作，需補測試）
    - `department-management` — 部門 CRUD（已實作，需補測試）
    - `clock-in-out` — 打卡（已實作，需補測試）
    - `leave-management` — 請假申請與簽核（已實作，需補測試與補休邏輯）
    - `overtime-management` — 加班申請與簽核（已實作，需補測試與補休邏輯）
    - `agent-system` — 代理人指定（已實作 Entity 層級，需補通知邏輯）

## 4. 影響評估
- **架構影響**: 低 — 維持現有分層架構（Controller → Service → Repository）
- **API 變動**: 新增補休相關邏輯（OvertimeService.approve 內增加），無破壞性變更
- **依賴項增減**: 無新依賴，現有依賴已足夠
