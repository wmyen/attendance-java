# Brainstorming: feat-auth-management（出缺勤管理系統）

## 1. GSD 環境掃描
- **當前上下文狀態**: 全新專案，僅有 OpenSpec 治理配置，無現有程式碼
- **專案健康度評估**: 綠地專案，無技術債

## 1.5 現有系統分析 (As-Is Analysis)
- **受影響的現有模組**: 無（新專案）
- **現有邏輯盲點/技術債**: 無

## 2. 問題定義
- **核心目標**: 建立出缺勤管理系統，涵蓋認證權限、打卡、請假/加班/簽核、Email 通知
- **利害關係人需求**:
  - 管理者：管理使用者、部門，查詢全公司出缺勤
  - 主管：簽核部屬請假/加班申請
  - 員工：打卡、申請請假/加班、查看餘額
- **技術限制與邊界**:
  - 前端：Vite + Vue 3 + Element Plus
  - 後端：Java Spring Boot 3.x
  - 資料庫：本機 MySQL
  - Email：Gmail SMTP

## 3. 解決方案方案探索
### 方案 A: Monolith Spring Boot + Vue SPA（選定）
- **優點**: 架構簡單、部署容易、Spring 生態完整
- **缺點/代價**: 後期水平擴展需重構

### 方案 B: Spring Cloud 微服務
- **優點**: 各服務獨立部署擴展
- **缺點/代價**: 初期建置成本高，對此規模過度設計

### 方案 C: Spring Boot + Nuxt.js SSR
- **優點**: SEO 較好
- **缺點/代價**: 內部系統不需要 SEO，增加複雜度

## 4. 最終決策路徑
- **選定方案**: 方案 A — Monolith Spring Boot + Vue SPA
- **關鍵理由**: 企業內部系統、使用者量可控、最務實的選擇

## 需求確認摘要
- **帳號管理**: Email 為帳號，管理者建立帳號時自動產密碼寄 Email，員工不可自行註冊
- **角色**: 三角色 — ADMIN / MANAGER / EMPLOYEE
- **簽核**: 單層主管簽核
- **打卡**: 網頁按鈕打卡
- **假別**: 固定假別（特休、事假、病假、喪假等）
- **Email**: Gmail SMTP
- **前端 UI**: Element Plus
- **代理人制度**: 員工可指定代理人，請假申請時顯示代理人資訊

## 專案分解（依依賴順序）
1. **feat-auth-management** — 認證、權限、使用者/部門 CRUD、Email 通知（本 feature）
2. **feat-attendance-clock** — 上下班打卡核心
3. **feat-leave-overtime** — 請假/加班/補休/年假/代理人/簽核流程
