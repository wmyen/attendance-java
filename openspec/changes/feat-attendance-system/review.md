# 變更審查 (Review) — 出缺勤管理系統

## 1. 實作一致性檢查
- [x] 代碼是否符合 `design.md` 的決策？
  - [x] 補休自動產生邏輯在 OvertimeService.approve() 中（BigDecimal + HALF_UP）
  - [x] 打卡狀態判定規則（09:00 上班 / 18:00 下班 / LATE 優先）
  - [x] 三層角色（ADMIN / MANAGER / EMPLOYEE）權限正確（@PreAuthorize）
  - [x] JWT 雙 Token 機制運作正常（Access + Refresh, STATELESS）
- [x] 所有 `spec.md` 中的 Scenarios 是否皆已通過測試？
  - [x] 認證與授權（6 scenarios）— 登入成功/失敗/停用帳號、Token 刷新成功/失敗、改密碼
  - [x] 使用者管理（5 scenarios）— CRUD + Email 重複檢查
  - [x] 部門管理（3 scenarios）— 建立/名稱重複/列表查詢
  - [x] 員工打卡（8 scenarios）— 正常/遲到/早退/重複/未打卡/查詢今日/月度報表
  - [x] 請假管理（7 scenarios）— 申請/代理人/核准/駁回/餘額不足/重複簽核/查詢餘額
  - [x] 加班管理（4 scenarios）— 申請/核准(含補休)/駁回/補休自動產生
  - [x] 代理人制度（3 scenarios）— 預設代理人/請假指定/未指定代理人
  - [x] Email 通知（5 scenarios）— 新使用者/請假通知/結果通知/加班通知/加班結果
- [x] 角色權限對照表（spec.md Section 9）實作正確
  - [x] EMPLOYEE: 打卡、查看自己出勤、請假/加班申請、查看自己餘額
  - [x] MANAGER: 同 EMPLOYEE + 查看他人出勤、簽核請假/加班
  - [x] ADMIN: 同 MANAGER + 使用者管理、部門管理、查看他人餘額

## 2. 工程品質檢查
- **代碼審核結果**: ✅ Phase 8 審查完成，發現並修復 2 個規格偏差
- **測試覆蓋率**: ✅ 95% 指令覆蓋率 / 86% 分支覆蓋率（JaCoCo）
- **測試數量**: 232 tests（Phase 0-7: 230 + Phase 8: +2）
- **API 文件**: 已由 design.md Section 3.1 完整記載（27 個端點 + brief endpoint = 28）

### Phase 8 修復項目
| # | 問題 | 修復方式 |
|---|------|----------|
| 1 | 認證失敗回傳 400（spec 要求 401） | 新增 `AuthenticationFailedException` + GlobalExceptionHandler 映射 401 |
| 2 | 部門名稱重複回傳 500（spec 要求 400） | DepartmentService 加入名稱檢查 + DataIntegrityViolationException handler |

## 3. GSD 狀態確認
- **環境清理狀態**: ✅ 已完成
- **分支狀態**: ✅ main 分支乾淨

## 4. 批准狀態
- **審查人/AI**: ✅ Claude Code (Phase 8)
- **結論**: **通過 (APPROVED)** — 所有 spec 場景驗證通過，技術決策一致，232 tests 全通過

## 5. 驗收標準 (Acceptance Criteria)
- [x] 後端 `mvn test` 全部通過（232 tests, 0 failures）
- [x] 前端 `npm run build` 成功
- [x] 資料庫初始化腳本可正確建立所有資料表
- [x] 完整使用者流程（登入→打卡→請假→簽核）E2E 驗證通過
- [x] 三種角色權限隔離驗證通過
- [x] Email 通知在所有觸發點正確發送（@Async + try-catch）
- [x] 補休在加班核准後自動產生（BigDecimal, HALF_UP, 8h=1d）
- [x] 代理人資訊正確記錄與通知（含預設代理人 fallback）
