package com.MKappshop.MKappshop.controller.product;
// 声明当前类所在的包，该控制器属于商品模块，供小程序端调用

// ========== Spring MVC 注解 ==========
import io.swagger.v3.oas.annotations.Operation;               // OpenAPI（Swagger）注解，用于描述接口的作用
import io.swagger.v3.oas.annotations.Parameter;               // OpenAPI 注解，描述接口参数的详细信息
import io.swagger.v3.oas.annotations.tags.Tag;               // OpenAPI 注解，为 Controller 添加分组标签
import lombok.RequiredArgsConstructor;                       // Lombok 注解：为所有 final 字段生成构造器（构造器注入）
import lombok.extern.slf4j.Slf4j;                           // Lombok 注解：自动生成 SLF4J 日志常量 log
import org.springframework.data.domain.Page;                 // Spring Data 分页结果封装接口
import org.springframework.data.domain.PageRequest;          // Spring Data 分页请求实现类
import org.springframework.data.domain.Pageable;            // Spring Data 分页请求接口
import org.springframework.data.domain.Sort;                // Spring Data 排序对象
import org.springframework.web.bind.annotation.*;           // Spring MVC Web 注解（@RestController, @RequestMapping, @GetMapping, @RequestParam, @PathVariable 等）

// ========== 项目内部依赖 ==========
import com.MKappshop.MKappshop.common.Result;               // 项目统一响应结果封装类
import com.MKappshop.MKappshop.dto.product.ProductDTO;     // 商品数据传输对象，用于返回给前端（不含密码、库存等敏感字段）
import com.MKappshop.MKappshop.service.product.ProductService; // 商品业务层接口

import jakarta.validation.constraints.Positive;             // Jakarta Bean Validation 注解：校验参数必须为正数
import org.springframework.validation.annotation.Validated; // Spring 注解：在类级别启用方法参数校验

/**
 * 商品查询控制器（小程序端使用）
 * <p>
 * 职责：提供商品列表（分页）、商品详情等公开查询接口。
 * 所有接口均为 GET 请求，无需登录验证（可根据业务需求调整）。
 * </p>
 *
 * @author MKappshop
 * @since 1.0
 */
@Slf4j                                                     // Lombok：生成名为 log 的 Logger 对象，用于打印日志
@Validated                                                 // 开启方法级别参数校验，使 @Positive 等校验注解生效
@RestController                                            // 标记为 REST 控制器，所有方法返回 JSON，等效于 @Controller + @ResponseBody
@RequestMapping("/api/products")                           // 控制器的基础请求路径，所有接口 URL 均以 /api/products 开头
@RequiredArgsConstructor                                   // Lombok：生成一个包含所有 final 字段的构造器，Spring 会通过该构造器自动注入依赖
@Tag(name = "商品查询接口", description = "商品列表、详情等公开查询") // Swagger 文档：描述该控制器
public class ProductController {

    /**
     * 商品业务服务，由 Spring 通过构造器自动注入。
     * final 修饰确保不可变，配合 @RequiredArgsConstructor 实现构造器注入（推荐方式）。
     */
    private final ProductService productService;

    // ========== 商品列表接口 ==========

    /**
     * 分页查询商品列表（按创建时间倒序排列）。
     * <p>
     * 请求示例：GET /api/products?page=0&size=10&sort=price,asc
     * </p>
     *
     * @param page 页码，从 0 开始，默认值 0
     * @param size 每页记录数，默认值 10，最大值 50（防止恶意拉取）
     * @param sort 排序字段和方向，格式：字段,方向（如 price,asc），默认按创建时间倒序
     * @return 统一分页结果，包含商品 DTO 列表、总记录数、总页数等
     */
    @GetMapping                                             // 映射 HTTP GET 请求到该方法，相对路径继承自类上的 @RequestMapping
    @Operation(summary = "分页查询商品列表", description = "支持分页、排序，不包含库存等敏感信息") // Swagger：接口说明
    public Result<Page<ProductDTO>> list(                   // 返回统一分页结果，泛型为 Page<ProductDTO>
                                                            @Parameter(description = "页码（从0开始）")     // Swagger：参数说明
                                                            @RequestParam(name = "page", defaultValue = "0") @Positive(message = "页码必须为正数") int page,
                                                            // @RequestParam：绑定请求参数 page，默认值 "0"，并校验必须为正数

                                                            @Parameter(description = "每页记录数（最大50）")
                                                            @RequestParam(name = "size", defaultValue = "10") @Positive(message = "每页数量必须为正数") int size,
                                                            // 每页大小参数，默认 10，必须为正数

                                                            @Parameter(description = "排序字段和方向，例如：price,desc")
                                                            @RequestParam(name = "sort", defaultValue = "createdAt,desc") String sort) {
        // 排序参数字符串，默认按 createdAt 字段降序

        // 1. 日志记录：接收到的请求参数（便于线上问题排查）
        log.info("请求商品列表 - page: {}, size: {}, sort: {}", page, size, sort);

        // 2. 参数安全校验：限制每页最大数量，避免数据库压力过大
        if (size > 50) {
            size = 50;
            log.warn("每页请求数量超过50，已强制设置为50");
        }

        // 3. 解析排序参数（简单实现，仅支持单字段排序）
        String[] sortParams = sort.split(",");              // 按逗号分割，例如 ["price", "asc"]
        String sortField = sortParams[0];                   // 第一个元素为字段名
        Sort.Direction direction = sortParams.length > 1 && "desc".equalsIgnoreCase(sortParams[1])
                ? Sort.Direction.DESC                       // 若第二个参数存在且为 desc，则降序
                : Sort.Direction.ASC;                      // 否则默认升序

        // 4. 构建 Spring Data 分页请求对象（包含页码、每页大小、排序规则）
        Pageable pageable = PageRequest.of(page, size, Sort.by(direction, sortField));

        // 5. 调用 Service 层执行分页查询，返回 Page<ProductDTO>
        Page<ProductDTO> pageResult = productService.list(pageable);

        // 6. 日志记录：查询结果数量
        log.info("商品列表查询完成 - 总记录数: {}, 当前页记录数: {}",
                pageResult.getTotalElements(), pageResult.getNumberOfElements());

        // 7. 封装为统一响应体返回
        return Result.success(pageResult);
    }

    // ========== 商品详情接口 ==========

    /**
     * 根据商品 ID 查询商品详细信息。
     *
     * @param id 商品主键（必须为正整数）
     * @return 统一响应结果，包含商品 DTO；若商品不存在则抛出异常，由全局处理器统一返回 404
     */
    @GetMapping("/{id}")                                   // 映射 GET 请求，URL 模板：/api/products/{id}
    @Operation(summary = "获取商品详情", description = "根据商品ID返回详细信息（不含库存）")
    public Result<ProductDTO> getById(
            @Parameter(description = "商品ID", required = true) // Swagger：必填参数
            @PathVariable @Positive(message = "商品ID必须为正数") Long id) {
        // @PathVariable：从 URL 路径中绑定 id 变量；@Positive 校验 id > 0

        // 1. 日志记录：请求的商品ID
        log.info("请求商品详情 - id: {}", id);

        // 2. 调用 Service 层获取商品 DTO（若不存在则抛出 ResourceNotFoundException）
        ProductDTO productDTO = productService.getById(id);

        // 3. 日志记录：查询成功
        log.info("商品详情查询成功 - id: {}, 名称: {}", id, productDTO.getName());

        // 4. 封装为统一响应体返回
        return Result.success(productDTO);
    }

    // ========== 以下为后台管理接口（暂不在此控制器实现）==========
    // 商品新增、修改、删除、库存管理请移步 Admin 模块下的 ProductAdminController
}