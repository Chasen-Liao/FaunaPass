package com.cazoo.animal.module.animal;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.cazoo.animal.common.BusinessException;
import com.cazoo.animal.common.ResultCode;
import com.cazoo.animal.module.animal.dto.AnimalCreateRequest;
import com.cazoo.animal.module.animal.dto.AnimalQueryRequest;
import com.cazoo.animal.module.animal.entity.Animal;
import com.cazoo.animal.module.animal.mapper.AnimalMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
@RequiredArgsConstructor
public class AnimalService {

    private final AnimalMapper animalMapper;
    private final FileStorageService fileStorageService;

    public Page<Animal> page(AnimalQueryRequest req) {
        LambdaQueryWrapper<Animal> qw = new LambdaQueryWrapper<Animal>()
                .likeRight(StringUtils.hasText(req.getName()), Animal::getName, req.getName())
                .eq(req.getType() != null, Animal::getType, req.getType() != null ? req.getType().getCode() : null)
                .orderByDesc(Animal::getCreatedAt);
        return animalMapper.selectPage(new Page<>(req.getPage(), req.getSize()), qw);
    }

    public Animal findById(Long id) {
        Animal a = animalMapper.selectById(id);
        if (a == null) {
            throw new BusinessException(ResultCode.ANIMAL_NOT_FOUND);
        }
        return a;
    }

    @Transactional
    public Animal create(AnimalCreateRequest req) {
        Animal a = new Animal();
        a.setName(req.getName());
        a.setType(req.getType().getCode());
        a.setArea(req.getArea());
        if (req.getCover() != null && !req.getCover().isEmpty()) {
            a.setCoverImage(fileStorageService.saveAnimalCover(req.getCover()));
        }
        animalMapper.insert(a);
        return a;
    }

    @Transactional
    public Animal update(Long id, AnimalCreateRequest req) {
        Animal a = findById(id);
        a.setName(req.getName());
        a.setType(req.getType().getCode());
        a.setArea(req.getArea());
        a.setUpdatedAt(java.time.LocalDateTime.now()); // PG 不靠触发器,应用层维护
        if (req.getCover() != null && !req.getCover().isEmpty()) {
            a.setCoverImage(fileStorageService.saveAnimalCover(req.getCover()));
        }
        animalMapper.updateById(a);
        return a;
    }

    @Transactional
    public void softDelete(Long id) {
        // 动物有打卡记录,不能硬删;软删保留历史
        Animal a = findById(id);
        animalMapper.deleteById(a.getId()); // @TableLogic 会转成 UPDATE deleted = 1
    }
}
