package com.cazoo.animal.module.checkin;

import com.cazoo.animal.common.BusinessException;
import com.cazoo.animal.common.Result;
import com.cazoo.animal.common.ResultCode;
import com.cazoo.animal.module.checkin.dto.CheckInCreateRequest;
import com.cazoo.animal.module.checkin.entity.CheckIn;
import com.cazoo.animal.module.checkin.vo.CheckInTimelineVO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
public class CheckInController {

    private final CheckInService checkInService;

    @PostMapping("/api/check-ins")
    @PreAuthorize("hasRole('STUDENT')")
    public Result<CheckIn> create(@Valid @RequestBody CheckInCreateRequest req) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        Long userId = Long.valueOf(auth.getName());
        return Result.success(checkInService.create(userId, req));
    }

    @GetMapping("/api/animals/{animalId}/check-ins")
    public Result<List<CheckInTimelineVO>> timeline(@PathVariable Long animalId) {
        return Result.success(checkInService.timeline(animalId));
    }
}
