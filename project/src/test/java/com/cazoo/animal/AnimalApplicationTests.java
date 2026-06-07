package com.cazoo.animal;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@ActiveProfiles("test")
@Disabled("被 AbstractIntegrationTest 覆盖:3 个核心集成测试已验证 context 加载")
class AnimalApplicationTests {

    @Test
    void contextLoads() {
    }
}
