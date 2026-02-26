// 包声明：当前接口位于 repository.admin 包下，负责管理员模块的数据访问层
package com.MKappshop.MKappshop.repository.admin;

// ========== Spring Data JPA ==========
// 导入 Spring Data JPA 的核心接口 JpaRepository，它提供了基础的 CRUD、分页、排序等方法
import org.springframework.data.jpa.repository.JpaRepository;
// 导入 Spring 的 @Repository 注解，用于将该接口标记为数据访问组件，并启用持久化异常转换
import org.springframework.stereotype.Repository;

// ========== 项目内部依赖 ==========
// 导入管理员实体类 Admin，该实体对应数据库中的 admin 表
import com.MKappshop.MKappshop.entity.admin.Admin;

// ========== Java 基础类库 ==========
// 导入 Optional 容器类，用于明确表示查询结果可能为空，避免直接返回 null
import java.util.Optional;

/**
 * 管理员数据访问层接口。
 * <p>
 * 核心职责：
 * <ul>
 *   <li>提供管理员实体的基础 CRUD 操作（继承自 {@link JpaRepository}）</li>
 *   <li>提供根据用户名查询管理员的接口（登录认证核心方法）</li>
 *   <li>支持分页、排序等高级查询特性</li>
 * </ul>
 * </p>
 * <p>
 * <strong>设计说明：</strong><br>
 * 本接口仅定义与管理员实体相关的数据库操作，不包含任何业务逻辑。
 * 所有业务逻辑应在 Service 层实现，Repository 层保持纯粹的数据访问职责。
 * </p>
 *
 * @author MKappshop
 * @since 1.0
 */
// @Repository 注解：将该接口标记为 Spring 管理的 Bean，并允许将持久化技术相关的异常转换为 Spring 的 DataAccessException
@Repository
// 接口定义：AdminRepository 继承 JpaRepository<Admin, Long>
// 泛型参数：第一个是实体类型（Admin），第二个是主键类型（Long）
public interface AdminRepository extends JpaRepository<Admin, Long> {

    /**
     * 根据用户名查询管理员。
     * <p>
     * 由于 admin 表的 username 字段具有唯一约束，该方法最多返回一个结果。
     * 使用 {@link Optional} 明确表示查询结果可能为空，避免调用者处理 null 值。
     * </p>
     * <p>
     * <strong>使用场景：</strong><br>
     * 管理员登录认证时，通过用户名获取管理员实体，用于密码校验、状态验证等。
     * </p>
     *
     * @param username 管理员登录账号（非空，唯一）
     * @return Optional&lt;Admin&gt; 包含管理员实体的容器对象，若不存在则为 {@link Optional#empty()}
     */
    // 方法定义：Spring Data JPA 会根据方法名自动解析并生成查询实现
    // 遵循命名规范：findBy + 字段名（Username），实现等价于 "SELECT a FROM Admin a WHERE a.username = ?1"
    Optional<Admin> findByUsername(String username);

    // ========== 后续可根据业务需求扩展其他查询方法 ==========
    // 示例：根据角色查询管理员列表（分页）
    // Page<Admin> findByRole(String role, Pageable pageable);
    //
    // 示例：查询状态为正常的所有管理员
    // List<Admin> findByStatus(Integer status);
}