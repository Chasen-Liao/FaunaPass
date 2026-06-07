package com.cazoo.animal.module.animal.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.cazoo.animal.module.animal.entity.Animal;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface AnimalMapper extends BaseMapper<Animal> {
}
