package com.cazoo.animal.module.animal.entity;

import com.baomidou.mybatisplus.annotation.EnumValue;
import com.fasterxml.jackson.annotation.JsonValue;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum AnimalType {
    CAT("CAT", "猫"),
    DOG("DOG", "狗");

    @EnumValue
    @JsonValue
    private final String code;
    private final String label;
}
