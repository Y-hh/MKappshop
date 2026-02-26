package com.MKappshop.MKappshop.dto.product;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.experimental.SuperBuilder;
import java.time.LocalDateTime;

@Data
@EqualsAndHashCode(callSuper = true)
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "后台商品信息（含库存）")  // 子类描述（新增）
public class AdminProductDTO extends ProductDTO {

    @Schema(description = "当前库存数量", example = "1000")
    private Integer stock;

    @Schema(description = "最后更新时间", example = "2026-02-13 15:30:00")
    private LocalDateTime updateTime;
}