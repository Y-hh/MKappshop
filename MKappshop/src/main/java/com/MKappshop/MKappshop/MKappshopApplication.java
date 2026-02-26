package com.MKappshop.MKappshop;

import com.MKappshop.MKappshop.dto.product.ProductCreateRequest;
import com.MKappshop.MKappshop.service.product.ProductService;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import java.math.BigDecimal;

@SpringBootApplication
@RequiredArgsConstructor  // 构造器注入
public class MKappshopApplication implements CommandLineRunner {

	private final ProductService productService;  // 注入 Service，不是 Repository

	public static void main(String[] args) {
		SpringApplication.run(MKappshopApplication.class, args);
	}

	@Override
	public void run(String... args) throws Exception {
		// 初始化示例商品（通过 Service 层，自动生成雪花ID）
		ProductCreateRequest request = new ProductCreateRequest();
		request.setName("纯棉T恤");
		request.setPrice(new BigDecimal("99.90"));
		request.setStock(1000);
		request.setImage("https://cdn.shop.com/images/1.jpg");
		request.setDescription("100%纯棉，舒适透气");

		productService.createProduct(request);  // ✅ Service 层已包含雪花ID生成
	}
}