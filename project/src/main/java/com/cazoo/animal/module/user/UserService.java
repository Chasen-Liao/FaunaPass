package com.cazoo.animal.module.user;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.cazoo.animal.common.BusinessException;
import com.cazoo.animal.common.ResultCode;
import com.cazoo.animal.module.user.entity.User;
import com.cazoo.animal.module.user.entity.UserRole;
import com.cazoo.animal.module.user.mapper.UserMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;

    public User findByUsername(String username) {
        return userMapper.selectOne(new LambdaQueryWrapper<User>()
                .eq(User::getUsername, username));
    }

    public User findById(Long id) {
        return userMapper.selectById(id);
    }

    @Transactional
    public User register(String username, String rawPassword, String nickname) {
        if (findByUsername(username) != null) {
            throw new BusinessException(ResultCode.USERNAME_EXISTS);
        }
        User u = new User();
        u.setUsername(username);
        u.setPassword(passwordEncoder.encode(rawPassword));
        u.setNickname(nickname);
        u.setRole(UserRole.STUDENT.name());
        userMapper.insert(u);
        return u;
    }
}
