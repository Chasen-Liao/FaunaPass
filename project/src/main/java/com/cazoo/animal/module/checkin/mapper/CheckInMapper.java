package com.cazoo.animal.module.checkin.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.cazoo.animal.module.checkin.entity.CheckIn;
import com.cazoo.animal.module.checkin.vo.CheckInTimelineVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface CheckInMapper extends BaseMapper<CheckIn> {

    /**
     * 时间轴查询:三表 JOIN,按 animal_id 过滤,按时间倒序。
     * LEFT JOIN animal 不过滤 deleted —— 即使动物被软删,旧打卡历史仍可见。
     */
    @Select("""
        SELECT
            c.id              AS id,
            c.content         AS content,
            c.created_at      AS createdAt,
            c.user_id         AS userId,
            u.nickname        AS userNickname,
            c.animal_id       AS animalId,
            a.name            AS animalName
        FROM check_in c
        LEFT JOIN user   u ON c.user_id   = u.id
        LEFT JOIN animal a ON c.animal_id = a.id
        WHERE c.animal_id = #{animalId}
        ORDER BY c.created_at DESC
        """)
    List<CheckInTimelineVO> selectTimelineByAnimalId(Long animalId);
}
