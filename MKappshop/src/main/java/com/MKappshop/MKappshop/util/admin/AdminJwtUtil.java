// 包声明：该工具类位于 util.admin 包下，专门负责管理员 JWT 的生成与解析，与小程序 JWT 完全隔离
package com.MKappshop.MKappshop.util.admin;

// =========================================================================
// 管理员 JWT 工具类，独立于微信小程序 JWT，使用独立的密钥和过期时间配置。
// 基于 JJWT 0.12.5 API，适配 JDK 17+，无 JAXB 依赖。
// =========================================================================

// ========== JJWT 核心类库 ==========
import io.jsonwebtoken.Claims;               // JWT 载荷体，包含所有自定义声明（如 adminId、username、role）
import io.jsonwebtoken.Jwts;                // JWT 核心 API，提供构建、解析 JWT 的静态方法
import io.jsonwebtoken.security.Keys;       // 密钥工具类，用于从字符串生成安全的 HMAC 签名密钥

// ========== Spring 注解 ==========
import org.springframework.beans.factory.annotation.Value; // 注入配置文件属性（支持默认值语法）
import org.springframework.stereotype.Component;           // 标记为 Spring 组件，自动扫描并注册为 Bean

// ========== 项目内部依赖 ==========
import com.MKappshop.MKappshop.entity.admin.Admin;        // 管理员实体，用于生成 Token 时提取必要信息

// ========== Java 基础类库 ==========
import javax.crypto.SecretKey;              // JWT 签名密钥接口，JJWT 0.12.x 强制使用 SecretKey 而非字符串
import java.util.Date;                     // 日期对象，用于设置签发时间（issuedAt）和过期时间（expiration）

/**
 * 管理员 JWT 工具类。
 * <p>
 * 核心职责：
 * <ul>
 *   <li>生成管理员 JWT Token（包含管理员ID、用户名、角色）</li>
 *   <li>解析 JWT Token，提取 Claims（用于拦截器获取当前用户）</li>
 * </ul>
 * </p>
 * <p>
 * <strong>安全说明：</strong><br>
 * 使用独立的签名密钥，与微信小程序 JWT 完全隔离，避免 Token 混用导致越权。
 * 密钥通过配置文件注入，生产环境务必设置强密码（至少32位）并妥善保管。
 * </p>
 * <p>
 * <strong>JJWT 版本说明：</strong><br>
 * 本工具类使用 JJWT 0.12.5 API，与之前升级的安全版本一致，无已知漏洞。
 * </p>
 *
 * @author MKappshop
 * @since 1.0
 */
@Component                                                      // Spring 注解：将该类实例化为单例 Bean，托管于 IOC 容器
public class AdminJwtUtil {

    /**
     * JWT 签名密钥，从配置文件中读取（jwt.admin.secret）。
     * <p>
     * 默认值 "defaultAdminSecretKeyForJWT" 仅用于开发测试，
     * 生产环境必须通过 application.yml 或环境变量设置为强密码（建议长度 ≥32 字符）。
     * </p>
     */
    @Value("${jwt.admin.secret:defaultAdminSecretKeyForJWT}")  // Spring EL：从配置文件中获取键 jwt.admin.secret 的值，若不存在则使用冒号后的默认值
    private String secret;

    /**
     * JWT Token 过期时间（单位：秒），从配置文件读取（jwt.admin.expire）。
     * <p>
     * 默认值 7200 秒 = 2 小时，可根据业务安全要求调整（例如：登录有效期、刷新策略）。
     * </p>
     */
    @Value("${jwt.admin.expire:7200}")                          // 默认 2 小时（7200秒）
    private Long expire;

    /**
     * 根据管理员实体生成 JWT Token。
     * <p>
     * 载荷（Claims）包含：
     * <ul>
     *   <li>subject：用户名（用于快速标识）</li>
     *   <li>adminId：管理员ID（雪花算法生成）</li>
     *   <li>username：登录账号</li>
     *   <li>role：管理员角色</li>
     *   <li>issuedAt：签发时间</li>
     *   <li>expiration：过期时间</li>
     * </ul>
     * </p>
     *
     * @param admin 管理员实体（必须包含 id, username, role）
     * @return JWT Token 字符串（格式：eyJhbGciOiJIUzI1NiJ9...）
     */
    public String generateToken(Admin admin) {
        // 1. 将字符串密钥转换为 SecretKey 对象（JJWT 0.12.x 强制要求使用 Key 对象而非 byte[] 或 String）
        SecretKey key = Keys.hmacShaKeyFor(secret.getBytes());  // 根据密钥字符串生成 HMAC-SHA 密钥

        // 2. 计算当前时间与过期时间
        Date now = new Date();                                 // 当前系统时间，作为签发时间
        Date expiryDate = new Date(now.getTime() + expire * 1000); // 过期时间 = 当前时间 + expire 秒（乘以1000转为毫秒）

        // 3. 构建并签名 JWT Token
        return Jwts.builder()
                .subject(admin.getUsername())                   // 设置标准声明 sub（主题），这里存放用户名，便于快速识别
                .claim("adminId", admin.getId())               // 添加自定义声明：管理员ID（Long 类型）
                .claim("username", admin.getUsername())        // 添加自定义声明：登录账号（String）
                .claim("role", admin.getRole())                // 添加自定义声明：角色（String）
                .issuedAt(now)                                 // 设置标准声明 iat（签发时间）
                .expiration(expiryDate)                        // 设置标准声明 exp（过期时间）
                .signWith(key)                                 // 使用 HMAC-SHA 算法和密钥进行签名
                .compact();                                    // 将 Header、Payload、Signature 合并为紧凑的 JWT 字符串
    }

    /**
     * 解析 JWT Token，返回 Claims（载荷体）。
     * <p>
     * 若 Token 无效、过期或签名不正确，返回 null（由拦截器处理异常）。
     * </p>
     *
     * @param token JWT Token 字符串
     * @return Claims 对象，包含管理员ID、用户名、角色等信息；解析失败返回 null
     */
    public Claims parseToken(String token) {
        try {
            // 1. 将字符串密钥转换为 SecretKey 对象（必须与生成 Token 时使用的密钥完全一致）
            SecretKey key = Keys.hmacShaKeyFor(secret.getBytes());

            // 2. 解析 JWT Token，验证签名，返回 Claims
            return Jwts.parser()                               // 获取 JWT 解析器构建器
                    .verifyWith(key)                           // 设置验证签名所需的密钥
                    .build()                                   // 构建线程安全的 JwtParser 实例
                    .parseSignedClaims(token)                 // 解析 Token，同时验证签名和过期时间
                    .getPayload();                             // 获取解析后的 Claims 对象（载荷体）
        } catch (Exception e) {
            // 任何解析失败的情况（如 Token 过期、签名错误、格式错误、密钥不匹配等）均返回 null
            // 注意：不在此处打印异常堆栈，避免日志冗余；由调用方（拦截器）记录警告日志
            return null;
        }
    }

    /**
     * 从 Claims 中获取管理员ID。
     *
     * @param claims JWT 载荷体
     * @return 管理员ID，若不存在返回 null
     */
    public Long getAdminId(Claims claims) {
        // claims.get("adminId", Long.class) 会自动将值转换为 Long 类型
        return claims.get("adminId", Long.class);
    }

    /**
     * 从 Claims 中获取用户名。
     *
     * @param claims JWT 载荷体
     * @return 登录账号
     */
    public String getUsername(Claims claims) {
        return claims.get("username", String.class);
    }

    /**
     * 从 Claims 中获取管理员角色。
     *
     * @param claims JWT 载荷体
     * @return 角色（如 ADMIN, SUPER_ADMIN）
     */
    public String getRole(Claims claims) {
        return claims.get("role", String.class);
    }
}