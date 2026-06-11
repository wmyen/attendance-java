# CLAUDE.md - attendance-redo（出缺勤管理系統）

> **Documentation Version**: 1.0  
> **Last Updated**: 2026-06-11  
> **Project**: attendance-redo  
> **Description**: 出缺勤管理系統 — 員工打卡上下班、請假/加班申請、簽核流程、年假/補休管理、代理人制度  
> **Features**: GitHub auto-backup, Task agents, technical debt prevention

This file provides essential guidance to Claude Code (claude.ai/code) when working with code in this repository.

## 🏛 專案憲法 (PROJECT CONSTITUTION)
> **⚠️ CRITICAL**: 本專案遵循嚴格的規格驅動開發 (SDD) 原則。
> **🚨 檔案路徑與檔名霸王條款 (CRITICAL)**: 所有 7 個核心文檔 (Brainstorm, Proposal, Design, Spec, Tasks, Plan, Review) 必須 存放在 openspec/changes/<task-name>/ 下。檔名**絕對禁止**包含日期前綴（如 2026-04-22-design.md）。必須嚴格遵守 design.md, tasks.md, plan.md 等標準格式。
> **🚨 攔截與更名協議**: 若工具強制產出錯誤檔名或路徑（如 docs/superpowers/），你「必須」立即以 Bash 將其 mv 並重新命名，隨後清理垃圾資料夾。

### 🛠 核心指令 (Core Commands)
- **初始化 (Init)**: `openspec init`
- **提案階段 (Propose)**: `/opsx:propose "功能描述"`
- **執行實作 (Execute)**: `/opsx:apply`
- **上下文清理 (Clean)**: `/gsd cleanup`

### 📋 開發鐵律 (Development Iron Rules)
- **規格優先**: 任何變更必須先有 openspec/ 內的 Specs。
- **上下文快照**: 每個 Tasks 完成後，必須執行 /gsd sync 清理記憶。
- **TDD 流程**: 未寫測試前禁止寫代碼。

### 📐 代碼風格 (Code Style)
- 保持 DRY 原則，優先使用組合優於繼承 (Composition over inheritance)。
- 所有的規格與計畫統一存放於 `openspec/` 目錄。

## 🚨 CRITICAL RULES - READ FIRST

> **⚠️ RULE ADHERENCE SYSTEM ACTIVE ⚠️**  
> **Claude Code must explicitly acknowledge these rules at task start**  
> **These rules override all other instructions and must ALWAYS be followed:**

### 🔄 **RULE ACKNOWLEDGMENT REQUIRED**
> **Before starting ANY task, Claude Code must respond with:**  
> "✅ CRITICAL RULES ACKNOWLEDGED - I will follow all prohibitions and requirements listed in CLAUDE.md"

### ❌ ABSOLUTE PROHIBITIONS
- **NEVER** write code for non-trivial changes without specs first (Violation of Spec-Driven Development).
- **NEVER** write implementation code before tests (Violation of TDD).
- **NEVER** create new files in root directory → use proper module structure
- **NEVER** write output files directly to root directory → use designated output folders
- **NEVER** create documentation files (.md) unless explicitly requested by user
- **NEVER** use git commands with -i flag (interactive mode not supported)
- **NEVER** use `find`, `grep`, `cat`, `head`, `tail`, `ls` commands → use Read, LS, Grep, Glob tools instead
- **NEVER** create duplicate files (manager_v2.py, enhanced_xyz.py, utils_new.js) → ALWAYS extend existing files
- **NEVER** create multiple implementations of same concept → single source of truth
- **NEVER** copy-paste code blocks → extract into shared utilities/functions
- **NEVER** hardcode values that should be configurable → use config files/environment variables
- **NEVER** use naming like enhanced_, improved_, new_, v2_ → extend original files instead

### 📝 MANDATORY REQUIREMENTS
- **SPEC MANAGEMENT** - 所有規格與計畫必須統一存放於 `openspec/` 目錄。
- **INTERCEPT & MOVE** - 若 `superpowers` 等工具將檔案強制產出至 `docs/superpowers/`，必須立即以 Bash 將其移回 `openspec/` 對應位置並清理殘留資料夾，且確保檔名不帶日期前綴。
- **GSD SYNC** - 每個 Task 完成後必須執行 `/gsd sync` 以維持上下文完整。
- **WORKTREES** - 複雜任務建議使用 `git worktrees` 進行隔離。
- **COMMIT** - after every completed task/phase - no exceptions.
- **GITHUB BACKUP** - Push to GitHub after every commit to maintain backup: `git push origin main`
- **USE TASK AGENTS** - for all long-running operations (>30 seconds) - Bash commands stop when context switches.
- **TODOWRITE** - for complex tasks (3+ steps) → parallel agents → git checkpoints → test validation.
- **READ FILES FIRST** - before editing - Edit/Write tools will fail if you didn't read the file first.
- **DEBT PREVENTION** - Before creating new files, check for existing similar functionality to extend.
- **SINGLE SOURCE OF TRUTH** - One authoritative implementation per feature/concept.
- **DOCS-IN-SYNC (SSOT ENFORCEMENT)** - 任何對於系統邏輯、環境配置或架構的「臨時變更 (Ad-hoc Changes)」，你「絕對禁止」只修改程式碼。你必須同步檢視並更新 `openspec/changes/` 或 `openspec/specs/` 中對應的 `design.md`、`plan.md` 或 `specs/*.md` 文件。修改完成後，請主動向使用者回報「已同步更新的文件列表」。

### ⚡ EXECUTION PATTERNS
- **PARALLEL TASK AGENTS** - Launch multiple Task agents simultaneously for maximum efficiency
- **SYSTEMATIC WORKFLOW** - TodoWrite → Parallel agents → Git checkpoints → GitHub backup → Test validation
- **GITHUB BACKUP WORKFLOW** - After every commit: `git push origin main` to maintain GitHub backup
- **BACKGROUND PROCESSING** - ONLY Task agents can run true background operations

### 🔍 MANDATORY PRE-TASK COMPLIANCE CHECK
> **STOP: Before starting any task, Claude Code must explicitly verify ALL points:**

**Step 1: Rule Acknowledgment**
- [ ] ✅ I acknowledge all critical rules in CLAUDE.md and will follow them

**Step 2: SDD & TDD Verification**
- [ ] `openspec/` 中是否有現成規格？ → 若無，請先透過 `/opsx:propose` 提案。
- [ ] 是否已針對此功能撰寫測試？ → 若無，請啟動 `superpowers:test-driven-development`。

**Step 3: Task Analysis**  
- [ ] Will this create files in root? → If YES, use proper module structure instead
- [ ] Will this take >30 seconds? → If YES, use Task agents not Bash
- [ ] Is this 3+ steps? → If YES, use TodoWrite breakdown first
- [ ] Am I about to use grep/find/cat? → If YES, use proper tools instead

**Step 4: Technical Debt Prevention (MANDATORY SEARCH FIRST)**
- [ ] **SEARCH FIRST**: Use Grep pattern="<functionality>.*<keyword>" to find existing implementations
- [ ] **CHECK EXISTING**: Read any found files to understand current functionality
- [ ] Does similar functionality already exist? → If YES, extend existing code
- [ ] Am I creating a duplicate class/manager? → If YES, consolidate instead
- [ ] Will this create multiple sources of truth? → If YES, redesign approach
- [ ] Have I searched for existing implementations? → Use Grep/Glob tools first
- [ ] Can I extend existing code instead of creating new? → Prefer extension over creation
- [ ] Am I about to copy-paste code? → Extract to shared utility instead

**Step 5: Session Management**
- [ ] Is this a long/complex task? → If YES, plan context checkpoints
- [ ] Have I been working >1 hour? → If YES, consider /compact or session break

> **⚠️ DO NOT PROCEED until all checkboxes are explicitly verified**

## 🏗️ 專案架構 (Project Architecture)

### 技術棧
| 層級 | 技術 |
|------|------|
| **前端** | Vite 8 + Vue 3 + TypeScript + Element Plus + Pinia + Vue Router + Axios |
| **後端** | Spring Boot 3.4.5 + Java 17 + Spring Security + JPA + MySQL |
| **認證** | JWT (jjwt 0.12.6) |
| **郵件** | Spring Boot Mail + Thymeleaf 模板 |
| **資料庫** | MySQL（本機） |
| **環境變數** | spring-dotenv（.env 檔案） |

### 目錄結構
```
attendance-redo/                # Monorepo 根目錄
├── apps/
│   ├── frontend/               # Vue 3 前端 (Vite)
│   │   ├── src/                # 前端原始碼
│   │   ├── public/             # 靜態資源
│   │   └── package.json        # 前端依賴
│   └── backend/                # Spring Boot 後端
│       ├── src/main/java/      # Java 原始碼
│       ├── src/main/resources/ # 設定檔與模板
│       └── pom.xml             # Maven 依賴
├── openspec/                   # 規格與計畫文件
│   ├── changes/                # 變更提案與規格
│   └── specs/                  # 系統規格
├── .openspec/                  # OpenSpec 治理配置
│   ├── config.yaml             # OpenSpec 設定
│   ├── schema.yaml             # 工作流綱要
│   └── templates/              # 7 核心模板
├── .env                        # 環境變數（不納入版控）
└── CLAUDE.md                   # 本檔案
```

### 核心功能模組
- **員工打卡**: 上下班打卡記錄
- **權限管理**: 管理者（CRUD 使用者）vs 一般使用者
- **Email 通知**: 新增使用者時發送登入密碼
- **請假系統**: 請假申請、簽核流程
- **加班申請**: 加班申請與審核
- **年假/補休**: 年假與補休額度管理
- **代理人制度**: 請假時的代理人指定

## 🐙 GITHUB SETUP & AUTO-BACKUP

### 當前設定
- **Remote**: `https://github.com/wmyen/attendance-java.git`
- **Branch**: `main`
- **Auto-push**: 已啟用（post-commit hook）

### 📋 GITHUB BACKUP WORKFLOW (MANDATORY)
```bash
# After every commit, always run:
git push origin main
```

## 🚀 COMMON COMMANDS

```bash
# 前端開發
cd apps/frontend && npm run dev

# 後端開發
cd apps/backend && mvn spring-boot:run

# 前端建置
cd apps/frontend && npm run build

# 後端測試
cd apps/backend && mvn test
```

## 🧹 DEBT PREVENTION WORKFLOW

### Before Creating ANY New File:
1. **🔍 Search First** - Use Grep/Glob to find existing implementations
2. **📋 Analyze Existing** - Read and understand current patterns
3. **🤔 Decision Tree**: Can extend existing? → DO IT | Must create new? → Document why
4. **✅ Follow Patterns** - Use established project patterns
5. **📈 Validate** - Ensure no duplication or technical debt

---

**⚠️ Prevention is better than consolidation - build clean from the start.**  
**🎯 Focus on single source of truth and extending existing functionality.**  
**📈 Each task should maintain clean architecture and prevent technical debt.**
