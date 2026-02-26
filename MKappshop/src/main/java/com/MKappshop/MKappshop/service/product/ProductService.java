// 包声明：当前类位于 service.product 包下，负责商品模块的业务逻辑处理
package com.MKappshop.MKappshop.service.product;

// ========== 项目内部 DTO 导入 ==========
import com.MKappshop.MKappshop.dto.product.AdminProductDTO;       // 后台商品 DTO（含库存等敏感字段）
import com.MKappshop.MKappshop.dto.product.ProductCreateRequest;  // 创建商品请求 DTO
import com.MKappshop.MKappshop.dto.product.ProductDTO;            // 前台商品 DTO（不含库存，公开接口使用）
import com.MKappshop.MKappshop.dto.product.ProductUpdateRequest;  // 更新商品请求 DTO

// ========== 项目内部实体与异常 ==========
import com.MKappshop.MKappshop.entity.product.Product;            // 商品实体，对应数据库 product 表
import com.MKappshop.MKappshop.exception.BizException;            // 自定义业务异常（预留，本类未直接使用）
import com.MKappshop.MKappshop.exception.ResourceNotFoundException; // 资源不存在异常，查询为空时抛出

// ========== 项目内部组件 ==========
import com.MKappshop.MKappshop.repository.product.ProductRepository; // 商品数据访问层接口
import com.MKappshop.MKappshop.util.SnowflakeIdWorker;              // 雪花算法 ID 生成器，用于分布式主键

// ========== Lombok 注解 ==========
import lombok.RequiredArgsConstructor;           // 生成包含所有 final 字段的构造器（构造器注入）
import lombok.extern.slf4j.Slf4j;               // 自动生成 SLF4J 日志常量 log

// ========== Spring 框架 ==========
import org.springframework.data.domain.Page;     // Spring Data 分页结果封装接口
import org.springframework.data.domain.Pageable; // Spring Data 分页请求参数接口
import org.springframework.stereotype.Service;   // 标记为业务层组件，自动注册为 Spring Bean
import org.springframework.transaction.annotation.Transactional; // 声明式事务注解

/**
 * 商品业务服务类。
 * <p>
 * 职责划分：
 * <ul>
 *   <li><strong>后台管理接口</strong>：商品创建、更新、删除、分页查询、详情查询，返回含库存的 AdminProductDTO。</li>
 *   <li><strong>小程序前台接口</strong>：商品列表分页、商品详情，返回不含库存的 ProductDTO。</li>
 * </ul>
 * 严格遵循分层架构，事务边界定义在 Service 层，Repository 层仅负责数据存取。
 * </p>
 *
 * @author MKappshop
 * @since 1.0
 */
@Slf4j                                          // Lombok：编译期生成 log = LoggerFactory.getLogger(ProductService.class)
@Service                                        // Spring：将该类标记为 Service Bean，纳入 IOC 容器管理
@RequiredArgsConstructor                       // Lombok：生成包含所有 final 字段的构造器（Spring 通过此构造器自动注入依赖）
public class ProductService {

    // --- 依赖注入字段（均为 final，通过构造器注入，保证不可变性和线程安全）---

    /**
     * 商品数据访问层接口，由 Spring 自动注入。
     */
    private final ProductRepository productRepository;

    /**
     * 雪花算法 ID 生成器，用于生成分布式全局唯一的商品主键。
     */
    private final SnowflakeIdWorker snowflakeIdWorker;

    // ========== 后台管理接口 ==========

    /**
     * 创建商品（后台）。
     * <p>
     * 业务逻辑：
     * <ol>
     *   <li>使用雪花算法生成新的商品 ID（分布式环境唯一）。</li>
     *   <li>将请求 DTO 中的字段复制到新创建的实体对象。</li>
     *   <li>保存实体到数据库（{@code createdAt} 和 {@code updateTime} 由 JPA 审计注解自动填充）。</li>
     *   <li>将保存后的实体转换为后台 DTO（含库存）并返回。</li>
     * </ol>
     * </p>
     *
     * @param request 创建商品请求 DTO，包含商品名称、价格、库存、图片、描述等信息
     * @return 后台商品 DTO，包含 ID、名称、价格、库存、图片、描述、创建时间等
     */
    @Transactional                              // 开启事务，保证创建操作的原子性
    public AdminProductDTO createProduct(ProductCreateRequest request) {
        // 1. 创建商品实体实例
        Product product = new Product();
        // 2. 生成雪花算法 ID（Long 类型）并设置为主键
        product.setId(snowflakeIdWorker.nextId());
        // 3. 将请求参数复制到实体（此处为手动赋值，也可使用 BeanUtils.copyProperties）
        product.setName(request.getName());
        product.setPrice(request.getPrice());
        product.setStock(request.getStock());
        product.setImage(request.getImage());
        product.setDescription(request.getDescription());
        // createdAt 和 updateTime 字段由实体类中的 @CreationTimestamp 和 @UpdateTimestamp 注解自动维护，无需手动设置

        // 4. 保存实体到数据库，返回持久化后的对象（包含自动填充的审计字段）
        Product savedProduct = productRepository.save(product);
        // 5. 记录成功日志，便于审计和问题排查
        log.info("商品创建成功 - ID: {}, 名称: {}", savedProduct.getId(), savedProduct.getName());

        // 6. 将实体转换为后台 DTO 并返回
        return convertToAdminDTO(savedProduct);
    }

    /**
     * 更新商品（后台）。
     * <p>
     * 实现部分字段更新：仅当请求 DTO 中的字段值不为 {@code null} 时，才更新对应字段。
     * 审计字段 {@code updateTime} 由 {@code @UpdateTimestamp} 注解自动刷新。
     * </p>
     *
     * @param id      要更新的商品 ID（雪花 ID）
     * @param request 更新商品请求 DTO，包含可选更新的字段
     * @return 更新后的后台商品 DTO（含库存）
     * @throws ResourceNotFoundException 当指定 ID 的商品不存在时抛出
     */
    @Transactional
    public AdminProductDTO updateProduct(Long id, ProductUpdateRequest request) {
        // 1. 根据 ID 查询商品，若不存在则抛出资源不存在异常（全局异常处理器会返回 404）
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("商品不存在，ID: " + id));

        // 2. 部分字段更新：仅当请求 DTO 中对应字段非 null 时执行更新
        if (request.getName() != null) {
            product.setName(request.getName());
        }
        if (request.getPrice() != null) {
            product.setPrice(request.getPrice());
        }
        if (request.getStock() != null) {
            product.setStock(request.getStock());
        }
        if (request.getImage() != null) {
            product.setImage(request.getImage());
        }
        if (request.getDescription() != null) {
            product.setDescription(request.getDescription());
        }
        // updateTime 字段会在实体被更新时由 @UpdateTimestamp 自动设置为当前数据库时间

        // 3. 保存更新后的实体（JPA 会执行 merge 操作）
        Product updatedProduct = productRepository.save(product);
        log.info("商品更新成功 - ID: {}", updatedProduct.getId());

        // 4. 转换为后台 DTO 返回
        return convertToAdminDTO(updatedProduct);
    }

    /**
     * 删除商品（后台）。
     * <p>
     * 执行物理删除，直接从数据库中移除记录。
     * </p>
     *
     * @param id 要删除的商品 ID
     * @throws ResourceNotFoundException 当指定 ID 的商品不存在时抛出
     */
    @Transactional
    public void deleteProduct(Long id) {
        // 1. 检查商品是否存在（existsById 比 findById 更高效，不加载实体）
        if (!productRepository.existsById(id)) {
            throw new ResourceNotFoundException("商品不存在，ID: " + id);
        }
        // 2. 执行删除操作
        productRepository.deleteById(id);
        log.info("商品删除成功 - ID: {}", id);
    }

    /**
     * 获取商品详情（后台）。
     * <p>
     * 返回包含库存信息的完整商品数据。
     * </p>
     *
     * @param id 商品 ID
     * @return 后台商品 DTO（含库存）
     * @throws ResourceNotFoundException 商品不存在时抛出
     */
    @Transactional(readOnly = true)             // 标记为只读事务，提高性能并避免脏读
    public AdminProductDTO getAdminProductById(Long id) {
        // 查询商品，若不存在则抛出异常
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("商品不存在，ID: " + id));
        return convertToAdminDTO(product);
    }

    /**
     * 分页查询商品列表（后台）。
     * <p>
     * 支持按商品名称关键字模糊搜索，若不提供关键字则返回全部分页数据。
     * 返回的 DTO 包含库存信息。
     * </p>
     *
     * @param keyword  商品名称关键字（可选），若为 null 或空字符串则忽略此条件
     * @param pageable 分页参数（页码、每页大小、排序规则）
     * @return 分页的后台商品 DTO 列表
     */
    @Transactional(readOnly = true)
    public Page<AdminProductDTO> listAdminProducts(String keyword, Pageable pageable) {
        Page<Product> productPage;
        // 判断关键字是否为空（包括 null 和全空格）
        if (keyword == null || keyword.trim().isEmpty()) {
            // 无条件分页查询
            productPage = productRepository.findAll(pageable);
        } else {
            // 按商品名称模糊分页查询（Containing 自动添加 % 通配符）
            productPage = productRepository.findByNameContaining(keyword.trim(), pageable);
        }
        // 将 Page<Product> 转换为 Page<AdminProductDTO>（使用 map 方法）
        return productPage.map(this::convertToAdminDTO);
    }

    // ========== 小程序前台接口 ==========

    /**
     * 分页查询商品列表（前台）。
     * <p>
     * 返回的 DTO 中不包含库存、内部审计字段等敏感信息，仅用于商品展示。
     * </p>
     *
     * @param pageable 分页参数
     * @return 分页的前台商品 DTO 列表（不含库存）
     */
    @Transactional(readOnly = true)
    public Page<ProductDTO> list(Pageable pageable) {
        // 直接调用 JpaRepository 的分页查询方法，并将每个 Product 实体转换为 ProductDTO
        return productRepository.findAll(pageable)
                .map(this::convertToDTO);
    }

    /**
     * 获取商品详情（前台）。
     * <p>
     * 返回的 DTO 中不包含库存信息，适用于小程序端公开展示。
     * </p>
     *
     * @param id 商品 ID
     * @return 前台商品 DTO（不含库存）
     * @throws ResourceNotFoundException 商品不存在时抛出
     */
    @Transactional(readOnly = true)
    public ProductDTO getById(Long id) {
        // 根据 ID 查询商品，若不存在抛出异常
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("商品不存在，ID: " + id));
        return convertToDTO(product);
    }

    // ========== 实体转换方法 ==========

    /**
     * 将 Product 实体转换为前台 ProductDTO。
     * <p>
     * 仅复制对外公开的字段：ID、名称、价格、主图、描述、创建时间。
     * 库存、更新时间等敏感或内部字段不暴露。
     * </p>
     *
     * @param product 商品实体
     * @return 前台商品 DTO
     */
    private ProductDTO convertToDTO(Product product) {
        ProductDTO dto = new ProductDTO();
        dto.setId(product.getId());
        dto.setName(product.getName());
        dto.setPrice(product.getPrice());
        dto.setImage(product.getImage());
        dto.setDescription(product.getDescription());
        dto.setCreatedAt(product.getCreatedAt());
        return dto;
    }

    /**
     * 将 Product 实体转换为后台 AdminProductDTO。
     * <p>
     * 复制所有管理端需要的字段，包括库存、更新时间等。
     * </p>
     *
     * @param product 商品实体
     * @return 后台商品 DTO（含库存、更新时间）
     */
    private AdminProductDTO convertToAdminDTO(Product product) {
        AdminProductDTO dto = new AdminProductDTO();
        dto.setId(product.getId());
        dto.setName(product.getName());
        dto.setPrice(product.getPrice());
        dto.setStock(product.getStock());               // 库存字段仅后台可见
        dto.setImage(product.getImage());
        dto.setDescription(product.getDescription());
        dto.setCreatedAt(product.getCreatedAt());
        dto.setUpdateTime(product.getUpdateTime());     // 更新时间仅后台可见
        return dto;
    }
}