package com.MKappshop.MKappshop.entity.product;

// =========================================================================
// 根据用户要求及架构设计决策：
// 1. Product（商品）为**核心业务实体**，主键采用雪花算法分布式ID，禁止自增。
// 2. 创建时间由 @CreationTimestamp 自动填充，更新时间由 @UpdateTimestamp 自动维护。
// 3. 统一使用 Lombok 简化代码，与 Order、WxUser 等实体保持一致的编码风格。
// 4. 所有字段均已添加完整 Javadoc 及行内注释，符合项目规范。
// =========================================================================

// ========== Jakarta Persistence (JPA) 核心注解 ==========
import jakarta.persistence.*;           // @Entity, @Table, @Id, @Column, @PrePersist 等
// ========== Java 基础类库 ==========
import java.math.BigDecimal;           // 高精度货币金额
import java.time.LocalDateTime;        // 本地日期时间（Java 8+）
// ========== Lombok 注解 ==========
import lombok.*;                     // @Getter, @Setter, @NoArgsConstructor, @AllArgsConstructor, @Builder
// ========== Hibernate 扩展注解 ==========
import org.hibernate.annotations.CreationTimestamp; // 自动填充创建时间（INSERT）
import org.hibernate.annotations.UpdateTimestamp;   // 自动填充更新时间（INSERT/UPDATE）

/**
 * 商品实体类，映射数据库中的 product 表。
 * <p>
 * 核心职责：
 * <ul>
 *   <li>存储商品核心信息：名称、价格、库存、图片、描述等</li>
 *   <li>作为订单明细（OrderItem）的关联对象，提供历史订单快照所需的商品信息</li>
 *   <li>为小程序前端提供商品展示、搜索、筛选等基础数据</li>
 *   <li>为后台管理提供完整的商品维护能力（含库存、审计字段）</li>
 * </ul>
 * </p>
 * <p>
 * <strong>主键策略说明：</strong><br>
 * 商品表是电商系统的核心基础表，未来必然面临分库分表需求（按商品ID取模分片）。
 * 因此主键ID不能使用数据库自增，而采用雪花算法分布式ID。
 * ID由Service层通过SnowflakeIdWorker生成并手动赋值，数据库列定义为BIGINT NOT NULL，无自增属性。
 * </p>
 * <p>
 * <strong>审计字段说明：</strong><br>
 * - {@code createdAt}：由 Hibernate 在 INSERT 时自动生成，不可更新。<br>
 * - {@code updateTime}：由 Hibernate 在 INSERT/UPDATE 时自动刷新，记录最后修改时间。
 * </p>
 *
 * @author MKappshop
 * @since 1.0
 */
@Getter                                 // Lombok：为所有非静态字段生成 getter 方法（编译期）
@Setter                                 // Lombok：为所有非静态字段生成 setter 方法（编译期）
@NoArgsConstructor                     // Lombok：生成无参构造方法，JPA 强制要求
@AllArgsConstructor                    // Lombok：生成包含所有字段的构造方法，便于快速构建对象
@Builder                               // Lombok：生成建造者模式，配合全参构造使用
@Entity                                // JPA：声明该类为 JPA 实体，与数据库表映射
@Table(name = "product")              // JPA：指定映射的表名为 "product"（非 SQL 关键字，无需转义）
public class Product {

        /**
         * 商品唯一主键，采用雪花算法分布式ID，由Service层手动赋值。
         * <p>
         * 数据库列定义：id BIGINT NOT NULL PRIMARY KEY
         * 注意：该字段不使用 {@code @GeneratedValue}，完全由业务层控制。
         * </p>
         */
        @Id                                                             // JPA：标记为主键
        @Column(name = "id", nullable = false, updatable = false)      // 列名 id，非空，不可更新
        private Long id;                                                // 雪花算法生成的全局唯一ID

        /**
         * 商品名称。
         * <p>
         * 数据库列：name VARCHAR(100) NOT NULL
         * </p>
         */
        @Column(name = "name", nullable = false, length = 100)         // 列名 name，非空，长度100
        private String name;                                            // 商品展示名称

        /**
         * 商品销售价格（保留两位小数）。
         * <p>
         * 数据库列：price DECIMAL(10,2) NOT NULL
         * 精度：共10位，小数2位，单位：元
         * </p>
         */
        @Column(name = "price", nullable = false, precision = 10, scale = 2) // DECIMAL(10,2)，非空
        private BigDecimal price;                                        // 当前售价，BigDecimal 保证精度

        /**
         * 商品库存数量。
         * <p>
         * 数据库列：stock INT NOT NULL
         * 业务层必须保证库存 ≥ 0，并做好并发控制。
         * </p>
         */
        @Column(name = "stock", nullable = false)                       // 列名 stock，非空
        private Integer stock;                                           // 当前可用库存

        /**
         * 商品主图URL地址。
         * <p>
         * 数据库列：image VARCHAR(255) DEFAULT NULL
         * </p>
         */
        @Column(name = "image", length = 255)                           // 列名 image，长度255，可为空
        private String image;                                            // 商品主图链接（CDN加速）

        /**
         * 商品详细描述（富文本或纯文本）。
         * <p>
         * 数据库列：description VARCHAR(500) DEFAULT NULL
         * </p>
         */
        @Column(name = "description", length = 500)                     // 列名 description，长度500，可为空
        private String description;                                      // 商品详情描述

        /**
         * 商品创建时间。
         * <p>
         * 由 Hibernate 的 {@link CreationTimestamp} 注解自动填充（INSERT 时生成数据库当前时间），不可更新。
         * 数据库列：created_at DATETIME NOT NULL
         * </p>
         */
        @CreationTimestamp                                              // Hibernate：插入时自动设为当前数据库时间
        @Column(name = "created_at", nullable = false, updatable = false) // 列名 created_at，非空，不可更新
        private LocalDateTime createdAt;                                // 创建时间，由框架自动维护

        /**
         * 商品信息最后更新时间。
         * <p>
         * 由 Hibernate 的 {@link UpdateTimestamp} 注解自动填充（INSERT/UPDATE 时刷新为数据库当前时间）。
         * 用于乐观锁、审计追踪及后台列表排序。
         * 数据库列：update_time DATETIME
         * </p>
         */
        @UpdateTimestamp                                                // Hibernate：插入/更新时自动刷新
        @Column(name = "update_time")                                   // 列名 update_time，可为空（但会自动填充）
        private LocalDateTime updateTime;                               // 最后更新时间，由框架自动维护

        // ========== Lombok 已自动生成的方法 ==========
        // 包括：无参构造、全参构造、Builder、所有字段的 Getter/Setter
        // 无需手写，保持代码简洁。

        // ========== 业务辅助方法（可在此扩展）==========
        // 示例：安全扣减库存（需在 Service 层配合事务与锁）
        // public void reduceStock(Integer quantity) { ... }
}