package com.MKappshop.MKappshop.controller.amdin;

// =========================================================================
// 后台商品管理控制器，提供管理员对商品的增删改查操作。
// 接口路径前缀：/admin/products
// 所有接口均需携带管理员 JWT Token（由 AdminAuthInterceptor 统一拦截校验）
// 使用 @CurrentAdmin 获取当前登录管理员，用于操作日志记录及权限校验
// =========================================================================

import com.MKappshop.MKappshop.annotation.CurrentAdmin;                // 注入当前管理员
import com.MKappshop.MKappshop.common.Result;                          // 统一响应体
import com.MKappshop.MKappshop.dto.product.AdminProductDTO;           // 后台商品响应DTO（含库存）
import com.MKappshop.MKappshop.dto.product.ProductCreateRequest;      // 创建商品请求DTO
import com.MKappshop.MKappshop.dto.product.ProductUpdateRequest;      // 更新商品请求DTO
import com.MKappshop.MKappshop.dto.product.ProductDTO;                // 前台商品DTO（无库存）
import com.MKappshop.MKappshop.entity.admin.Admin;                    // 管理员实体
import com.MKappshop.MKappshop.service.product.ProductService;        // 商品业务服务
import io.swagger.v3.oas.annotations.Operation;                       // API文档：接口说明
import io.swagger.v3.oas.annotations.tags.Tag;                       // API文档：控制器标签
import jakarta.validation.Valid;                                     // 启用请求参数校验
import lombok.RequiredArgsConstructor;                               // 构造器注入
import lombok.extern.slf4j.Slf4j;                                   // 日志对象
import org.springframework.data.domain.Page;                         // 分页结果
import org.springframework.data.domain.Pageable;                     // 分页参数
import org.springframework.data.domain.Sort;                         // 排序
import org.springframework.data.web.PageableDefault;                 // 分页参数默认值
import org.springframework.web.bind.annotation.*;



@Slf4j
@RestController
@RequestMapping("/admin/products")
@RequiredArgsConstructor
@Tag(name = "后台商品管理接口", description = "商品创建、更新、删除、查询（含库存）")
public class ProductAdminController {

    private final ProductService productService;

    // ========== 创建商品 ==========

    @PostMapping
    @Operation(summary = "创建商品", description = "管理员创建新商品，返回完整商品信息（含库存）")
    public Result<AdminProductDTO> createProduct(
            @RequestBody @Valid ProductCreateRequest createRequest,
            @CurrentAdmin Admin admin) {                          // 注入当前操作的管理员（用于日志）

        log.info("管理员创建商品 - 管理员ID: {}, 用户名: {}, 商品名称: {}",
                admin.getId(), admin.getUsername(), createRequest.getName());

        AdminProductDTO product = productService.createProduct(createRequest);

        log.info("商品创建成功 - 商品ID: {}, 商品名称: {}", product.getId(), product.getName());
        return Result.success(product);
    }

    // ========== 更新商品 ==========

    @PutMapping("/{id}")
    @Operation(summary = "更新商品", description = "根据ID更新商品信息（支持部分字段更新）")
    public Result<AdminProductDTO> updateProduct(
            @PathVariable Long id,
            @RequestBody @Valid ProductUpdateRequest updateRequest,
            @CurrentAdmin Admin admin) {

        log.info("管理员更新商品 - 管理员ID: {}, 用户名: {}, 商品ID: {}",
                admin.getId(), admin.getUsername(), id);

        AdminProductDTO product = productService.updateProduct(id, updateRequest);

        log.info("商品更新成功 - 商品ID: {}, 商品名称: {}", product.getId(), product.getName());
        return Result.success(product);
    }

    // ========== 删除商品 ==========

    @DeleteMapping("/{id}")
    @Operation(summary = "删除商品", description = "根据ID删除商品（物理删除，谨慎操作）")
    public Result<Void> deleteProduct(
            @PathVariable Long id,
            @CurrentAdmin Admin admin) {

        log.info("管理员删除商品 - 管理员ID: {}, 用户名: {}, 商品ID: {}",
                admin.getId(), admin.getUsername(), id);

        productService.deleteProduct(id);

        log.info("商品删除成功 - 商品ID: {}", id);
        return Result.success();
    }

    // ========== 获取商品详情（后台）==========

    @GetMapping("/{id}")
    @Operation(summary = "获取商品详情（后台）", description = "返回商品完整信息，包含库存等敏感字段")
    public Result<AdminProductDTO> getProductDetail(@PathVariable Long id) {

        log.info("后台查询商品详情 - 商品ID: {}", id);

        AdminProductDTO product = productService.getAdminProductById(id);

        return Result.success(product);
    }

    // ========== 分页查询商品列表（后台）==========

    @GetMapping
    @Operation(summary = "分页查询商品列表（后台）", description = "支持分页、排序、按名称模糊搜索，返回包含库存的完整信息")
    public Result<Page<AdminProductDTO>> listProducts(
            @RequestParam(required = false) String keyword,     // 商品名称关键字（可选）
            @PageableDefault(size = 10, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {

        log.info("后台分页查询商品 - 关键字: {}, 页码: {}, 每页大小: {}",
                keyword, pageable.getPageNumber(), pageable.getPageSize());

        Page<AdminProductDTO> page = productService.listAdminProducts(keyword, pageable);

        log.info("后台查询商品完成 - 总记录数: {}", page.getTotalElements());
        return Result.success(page);
    }
}