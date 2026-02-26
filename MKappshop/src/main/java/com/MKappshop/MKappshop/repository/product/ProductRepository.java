// 包声明：当前接口位于 repository.product 包下，负责商品模块的数据访问层
package com.MKappshop.MKappshop.repository.product;

// ========== Spring Data JPA ==========
import org.springframework.data.domain.Page;               // 导入 Spring Data 分页结果封装接口
import org.springframework.data.domain.Pageable;          // 导入 Spring Data 分页请求参数接口
import org.springframework.data.jpa.repository.JpaRepository; // 导入 JpaRepository 核心接口，提供基础 CRUD 和分页功能
import org.springframework.stereotype.Repository;        // 导入 @Repository 注解，标记为 Spring 数据访问组件

// ========== 项目内部依赖 ==========
import com.MKappshop.MKappshop.entity.product.Product;   // 导入商品实体类，对应数据库 product 表，主键为雪花算法生成的 Long 类型

// ========== Java 基础类库 ==========
import java.util.Optional;                                // 导入 Optional 容器，明确表示查询结果可能为空

/**
 * 商品数据访问层接口。
 * <p>
 * 核心职责：
 * <ul>
 *   <li>提供商品实体的基础 CRUD 操作（继承自 {@link JpaRepository}）</li>
 *   <li>提供分页查询、按名称模糊搜索等自定义查询方法</li>
 *   <li>支持通过雪花算法生成的 Long 类型主键进行查询</li>
 * </ul>
 * </p>
 * <p>
 * <strong>主键策略说明：</strong><br>
 * 商品表主键采用雪花算法分布式ID，由 Service 层手动赋值，数据库列为 BIGINT NOT NULL，
 * 因此本 Repository 的主键类型为 {@link Long}，与实体定义一致。
 * </p>
 *
 * @author MKappshop
 * @since 1.0
 */
@Repository                                                     // Spring 注解：将该接口标记为数据访问 Bean，并启用持久化异常转换
public interface ProductRepository extends JpaRepository<Product, Long> {
    // 继承 JpaRepository<Product, Long>，泛型参数：实体类型 Product，主键类型 Long
    // JpaRepository 已内置以下常用方法（无需手动定义）：
    // - findAll()                          : 查询所有
    // - findAll(Pageable)                 : 分页查询
    // - findById(Long id)                : 根据 ID 查询（返回 Optional）
    // - save(S entity)                   : 保存/更新
    // - deleteById(Long id)             : 根据 ID 删除
    // - count()                          : 统计总数

    /**
     * 根据商品名称模糊查询（不分页）。
     * <p>
     * 示例：findByNameContaining("纯棉") 可匹配商品名包含“纯棉”的所有商品。
     * </p>
     *
     * @param name 商品名称关键字（支持模糊匹配，数据库层面使用 LIKE %:name%）
     * @return 符合条件的商品列表（若没有匹配项，返回空列表，非 null）
     */
    // List<Product> findByNameContaining(String name);  // 暂不启用，按需开放（此处注释掉，未实际使用）

    /**
     * 根据商品名称模糊查询（分页）。
     * <p>
     * 小程序端商品搜索功能使用此方法，支持分页和排序。
     * </p>
     *
     * @param name     商品名称关键字
     * @param pageable 分页参数（页码、每页大小、排序规则）
     * @return 分页的商品列表，包含分页元数据
     */
    // Spring Data JPA 方法命名规则：findBy + 字段名 + Containing，自动生成 LIKE %:name% 查询
    Page<Product> findByNameContaining(String name, Pageable pageable);

    /**
     * 根据商品名称精确查询。
     * <p>
     * 用于后台管理检查商品名是否已存在，避免重复上架相同名称商品。
     * </p>
     *
     * @param name 商品名称（精确匹配）
     * @return Optional 商品实体，若不存在则为 {@link Optional#empty()}
     */
    // 方法命名规则：findBy + 字段名（Name），自动生成 WHERE name = ?1
    Optional<Product> findByName(String name);

    // ========== JpaRepository 已提供的方法（无需重复定义）==========
    // 此处仅保留必要的自定义查询方法，避免重复代码
}