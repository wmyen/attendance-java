# 規格書: [能力名稱]

> **⚠️ OPEN_SPEC DELTA 標記規範 (CRITICAL)**:
> 本文件是變更檔 (Delta)。所有的標題 (Requirement 或 Scenario) 都必須標註變更類型：
> - `[ADDED]`：新增的需求或場景
> - `[MODIFIED]`：修改現有需求或場景
> - `[REMOVED]`：移除的需求或場景
> - `[RENAMED: 舊名稱]`：重新命名的項目

## 1. 需求定義
### [ADDED/MODIFIED] Requirement: [名稱]
系統 **SHALL/MUST** [描述行為]。

## 2. 測試場景 (Scenarios)
> **CRITICAL**: Scenarios 必須使用 4 個 hashtags (####) 以供 Superpowers 辨識，並緊接 Delta 標記。

#### [ADDED/MODIFIED] Scenario: [正常路徑名稱]
- **WHEN**: [初始條件/動作]
- **AND**: [補充條件]
- **THEN**: [預期結果]

#### [ADDED/MODIFIED] Scenario: [異常/邊界案例名稱]
- **WHEN**: [觸發錯誤的條件]
- **THEN**: [錯誤處理結果]
