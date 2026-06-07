package com.cazoo.animal.module.animal.dto;

import com.cazoo.animal.module.animal.entity.AnimalType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;
import org.springframework.web.multipart.MultipartFile;

@Data
public class AnimalCreateRequest {
    @NotBlank(message = "名字不能为空")
    @Size(max = 50)
    private String name;

    @NotNull(message = "类型不能为空")
    private AnimalType type;

    @NotBlank(message = "区域不能为空")
    @Size(max = 100)
    private String area;

    /** 封面图,可空(允许先建档后补图) */
    private MultipartFile cover;
}
