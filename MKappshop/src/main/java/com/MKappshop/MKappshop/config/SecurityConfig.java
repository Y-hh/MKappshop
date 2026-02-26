package com.MKappshop.MKappshop.config;  // 定义当前类所在的包，该包存放配置类

import org.springframework.context.annotation.Bean;  // 导入 @Bean 注解，用于将方法返回值注册为 Spring 容器中的 Bean
import org.springframework.context.annotation.Configuration;  // 导入 @Configuration 注解，标识当前类为配置类
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;  // 导入 BCrypt 密码编码器的具体实现类
import org.springframework.security.crypto.password.PasswordEncoder;  // 导入 Spring Security 的密码编码器接口

/**
 * 安全配置类。
 * <p>
 * 仅提供 BCrypt 密码编码器，不启用 Spring Security 的任何过滤器或自动配置。
 * 适用于仅需密码加密/匹配功能的场景。
 * </p>
 *
 * @author MKappshop
 * @since 1.0
 */
@Configuration  // 声明当前类是一个 Spring 配置类，容器会加载其中的 @Bean 定义
public class SecurityConfig {

    /**
     * BCrypt 密码编码器（强度 10，默认）。
     * <p>
     * BCrypt 算法自动包含盐值，每次加密结果不同，但 matches 方法可正确匹配。
     * 推荐用于生产环境密码存储。
     * </p>
     *
     * @return PasswordEncoder 实例
     */
    @Bean  // 将 passwordEncoder() 方法的返回值注册为 Spring 容器中的一个 Bean，默认 Bean 名称为方法名 "passwordEncoder"
    public PasswordEncoder passwordEncoder() {  // 定义 Bean 的方法，返回类型为 PasswordEncoder 接口
        return new BCryptPasswordEncoder();  // 创建 BCryptPasswordEncoder 实例（默认强度为 10）并返回
    }
}