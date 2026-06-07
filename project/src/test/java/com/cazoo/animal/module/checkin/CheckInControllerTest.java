package com.cazoo.animal.module.checkin;

import com.cazoo.animal.AbstractIntegrationTest;
import com.cazoo.animal.module.checkin.entity.CheckIn;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import java.time.LocalDateTime;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class CheckInControllerTest extends AbstractIntegrationTest {

    @Test
    void timelineOrderByCreatedAtDesc() throws Exception {
        // 准备数据:给 animal 1 加 3 条打卡
        CheckIn c1 = new CheckIn();
        c1.setAnimalId(1L);
        c1.setUserId(2L);
        c1.setContent("最早的");
        c1.setCreatedAt(LocalDateTime.of(2026, 5, 1, 10, 0));
        checkInMapper.insert(c1);

        CheckIn c2 = new CheckIn();
        c2.setAnimalId(1L);
        c2.setUserId(2L);
        c2.setContent("中间的");
        c2.setCreatedAt(LocalDateTime.of(2026, 5, 15, 10, 0));
        checkInMapper.insert(c2);

        CheckIn c3 = new CheckIn();
        c3.setAnimalId(1L);
        c3.setUserId(3L);
        c3.setContent("最新的");
        c3.setCreatedAt(LocalDateTime.of(2026, 5, 30, 10, 0));
        checkInMapper.insert(c3);

        String token = loginAndGetToken("student01", "123456");
        mockMvc.perform(get("/api/animals/1/check-ins")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.length()").value(3))
                .andExpect(jsonPath("$.data[0].content").value("最新的"))
                .andExpect(jsonPath("$.data[1].content").value("中间的"))
                .andExpect(jsonPath("$.data[2].content").value("最早的"))
                // 验证 JOIN 字段填充
                .andExpect(jsonPath("$.data[0].userNickname").value("李四"))
                .andExpect(jsonPath("$.data[0].animalName").value("奶牛猫"));
    }

    @Test
    void timelineShowsHistoryForSoftDeletedAnimal() throws Exception {
        // 给 animal 1 加一条打卡,然后软删 animal 1
        CheckIn c = new CheckIn();
        c.setAnimalId(1L);
        c.setUserId(2L);
        c.setContent("动物还在的记录");
        c.setCreatedAt(LocalDateTime.of(2026, 5, 10, 12, 0));
        checkInMapper.insert(c);

        String adminToken = loginAndGetToken("admin", "admin123");
        mockMvc.perform(delete("/api/animals/1")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk());

        // 时间轴仍然能查到(产品决策 A:LEFT JOIN 不过滤 deleted)
        String studentToken = loginAndGetToken("student01", "123456");
        mockMvc.perform(get("/api/animals/1/check-ins")
                        .header("Authorization", "Bearer " + studentToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.length()").value(1))
                .andExpect(jsonPath("$.data[0].content").value("动物还在的记录"));
    }

    /**
     * 回归测试:Spring Security 6 method security 拒绝时,GlobalExceptionHandler
     * 兜底应返回 4003 而非 9999。
     * (修复前: ADMIN 调 @PreAuthorize("hasRole('STUDENT')") 接口 → 9999;
     *  修复后: → 4003 ACCESS_DENIED)
     */
    @Test
    void createByAdminShouldReturnAccessDenied() throws Exception {
        String adminToken = loginAndGetToken("admin", "admin123");
        String body = "{\"animalId\":1,\"content\":\"admin 试图发打卡\"}";
        mockMvc.perform(post("/api/check-ins")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(4003));
    }
}
