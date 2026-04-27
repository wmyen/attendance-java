# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## 專案資訊

- **專案名稱**: attendance-java
- **描述**: 考勤管理系統
- **類型**: Monorepo (前後端分離)
- **技術棧**: TypeScript/JavaScript 前端 + Spring Boot 後端
- **建立日期**: 2026-04-27

## 🏛 專案憲法 (PROJECT CONSTITUTION)

> **⚠️ CRITICAL**: 本專案遵循嚴格的規格驅動開發 (SDD) 原則。
> **🚨 檔案路徑與檔名霸王條款**: 所有 7 個核心文檔 (Brainstorm, Proposal, Design, Spec, Tasks, Plan, Review) 必須存放在 `openspec/changes/<task-name>/` 下。檔名**絕對禁止**包含日期前綴（如 2026-04-22-design.md）。必須嚴格遵守 design.md, tasks.md, plan.md 等標準格式。
> **🚨 攔截與更名協議**: 若工具強制產出錯誤檔名或路徑（如 docs/superpowers/），你「必須」立即以 Bash 將其 mv 並重新命名，隨後清理垃圾資料夾。

## 🚨 CRITICAL RULES

### ❌ ABSOLUTE PROHIBITIONS
- **NEVER** write code for non-trivial changes without specs first (Violation of Spec-Driven Development).
- **NEVER** write implementation code before tests (Violation of TDD).
- **NEVER** create new files in root directory → use proper module structure
- **NEVER** use git commands with -i flag (interactive mode not supported)
- **NEVER** use `find`, `grep`, `cat`, `head`, `tail`, `ls` commands → use Read, LS, Grep, Glob tools instead
- **NEVER** create duplicate files (manager_v2.py, enhanced_xyz.py, utils_new.js) → ALWAYS extend existing files
- **NEVER** copy-paste code blocks → extract into shared utilities/functions
- **NEVER** hardcode values that should be configurable → use config files/environment variables
- **NEVER** use naming like enhanced_, improved_, new_, v2_ → extend original files instead

### 📝 MANDATORY REQUIREMENTS
- **SPEC MANAGEMENT** - 所有規格與計畫必須統一存放於 `openspec/` 目錄。
- **INTERCEPT & MOVE** - 若 `superpowers` 等工具將檔案強制產出至 `docs/superpowers/`，必須立即以 Bash 將其移回 `openspec/` 對應位置並清理殘留資料夾，且確保檔名不帶日期前綴。
- **GSD SYNC** - 每個 Task 完成後必須執行 `/gsd sync` 以維持上下文完整。
- **COMMIT** - after every completed task/phase - no exceptions.
- **GITHUB BACKUP** - Push to GitHub after every commit: `git push origin main`
- **USE TASK AGENTS** - for all long-running operations (>30 seconds)
- **READ FILES FIRST** - before editing - Edit/Write tools will fail if you didn't read the file first.
- **DEBT PREVENTION** - Before creating new files, check for existing similar functionality to extend.
- **SINGLE SOURCE OF TRUTH** - One authoritative implementation per feature/concept.

## 專案結構

```
attendance-java/
├── apps/
│   ├── frontend/     # TypeScript/JavaScript 前端應用
│   └── backend/      # Spring Boot 後端服務
├── infrastructure/   # 基礎設施配置 (Docker, CI/CD, K8s 等)
├── openspec/         # OpenSpec 規格文檔 (changes/<task-name>/)
├── .openspec/        # OpenSpec 配置與模板
├── docs/             # 專案文件
├── CLAUDE.md         # 本檔案
├── README.md         # 專案說明
└── .gitignore
```

## 🛠 核心指令

- **初始化 (Init)**: `openspec init`
- **提案階段 (Propose)**: `/opsx:propose "功能描述"`
- **執行實作 (Execute)**: `/opsx:apply`
- **上下文清理 (Clean)**: `/gsd cleanup`

## 📐 代碼風格

- 保持 DRY 原則，優先使用組合優於繼承 (Composition over inheritance)。
- 所有的規格與計畫統一存放於 `openspec/` 目錄。

## 🔍 PRE-TASK COMPLIANCE CHECK

> **STOP: Before starting any task, verify ALL points:**

**Step 1: Rule Acknowledgment**
- [ ] ✅ I acknowledge all critical rules in CLAUDE.md and will follow them

**Step 2: SDD & TDD Verification**
- [ ] `openspec/` 中是否有現成規格？ → 若無，請先透過 `/opsx:propose` 提案。
- [ ] 是否已針對此功能撰寫測試？ → 若無，請啟動 `superpowers:test-driven-development`。

**Step 3: Task Analysis**
- [ ] Will this create files in root? → If YES, use proper module structure instead
- [ ] Will this take >30 seconds? → If YES, use Task agents not Bash
- [ ] Am I about to use grep/find/cat? → If YES, use proper tools instead

**Step 4: Technical Debt Prevention**
- [ ] **SEARCH FIRST**: Use Grep/Glob to find existing implementations
- [ ] Does similar functionality already exist? → If YES, extend existing code
- [ ] Can I extend existing code instead of creating new? → Prefer extension over creation

## 🧹 DEBT PREVENTION WORKFLOW

1. **🔍 Search First** - Use Grep/Glob to find existing implementations
2. **📋 Analyze Existing** - Read and understand current patterns
3. **🤔 Decision Tree**: Can extend existing? → DO IT | Must create new? → Document why
4. **✅ Follow Patterns** - Use established project patterns
