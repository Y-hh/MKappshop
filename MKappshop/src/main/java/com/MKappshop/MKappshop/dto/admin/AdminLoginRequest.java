// 包声明：当前 DTO 类位于 admin 模块下，与管理员认证相关
package com.MKappshop.MKappshop.dto.admin;

// ========== Jakarta Bean Validation 注解 ==========
import jakarta.validation.constraints.NotBlank; // 非空校验注解：字段不能为 null 且不能是空白字符串
import jakarta.validation.constraints.Size;     // 长度校验注解：限制字符串最小/最大长度

// ========== Lombok 注解 ==========
import lombok.Data; // Lombok 注解：自动生成 getter/setter、equals、hashCode、toString、requiredArgsConstructor 等

/**
 * 管理员登录请求数据传输对象。
 * <p>
 * 用于接收前端传递的管理员登录表单数据（用户名 + 密码）。
 * 配合 {@link org.springframework.web.bind.annotation.RequestBody} 和 {@link jakarta.validation.Valid} 实现自动参数校验。
 * </p>
 *
 * @author MKappshop
 * @since 1.0
 */
@Data // Lombok 注解：编译期自动为所有非 final 字段生成 getter/setter，并生成 toString、equals、hashCode 和构造方法
public class AdminLoginRequest {

    /**
     * 管理员用户名
     * <p>
     * 约束条件：
     * <ul>
     *   <li>不能为空（包含 null、空串、纯空格）</li>
     *   <li>最大长度 50 个字符</li>
     * </ul>
     * </p>
     */
    @NotBlank(message = "用户名不能为空") // 校验注解：字段值不能为 null，且 trim() 后长度必须 > 0
    @Size(max = 50, message = "用户名长度不能超过50") // 校验注解：字符串长度 ≤ 50（仅当字段值不为 null 时校验）
    private String username;

    /**
     * 管理员密码
     * <p>
     * 约束条件：
     * <ul>
     *   <li>不能为空（包含 null、空串、纯空格）</li>
     *   <li>最大长度 255 个字符（兼容加密后的长度）</li>
     * </ul>
     * </p>
     */
    @NotBlank(message = "密码不能为空") // 校验注解：密码字段必须提供非空白值
    @Size(max = 255, message = "密码长度不能超过255") // 校验注解：最大长度 255（BCrypt 加密后通常为 60，预留空间）
    private String password;
}