-- ============================================================
-- 校园流浪动物图鉴与动态打卡系统 - PostgreSQL 启动脚本(完全幂等)
-- 来源: docs/superpowers/specs/2026-06-07-campus-stray-animal-system-design.md §4.1
-- 适配 PG 18 语法: BIGSERIAL / TIMESTAMP / ON CONFLICT / DROP IF EXISTS
-- 作用: 启动时幂等执行,既能初始化新库,也能在已建库的 PG 上安全"保留"数据
-- 注意: Spring 3.5 spring.sql.init 按 ; 切,本文件不用 plpgsql $$ 块
-- ============================================================

-- -----------------------------------------------------------------------------
-- 1. user 表(IF NOT EXISTS,不再 DROP)
-- -----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS "user" (
  id         BIGSERIAL    PRIMARY KEY,
  username   VARCHAR(50)  NOT NULL,
  password   VARCHAR(100) NOT NULL,
  nickname   VARCHAR(50)  NOT NULL,
  role       VARCHAR(20)  NOT NULL,
  created_at TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP
);
CREATE UNIQUE INDEX IF NOT EXISTS uk_username ON "user"(username);
COMMENT ON TABLE  "user"            IS '系统账号';
COMMENT ON COLUMN "user".id         IS '主键';
COMMENT ON COLUMN "user".username   IS '登录名(全局唯一)';
COMMENT ON COLUMN "user".password   IS 'BCrypt 哈希(10 轮)';
COMMENT ON COLUMN "user".nickname   IS '昵称(时间轴显示)';
COMMENT ON COLUMN "user".role       IS 'ADMIN / STUDENT';
COMMENT ON COLUMN "user".created_at IS '注册时间';

-- -----------------------------------------------------------------------------
-- 2. animal 表
-- -----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS animal (
  id          BIGSERIAL    PRIMARY KEY,
  name        VARCHAR(50)  NOT NULL,
  type        VARCHAR(10)  NOT NULL,
  area        VARCHAR(100) NOT NULL,
  cover_image VARCHAR(255),
  deleted     SMALLINT     NOT NULL DEFAULT 0,
  created_at  TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at  TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX IF NOT EXISTS idx_name         ON animal(name);
CREATE INDEX IF NOT EXISTS idx_type_deleted ON animal(type, deleted);
COMMENT ON TABLE  animal             IS '动物图鉴档案';
COMMENT ON COLUMN animal.id          IS '主键';
COMMENT ON COLUMN animal.name        IS '暂定名字';
COMMENT ON COLUMN animal.type        IS 'CAT=猫 / DOG=狗';
COMMENT ON COLUMN animal.area        IS '常驻区域';
COMMENT ON COLUMN animal.cover_image IS '封面图相对路径(/uploads/animal/yyyy/MM/...)';
COMMENT ON COLUMN animal.deleted     IS '0=正常 1=已软删';
COMMENT ON COLUMN animal.created_at  IS '建档时间';
COMMENT ON COLUMN animal.updated_at  IS '更新时间(应用层维护)';

-- -----------------------------------------------------------------------------
-- 3. check_in 表
-- -----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS check_in (
  id         BIGSERIAL    PRIMARY KEY,
  animal_id  BIGINT       NOT NULL,
  user_id    BIGINT       NOT NULL,
  content    VARCHAR(500) NOT NULL,
  created_at TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX IF NOT EXISTS idx_animal_created ON check_in(animal_id, created_at);
CREATE INDEX IF NOT EXISTS idx_user           ON check_in(user_id);
COMMENT ON TABLE  check_in             IS '偶遇打卡';
COMMENT ON COLUMN check_in.id          IS '主键';
COMMENT ON COLUMN check_in.animal_id   IS '关联动物 id';
COMMENT ON COLUMN check_in.user_id     IS '发布人 id';
COMMENT ON COLUMN check_in.content     IS '打卡文字(500 字以内)';
COMMENT ON COLUMN check_in.created_at  IS '发布时间';

-- 外键:拆成多个单语句,DROP IF EXISTS + ADD 各自一行,DROP 不存在不报错,ADD 已存在会报"重复"——但 DROP 后 ADD 一定成功
-- 用 statement-level 而不是 constraint-level 的 IF NOT EXISTS(PG 不支持)
ALTER TABLE check_in DROP CONSTRAINT IF EXISTS fk_checkin_animal;
ALTER TABLE check_in ADD  CONSTRAINT fk_checkin_animal FOREIGN KEY (animal_id) REFERENCES animal(id) ON DELETE RESTRICT;
ALTER TABLE check_in DROP CONSTRAINT IF EXISTS fk_checkin_user;
ALTER TABLE check_in ADD  CONSTRAINT fk_checkin_user FOREIGN KEY (user_id) REFERENCES "user"(id) ON DELETE RESTRICT;

-- -----------------------------------------------------------------------------
-- 4. 种子数据(用 ON CONFLICT 兼容重复启动)
-- -----------------------------------------------------------------------------
INSERT INTO "user" (id, username, password, nickname, role) VALUES
  (1, 'admin',     '$2a$10$phN770udqqlg8XmpztPRReIAp66P94CreOWGpiZfLEyb3OtzQ2uI2', '系统管理员', 'ADMIN'),
  (2, 'student01', '$2a$10$XoJyDmw28JOOGBgFnLLYNujPtt8ZpB79z0d15Wds0/vQvNH5IoFJm', '张三',      'STUDENT'),
  (3, 'student02', '$2a$10$XoJyDmw28JOOGBgFnLLYNujPtt8ZpB79z0d15Wds0/vQvNH5IoFJm', '李四',      'STUDENT')
ON CONFLICT (id) DO NOTHING;
INSERT INTO "user" (id, username, password, nickname, role)
SELECT v.id, v.username, v.password, v.nickname, v.role FROM (VALUES
  (1, 'admin',     '$2a$10$phN770udqqlg8XmpztPRReIAp66P94CreOWGpiZfLEyb3OtzQ2uI2', '系统管理员', 'ADMIN'),
  (2, 'student01', '$2a$10$XoJyDmw28JOOGBgFnLLYNujPtt8ZpB79z0d15Wds0/vQvNH5IoFJm', '张三',      'STUDENT'),
  (3, 'student02', '$2a$10$XoJyDmw28JOOGBgFnLLYNujPtt8ZpB79z0d15Wds0/vQvNH5IoFJm', '李四',      'STUDENT')
) AS v(id, username, password, nickname, role)
ON CONFLICT (username) DO NOTHING;

INSERT INTO animal (id, name, type, area, cover_image) VALUES
  (1, '奶牛猫', 'CAT', '图书馆草坪', NULL),
  (2, '橘猫',   'CAT', '西区食堂',   NULL),
  (3, '黑狗',   'DOG', '东门',       NULL),
  (4, '白猫',   'CAT', '教学楼后',   NULL)
ON CONFLICT (id) DO NOTHING;

INSERT INTO check_in (id, animal_id, user_id, content, created_at) VALUES
  (1,  1, 2, '今天在图书馆草坪看到它了,精神不错,在晒太阳',     '2026-05-20 10:30:00'),
  (2,  1, 3, '下午又碰到了,还在草坪上,这次带了猫条喂它',         '2026-05-22 15:45:00'),
  (3,  1, 2, '连续三天了都在同一个位置,看来是常驻居民了',        '2026-05-25 09:15:00'),
  (4,  2, 3, '西区食堂门口有只橘猫在蹭饭,胖乎乎的特别可爱',     '2026-05-21 12:00:00'),
  (5,  2, 2, '中午又遇到它,这次吃完直接躺门口石凳上睡觉了',      '2026-05-23 12:30:00'),
  (6,  3, 2, '东门保安亭旁有只黑狗,很温顺可以摸',              '2026-05-24 17:20:00'),
  (7,  3, 3, '黑狗今天趴在保安亭的台阶上,看起来刚吃饱',         '2026-05-26 17:45:00'),
  (8,  4, 2, '教学楼后的小白猫今天没看到,可能换位置了',          '2026-05-22 14:00:00'),
  (9,  4, 3, '在垃圾桶旁边发现了它,毛发很干净像是有人照顾',      '2026-05-25 14:30:00')
ON CONFLICT (id) DO NOTHING;

-- 序列对齐
SELECT setval('"user_id_seq"',   (SELECT COALESCE(MAX(id), 1) FROM "user"),   true);
SELECT setval('animal_id_seq',   (SELECT COALESCE(MAX(id), 1) FROM animal),   true);
SELECT setval('check_in_id_seq', (SELECT COALESCE(MAX(id), 1) FROM check_in), true);
