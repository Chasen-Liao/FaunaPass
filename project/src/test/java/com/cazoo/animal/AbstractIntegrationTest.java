package com.cazoo.animal;

import com.cazoo.animal.module.animal.entity.Animal;
import com.cazoo.animal.module.animal.entity.AnimalType;
import com.cazoo.animal.module.animal.mapper.AnimalMapper;
import com.cazoo.animal.module.checkin.entity.CheckIn;
import com.cazoo.animal.module.checkin.mapper.CheckInMapper;
import com.cazoo.animal.module.user.entity.User;
import com.cazoo.animal.module.user.entity.UserRole;
import com.cazoo.animal.module.user.mapper.UserMapper;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.transaction.annotation.Transactional;

/**
 * 集成测试基类。
 * <p>
 * H2 兼容性说明:
 * 1) H2 2.x 即使在 MODE=MySQL 下也把 USER 视为保留字,导致
 *    {@code spring.sql.init} 在 context 启动阶段跑 init-test.sql 时
 *    {@code DROP TABLE IF EXISTS user} 解析失败。解决:在测试 class 上用
 *    {@code spring.sql.init.mode=never} 关闭 spring.sql.init 阶段,
 *    改由本类 {@link #initSchema()} 用 JdbcTemplate 手动建表(对 user 加引号)。
 * 2) MyBatis-Plus 3.5.7 生成的 {@code DELETE FROM user} 不会加引号,
 *    必须让 H2 不再把 USER 视作关键字。解决:在 datasource URL 追加
 *    {@code ;NON_KEYWORDS=USER}。这里通过 {@code @SpringBootTest(properties=...)}
 *    覆盖 URL,不动 application.yml。
 */
@SpringBootTest(properties = {
        "spring.sql.init.mode=never",
        "spring.sql.init.schema-locations=",
        "spring.datasource.url=jdbc:h2:mem:test;MODE=MySQL;DB_CLOSE_DELAY=-1;NON_KEYWORDS=USER"
})
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
public abstract class AbstractIntegrationTest {

    @Autowired
    protected MockMvc mockMvc;
    @Autowired
    protected ObjectMapper objectMapper;
    @Autowired
    protected UserMapper userMapper;
    @Autowired
    protected AnimalMapper animalMapper;
    @Autowired
    protected CheckInMapper checkInMapper;
    @Autowired
    protected PasswordEncoder passwordEncoder;
    @Autowired
    protected JdbcTemplate jdbcTemplate;

    private static final java.util.concurrent.atomic.AtomicInteger SCHEMA_VERSION =
            new java.util.concurrent.atomic.AtomicInteger(0);

    @BeforeAll
    static void resetSchema() {
        SCHEMA_VERSION.set(0);
    }

    @BeforeEach
    void setUp() {
        // 第一个测试 class 第一次 @BeforeEach 时建表,后续共享
        if (SCHEMA_VERSION.compareAndSet(0, 1)) {
            initSchema();
        }
        seed();
    }

    private void initSchema() {
        // 防御:即便 URL 上次已建过,这里只 DROP+CREATE,幂等
        jdbcTemplate.execute("DROP ALL OBJECTS");

        // 业务 schema(等价 init-test.sql)
        // 注意:由于 datasource URL 设了 NON_KEYWORDS=USER,user 已是普通 identifier;
        // 不带引号的 user 会被 H2 upper 化为 USER,正好与 MyBatis-Plus 生成的
        // DELETE FROM user (parser 后大写化为 USER) 匹配。
        jdbcTemplate.execute("CREATE TABLE user (" +
                "  id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY," +
                "  username VARCHAR(50) NOT NULL," +
                "  password VARCHAR(100) NOT NULL," +
                "  nickname VARCHAR(50) NOT NULL," +
                "  role VARCHAR(20) NOT NULL," +
                "  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP" +
                ")");
        jdbcTemplate.execute("CREATE UNIQUE INDEX uk_username ON user(username)");

        jdbcTemplate.execute("CREATE TABLE animal (" +
                "  id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY," +
                "  name VARCHAR(50) NOT NULL," +
                "  type VARCHAR(10) NOT NULL," +
                "  area VARCHAR(100) NOT NULL," +
                "  cover_image VARCHAR(255)," +
                "  deleted TINYINT NOT NULL DEFAULT 0," +
                "  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP," +
                "  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP" +
                ")");
        jdbcTemplate.execute("CREATE INDEX idx_name ON animal(name)");
        jdbcTemplate.execute("CREATE INDEX idx_type_deleted ON animal(type, deleted)");

        jdbcTemplate.execute("CREATE TABLE check_in (" +
                "  id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY," +
                "  animal_id BIGINT NOT NULL," +
                "  user_id BIGINT NOT NULL," +
                "  content VARCHAR(500) NOT NULL," +
                "  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP" +
                ")");
        jdbcTemplate.execute("CREATE INDEX idx_animal_created ON check_in(animal_id, created_at)");
        jdbcTemplate.execute("CREATE INDEX idx_user ON check_in(user_id)");
        jdbcTemplate.execute("ALTER TABLE check_in ADD CONSTRAINT fk_checkin_animal FOREIGN KEY (animal_id) REFERENCES animal(id)");
        jdbcTemplate.execute("ALTER TABLE check_in ADD CONSTRAINT fk_checkin_user FOREIGN KEY (user_id) REFERENCES user(id)");
    }

    private void seed() {
        // 先清(顺序: check_in -> animal -> user)
        checkInMapper.delete(null);
        animalMapper.delete(null);
        userMapper.delete(null);

        // 重置 AUTO_INCREMENT,保证后续测试断言 id=1 等成立
        jdbcTemplate.execute("ALTER TABLE user ALTER COLUMN id RESTART WITH 1");
        jdbcTemplate.execute("ALTER TABLE animal ALTER COLUMN id RESTART WITH 1");
        jdbcTemplate.execute("ALTER TABLE check_in ALTER COLUMN id RESTART WITH 1");

        User admin = new User();
        admin.setUsername("admin");
        admin.setPassword(passwordEncoder.encode("admin123"));
        admin.setNickname("系统管理员");
        admin.setRole(UserRole.ADMIN.name());
        userMapper.insert(admin);

        User s1 = new User();
        s1.setUsername("student01");
        s1.setPassword(passwordEncoder.encode("123456"));
        s1.setNickname("张三");
        s1.setRole(UserRole.STUDENT.name());
        userMapper.insert(s1);

        User s2 = new User();
        s2.setUsername("student02");
        s2.setPassword(passwordEncoder.encode("123456"));
        s2.setNickname("李四");
        s2.setRole(UserRole.STUDENT.name());
        userMapper.insert(s2);

        Animal a1 = new Animal();
        a1.setName("奶牛猫");
        a1.setType(AnimalType.CAT.getCode());
        a1.setArea("图书馆草坪");
        animalMapper.insert(a1);

        Animal a2 = new Animal();
        a2.setName("橘猫");
        a2.setType(AnimalType.CAT.getCode());
        a2.setArea("西区食堂");
        animalMapper.insert(a2);
    }

    protected String loginAndGetToken(String username, String password) throws Exception {
        String body = "{\"username\":\"" + username + "\",\"password\":\"" + password + "\"}";
        String response = mockMvc.perform(
                MockMvcRequestBuilders
                        .post("/api/auth/login")
                        .contentType("application/json")
                        .content(body))
                .andReturn().getResponse().getContentAsString();
        JsonNode root = objectMapper.readTree(response);
        return root.path("data").path("token").asText();
    }
}
