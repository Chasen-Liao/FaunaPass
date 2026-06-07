package com.cazoo.animal.module.animal.dto;

import com.cazoo.animal.module.animal.entity.AnimalType;
import lombok.Data;

@Data
public class AnimalQueryRequest {
    private String name;
    private AnimalType type;
    private Integer page = 1;
    private Integer size = 10;
}
