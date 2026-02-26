package com.MKappshop.MKappshop.entity.wx;

// =========================================================================
// 根据用户要求及架构设计决策：
// 1. WxUser（微信小程序用户）为**核心业务实体**，用户量可达千万级，
//    未来必然面临分库分表（按user_id取模或按openid哈希分片）。
// 2. 用户ID被订单（Order）、购物车、收藏、优惠券等多个模块作为外键引用，
//    必须保证全局唯一且不可预测，以防止商业数据被恶意爬取。
// 3. 数据库自增ID在分库分表环境下无法保证全局唯一，且连续可预测的特性存在安全风险。
// 4. 因此主键策略**必须采用分布式ID（雪花算法）**，禁止使用自增。
//    本实体完全遵循此决策，ID由Service层通过SnowflakeIdWorker生成并手动赋值，
//    数据库列定义为BIGINT NOT NULL，无AUTO_INCREMENT属性。
// 5. 统一使用Lombok简化代码，与Product、Order等核心实体保持一致的编码风格。
// 6. 注册时间由@CreationTimestamp自动填充，更新时间由@UpdateTimestamp自动维护。
// =========================================================================

// ========== Jakarta Persistence (JPA) 核心注解 ==========
import jakarta.persistence.*;           // 导入 JPA 所有注解：@Entity, @Table, @Id, @Column 等
// ========== Java 基础类库 ==========
import java.time.LocalDateTime;        // Java 8+ 日期时间 API，无时区问题，用于记录用户注册/登录时间
// ========== Lombok 注解 ==========
import lombok.*;                     // 批量导入 Lombok 注解：@Getter, @Setter, @NoArgsConstructor, @AllArgsConstructor, @Builder 等
// ========== Hibernate 扩展注解 ==========
import org.hibernate.annotations.CreationTimestamp; // Hibernate：自动填充创建时间（INSERT 时）
import org.hibernate.annotations.UpdateTimestamp;   // Hibernate：自动填充更新时间（INSERT/UPDATE 时）

/**
 * 微信小程序用户实体类，映射数据库中的 wx_user 表。
 * <p>
 * 核心职责：
 * <ul>
 *   <li>存储微信用户唯一标识（openid）、会话密钥、基础资料（昵称、头像、手机号、性别）</li>
 *   <li>记录用户状态、最后登录时间、注册时间及信息更新时间</li>
 *   <li>与订单（Order）建立一对多关联（一个用户可有多笔订单）</li>
 * </ul>
 * </p>
 * <p>
 * <strong>主键策略说明：</strong><br>
 * 用户表是电商系统的核心基础表，未来必然面临分库分表需求（按用户ID取模分片或按openid路由）。
 * 因此主键ID不能使用数据库自增，而采用雪花算法分布式ID。
 * ID由Service层通过SnowflakeIdWorker生成并手动赋值，数据库列定义为BIGINT NOT NULL，无自增属性。
 * </p>
 *
 * @author MKappshop
 * @since 1.0
 */
@Getter                             // Lombok：为所有非静态字段生成 getter 方法，编译期生效
@Setter                             // Lombok：为所有非静态字段生成 setter 方法，编译期生效
@NoArgsConstructor                 // Lombok：生成无参构造方法，JPA 规范强制要求（反射创建实体时使用）
@AllArgsConstructor                // Lombok：生成包含所有字段的构造方法，便于快速创建完整用户对象（单元测试/手动构造）
@Builder                           // Lombok：生成建造者模式 API，配合 @AllArgsConstructor 使用，提供链式赋值
@Entity                            // JPA：声明此类为 JPA 实体，将被 Hibernate 管理，映射到数据库表
@Table(name = "wx_user")          // JPA：指定映射的表名为 "wx_user"（非 SQL 关键字，无需转义）
public class WxUser {

    /**
     * 微信小程序用户唯一主键，采用雪花算法分布式ID，由Service层手动赋值。
     * <p>
     * 数据库列定义：id BIGINT NOT NULL PRIMARY KEY
     * 注意：该字段不使用 @GeneratedValue，完全由业务层控制。
     * 自增ID在分库分表场景下无法保证全局唯一且存在安全风险，故放弃。
     * </p>
     */
    @Id                                                     // JPA：标记该字段为实体主键
    @Column(name = "id", nullable = false, updatable = false) // 映射列名 id，非空，不可更新（主键生成后不允许修改）
    private Long id;                                        // 用户ID，由雪花算法生成并手动设置，业务层必须保证全局唯一性

    /**
     * 微信小程序用户唯一标识（openid）。
     * <p>
     * 通过前端 wx.login() 获取 code，后端调用微信接口换取。
     * 每个小程序用户在不同小程序下有独立的 openid，是用户在该小程序的唯一身份标识。
     * 数据库列：openid VARCHAR(64) NOT NULL UNIQUE
     * </p>
     */
    @Column(name = "openid", nullable = false, unique = true, length = 128) // 非空，唯一约束，长度128
    private String openid;                                   // 微信 openid，业务层从微信接口获取并设置

    /**
     * 微信会话密钥（session_key）。
     * <p>
     * 用于解密微信加密数据（如手机号、用户信息等），**严禁返回给前端**。
     * 每次用户登录都会刷新，建议存储在 Redis 中并设置与 JWT 一致的过期时间。
     * 数据库字段仅作为持久化备份，非必须字段。
     * 数据库列：session_key VARCHAR(64) DEFAULT NULL
     * </p>
     */
    @Column(name = "session_key", length = 64)              // 列名 session_key，长度64，可为空
    private String sessionKey;                              // 微信会话密钥，仅后端使用，建议存 Redis

    /**
     * 微信开放平台唯一标识（unionid）。
     * <p>
     * 同一微信开放平台下的多个应用（小程序、公众号、App）共享 unionid，用于识别同一用户。
     * 非必填字段，仅在微信开放平台绑定后才可获取。
     * 数据库列：unionid VARCHAR(64) DEFAULT NULL
     * </p>
     */
    @Column(name = "unionid", length = 128)                  // 列名 unionid，长度128，可为空
    private String unionid;                                 // 微信 unionid，用于跨应用用户识别

    /**
     * 用户昵称。
     * <p>
     * 从微信用户信息中获取，或允许用户自行修改。
     * 数据库列：nickname VARCHAR(50) DEFAULT NULL
     * </p>
     */
    @Column(name = "nickname", length = 50)                 // 列名 nickname，长度50，可为空
    private String nickname;                                // 用户昵称，用于前端展示

    /**
     * 用户头像 URL。
     * <p>
     * 从微信用户信息中获取，通常为 132x132 像素。
     * 数据库列：avatar_url VARCHAR(255) DEFAULT NULL
     * </p>
     */
    @Column(name = "avatar_url", length = 255)              // 列名 avatar_url，长度255，可为空
    private String avatarUrl;                               // 用户头像链接，支持 CDN 加速

    /**
     * 用户手机号。
     * <p>
     * 通过前端 button 组件的 open-type="getPhoneNumber" 获取加密数据，后端使用 session_key 解密获得。
     * 非必填字段，仅在用户授权后才会存储。
     * 数据库列：phone VARCHAR(20) DEFAULT NULL
     * </p>
     */
    @Column(name = "phone", length = 20)                    // 列名 phone，长度20，可为空
    private String phone;                                   // 手机号，用于营销通知、订单提醒等

    /**
     * 用户性别。
     * <p>
     * 从微信用户信息中获取：
     * <ul>
     *   <li>0 - 未知</li>
     *   <li>1 - 男</li>
     *   <li>2 - 女</li>
     * </ul>
     * 数据库列：gender TINYINT DEFAULT 0
     * </p>
     */
    @Column(name = "gender")                                // 列名 gender，可为空，默认0由Java字段初始值保证
    private Integer gender = 0;                             // 性别，默认未知

    /**
     * 账号状态。
     * <p>
     * 1：正常；0：禁用（逻辑删除）。
     * 默认值为 1（正常状态），在字段定义时直接赋值。
     * 数据库列：status TINYINT NOT NULL DEFAULT 1
     * </p>
     */
    @Column(name = "status", nullable = false)              // 列名 status，非空，默认值由数据库或Java赋值保证
    private Integer status = 1;                             // 状态：1-正常，0-禁用，默认正常

    /**
     * 最后登录时间。
     * <p>
     * 每次用户通过 wx.login() 成功登录时，由 Service 层手动更新。
     * 可为空（表示注册后从未登录）。
     * 数据库列：last_login_time DATETIME DEFAULT NULL
     * </p>
     */
    @Column(name = "last_login_time")                       // 列名 last_login_time，可为空
    private LocalDateTime lastLoginTime;                    // 最后登录时间，用于安全审计和活跃度分析

    /**
     * 用户注册时间。
     * <p>
     * 由 Hibernate 的 {@link CreationTimestamp} 注解自动填充（INSERT 时生成数据库当前时间），不可更新。
     * 无需手动赋值，比数据库默认值更可控。
     * 数据库列：create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP
     * </p>
     */
    @CreationTimestamp                                      // Hibernate：插入时自动设为当前数据库时间
    @Column(name = "create_time", nullable = false, updatable = false) // 列名 create_time，非空，不可更新
    private LocalDateTime createTime;                       // 注册时间，由 Hibernate 自动维护

    /**
     * 用户信息最后更新时间。
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
    // 若需要自定义构造逻辑（如首次登录初始化默认值），请手动添加构造方法并保留 @NoArgsConstructor 注解。

    // ========== 业务辅助方法 ==========
    // 本实体暂无需要手写的业务辅助方法。
    // 更新最后登录时间、解密手机号等复杂业务逻辑应在 Service 层实现。
}