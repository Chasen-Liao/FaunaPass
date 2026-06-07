-- =============================================================================
-- H2 in-memory 模式专用:DDL + 种子数据合一
-- 来源: project/sql/init.sql + project/sql/seed.sql
-- H2 MODE=MySQL + NON_KEYWORDS=USER 让 user 不被识别为保留字
-- =============================================================================

-- 1. user 表
CREATE TABLE user (
  id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
  username VARCHAR(50) NOT NULL,
  password VARCHAR(100) NOT NULL,
  nickname VARCHAR(50) NOT NULL,
  role VARCHAR(20) NOT NULL,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP
);
CREATE UNIQUE INDEX uk_username ON user(username);

-- 2. animal 表
CREATE TABLE animal (
  id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
  name VARCHAR(50) NOT NULL,
  type VARCHAR(10) NOT NULL,
  area VARCHAR(100) NOT NULL,
  cover_image VARCHAR(255),
  deleted TINYINT(1) NOT NULL DEFAULT 0,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX idx_name ON animal(name);
CREATE INDEX idx_type_deleted ON animal(type, deleted);

-- 3. check_in 表
CREATE TABLE check_in (
  id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
  animal_id BIGINT NOT NULL,
  user_id BIGINT NOT NULL,
  content VARCHAR(500) NOT NULL,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX idx_animal_created ON check_in(animal_id, created_at);
CREATE INDEX idx_user ON check_in(user_id);
ALTER TABLE check_in ADD CONSTRAINT fk_checkin_animal FOREIGN KEY (animal_id) REFERENCES animal(id);
ALTER TABLE check_in ADD CONSTRAINT fk_checkin_user FOREIGN KEY (user_id) REFERENCES user(id);

-- =============================================================================
-- 4. 种子数据
-- =============================================================================
-- 用户种子(密码为 BCrypt 10 轮哈希)
--   admin     / admin123
--   student01 / 123456
--   student02 / 123456
INSERT INTO user (id, username, password, nickname, role) VALUES
  (1, 'admin',     '$2a$10$phN770udqqlg8XmpztPRReIAp66P94CreOWGpiZfLEyb3OtzQ2uI2', '系统管理员', 'ADMIN'),
  (2, 'student01', '$2a$10$XoJyDmw28JOOGBgFnLLYNujPtt8ZpB79z0d15Wds0/vQvNH5IoFJm', '张三',      'STUDENT'),
  (3, 'student02', '$2a$10$XoJyDmw28JOOGBgFnLLYNujPtt8ZpB79z0d15Wds0/vQvNH5IoFJm', '李四',      'STUDENT');

-- 动物档案种子(4 只,无封面图)
INSERT INTO animal (id, name, type, area, cover_image) VALUES
  (1, '奶牛猫', 'CAT', '图书馆草坪', NULL),
  (2, '橘猫',   'CAT', '西区食堂',   NULL),
  (3, '黑狗',   'DOG', '东门',       NULL),
  (4, '白猫',   'CAT', '教学楼后',   NULL);

-- 打卡种子
INSERT INTO check_in (animal_id, user_id, content, created_at) VALUES
  (1, 2, '今天在图书馆草坪看到它了,精神不错,在晒太阳',     '2026-05-20 10:30:00'),
  (1, 3, '下午又碰到了,还在草坪上,这次带了猫条喂它',         '2026-05-22 15:45:00'),
  (1, 2, '连续三天了都在同一个位置,看来是常驻居民了',        '2026-05-25 09:15:00'),
  (2, 3, '西区食堂门口有只橘猫在蹭饭,胖乎乎的特别可爱',     '2026-05-21 12:00:00'),
  (2, 2, '中午又遇到它,这次吃完直接躺门口石凳上睡觉了',      '2026-05-23 12:30:00'),
  (3, 2, '东门保安亭旁有只黑狗,很温顺可以摸',              '2026-05-24 17:20:00'),
  (3, 3, '黑狗今天趴在保安亭的台阶上,看起来刚吃饱',         '2026-05-26 17:45:00'),
  (4, 2, '教学楼后的小白猫今天没看到,可能换位置了',          '2026-05-22 14:00:00'),
  (4, 3, '在垃圾桶旁边发现了它,毛发很干净像是有人照顾',      '2026-05-25 14:30:00');
