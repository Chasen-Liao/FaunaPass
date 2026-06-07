package com.cazoo.animal.module.animal;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.cazoo.animal.common.Result;
import com.cazoo.animal.module.animal.dto.AnimalCreateRequest;
import com.cazoo.animal.module.animal.dto.AnimalQueryRequest;
import com.cazoo.animal.module.animal.entity.Animal;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/animals")
@RequiredArgsConstructor
public class AnimalController {

    private final AnimalService animalService;

    @GetMapping
    public Result<Page<Animal>> list(AnimalQueryRequest req) {
        return Result.success(animalService.page(req));
    }

    @GetMapping("/{id}")
    public Result<Animal> detail(@PathVariable Long id) {
        return Result.success(animalService.findById(id));
    }

    @PostMapping(consumes = "multipart/form-data")
    @PreAuthorize("hasRole('ADMIN')")
    public Result<Animal> create(@Valid @ModelAttribute AnimalCreateRequest req) {
        return Result.success(animalService.create(req));
    }

    @PutMapping(value = "/{id}", consumes = "multipart/form-data")
    @PreAuthorize("hasRole('ADMIN')")
    public Result<Animal> update(@PathVariable Long id,
                                 @Valid @ModelAttribute AnimalCreateRequest req) {
        return Result.success(animalService.update(id, req));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public Result<Void> delete(@PathVariable Long id) {
        animalService.softDelete(id);
        return Result.success(null);
    }
}
