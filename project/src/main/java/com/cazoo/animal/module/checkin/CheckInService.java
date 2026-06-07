package com.cazoo.animal.module.checkin;

import com.cazoo.animal.common.BusinessException;
import com.cazoo.animal.common.ResultCode;
import com.cazoo.animal.module.checkin.dto.CheckInCreateRequest;
import com.cazoo.animal.module.checkin.entity.CheckIn;
import com.cazoo.animal.module.checkin.mapper.CheckInMapper;
import com.cazoo.animal.module.checkin.vo.CheckInTimelineVO;
import com.cazoo.animal.module.animal.AnimalService;
import com.cazoo.animal.module.user.UserService;
import com.cazoo.animal.module.user.entity.User;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class CheckInService {

    private final CheckInMapper checkInMapper;
    private final AnimalService animalService;
    private final UserService userService;

    @Transactional
    public CheckIn create(Long currentUserId, CheckInCreateRequest req) {
        // 校验动物存在(AnimalService.findById 内部会抛 BusinessException)
        animalService.findById(req.getAnimalId());

        CheckIn c = new CheckIn();
        c.setAnimalId(req.getAnimalId());
        c.setUserId(currentUserId);
        c.setContent(req.getContent());
        checkInMapper.insert(c);
        return c;
    }

    public List<CheckInTimelineVO> timeline(Long animalId) {
        // 决策 A:即使动物被软删,也允许看历史打卡
        // (AnimalService.findById 会过滤 deleted,删除后 404,与 LEFT JOIN 语义冲突)
        // 如果要"动物不存在(包括软删)就不让看",把下面这行加回来:
        // animalService.findById(animalId);
        return checkInMapper.selectTimelineByAnimalId(animalId);
    }
}
