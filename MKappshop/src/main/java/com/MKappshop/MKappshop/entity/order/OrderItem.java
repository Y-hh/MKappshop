package com.MKappshop.MKappshop.entity.order;

// =========================================================================
// 根据用户要求及架构设计决策：
// 1. OrderItem（订单明细）是**订单（Order）的子表**，与订单构成一对多关联。
// 2. 订单表作为核心业务表，未来必然进行分库分表（按order_id或user_id分片），
//    订单明细表必须与订单表使用相同的分片键，并跟随订单数据分布在同一分片。
// 3. 在分库分表环境下，数据库自增ID无法保证全局唯一，且存在跨分片合并时的ID冲突风险。
// 4. 因此OrderItem的主键策略**必须与Order保持一致，采用分布式ID（雪花算法）**，
//    禁止使用数据库自增。ID由Service层通过SnowflakeIdWorker生成并手动赋值，
//    数据库列定义为BIGINT NOT NULL，无AUTO_INCREMENT属性。
// 5. 本实体完全遵循此决策，确保与订单表在分库分表场景下的数据一致性和扩展性。
// =========================================================================

// ========== Jakarta Persistence (JPA) 核心注解 ==========
import jakarta.persistence.*;           // 导入 JPA 所有注解：@Entity, @Table, @Id, @Column, @ManyToOne, @JoinColumn 等
// ========== Java 基础类库 ==========
import java.math.BigDecimal;           // 高精度十进制数，用于记录下单时的商品单价，防止浮点精度丢失
// ========== 项目内部依赖 ==========
import com.MKappshop.MKappshop.entity.product.Product; // 商品实体，标识该明细对应的商品（历史快照关联）
// ========== Lombok 注解 ==========
import lombok.*;                     // 批量导入 Lombok 注解：@Getter, @Setter, @NoArgsConstructor, @AllArgsConstructor, @Builder 等

/**
 * 订单明细实体类，映射数据库中的 order_item 表。
 * <p>
 * 核心职责：
 * <ul>
 *   <li>记录订单中每个商品的购买数量、下单时的单价（价格快照）</li>
 *   <li>与订单（Order）建立多对一关联（一个订单包含多个明细）</li>
 *   <li>与商品（Product）建立多对一关联（一个商品可出现在多个订单明细中）</li>
 * </ul>
 * </p>
 * <p>
 * <strong>主键策略说明：</strong><br>
 * 订单明细表是订单表的子表，在分库分表场景下必须与订单表使用相同的分布键，
 * 并存储在同一分片。因此主键ID不能使用数据库自增，而采用与Order一致的雪花算法分布式ID。
 * ID由Service层通过SnowflakeIdWorker生成并手动赋值，数据库列定义为BIGINT NOT NULL，无自增属性。
 * </p>
 *
 * @author MKappshop
 * @since 1.0
 */
@Getter                             // Lombok：为所有非静态字段生成 getter 方法，编译期生效
@Setter                             // Lombok：为所有非静态字段生成 setter 方法，编译期生效
@NoArgsConstructor                 // Lombok：生成无参构造方法，JPA 规范强制要求（反射创建实体时使用）
@AllArgsConstructor                // Lombok：生成包含所有字段的构造方法，便于快速创建完整明细对象（单元测试/手动构造）
@Builder                           // Lombok：生成建造者模式 API，配合 @AllArgsConstructor 使用，提供链式赋值
@Entity                            // JPA：声明此类为 JPA 实体，将被 Hibernate 管理，映射到数据库表
@Table(name = "order_item")       // JPA：指定映射的表名为 "order_item"（非 SQL 关键字，无需转义）
public class OrderItem {

    /**
     * 订单明细唯一主键，采用雪花算法分布式ID，由Service层手动赋值。
     * <p>
     * 数据库列定义：id BIGINT NOT NULL PRIMARY KEY
     * 注意：该字段不使用 @GeneratedValue，完全由业务层控制。
     * 作为订单子表，必须与订单表保持一致的ID策略，以支持未来分库分表。
     * </p>
     */
    @Id                                                     // JPA：标记该字段为实体主键
    @Column(name = "id", nullable = false, updatable = false) // 映射列名 id，非空，不可更新（主键生成后不允许修改）
    private Long id;                                        // 明细ID，由雪花算法生成并手动设置，业务层必须保证全局唯一性

    /**
     * 所属订单（多对一关联）。
     * <p>
     * 多个明细可属于同一个订单，使用懒加载避免不必要的联表查询。
     * 外键字段：order_id，关联 order 表的 id 列。
     * </p>
     */
    @ManyToOne(fetch = FetchType.LAZY)                      // JPA：多对一关系，懒加载（查询明细时不立即加载订单）
    @JoinColumn(name = "order_id", nullable = false)        // 外键列名 order_id，非空，引用 order.id
    private Order order;                                    // 所属订单，Lombok 生成 getOrder()/setOrder()

    /**
     * 关联商品（多对一关联）。
     * <p>
     * 多个明细可对应同一个商品（历史订单快照），使用懒加载。
     * 外键字段：product_id，关联 product 表的 id 列。
     * </p>
     */
    @ManyToOne(fetch = FetchType.LAZY)                      // JPA：多对一关系，懒加载
    @JoinColumn(name = "product_id", nullable = false)      // 外键列名 product_id，非空，引用 product.id
    private Product product;                                // 关联的商品，Lombok 生成 getProduct()/setProduct()

    /**
     * 购买数量。
     * <p>
     * 数据库列：quantity INT NOT NULL
     * 业务层必须保证为正整数。
     * </p>
     */
    @Column(name = "quantity", nullable = false)            // 映射列名 quantity，非空
    private Integer quantity;                               // 购买数量，由前端传入，业务层校验

    /**
     * 下单时的商品单价（价格快照）。
     * <p>
     * 数据库列：price DECIMAL(10,2) NOT NULL
     * 精度：共10位，小数2位，单位：元
     * 使用快照而非实时关联商品价格，避免历史订单因商品价格变动而出现金额不一致问题。
     * </p>
     */
    @Column(name = "price", nullable = false, precision = 10, scale = 2) // DECIMAL(10,2)，非空
    private BigDecimal price;                               // 单价快照，下单时从商品表读取并固化，使用 BigDecimal 保证货币精度

    // ========== Lombok 已自动生成的方法 ==========
    // 以下方法均在编译期由 Lombok 自动生成，无需手写，此处仅为说明：
    // - Getter/Setter 方法（所有字段）
    // - 无参构造方法（@NoArgsConstructor）
    // - 全参构造方法（@AllArgsConstructor）
    // - 建造者模式（@Builder）
    // 这些方法使实体类保持极简，同时满足 JPA 规范和开发便利性。
    // 若需要自定义构造逻辑，请手动添加构造方法并保留 @NoArgsConstructor 注解。

    // ========== 业务辅助方法 ==========
    // 双向关联的维护由 Order 实体中的 addOrderItem()/removeOrderItem() 完成，
    // 本实体只需提供标准的 setOrder() 方法（已由 Lombok 生成），无需额外逻辑。
    // 注意：禁止直接调用 order.getOrderItems().add() 或 item.setOrder(null) 而不通过辅助方法，
    // 否则会导致内存状态与数据库外键关系不一致。
}