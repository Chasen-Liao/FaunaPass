package com.cazoo.animal.module.auth;

import com.cazoo.animal.common.BusinessException;
import com.cazoo.animal.common.Result;
import com.cazoo.animal.common.ResultCode;
import com.cazoo.animal.module.auth.dto.LoginRequest;
import com.cazoo.animal.module.auth.dto.RegisterRequest;
import com.cazoo.animal.module.auth.vo.LoginResponse;
import com.cazoo.animal.module.auth.vo.UserInfo;
import com.cazoo.animal.module.user.UserService;
import com.cazoo.animal.module.user.entity.User;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;
    private final UserService userService;

    @PostMapping("/register")
    public Result<LoginResponse> register(@Valid @RequestBody RegisterRequest req) {
        return Result.success(authService.register(req));
    }

    @PostMapping("/login")
    public Result<LoginResponse> login(@Valid @RequestBody LoginRequest req) {
        return Result.success(authService.login(req));
    }

    @GetMapping("/me")
    public Result<UserInfo> me() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || auth.getPrincipal() == null || "anonymousUser".equals(auth.getPrincipal())) {
            throw new BusinessException(ResultCode.UNAUTHORIZED);
        }
        Long userId = Long.valueOf(auth.getName());
        User u = userService.findById(userId);
        if (u == null) {
            throw new BusinessException(ResultCode.USER_NOT_FOUND);
        }
        return Result.success(new UserInfo(u.getId(), u.getUsername(), u.getNickname(), u.getRole()));
    }
}
