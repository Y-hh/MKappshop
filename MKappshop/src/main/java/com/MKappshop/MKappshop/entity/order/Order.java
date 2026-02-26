package com.MKappshop.MKappshop.entity.order;

// =========================================================================
// 根据用户要求及架构设计决策：
// 1. Order（订单）为**核心业务实体**，未来数据量巨大（千万级~亿级），
//    必然面临分库分表（按order_id或user_id分片）。
// 2. 数据库自增ID在分库分表环境下无法保证全局唯一，且存在安全性风险
//    （可被遍历爬取订单数据）。
// 3. 因此主键策略**必须采用分布式ID（雪花算法）**，禁止使用自增。
// 4. 本实体完全遵循此决策，ID由Service层通过SnowflakeIdWorker手动赋值，
//    数据库列定义为BIGINT NOT NULL，无AUTO_INCREMENT属性。
// 5. 保持与Product、WxUser等核心实体一致的ID策略，为未来分布式扩展奠定基础。
// =========================================================================

// ========== Jakarta Persistence (JPA) 核心注解 ==========
import jakarta.persistence.*;           // 导入 JPA 所有注解：@Entity, @Table, @Id, @Column, @ManyToOne, @OneToMany, @JoinColumn, FetchType, CascadeType 等
// ========== Java 基础类库 ==========
import java.math.BigDecimal;           // 高精度十进制数，用于订单金额，避免浮点精度丢失
import java.time.LocalDateTime;        // Java 8+ 日期时间 API，无时区问题，推荐用于记录时间戳
import java.util.ArrayList;           // 动态数组实现类，用于初始化订单明细集合
import java.util.List;               // 集合接口，表示订单明细的列表

// ========== 项目内部依赖 ==========
import com.MKappshop.MKappshop.entity.wx.WxUser;      // 微信小程序用户实体，订单所属用户

// ========== Lombok 注解 ==========
import lombok.*;                     // 批量导入 Lombok 注解：@Getter, @Setter, @NoArgsConstructor, @AllArgsConstructor, @Builder 等
// ========== Hibernate 扩展注解 ==========
import org.hibernate.annotations.CreationTimestamp; // Hibernate 注解：自动填充创建时间（INSERT 时）

/**
 * 订单实体类，映射数据库中的 order 表。
 * <p>
 * 核心职责：
 * <ul>
 *   <li>记录订单核心信息：订单号、总金额、状态、创建时间</li>
 *   <li>与微信用户（WxUser）建立多对一关联（一个用户可有多笔订单）</li>
 *   <li>与订单明细（OrderItem）建立一对多关联，并级联维护（保存订单自动保存明细）</li>
 *   <li>提供双向关联辅助方法，保证对象关系一致性</li>
 * </ul>
 * </p>
 * <p>
 * <strong>主键策略说明：</strong><br>
 * 订单表是电商系统数据量最大的核心表，未来必然需要分库分表。
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
@AllArgsConstructor                // Lombok：生成包含所有字段的构造方法，便于快速创建完整订单对象（单元测试/手动构造）
@Builder                           // Lombok：生成建造者模式 API，配合 @AllArgsConstructor 使用，提供链式赋值
@Entity                            // JPA：声明此类为 JPA 实体，将被 Hibernate 管理，映射到数据库表
@Table(name = "`order`")          // JPA：指定映射的表名为 "order"（order 是 SQL 关键字，使用反引号转义；生产环境建议将表名改为 orders 彻底避免转义问题）
public class Order {

    /**
     * 订单唯一主键，采用雪花算法分布式ID，由Service层手动赋值。
     * <p>
     * 数据库列定义：id BIGINT NOT NULL PRIMARY KEY
     * 注意：该字段不使用 @GeneratedValue，完全由业务层控制。
     * 自增ID在分库分表场景下无法保证全局唯一，故放弃。
     * </p>
     */
    @Id                                                     // JPA：标记该字段为实体主键
    @Column(name = "id", nullable = false, updatable = false) // 映射列名 id，非空，不可更新（主键生成后不允许修改）
    private Long id;                                        // 订单ID，由雪花算法生成并手动设置，业务层必须保证唯一性

    /**
     * 订单编号，全局唯一，业务查询标识。
     * <p>
     * 生成规则：通常由 Service 层生成，如“时间戳 + 随机数/用户ID”等算法保证唯一性。
     * 该字段用于前端展示、客服查询、支付接口调用，不暴露主键ID。
     * 数据库列：order_no VARCHAR(32) UNIQUE NOT NULL
     * </p>
     */
    @Column(name = "order_no", unique = true, nullable = false, length = 32) // 唯一约束，非空，长度32
    private String orderNo;                                 // 业务订单号，全局唯一，由Service层生成

    /**
     * 下单用户（多对一关联）。
     * <p>
     * 多个订单可属于同一个微信用户，使用懒加载避免不必要的联表查询。
     * 外键字段：user_id，关联 wx_user 表的 id 列。
     * </p>
     */
    @ManyToOne(fetch = FetchType.LAZY)                      // JPA：多对一关系，懒加载（查询订单时不立即加载用户）
    @JoinColumn(name = "user_id", nullable = false)        // 外键列名 user_id，非空，引用 wx_user.id
    private WxUser wxUser;                                  // 订单所属用户，Lombok 生成 getWxUser()/setWxUser()

    /**
     * 订单总金额（含商品总价、运费等）。
     * <p>
     * 数据库列：total_amount DECIMAL(10,2) NOT NULL
     * 精度：共10位，小数2位，单位：元
     * </p>
     */
    @Column(name = "total_amount", nullable = false, precision = 10, scale = 2) // DECIMAL(10,2)，非空
    private BigDecimal totalAmount;                         // 总金额，使用 BigDecimal 保证货币精度

    /**
     * 订单状态（整型表示）。
     * <p>
     * 业务含义（建议使用常量类或枚举管理，避免魔法值）：
     * <ul>
     *   <li>0 - 待付款</li>
     *   <li>1 - 已付款</li>
     *   <li>2 - 已发货</li>
     *   <li>3 - 已完成</li>
     *   <li>4 - 已取消</li>
     * </ul>
     * 数据库列：status INT NOT NULL
     * </p>
     */
    @Column(name = "status", nullable = false)              // 映射列名 status，非空
    private Integer status;                                 // 订单状态码，由业务层维护

    /**
     * 订单创建时间。
     * <p>
     * 由 Hibernate 的 {@link CreationTimestamp} 注解自动填充（INSERT 时生成数据库当前时间），不可更新。
     * 无需手动赋值，比数据库默认值更可控。
     * 数据库列：created_at DATETIME NOT NULL
     * </p>
     */
    @CreationTimestamp                                      // Hibernate：插入时自动设为当前数据库时间
    @Column(name = "created_at", nullable = false, updatable = false) // 列名 created_at，非空，不可更新
    private LocalDateTime createdAt;                        // 创建时间，由 Hibernate 自动维护，业务层无需干预

    /**
     * 订单包含的商品明细列表。
     * <p>
     * 一对多关联，由 OrderItem 实体中的 order 字段维护外键（mappedBy）。
     * 级联策略：
     * <ul>
     *   <li>CascadeType.ALL：对订单的增删改操作会同步至明细表（保存订单自动保存明细）</li>
     *   <li>orphanRemoval = true：从集合中移除的明细项会被自动删除（脱离关系即删除）</li>
     * </ul>
     * 集合初始化为空 ArrayList，避免 null 指针异常。
     * </p>
     */
    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true) // 一对多，由对方维护关系，全部级联，孤儿删除
    private List<OrderItem> orderItems = new ArrayList<>(); // 订单明细列表，初始为空集合，禁止直接赋值为 null

    // ========== 业务辅助方法（手动维护双向关联）==========
    // 这些方法替代直接对集合的操作，确保内存模型与数据库外键关系一致。
    // 使用这些方法后，只需保存订单，明细会自动级联保存/更新/删除。

    /**
     * 添加订单明细项，并维护双向关联关系。
     * <p>
     * 此方法确保：
     * <ol>
     *   <li>将明细项加入本订单的 orderItems 集合</li>
     *   <li>将明细项的 order 属性设置为当前订单对象（维护反向指针）</li>
     * </ol>
     * 禁止直接调用 orderItems.add()，否则会造成关联不一致（明细不知道所属订单）。
     * </p>
     *
     * @param item 待添加的订单明细项（不可为 null）
     */
    public void addOrderItem(OrderItem item) {
        orderItems.add(item);       // 将明细添加到当前订单的明细集合中
        item.setOrder(this);        // 设置明细的订单属性为当前订单对象（建立双向关联）
    }

    /**
     * 移除订单明细项，并维护双向关联关系。
     * <p>
     * 此方法确保：
     * <ol>
     *   <li>将明细项从本订单的 orderItems 集合中移除</li>
     *   <li>将明细项的 order 属性置为 null（解除关联）</li>
     * </ol>
     * 配合 orphanRemoval = true，被移除的明细项将从数据库自动删除。
     * </p>
     *
     * @param item 待移除的订单明细项
     */
    public void removeOrderItem(OrderItem item) {
        orderItems.remove(item);    // 从当前订单的明细集合中移除该明细
        item.setOrder(null);        // 断开明细对订单的引用（成为孤儿，将被自动删除）
    }

}