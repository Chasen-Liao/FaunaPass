-- =============================================================================
-- 校园流浪动物图鉴与动态打卡系统 - 数据库初始化脚本
-- 来源: docs/superpowers/specs/2026-06-07-campus-stray-animal-system-design.md §4.1
-- =============================================================================

-- 1. 创建数据库
DROP DATABASE IF EXISTS campus_animal;
CREATE DATABASE campus_animal DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE campus_animal;

-- -----------------------------------------------------------------------------
-- 2. user 表 - 系统账号(学生 / 管理员)
-- -----------------------------------------------------------------------------
CREATE TABLE user (
  id         BIGINT       NOT NULL AUTO_INCREMENT             COMMENT '主键',
  username   VARCHAR(50)  NOT NULL                            COMMENT '登录名(全局唯一)',
  password   VARCHAR(100) NOT NULL                            COMMENT 'BCrypt 哈希(10 轮)',
  nickname   VARCHAR(50)  NOT NULL                            COMMENT '昵称(时间轴显示)',
  role       VARCHAR(20)  NOT NULL                            COMMENT 'ADMIN / STUDENT',
  created_at DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP  COMMENT '注册时间',
  PRIMARY KEY (id),
  UNIQUE KEY uk_username (username)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='系统账号';

-- -----------------------------------------------------------------------------
-- 3. animal 表 - 动物图鉴档案(管理员维护,支持软删)
-- -----------------------------------------------------------------------------
CREATE TABLE animal (
  id           BIGINT       NOT NULL AUTO_INCREMENT                                  COMMENT '主键',
  name         VARCHAR(50)  NOT NULL                                                 COMMENT '暂定名字',
  type         VARCHAR(10)  NOT NULL                                                 COMMENT 'CAT=猫 / DOG=狗',
  area         VARCHAR(100) NOT NULL                                                 COMMENT '常驻区域',
  cover_image  VARCHAR(255) NULL                                                     COMMENT '封面图相对路径(/uploads/animal/yyyy/MM/...)',
  deleted      TINYINT(1)   NOT NULL DEFAULT 0                                       COMMENT '0=正常 1=已软删',
  created_at   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP                       COMMENT '建档时间',
  updated_at   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (id),
  KEY idx_name (name),
  KEY idx_type_deleted (type, deleted)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='动物图鉴档案';

-- -----------------------------------------------------------------------------
-- 4. check_in 表 - 偶遇打卡(课程设计核心: animal 1 : N check_in)
-- -----------------------------------------------------------------------------
CREATE TABLE check_in (
  id         BIGINT       NOT NULL AUTO_INCREMENT             COMMENT '主键',
  animal_id  BIGINT       NOT NULL                            COMMENT '关联动物 id',
  user_id    BIGINT       NOT NULL                            COMMENT '发布人 id',
  content    VARCHAR(500) NOT NULL                            COMMENT '打卡文字(500 字以内)',
  created_at DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP  COMMENT '发布时间',
  PRIMARY KEY (id),
  KEY idx_animal_created (animal_id, created_at),
  KEY idx_user (user_id),
  CONSTRAINT fk_checkin_animal FOREIGN KEY (animal_id) REFERENCES animal(id) ON DELETE RESTRICT,
  CONSTRAINT fk_checkin_user   FOREIGN KEY (user_id)   REFERENCES user(id)   ON DELETE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='偶遇打卡';
