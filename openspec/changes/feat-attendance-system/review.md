# 變更審查 (Review) — 出缺勤管理系統

## 1. 實作一致性檢查
- [ ] 代碼是否符合 `design.md` 的決策？
  - [ ] 補休自動產生邏輯在 OvertimeService.approve() 中
  - [ ] 打卡狀態判定規則（09:00 上班 / 18:00 下班）
  - [ ] 三層角色（ADMIN / MANAGER / EMPLOYEE）權限正確
  - [ ] JWT 雙 Token 機制運作正常
- [ ] 所有 `spec.md` 中的 Scenarios 是否皆已通過測試？
  - [ ] 認證與授權（4 scenarios）
  - [ ] 使用者管理（5 scenarios）
  - [ ] 部門管理（3 scenarios）
  - [ ] 員工打卡（8 scenarios）
  - [ ] 請假管理（7 scenarios）
  - [ ] 加班管理（4 scenarios）
  - [ ] 代理人制度（3 scenarios）
  - [ ] Email 通知（5 scenarios）

## 2. 工程品質檢查
- **代碼審核結果**: 待執行
- **測試覆蓋率**: 目標 > 80%（Service 層）
- **API 文件**: 待確認是否需要 Swagger/OpenAPI 整合

## 3. GSD 狀態確認
- **環境清理狀態**: 待執行最後的 sync
- **分支狀態**: 確認 main 分支乾淨

## 4. 批准狀態
- **審查人/AI**: 待審查
- **結論**: [待定]

## 5. 驗收標準 (Acceptance Criteria)
- [ ] 後端 `mvn test` 全部通過
- [ ] 前端 `npm run build` 成功
- [ ] 資料庫初始化腳本可正確建立所有資料表
- [ ] 完整使用者流程（登入→打卡→請假→簽核）E2E 驗證通過
- [ ] 三種角色權限隔離驗證通過
- [ ] Email 通知在所有觸發點正確發送
- [ ] 補休在加班核准後自動產生
- [ ] 代理人資訊正確記錄與通知
