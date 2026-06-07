package com.cazoo.animal;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
@MapperScan("com.cazoo.animal.module.**.mapper")
public class AnimalApplication {
    public static void main(String[] args) {
        SpringApplication.run(AnimalApplication.class, args);
    }
}
