# attendance-redo（出缺勤管理系統）

> 出缺勤管理系統 — 員工打卡上下班、請假/加班申請、簽核流程、年假/補休管理、代理人制度

## 技術棧

| 層級 | 技術 |
|------|------|
| **前端** | Vite 8 + Vue 3 + TypeScript + Element Plus + Pinia |
| **後端** | Spring Boot 3.4.5 + Java 17 + Spring Security + JPA + MySQL |
| **認證** | JWT (jjwt 0.12.6) |
| **郵件** | Spring Boot Mail + Thymeleaf 模板 |
| **資料庫** | MySQL（本機） |

## 專案結構

```
attendance-redo/
├── apps/
│   ├── frontend/          # Vue 3 前端 (Vite + TypeScript)
│   │   ├── src/           # 前端原始碼
│   │   └── package.json
│   └── backend/           # Spring Boot 後端 (Java 17)
│       ├── src/main/java/ # Java 原始碼
│       └── pom.xml        # Maven 依賴
├── openspec/              # 規格與計畫文件
│   ├── changes/           # 變更提案與規格
│   └── specs/             # 系統規格
├── .openspec/             # OpenSpec 治理配置
│   ├── config.yaml
│   ├── schema.yaml
│   └── templates/         # 7 核心模板
├── .env                   # 環境變數（不納入版控）
└── CLAUDE.md              # Claude Code 開發規則
```

## Quick Start

1. **Read CLAUDE.md first** - Contains essential rules for Claude Code
2. Follow the pre-task compliance checklist before starting any work
3. Use proper module structure under `apps/`
4. Commit after every completed task

### 前端

```bash
cd apps/frontend
npm install
npm run dev
```

### 後端

```bash
cd apps/backend
mvn spring-boot:run
```

## 核心功能

- **員工打卡**: 上下班打卡記錄
- **權限管理**: 管理者（CRUD 使用者）vs 一般使用者
- **Email 通知**: 新增使用者時發送登入密碼
- **請假系統**: 請假申請、簽核流程
- **加班申請**: 加班申請與審核
- **年假/補休**: 年假與補休額度管理
- **代理人制度**: 請假時的代理人指定

## Development Guidelines

- **Always search first** before creating new files
- **Extend existing** functionality rather than duplicating
- **Use Task agents** for operations >30 seconds
- **Single source of truth** for all functionality
- **Spec-Driven Development** - specs in `openspec/` before implementation
- **TDD** - tests before implementation code
