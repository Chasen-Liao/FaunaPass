package com.cazoo.animal.module.auth;

import com.cazoo.animal.common.BusinessException;
import com.cazoo.animal.common.ResultCode;
import com.cazoo.animal.module.auth.dto.LoginRequest;
import com.cazoo.animal.module.auth.dto.RegisterRequest;
import com.cazoo.animal.module.auth.vo.LoginResponse;
import com.cazoo.animal.module.user.UserService;
import com.cazoo.animal.module.user.entity.User;
import com.cazoo.animal.security.JwtUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserService userService;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;

    public LoginResponse login(LoginRequest req) {
        User u = userService.findByUsername(req.getUsername());
        if (u == null || !passwordEncoder.matches(req.getPassword(), u.getPassword())) {
            throw new BusinessException(ResultCode.INVALID_CREDENTIALS);
        }
        String token = jwtUtil.generate(u.getId(), u.getUsername(), u.getRole());
        return new LoginResponse(token, u.getId(), u.getUsername(), u.getNickname(), u.getRole());
    }

    public LoginResponse register(RegisterRequest req) {
        User u = userService.register(req.getUsername(), req.getPassword(), req.getNickname());
        String token = jwtUtil.generate(u.getId(), u.getUsername(), u.getRole());
        return new LoginResponse(token, u.getId(), u.getUsername(), u.getNickname(), u.getRole());
    }
}
