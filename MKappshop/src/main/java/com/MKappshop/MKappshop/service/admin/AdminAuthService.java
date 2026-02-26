// =========================================================================
// 包声明：该服务类位于 service.admin 包下，负责管理员模块的业务逻辑
package com.MKappshop.MKappshop.service.admin;

// =========================================================================
// 项目架构设计说明及管理员认证核心职责（文档注释）
/**
 * 根据项目架构设计及管理员认证需求：
 * 1. AdminAuthService 是后台管理员认证的核心业务服务，与小程序用户认证完全分离。
 * 2. 负责管理员登录、密码校验、最后登录时间更新、JWT Token 签发等核心逻辑。
 * 3. 使用 BCrypt 密码加密，确保密码存储安全；使用独立 JWT 密钥，与小程序端隔离。
 * 4. 严格遵循分层架构：Controller → Service → Repository，事务边界在 Service 层。
 * 5. 所有业务异常均通过自定义 BizException 抛出，由全局异常处理器统一处理。
 */

// ========== Spring 核心注解 ==========
import lombok.RequiredArgsConstructor;           // Lombok：生成包含所有 final 字段的构造器（实现构造器注入）
import lombok.extern.slf4j.Slf4j;               // Lombok：自动生成 SLF4J 日志常量 log
import org.springframework.security.crypto.password.PasswordEncoder; // Spring Security：密码加密匹配器接口（此处使用 BCrypt 实现）
import org.springframework.stereotype.Service;   // Spring：标记当前类为业务层组件，自动注册为 Spring Bean
import org.springframework.transaction.annotation.Transactional; // Spring：声明式事务注解，用于控制事务边界

// ========== 项目内部依赖 ==========
import com.MKappshop.MKappshop.dto.admin.AdminLoginRequest;   // 管理员登录请求 DTO（用户名、密码）
import com.MKappshop.MKappshop.dto.admin.AdminLoginResponse;  // 管理员登录响应 DTO（Token、管理员信息）
import com.MKappshop.MKappshop.entity.admin.Admin;            // 管理员实体类（对应数据库表）
import com.MKappshop.MKappshop.exception.BizException;        // 自定义业务异常，用于统一异常处理
import com.MKappshop.MKappshop.repository.admin.AdminRepository; // 管理员数据访问接口（JPA）
import com.MKappshop.MKappshop.util.admin.AdminJwtUtil;            // 管理员专用 JWT 工具类（独立密钥）

// ========== Java 基础类库 ==========
import java.time.LocalDateTime;                 // Java 8 时间 API，用于记录最后登录时间

/**
 * 管理员认证业务服务类。
 * <p>
 * 核心职责：
 * <ul>
 *   <li>管理员登录认证：根据用户名查询管理员，校验密码，校验账号状态</li>
 *   <li>更新最后登录时间（审计）</li>
 *   <li>签发管理员专用 JWT Token（与小程序用户 Token 使用不同密钥）</li>
 * </ul>
 * </p>
 * <p>
 * <strong>密码安全说明：</strong><br>
 * 所有管理员密码均使用 BCrypt 算法加密存储，Service 层通过 {@link PasswordEncoder}
 * 完成密码匹配，绝不对密码进行解密操作。数据库禁止存储明文密码。
 * </p>
 * <p>
 * <strong>事务说明：</strong><br>
 * 登录操作涉及管理员实体的更新（last_login_time），使用 {@link Transactional} 注解
 * 保证原子性，避免部分更新导致数据不一致。
 * </p>
 *
 * @author MKappshop
 * @since 1.0
 */
@Slf4j                                          // Lombok：编译时生成 org.slf4j.Logger log = LoggerFactory.getLogger(AdminAuthService.class);
@Service                                        // Spring：将该类标记为 Service 组件，纳入 IOC 容器管理
@RequiredArgsConstructor                       // Lombok：生成一个包含所有 final 字段的公共构造器（Spring 会调用此构造器自动注入依赖）
public class AdminAuthService {

    // --- 字段声明（所有字段均为 final，通过构造器注入，保证不可变性和线程安全）---

    /**
     * 管理员数据访问层接口，由 Spring 通过构造器自动注入。
     */
    private final AdminRepository adminRepository;

    /**
     * 密码加密匹配器（BCrypt），由 Spring Security 提供，需在配置类中声明为 Bean。
     * 用于校验用户输入的明文密码与数据库中存储的密文是否匹配。
     */
    private final PasswordEncoder passwordEncoder;

    /**
     * 管理员 JWT 工具类，负责生成和解析管理员专用 Token。
     * 该工具类使用独立的密钥和过期时间配置，与小程序端完全隔离。
     */
    private final AdminJwtUtil adminJwtUtil;

    // --- 业务方法 ---

    /**
     * 管理员登录认证。
     * <p>
     * 业务逻辑步骤：
     * <ol>
     *   <li>根据用户名查询管理员，若不存在则抛出 {@link BizException}（模糊提示“用户名或密码错误”）</li>
     *   <li>校验密码：使用 {@link PasswordEncoder#matches(CharSequence, String)} 比对明文和密文</li>
     *   <li>校验账号状态：仅当 status = 1（正常）时允许登录</li>
     *   <li>更新管理员最后登录时间为当前系统时间</li>
     *   <li>调用 {@link AdminJwtUtil#generateToken(Admin)} 生成 JWT Token</li>
     *   <li>构建并返回登录响应 DTO（包含 Token、管理员ID、用户名、昵称、角色）</li>
     * </ol>
     * </p>
     * <p>
     * <strong>安全提示：</strong><br>
     * 为防范用户枚举攻击，用户名不存在和密码错误均返回相同的错误信息“用户名或密码错误”，
     * 不提供具体是用户名错误还是密码错误的提示。
     * </p>
     *
     * @param loginRequest 登录请求对象，包含用户名和明文密码（已由 Controller 层完成基础校验）
     * @return 登录响应 DTO，包含 JWT Token 及管理员基本信息（不含密码）
     * @throws BizException 当用户名不存在、密码错误、账号被禁用时抛出
     */
    @Transactional                                                  // 开启 Spring 声明式事务，确保 last_login_time 更新与查询在同一事务内
    public AdminLoginResponse login(AdminLoginRequest loginRequest) {
        // 1. 从请求对象中提取用户名和明文密码
        String username = loginRequest.getUsername();
        String rawPassword = loginRequest.getPassword();

        // 2. 根据用户名查询管理员（利用 JPA 方法名解析，返回 Optional<Admin>）
        Admin admin = adminRepository.findByUsername(username)
                .orElseThrow(() -> {
                    // 使用 orElseThrow 在 Optional 为空时抛出异常
                    // 记录警告日志：用户名不存在（仅服务端可见，不返回给客户端）
                    log.warn("管理员登录失败 - 用户名不存在: {}", username);
                    // 抛出 BizException，全局异常处理器会将其转换为 JSON 响应
                    return new BizException("用户名或密码错误");
                });

        // 3. 密码校验：passwordEncoder.matches(明文, 密文) 返回 true 表示匹配成功
        if (!passwordEncoder.matches(rawPassword, admin.getPassword())) {
            // 记录警告日志：密码错误（注意：不要记录明文密码）
            log.warn("管理员登录失败 - 密码错误: 用户名={}", username);
            // 抛出与“用户不存在”相同的提示信息，防止用户名枚举
            throw new BizException("用户名或密码错误");
        }

        // 4. 账号状态校验：实体中 status 字段约定 1=正常，0=禁用（或其他非1值均为禁用）
        if (admin.getStatus() != 1) {
            log.warn("管理员登录失败 - 账号已被禁用: 用户名={}, 状态={}", username, admin.getStatus());
            // 明确提示账号被禁用，此信息可返回给客户端（不涉及安全风险）
            throw new BizException("账号已被禁用，请联系系统管理员");
        }

        // 5. 更新最后登录时间为当前数据库时间（这里使用 Java 系统时间）
        admin.setLastLoginTime(LocalDateTime.now());
        // 调用 save 方法持久化更新（JpaRepository 的 save 方法执行 merge 操作）
        adminRepository.save(admin);
        // 记录成功登录日志，包含管理员 ID 和角色，便于审计
        log.info("管理员登录成功 - 用户名: {}, 管理员ID: {}, 角色: {}",
                admin.getUsername(), admin.getId(), admin.getRole());

        // 6. 生成 JWT Token（管理员专用）
        String token = adminJwtUtil.generateToken(admin);

        // 7. 使用 Builder 模式构建登录响应 DTO，仅返回非敏感信息
        return AdminLoginResponse.builder()
                .token(token)                               // JWT 字符串，客户端需在后续请求的 Authorization 头中携带
                .adminId(admin.getId())                     // 管理员唯一标识（雪花算法 Long 型）
                .username(admin.getUsername())              // 登录账号
                .nickname(admin.getNickname())              // 昵称（可为 null）
                .role(admin.getRole())                      // 角色标识（如 ADMIN, OPERATOR）
                .build();
    }

    // ========== 其他管理员认证相关方法（如有）可在此扩展 ==========
    // 例如：修改密码、刷新 Token、登出记录等，遵循当前项目架构规范。
}