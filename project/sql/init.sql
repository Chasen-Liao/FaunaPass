-- ============================================================
-- 校园流浪动物图鉴与动态打卡系统 - PostgreSQL DDL
-- 适配 PG 18 语法: BIGSERIAL / TEXT / TIMESTAMP / trigger 模拟 ON UPDATE
-- 来源: docs/superpowers/specs/2026-06-07-campus-stray-animal-system-design.md §4.1
-- 说明: 课设演示用,本脚本会 DROP 重建三张表,谨慎用于生产
-- ============================================================

DROP TABLE IF EXISTS check_in CASCADE;
DROP TABLE IF EXISTS animal   CASCADE;
DROP TABLE IF EXISTS "user"   CASCADE;  -- user 是 PG 关键字,需要双引号

-- -----------------------------------------------------------------------------
-- 1. user 表 - 系统账号(学生 / 管理员)
-- -----------------------------------------------------------------------------
CREATE TABLE "user" (
  id         BIGSERIAL    PRIMARY KEY,
  username   VARCHAR(50)  NOT NULL,
  password   VARCHAR(100) NOT NULL,
  nickname   VARCHAR(50)  NOT NULL,
  role       VARCHAR(20)  NOT NULL,
  created_at TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP
);
CREATE UNIQUE INDEX uk_username ON "user"(username);
COMMENT ON TABLE  "user"            IS '系统账号';
COMMENT ON COLUMN "user".id         IS '主键';
COMMENT ON COLUMN "user".username   IS '登录名(全局唯一)';
COMMENT ON COLUMN "user".password   IS 'BCrypt 哈希(10 轮)';
COMMENT ON COLUMN "user".nickname   IS '昵称(时间轴显示)';
COMMENT ON COLUMN "user".role       IS 'ADMIN / STUDENT';
COMMENT ON COLUMN "user".created_at IS '注册时间';

-- -----------------------------------------------------------------------------
-- 2. animal 表 - 动物图鉴档案(管理员维护,支持软删)
-- -----------------------------------------------------------------------------
CREATE TABLE animal (
  id          BIGSERIAL    PRIMARY KEY,
  name        VARCHAR(50)  NOT NULL,
  type        VARCHAR(10)  NOT NULL,
  area        VARCHAR(100) NOT NULL,
  cover_image VARCHAR(255),
  deleted     SMALLINT     NOT NULL DEFAULT 0,
  created_at  TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at  TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX idx_name         ON animal(name);
CREATE INDEX idx_type_deleted ON animal(type, deleted);
COMMENT ON TABLE  animal             IS '动物图鉴档案';
COMMENT ON COLUMN animal.id          IS '主键';
COMMENT ON COLUMN animal.name        IS '暂定名字';
COMMENT ON COLUMN animal.type        IS 'CAT=猫 / DOG=狗';
COMMENT ON COLUMN animal.area        IS '常驻区域';
COMMENT ON COLUMN animal.cover_image IS '封面图相对路径(/uploads/animal/yyyy/MM/...)';
COMMENT ON COLUMN animal.deleted     IS '0=正常 1=已软删';
COMMENT ON COLUMN animal.created_at  IS '建档时间';
COMMENT ON COLUMN animal.updated_at  IS '更新时间';

-- 触发器:animal UPDATE 时自动 updated_at = now()
CREATE OR REPLACE FUNCTION animal_set_updated_at() RETURNS TRIGGER AS $$
BEGIN
  NEW.updated_at = CURRENT_TIMESTAMP;
  RETURN NEW;
END;
$$ LANGUAGE plpgsql;
CREATE TRIGGER trg_animal_updated_at
  BEFORE UPDATE ON animal
  FOR EACH ROW EXECUTE FUNCTION animal_set_updated_at();

-- -----------------------------------------------------------------------------
-- 3. check_in 表 - 偶遇打卡(课程设计核心: animal 1 : N check_in)
-- -----------------------------------------------------------------------------
CREATE TABLE check_in (
  id         BIGSERIAL    PRIMARY KEY,
  animal_id  BIGINT       NOT NULL,
  user_id    BIGINT       NOT NULL,
  content    VARCHAR(500) NOT NULL,
  created_at TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX idx_animal_created ON check_in(animal_id, created_at);
CREATE INDEX idx_user           ON check_in(user_id);
COMMENT ON TABLE  check_in             IS '偶遇打卡';
COMMENT ON COLUMN check_in.id          IS '主键';
COMMENT ON COLUMN check_in.animal_id   IS '关联动物 id';
COMMENT ON COLUMN check_in.user_id     IS '发布人 id';
COMMENT ON COLUMN check_in.content     IS '打卡文字(500 字以内)';
COMMENT ON COLUMN check_in.created_at  IS '发布时间';

-- 外键(等价 MySQL 的 ON DELETE RESTRICT,PG 默认就是 RESTRICT,显式声明更清晰)
ALTER TABLE check_in ADD CONSTRAINT fk_checkin_animal
  FOREIGN KEY (animal_id) REFERENCES animal(id) ON DELETE RESTRICT;
ALTER TABLE check_in ADD CONSTRAINT fk_checkin_user
  FOREIGN KEY (user_id)   REFERENCES "user"(id) ON DELETE RESTRICT;
