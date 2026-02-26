package com.MKappshop.MKappshop.common;
// 声明包路径，该 Result 类位于 common 包下，作为全局通用组件

import io.swagger.v3.oas.annotations.media.Schema;
// 导入 Swagger/OpenAPI 注解，用于生成 API 文档时描述字段含义
import lombok.Data;
// 导入 Lombok 的 @Data 注解，自动生成 getter/setter、equals、hashCode、toString 等

/**
 * 统一 API 响应结果封装类。
 * <p>
 * 所有控制器方法均应返回此对象，保证前端接收格式一致。
 * </p>
 *
 * @param <T> 响应数据的类型
 * @author MKappshop
 * @since 1.0
 */
@Data
// Lombok 注解：为所有字段生成 getter/setter，并生成 toString、equals、hashCode 等
@Schema(description = "统一响应结果")
// Swagger 注解：描述该类在 API 文档中的含义
public class Result<T> {
    // 泛型类 T 表示响应体中携带的数据类型，可以是任意对象、List、Page 等

    /**
     * 状态码：200 表示成功，其他为失败（具体含义见全局常量）
     */
    @Schema(description = "状态码", example = "200")
    // Swagger 注解：描述字段为状态码，示例值为 200
    private Integer code;
    // 响应状态码，通常 200 成功，4xx/5xx 表示各种错误

    /**
     * 提示信息：成功时为 "success"，失败时为具体错误描述
     */
    @Schema(description = "提示信息", example = "success")
    // Swagger 注解：描述字段为提示信息，示例值为 "success"
    private String message;
    // 响应消息，成功时一般为 "success"，失败时为错误详情

    /**
     * 响应数据：泛型，可为分页对象、单个对象或 null
     */
    @Schema(description = "响应数据")
    // Swagger 注解：描述字段为响应数据，具体类型由泛型决定
    private T data;
    // 实际业务数据，可以是对象、数组、分页数据等，成功且无数据时可为 null

    /**
     * 私有构造方法，禁止直接实例化，请使用静态工厂方法。
     *
     * @param code    状态码
     * @param message 提示信息
     * @param data    响应数据
     */
    private Result(Integer code, String message, T data) {
        // 构造器私有化，强制使用者通过静态方法创建 Result 对象，保证语义明确
        this.code = code;
        this.message = message;
        this.data = data;
    }

    /**
     * 成功响应（无数据返回体）。
     *
     * @param <T> 数据类型（此处为 null）
     * @return 成功结果
     */
    public static <T> Result<T> success() {
        // 静态工厂方法：返回一个表示“成功”且不含数据的 Result 对象
        return new Result<>(200, "success", null);
        // 硬编码状态码 200，消息 "success"，数据为 null
    }

    /**
     * 成功响应（带数据）。
     *
     * @param data 响应数据
     * @param <T>  数据类型
     * @return 成功结果
     */
    public static <T> Result<T> success(T data) {
        // 静态工厂方法：返回一个表示“成功”且携带业务数据的 Result 对象
        return new Result<>(200, "success", data);
        // 状态码和消息同上，数据由调用者传入
    }

    /**
     * 失败响应（自定义状态码和消息）。
     *
     * @param code    错误码
     * @param message 错误描述
     * @param <T>     数据类型
     * @return 失败结果
     */
    public static <T> Result<T> error(Integer code, String message) {
        // 静态工厂方法：返回一个表示“失败”的 Result 对象，允许自定义错误码和消息
        return new Result<>(code, message, null);
        // 失败时一般不携带业务数据，data 设为 null
    }

    /**
     * 失败响应（使用默认错误码 500）。
     *
     * @param message 错误描述
     * @param <T>     数据类型
     * @return 失败结果
     */
    public static <T> Result<T> error(String message) {
        // 静态工厂方法：返回一个表示“失败”的 Result 对象，使用默认错误码 500（服务器内部错误）
        return new Result<>(500, message, null);
        // 500 是通用服务器错误码，也可根据业务定义其他默认值
    }
}