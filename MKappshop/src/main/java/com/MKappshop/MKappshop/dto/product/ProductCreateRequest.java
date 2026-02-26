package com.MKappshop.MKappshop.dto.product;

import jakarta.validation.constraints.*;
import lombok.Data;
import java.math.BigDecimal;

/**
 * 创建商品请求DTO。
 * <p>
 * 用于后台管理员创建新商品时接收请求参数。
 * 所有字段均包含校验注解，确保数据合法性。
 * </p>
 *
 * @author MKappshop
 * @since 1.0
 */
@Data
public class ProductCreateRequest {

    @NotBlank(message = "商品名称不能为空")
    @Size(max = 100, message = "商品名称长度不能超过100")
    private String name;

    @NotNull(message = "商品价格不能为空")
    @DecimalMin(value = "0.01", message = "价格必须大于0")
    @DecimalMax(value = "999999.99", message = "价格不能超过999999.99")
    private BigDecimal price;

    @NotNull(message = "商品库存不能为空")
    @Min(value = 0, message = "库存不能为负数")
    private Integer stock;

    @Size(max = 255, message = "图片URL长度不能超过255")
    private String image;

    @Size(max = 500, message = "描述长度不能超过500")
    private String description;
}