package com.MKappshop.MKappshop.entity.admin;

// =========================================================================
// 根据用户要求及架构设计决策：
// 1. Admin（后台管理员）为内部管理实体，数据量极小（通常不超过100条），
//    无分库分表需求，不对外暴露，不存在被爬虫遍历的安全风险。
// 2. 因此主键策略**保留数据库自增（IDENTITY）**，不采用雪花算法等分布式ID。
// 3. 保持与核心业务实体（Product/Order/WxUser）的ID策略差异，符合“合适场景用合适技术”原则。
// 4. 本实体采用Lombok简化代码，所有字段均包含完整Javadoc及行内注释。
// =========================================================================

// ========== Jakarta Persistence (JPA) 注解 ==========
import jakarta.persistence.Column;        // 映射数据库列，可定义列名、长度、是否为空等
import jakarta.persistence.Entity;       // 标记该类为 JPA 实体，与数据库表映射
import jakarta.persistence.GeneratedValue; // 配置主键生成策略
import jakarta.persistence.GenerationType; // 提供主键生成方式枚举（如 IDENTITY）
import jakarta.persistence.Id;           // 标记该字段为数据库表的主键
import jakarta.persistence.Table;        // 指定实体对应的数据库表名

// ========== Bean Validation 参数校验注解 ==========
import jakarta.validation.constraints.NotNull; // 标记字段不能为 null（校验）
import jakarta.validation.constraints.Size;    // 标记字段长度或集合大小限制

// ========== Lombok 注解 ==========
import lombok.AllArgsConstructor;        // 生成包含所有字段的构造方法
import lombok.Builder;                  // 生成建造者模式 API
import lombok.Getter;                   // 生成所有字段的 Getter 方法
import lombok.NoArgsConstructor;        // 生成无参构造方法（JPA 强制）
import lombok.Setter;                   // 生成所有字段的 Setter 方法

// ========== Hibernate 扩展注解 ==========
import org.hibernate.annotations.CreationTimestamp; // 自动填充创建时间（插入时）
import org.hibernate.annotations.UpdateTimestamp;   // 自动填充更新时间（插入/更新时）

// ========== Java 8+ 日期时间 API ==========
import java.time.LocalDateTime; // 使用 LocalDateTime 存储日期时间，无时区问题

/**
 * 后台管理员实体类，映射数据库中的 admin 表。
 * <p>
 * 核心职责：
 * <ul>
 *   <li>存储管理员账号、加密密码、昵称、角色、状态等核心信息</li>
 *   <li>自动管理创建时间、更新时间（通过 Hibernate 注解）</li>
 *   <li>与小程序用户（WxUser）完全分离，独立认证与授权</li>
 * </ul>
 * </p>
 * <p>
 * <strong>主键策略说明：</strong><br>
 * 后台管理员表数据量极少（通常 ≤ 100 条），无分库分表需求，不对外暴露，
 * 不存在被恶意遍历的风险，因此采用最简单的数据库自增主键（IDENTITY）。
 * 这与核心业务表（商品、订单、用户）使用的雪花算法分布式ID策略形成明确区分，
 * 符合“不同场景采用不同技术”的架构原则。
 * </p>
 *
 * @author MKappshop
 * @since 1.0
 */
@Getter                     // Lombok：为所有非静态字段生成 getter 方法，编译期生效
@Setter                     // Lombok：为所有非静态字段生成 setter 方法，编译期生效
@NoArgsConstructor          // Lombok：生成无参构造方法，JPA 规范强制要求（反射创建实体时使用）
@AllArgsConstructor         // Lombok：生成包含所有字段的构造方法，便于快速创建完整管理员对象（单元测试/手动构造）
@Builder                    // Lombok：生成建造者模式 API，配合 @AllArgsConstructor 使用，提供链式赋值
@Entity                     // JPA：声明此类为 JPA 实体，将被 Hibernate 管理，映射到数据库表
@Table(name = "admin")      // JPA：指定映射的表名为 "admin"（非 SQL 关键字，无需转义）
public class Admin {

    /**
     * 管理员唯一主键，自增长。
     * <p>
     * 数据库列定义：id BIGINT AUTO_INCREMENT PRIMARY KEY
     * JPA 策略：IDENTITY 表示使用数据库自增字段。
     * 该字段插入后不可更新。
     * </p>
     */
    @Id                                                     // JPA：标记该字段为实体主键
    @GeneratedValue(strategy = GenerationType.IDENTITY)     // JPA：主键生成策略为数据库自增（IDENTITY）
    @Column(name = "id", nullable = false, updatable = false) // 映射列名 id，非空，不可更新
    private Long id;                                        // 管理员唯一标识 ID，由数据库自动生成，业务层不应手动赋值

    /**
     * 管理员登录账号，全局唯一。
     * <p>
     * 校验：长度 1~50，非空，唯一约束。
     * 数据库列：username VARCHAR(50) NOT NULL UNIQUE
     * </p>
     */
    @Size(max = 50, message = "用户名长度不能超过50个字符")   // Bean Validation：限制字符串最大长度
    @NotNull(message = "用户名不能为空")                     // Bean Validation：禁止为空
    @Column(name = "username", nullable = false, unique = true, length = 50) // 列名 username，非空，唯一，长度50
    private String username;                                // 管理员登录账号，用于后台登录认证

    /**
     * 管理员登录密码，必须使用 BCrypt 加密后存储。
     * <p>
     * 校验：长度 ≤ 255（BCrypt 密文通常为60字符，预留足够空间），非空。
     * 数据库列：password VARCHAR(255) NOT NULL
     * </p>
     * <strong>安全警告：</strong>绝对禁止明文存储，Service 层必须使用 BCryptPasswordEncoder 加密。
     */
    @Size(max = 255, message = "密码长度不能超过255个字符")  // Bean Validation：最大长度限制
    @NotNull(message = "密码不能为空")                       // Bean Validation：禁止为空
    @Column(name = "password", nullable = false)            // 列名 password，非空，长度默认255（由 varchar 定义）
    private String password;                                // 加密后的密码字符串，永远不会明文存储或传输

    /**
     * 管理员昵称（显示名称），非必填。
     * <p>
     * 校验：长度 ≤ 50。
     * 数据库列：nickname VARCHAR(50) DEFAULT NULL
     * </p>
     */
    @Size(max = 50, message = "昵称长度不能超过50个字符")   // Bean Validation：最大长度限制
    @Column(name = "nickname", length = 50)                 // 列名 nickname，长度50，可为空（默认）
    private String nickname;                                // 管理员昵称，可为 null，用于后台界面展示

    /**
     * 管理员角色。
     * <p>
     * 简单场景使用字符串，例如 "ADMIN"、"SUPER_ADMIN"。
     * 默认值为 "ADMIN"（在 Java 字段定义时直接赋值）。
     * 数据库列：role VARCHAR(20) DEFAULT 'ADMIN'
     * </p>
     */
    @Column(name = "role", length = 20)                     // 列名 role，长度20，可为空，但有默认值处理
    private String role = "ADMIN";                          // 角色，默认普通管理员，可根据业务扩展

    /**
     * 账号状态。
     * <p>
     * 1：正常；0：禁用（逻辑删除）。
     * 默认值为 1（正常状态），在字段定义时直接赋值。
     * 数据库列：status TINYINT NOT NULL DEFAULT 1
     * </p>
     */
    @Column(name = "status", nullable = false)              // 列名 status，非空，默认值由数据库或 Java 赋值保证
    private Integer status = 1;                             // 状态：1-正常，0-禁用，默认正常

    /**
     * 最后登录时间。
     * <p>
     * 每次登录成功时由 Service 层手动更新（调用 setLastLoginTime）。
     * 可为空（表示从未登录）。
     * 数据库列：last_login_time DATETIME
     * </p>
     */
    @Column(name = "last_login_time")                       // 列名 last_login_time，可为空
    private LocalDateTime lastLoginTime;                    // 最后登录时间戳，用于安全审计

    /**
     * 管理员账号创建时间。
     * <p>
     * 由 Hibernate 的 {@link CreationTimestamp} 注解自动填充（INSERT 时生成数据库当前时间）。
     * 不可更新，插入后不再改变。
     * 数据库列：create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP
     * </p>
     */
    @CreationTimestamp                                      // Hibernate：插入时自动设为当前数据库时间
    @Column(name = "create_time", nullable = false, updatable = false) // 列名 create_time，非空，不可更新
    private LocalDateTime createTime;                       // 创建时间，由 Hibernate 自动维护，业务层无需干预

    /**
     * 管理员信息最后更新时间。
     * <p>
     * 由 Hibernate 的 {@link UpdateTimestamp} 注解自动填充（INSERT/UPDATE 时刷新为数据库当前时间）。
     * 用于乐观锁或审计追踪。
     * 数据库列：update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
     * </p>
     */
    @UpdateTimestamp                                        // Hibernate：插入/更新时自动刷新为当前数据库时间
    @Column(name = "update_time")                           // 列名 update_time，可为空（但有数据库默认处理）
    private LocalDateTime updateTime;                       // 最后更新时间，由 Hibernate 自动维护

    // ========== Lombok 已自动生成的方法 ==========
    // 以下方法均在编译期由 Lombok 自动生成，无需手写，此处仅为说明：
    // - Getter/Setter 方法（所有字段）
    // - 无参构造方法（@NoArgsConstructor）
    // - 全参构造方法（@AllArgsConstructor）
    // - 建造者模式（@Builder）
    // 这些方法使实体类保持极简，同时满足 JPA 规范和开发便利性。
    // 若需要自定义构造逻辑，请手动添加构造方法并保留 @NoArgsConstructor 注解。

}