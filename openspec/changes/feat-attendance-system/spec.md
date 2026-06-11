# 規格書: 出缺勤管理系統

> **⚠️ OPEN_SPEC DELTA 標記規範 (CRITICAL)**: 
> 本文件記錄系統完整功能規格，所有標題皆標註 Delta 類型。

---

## 1. 認證與授權 (Authentication & Authorization)

### [ADDED] Requirement: 使用者登入
系統 **MUST** 提供以 Email + 密碼的登入機制，成功後回傳 JWT Access Token 與 Refresh Token。

#### [ADDED] Scenario: 正常登入
- **WHEN**: 使用者以正確的 Email 與密碼呼叫 `POST /api/v1/auth/login`
- **THEN**: 回傳 200 + `LoginResponse`（含 accessToken、refreshToken、role、mustChangePassword）
- **AND**: Token 有效期符合 SecurityConfig 設定

#### [ADDED] Scenario: 登入失敗
- **WHEN**: 使用者以錯誤的 Email 或密碼登入
- **THEN**: 回傳 401 Unauthorized

### [ADDED] Requirement: Token 刷新
系統 **MUST** 提供 Refresh Token 機制，用於在 Access Token 過期後取得新的 Token。

#### [ADDED] Scenario: 有效 Refresh Token
- **WHEN**: 以有效的 Refresh Token 呼叫 `POST /api/v1/auth/refresh`
- **THEN**: 回傳 200 + 新的 `LoginResponse`

#### [ADDED] Scenario: 無效 Refresh Token
- **WHEN**: 以無效或過期的 Refresh Token 刷新
- **THEN**: 回傳 401 Unauthorized

### [ADDED] Requirement: 修改密碼
系統 **MUST** 允許已登入的使用者修改密碼。

#### [ADDED] Scenario: 首次登入強制改密碼
- **WHEN**: 使用者 `mustChangePassword = true` 時登入
- **THEN**: 前端導向修改密碼頁面，強制使用者設定新密碼

#### [ADDED] Scenario: 正常修改密碼
- **WHEN**: 使用者呼叫 `POST /api/v1/auth/change-password` 提供正確的舊密碼與新密碼
- **THEN**: 密碼更新成功，`mustChangePassword` 設為 false
- **AND**: 回傳 200

---

## 2. 使用者管理 (User Management)

### [ADDED] Requirement: 使用者 CRUD
系統 **MUST** 允許 ADMIN 角色進行使用者的建立、查詢、更新、停用操作。

#### [ADDED] Scenario: 建立使用者
- **WHEN**: ADMIN 呼叫 `POST /api/v1/users` 提供完整的 user 資料
- **THEN**: 系統自動產生隨機密碼，加密儲存，並寄送 Email 通知新使用者
- **AND**: `mustChangePassword` 設為 true
- **AND**: `isActive` 設為 true
- **AND**: 回傳 200 + `UserResponse`

#### [ADDED] Scenario: Email 重複
- **WHEN**: 建立使用者時 Email 已存在
- **THEN**: 回傳 400 + 錯誤訊息「Email 已被使用」

#### [ADDED] Scenario: 查詢使用者列表
- **WHEN**: ADMIN 呼叫 `GET /api/v1/users` 可帶分頁參數與搜尋
- **THEN**: 回傳分頁的使用者列表

#### [ADDED] Scenario: 更新使用者
- **WHEN**: ADMIN 呼叫 `PUT /api/v1/users/{id}` 更新使用者資料
- **THEN**: 僅更新提供的欄位（partial update），回傳 200 + `UserResponse`

#### [ADDED] Scenario: 停用使用者
- **WHEN**: ADMIN 呼叫 `DELETE /api/v1/users/{id}`
- **THEN**: 使用者 `isActive` 設為 false（軟刪除），回傳 200

---

## 3. 部門管理 (Department Management)

### [ADDED] Requirement: 部門 CRUD
系統 **MUST** 允許 ADMIN 管理部門，一般使用者可查詢部門列表。

#### [ADDED] Scenario: 建立部門
- **WHEN**: ADMIN 呼叫 `POST /api/v1/departments`
- **THEN**: 建立部門，回傳 200 + `DepartmentResponse`

#### [ADDED] Scenario: 部門名稱重複
- **WHEN**: 建立部門時名稱已存在
- **THEN**: 回傳 400 + 錯誤訊息

#### [ADDED] Scenario: 查詢部門列表
- **WHEN**: 任何已登入使用者呼叫 `GET /api/v1/departments`
- **THEN**: 回傳所有部門列表

---

## 4. 員工打卡 (Clock In/Out)

### [ADDED] Requirement: 上下班打卡
系統 **MUST** 記錄員工的上下班打卡時間，並自動判定出勤狀態。

#### [ADDED] Scenario: 正常上班打卡
- **WHEN**: 員工於 09:00（含）之前呼叫 `POST /api/v1/attendance/clock-in`
- **THEN**: 記錄 clockIn 時間，狀態設為 `NORMAL`

#### [ADDED] Scenario: 遲到上班打卡
- **WHEN**: 員工於 09:00 之後打卡上班
- **THEN**: 記錄 clockIn 時間，狀態設為 `LATE`

#### [ADDED] Scenario: 重複上班打卡
- **WHEN**: 員工已打上班卡，再次呼叫 clock-in
- **THEN**: 回傳 400 + 「今日已打上班卡」

#### [ADDED] Scenario: 正常下班打卡
- **WHEN**: 員工於 18:00（含）之後呼叫 `POST /api/v1/attendance/clock-out`
- **THEN**: 記錄 clockOut 時間，若上班未遲到則狀態保持 `NORMAL`

#### [ADDED] Scenario: 早退下班打卡
- **WHEN**: 員工於 18:00 之前打卡下班
- **THEN**: 狀態設為 `EARLY_LEAVE`（除非已為 LATE）

#### [ADDED] Scenario: 未打上班卡即打下班卡
- **WHEN**: 員工尚未打上班卡即呼叫 clock-out
- **THEN**: 回傳 400 + 「今日尚未打上班卡」

#### [ADDED] Scenario: 查詢今日打卡
- **WHEN**: 員工呼叫 `GET /api/v1/attendance/today`
- **THEN**: 回傳今日打卡記錄（若存在），若無則回傳 204 No Content

#### [ADDED] Scenario: 月度出勤報表
- **WHEN**: 員工呼叫 `GET /api/v1/attendance/monthly`（可指定年月）
- **THEN**: 回傳該月份所有出勤記錄
- **AND**: MANAGER/ADMIN 可透過 `userId` 參數查看其他員工的出勤

---

## 5. 請假管理 (Leave Management)

### [ADDED] Requirement: 請假申請
系統 **MUST** 允許員工提出請假申請，支援假別選擇與代理人指定。

#### [ADDED] Scenario: 正常請假申請
- **WHEN**: 員工呼叫 `POST /api/v1/leaves` 提供假別、起迄時間、事由
- **THEN**: 建立請假單（狀態 `PENDING`），回傳 200 + `LeaveResponse`
- **AND**: 若使用者有主管，非同步寄送 Email 通知主管

#### [ADDED] Scenario: 請假含代理人
- **WHEN**: 請假申請包含 `agentId`
- **THEN**: 請假單記錄代理人，Email 通知中包含代理人資訊

#### [ADDED] Scenario: 主管核准請假
- **WHEN**: 主管呼叫 `PUT /api/v1/leaves/{id}/approve`
- **THEN**: 
  - 檢查假單狀態為 `PENDING`
  - 檢查假別餘額是否足夠
  - 扣減假別餘額（usedDays += 請假天數）
  - 狀態改為 `APPROVED`
  - 記錄簽核人與時間
  - 寄送 Email 通知申請人
  - 回傳 200 + `LeaveResponse`

#### [ADDED] Scenario: 主管駁回請假
- **WHEN**: 主管呼叫 `PUT /api/v1/leaves/{id}/reject`
- **THEN**: 狀態改為 `REJECTED`，記錄簽核人與時間，寄送 Email 通知申請人

#### [ADDED] Scenario: 餘額不足核准失敗
- **WHEN**: 主管核准假單但假別餘額不足
- **THEN**: 回傳 400 + 「假別餘額不足」

#### [ADDED] Scenario: 重複簽核
- **WHEN**: 對已簽核（非 PENDING）的假單再次簽核
- **THEN**: 回傳 400 + 「此請假單已簽核，無法重複操作」

#### [ADDED] Scenario: 查詢假別餘額
- **WHEN**: 員工呼叫 `GET /api/v1/leaves/balance`（可指定年份）
- **THEN**: 回傳該年度所有假別的 totalDays、usedDays、remainingDays
- **AND**: ADMIN 可透過 `userId` 查看其他員工的餘額

---

## 6. 加班管理 (Overtime Management)

### [ADDED] Requirement: 加班申請
系統 **MUST** 允許員工提出加班申請，經主管核准後自動產生補休額度。

#### [ADDED] Scenario: 正常加班申請
- **WHEN**: 員工呼叫 `POST /api/v1/overtimes` 提供起迄時間、事由
- **THEN**: 建立加班申請（狀態 `PENDING`），回傳 200 + `OvertimeResponse`
- **AND**: 若使用者有主管，非同步寄送 Email 通知主管

#### [ADDED] Scenario: 主管核准加班
- **WHEN**: 主管呼叫 `PUT /api/v1/overtimes/{id}/approve`
- **THEN**: 
  - 狀態改為 `APPROVED`，記錄簽核人與時間
  - **自動計算加班時數並產生/累加補休餘額**
  - 寄送 Email 通知申請人
  - 回傳 200 + `OvertimeResponse`

#### [ADDED] Scenario: 主管駁回加班
- **WHEN**: 主管呼叫 `PUT /api/v1/overtimes/{id}/reject`
- **THEN**: 狀態改為 `REJECTED`，寄送 Email 通知申請人

#### [ADDED] Scenario: 補休自動產生
- **WHEN**: 加班核准後
- **THEN**: 系統自動計算加班時數（endTime - startTime），轉換為補休天數（8 小時 = 1 天）
- **AND**: 查找或建立該員工當年度的「補休」LeaveBalance
- **AND**: 將加班天數累加至 totalDays

---

## 7. 代理人制度 (Agent System)

### [ADDED] Requirement: 代理人指定
系統 **MUST** 允許使用者設定預設代理人，並在請假時指定代理人。

#### [ADDED] Scenario: 設定預設代理人
- **WHEN**: ADMIN 更新使用者時指定 `agentId`
- **THEN**: 使用者的 `agent` 欄位更新為指定代理人

#### [ADDED] Scenario: 請假指定代理人
- **WHEN**: 員工請假時在申請中指定 `agentId`
- **THEN**: 請假單記錄代理人，Email 通知主管時包含代理人資訊

#### [ADDED] Scenario: 未指定代理人
- **WHEN**: 員工請假時未指定 `agentId`
- **THEN**: 請假單代理人欄位為 null，Email 中標示「未指定代理人」

---

## 8. Email 通知 (Email Notifications)

### [ADDED] Requirement: Email 通知
系統 **MUST** 在特定事件發生時發送 HTML 格式的 Email 通知。

#### [ADDED] Scenario: 新使用者建立通知
- **WHEN**: 管理者建立新使用者
- **THEN**: 發送 Email 至新使用者信箱，包含登入密碼

#### [ADDED] Scenario: 請假申請通知主管
- **WHEN**: 員工提出請假申請且員工有主管
- **THEN**: 發送 Email 通知主管，包含申請人、假別、時間、代理人

#### [ADDED] Scenario: 請假結果通知申請人
- **WHEN**: 主管核准或駁回假單
- **THEN**: 發送 Email 通知申請人核准/駁回結果

#### [ADDED] Scenario: 加班申請通知主管
- **WHEN**: 員工提出加班申請且員工有主管
- **THEN**: 發送 Email 通知主管

#### [ADDED] Scenario: 加班結果通知申請人
- **WHEN**: 主管核准或駁回加班申請
- **THEN**: 發送 Email 通知申請人

---

## 9. 角色權限對照表

### [ADDED] Requirement: 角色存取控制
系統 **MUST** 依據使用者角色限制 API 存取。

| 功能 | EMPLOYEE | MANAGER | ADMIN |
|------|:--------:|:-------:|:-----:|
| 登入/改密碼 | ✅ | ✅ | ✅ |
| 打卡 | ✅ | ✅ | ✅ |
| 查看自己出勤 | ✅ | ✅ | ✅ |
| 查看他人出勤 | ❌ | ✅ | ✅ |
| 請假/加班申請 | ✅ | ✅ | ✅ |
| 查看自己假別餘額 | ✅ | ✅ | ✅ |
| 簽核請假/加班 | ❌ | ✅ | ✅ |
| 使用者管理 | ❌ | ❌ | ✅ |
| 部門管理 | ❌ | ❌ | ✅ |
| 查看他人假別餘額 | ❌ | ❌ | ✅ |
