package com.MKappshop.MKappshop.dto.product;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.experimental.SuperBuilder;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "前台商品信息（无库存）")  // 父类描述
public class ProductDTO {

    @Schema(description = "商品ID", example = "1798523456789012480")
    private Long id;

    @Schema(description = "商品名称", example = "纯棉T恤")
    private String name;

    @Schema(description = "商品价格（元）", example = "99.90")
    private BigDecimal price;

    @Schema(description = "商品主图URL", example = "https://cdn.shop.com/images/1.jpg")
    private String image;

    @Schema(description = "商品描述", example = "100%纯棉，舒适透气")
    private String description;

    @Schema(description = "上架时间", example = "2026-02-01 10:00:00")
    private LocalDateTime createdAt;
}