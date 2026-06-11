-- ============================================================
-- 出缺勤管理系統 - 資料庫初始化腳本
-- Attendance Management System - Schema Initialization
-- ============================================================
-- 使用方式:
--   /usr/local/mysql/bin/mysql -u root -p < schema.sql
-- ============================================================

CREATE DATABASE IF NOT EXISTS attendance_db
  CHARACTER SET utf8mb4
  COLLATE utf8mb4_unicode_ci;

USE attendance_db;

-- -----------------------------------------------------------
-- 部門表
-- -----------------------------------------------------------
CREATE TABLE IF NOT EXISTS departments (
    id          BIGINT       NOT NULL AUTO_INCREMENT,
    name        VARCHAR(100) NOT NULL UNIQUE,
    created_at  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id)
) ENGINE=InnoDB;

-- -----------------------------------------------------------
-- 使用者表
-- -----------------------------------------------------------
CREATE TABLE IF NOT EXISTS users (
    id                   BIGINT       NOT NULL AUTO_INCREMENT,
    email                VARCHAR(255) NOT NULL UNIQUE,
    password             VARCHAR(255) NOT NULL,
    name                 VARCHAR(100) NOT NULL,
    role                 VARCHAR(20)  NOT NULL COMMENT 'ADMIN, MANAGER, EMPLOYEE',
    dept_id              BIGINT       NULL,
    manager_id           BIGINT       NULL,
    agent_id             BIGINT       NULL,
    must_change_password BIT          NOT NULL DEFAULT 0,
    is_active            BIT          NOT NULL DEFAULT 1,
    created_at           DATETIME     NULL,
    updated_at           DATETIME     NULL,
    PRIMARY KEY (id),
    CONSTRAINT fk_user_dept     FOREIGN KEY (dept_id)    REFERENCES departments(id),
    CONSTRAINT fk_user_manager  FOREIGN KEY (manager_id) REFERENCES users(id),
    CONSTRAINT fk_user_agent    FOREIGN KEY (agent_id)   REFERENCES users(id)
) ENGINE=InnoDB;

-- -----------------------------------------------------------
-- 出勤記錄表
-- -----------------------------------------------------------
CREATE TABLE IF NOT EXISTS attendance (
    id         BIGINT       NOT NULL AUTO_INCREMENT,
    user_id    BIGINT       NOT NULL,
    date       DATE         NOT NULL,
    clock_in   DATETIME     NULL,
    clock_out  DATETIME     NULL,
    status     VARCHAR(20)  NOT NULL COMMENT 'NORMAL, LATE, EARLY_LEAVE, ABSENT',
    PRIMARY KEY (id),
    CONSTRAINT fk_attendance_user FOREIGN KEY (user_id) REFERENCES users(id),
    UNIQUE KEY uk_user_date (user_id, date)
) ENGINE=InnoDB;

-- -----------------------------------------------------------
-- 假別表
-- -----------------------------------------------------------
CREATE TABLE IF NOT EXISTS leave_types (
    id           BIGINT      NOT NULL AUTO_INCREMENT,
    name         VARCHAR(50) NOT NULL,
    code         VARCHAR(20) NOT NULL UNIQUE COMMENT 'ANNUAL, SICK, PERSONAL, COMPENSATORY, MATERNITY, MARRIAGE, BEREAVEMENT',
    is_paid      BIT         NOT NULL DEFAULT 0,
    requires_doc BIT         NOT NULL DEFAULT 0,
    PRIMARY KEY (id)
) ENGINE=InnoDB;

-- -----------------------------------------------------------
-- 假別餘額表
-- -----------------------------------------------------------
CREATE TABLE IF NOT EXISTS leave_balances (
    id            BIGINT      NOT NULL AUTO_INCREMENT,
    user_id       BIGINT      NOT NULL,
    leave_type_id BIGINT      NOT NULL,
    year          INT         NOT NULL,
    total_days    DECIMAL(5,1) NOT NULL DEFAULT 0.0,
    used_days     DECIMAL(5,1) NOT NULL DEFAULT 0.0,
    PRIMARY KEY (id),
    CONSTRAINT fk_balance_user       FOREIGN KEY (user_id)       REFERENCES users(id),
    CONSTRAINT fk_balance_leave_type FOREIGN KEY (leave_type_id) REFERENCES leave_types(id),
    UNIQUE KEY uk_user_type_year (user_id, leave_type_id, year)
) ENGINE=InnoDB;

-- -----------------------------------------------------------
-- 請假單表
-- -----------------------------------------------------------
CREATE TABLE IF NOT EXISTS leave_requests (
    id           BIGINT  NOT NULL AUTO_INCREMENT,
    user_id      BIGINT  NOT NULL,
    leave_type_id BIGINT NOT NULL,
    start_time   DATETIME NOT NULL,
    end_time     DATETIME NOT NULL,
    reason       TEXT     NULL,
    agent_id     BIGINT   NULL,
    status       VARCHAR(20) NOT NULL DEFAULT 'PENDING' COMMENT 'PENDING, APPROVED, REJECTED',
    approved_by  BIGINT   NULL,
    approved_at  DATETIME NULL,
    created_at   DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    CONSTRAINT fk_leave_user       FOREIGN KEY (user_id)      REFERENCES users(id),
    CONSTRAINT fk_leave_type       FOREIGN KEY (leave_type_id) REFERENCES leave_types(id),
    CONSTRAINT fk_leave_agent      FOREIGN KEY (agent_id)     REFERENCES users(id),
    CONSTRAINT fk_leave_approver   FOREIGN KEY (approved_by)  REFERENCES users(id)
) ENGINE=InnoDB;

-- -----------------------------------------------------------
-- 加班申請表
-- -----------------------------------------------------------
CREATE TABLE IF NOT EXISTS overtime_requests (
    id          BIGINT      NOT NULL AUTO_INCREMENT,
    user_id     BIGINT      NOT NULL,
    start_time  DATETIME    NOT NULL,
    end_time    DATETIME    NOT NULL,
    reason      TEXT        NULL,
    status      VARCHAR(20) NOT NULL DEFAULT 'PENDING' COMMENT 'PENDING, APPROVED, REJECTED',
    approved_by BIGINT      NULL,
    approved_at DATETIME    NULL,
    created_at  DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    CONSTRAINT fk_overtime_user     FOREIGN KEY (user_id)     REFERENCES users(id),
    CONSTRAINT fk_overtime_approver FOREIGN KEY (approved_by) REFERENCES users(id)
) ENGINE=InnoDB;
