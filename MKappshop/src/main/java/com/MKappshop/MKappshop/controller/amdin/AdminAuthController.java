package com.MKappshop.MKappshop.controller.amdin;
// 声明当前类所在的包，注意包名拼写应为 "admin"，此处为 "amdin"（可能笔误）

// ========== Spring MVC 注解 ==========
import com.MKappshop.MKappshop.annotation.CurrentAdmin;   // 自定义注解，用于注入当前登录的管理员对象
import io.swagger.v3.oas.annotations.Operation;          // Swagger/OpenAPI：接口方法说明
import io.swagger.v3.oas.annotations.tags.Tag;          // Swagger/OpenAPI：Controller 分组标签
import jakarta.validation.Valid;                        // JSR-303 验证注解，用于触发对象参数校验
import lombok.RequiredArgsConstructor;                  // Lombok：为所有 final 字段生成构造器，实现依赖注入
import lombok.extern.slf4j.Slf4j;                      // Lombok：自动生成 SLF4J 日志对象 log
import org.springframework.web.bind.annotation.*;      // Spring MVC 的 Web 绑定注解（GetMapping、PostMapping、RequestBody 等）

// ========== 项目内部依赖 ==========
import com.MKappshop.MKappshop.common.Result;               // 统一 API 响应封装类
import com.MKappshop.MKappshop.dto.admin.AdminLoginRequest; // 管理员登录请求数据传输对象（DTO）
import com.MKappshop.MKappshop.dto.admin.AdminLoginResponse;// 管理员登录响应 DTO
import com.MKappshop.MKappshop.entity.admin.Admin;          // 管理员实体类
import com.MKappshop.MKappshop.service.admin.AdminAuthService; // 管理员认证业务层接口

/**
 * 后台管理员认证控制器。
 * <p>
 * 职责：
 * <ul>
 *   <li>提供管理员登录接口，签发 JWT Token</li>
 *   <li>提供获取当前登录管理员信息接口</li>
 * </ul>
 * 该控制器与小程序用户认证完全分离，使用独立的 JWT 密钥和拦截器，
 * 保证后台系统与前端小程序的安全隔离。
 * </p>
 *
 * @author MKappshop
 * @since 1.0
 */
@Slf4j                                                      // Lombok：自动创建名为 log 的日志常量（org.slf4j.Logger）
@RestController                                             // 组合注解：@Controller + @ResponseBody，所有处理器方法返回值直接写入 HTTP 响应体（JSON）
@RequestMapping("/admin/auth")                              // 映射该控制器的请求基础路径，所有接口都以 /admin/auth 开头
@RequiredArgsConstructor                                   // Lombok：生成一个包含所有 final 字段的构造器（Spring 会通过此构造器自动注入）
@Tag(name = "管理员认证接口", description = "登录、获取当前管理员信息") // Swagger：为 Controller 添加文档标签
public class AdminAuthController {

    /**
     * 管理员认证服务，由 Spring 通过构造器自动注入。
     * final 修饰确保不可变，配合 @RequiredArgsConstructor 实现构造器注入。
     */
    private final AdminAuthService adminAuthService;        // 业务层依赖，处理登录认证、Token 签发等核心逻辑

    // ========== 管理员登录接口 ==========

    /**
     * 管理员登录。
     * <p>
     * 请求示例：POST /admin/auth/login
     * Content-Type: application/json
     * Body: {"username": "admin", "password": "123456"}
     * </p>
     *
     * @param loginRequest 登录请求体，包含用户名和密码（均不能为空）
     * @return 统一响应结果，包含 JWT Token 和管理员基本信息
     */
    @PostMapping("/login")                                 // 将方法映射到 HTTP POST 请求，URL 为 /admin/auth/login
    @Operation(summary = "管理员登录", description = "校验用户名密码，签发 JWT Token") // Swagger：接口说明
    public Result<AdminLoginResponse> login(               // 返回类型为统一响应 Result，泛型为登录响应 DTO
                                                           @RequestBody @Valid AdminLoginRequest loginRequest) {   // @RequestBody：将 JSON 请求体绑定到参数；@Valid：触发校验注解（如 @NotBlank）

        log.info("管理员登录请求 - 用户名: {}", loginRequest.getUsername()); // 使用 Lombok 生成的 log 对象打印 INFO 日志

        // 调用 Service 层执行登录逻辑，返回登录响应 DTO（包含 Token 和管理员信息）
        AdminLoginResponse response = adminAuthService.login(loginRequest);

        log.info("管理员登录成功 - 用户名: {}, 管理员ID: {}",    // 登录成功日志
                loginRequest.getUsername(), response.getAdminId());

        return Result.success(response);                   // 返回成功响应，携带登录数据
    }

    // ========== 获取当前登录管理员信息接口 ==========

    /**
     * 获取当前登录的管理员信息。
     * <p>
     * 该接口需要携带有效的管理员 JWT Token（放在请求头 Authorization 中）。
     * 拦截器会自动解析 Token 并注入 {@link com.MKappshop.MKappshop.annotation.CurrentAdmin} 注解。
     * </p>
     *
     * @param admin 当前登录的管理员实体，由自定义参数解析器注入
     * @return 统一响应结果，包含管理员基本信息（脱敏，不含密码）
     */
    @GetMapping("/me")                                     // 映射 HTTP GET 请求，URL 为 /admin/auth/me
    @Operation(summary = "获取当前登录管理员信息", description = "根据 Token 返回当前管理员基本信息")
    public Result<Admin> me(
            @CurrentAdmin Admin admin) {                  // 自定义注解：从 JWT 解析并注入完整的管理员实体

        log.info("获取当前管理员信息 - 管理员ID: {}, 用户名: {}", admin.getId(), admin.getUsername());

        // 出于安全考虑，清除密码字段，避免返回给前端
        admin.setPassword(null);                          // 将密码置空，防止敏感信息泄露

        return Result.success(admin);                    // 返回成功响应，携带管理员对象
    }

    // ========== 管理员登出接口（可选）==========

    /**
     * 管理员登出（前端只需丢弃本地 Token，本接口仅作记录）。
     * <p>
     * 由于 JWT 是无状态的，服务器端无需存储 Session。
     * 此处仅记录登出日志，实际登出由前端删除 Token 完成。
     * </p>
     *
     * @param admin 当前登录的管理员（由拦截器注入）
     * @return 登出成功消息
     */
    @PostMapping("/logout")                               // 映射 HTTP POST 请求，URL 为 /admin/auth/logout
    @Operation(summary = "管理员登出", description = "仅记录登出日志，前端需自行删除 Token")
    public Result<String> logout(@CurrentAdmin Admin admin) { // 注入当前管理员，仅用于日志记录

        log.info("管理员登出 - 管理员ID: {}, 用户名: {}", admin.getId(), admin.getUsername());

        return Result.success("登出成功");                // 返回成功提示
    }
}