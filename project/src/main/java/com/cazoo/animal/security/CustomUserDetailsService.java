package com.cazoo.animal.security;

import com.cazoo.animal.module.user.UserService;
import com.cazoo.animal.module.user.entity.User;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 注:本项目不走 Spring Security 的 AuthenticationManager + DaoAuthenticationProvider 流程,
 *     登录直接由 AuthService 用 BCrypt 校验后签发 JWT。
 *     本类保留仅作"如果未来加扩展登录方式(短信/SSO)的预留",当前未被 Spring 容器调用。
 *     JwtAuthenticationFilter 直接从 JWT 解析出 userId+role,绕开 UserDetailsService。
 */
@Service
@RequiredArgsConstructor
public class CustomUserDetailsService implements UserDetailsService {

    private final UserService userService;

    @Override
    public UserDetails loadUserByUsername(String username) {
        User u = userService.findByUsername(username);
        if (u == null) {
            throw new UsernameNotFoundException("用户不存在: " + username);
        }
        // ⚠️ hasRole('X') 要求 authorities 含 "ROLE_X" 前缀
        return org.springframework.security.core.userdetails.User
                .withUsername(u.getUsername())
                .password(u.getPassword())
                .authorities(List.of(new SimpleGrantedAuthority("ROLE_" + u.getRole())))
                .build();
    }
}
