// 包声明：当前类位于 admin 模块下的 dto（数据传输对象）包中，用于封装管理员认证相关的响应数据
package com.MKappshop.MKappshop.dto.admin;

// Lombok 的 @Builder 注解导入：为类生成建造者模式的 Builder 内部类及相关方法
import lombok.Builder;
// Lombok 的 @Data 注解导入：组合注解，包含 @Getter/@Setter/@ToString/@EqualsAndHashCode/@RequiredArgsConstructor
import lombok.Data;

/**
 * 管理员登录响应数据传输对象。
 * <p>
 * 用于封装管理员登录成功后的返回数据，包含 JWT Token、管理员基本信息及角色。
 * 该对象将自动序列化为 JSON 返回给前端。
 * </p>
 *
 * @author MKappshop
 * @since 1.0
 */
// @Data：Lombok 注解，编译时自动生成所有字段的 getter/setter、toString()、equals()、hashCode() 以及全参构造器（若需要）
@Data
// @Builder：Lombok 注解，为当前类生成建造者模式代码，允许使用链式调用构建对象，例如：
// AdminLoginResponse.builder().token("xxx").adminId(1L).build()
@Builder
public class AdminLoginResponse {

    /**
     * JWT Token，需在请求头 Authorization 中携带。
     * <p>
     * 客户端后续请求需在 HTTP Header 中添加 "Authorization: Bearer {token}"。
     * </p>
     */
    private String token;          // JWT Token 字符串，由服务端签发，包含管理员身份信息

    /**
     * 管理员 ID。
     * <p>
     * 数据库主键，唯一标识一个管理员。
     * </p>
     */
    private Long adminId;         // 管理员唯一标识，长整型，对应数据库表的主键

    /**
     * 管理员登录账号。
     * <p>
     * 用于登录的用户名，通常唯一且不可修改（或受限修改）。
     * </p>
     */
    private String username;      // 管理员登录账号，字符串类型

    /**
     * 管理员昵称。
     * <p>
     * 用于界面展示的友好名称，允许重复。
     * </p>
     */
    private String nickname;      // 管理员昵称，可选字段，可为空

    /**
     * 管理员角色。
     * <p>
     * 用于权限控制，例如："ADMIN"、"SUPER_ADMIN"、"OPERATOR" 等。
     * </p>
     */
    private String role;         // 角色标识，用于授权判断
}