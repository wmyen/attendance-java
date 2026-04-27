# attendance-java

考勤管理系統 - Monorepo 架構

## 專案結構

```
attendance-java/
├── apps/
│   ├── frontend/     # TypeScript/JavaScript 前端應用
│   └── backend/      # Spring Boot 後端服務
├── infrastructure/   # 基礎設施配置
├── openspec/         # OpenSpec 規格文檔
└── docs/             # 專案文件
```

## Quick Start

1. **Read CLAUDE.md first** - Contains essential rules for Claude Code
2. Follow the pre-task compliance checklist before starting any work
3. Use proper module structure under `apps/`
4. Commit after every completed task

## Development Guidelines

- **Always search first** before creating new files
- **Extend existing** functionality rather than duplicating
- **Use Task agents** for operations >30 seconds
- **Single source of truth** for all functionality
- **Spec-Driven Development** - specs in `openspec/` before implementation
