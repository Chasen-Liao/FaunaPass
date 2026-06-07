package com.cazoo.animal.module.checkin.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class CheckInCreateRequest {
    @NotNull(message = "animalId 不能为空")
    private Long animalId;

    @NotBlank(message = "打卡内容不能为空")
    @Size(max = 500, message = "打卡内容不超过 500 字")
    private String content;
}
