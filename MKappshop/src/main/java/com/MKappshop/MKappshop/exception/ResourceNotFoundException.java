// 包声明：该异常类位于 exception 包下，与 BizException 等自定义异常统一存放
package com.MKappshop.MKappshop.exception;

/**
 * 资源不存在异常。
 * <p>
 * 当根据 ID 查询数据库记录为空时抛出，将被全局异常处理器捕获并返回 404。
 * </p>
 *
 * @author MKappshop
 * @since 1.0
 */
// 定义 ResourceNotFoundException 类，继承 RuntimeException（非受检异常）
public class ResourceNotFoundException extends RuntimeException {

    /**
     * 构造一个仅有错误消息的资源不存在异常。
     *
     * @param message 异常描述信息（例如 "商品不存在"），最终会返回给前端
     */
    public ResourceNotFoundException(String message) {
        // 调用父类 RuntimeException 的构造方法，传入消息字符串
        super(message);
    }

    /**
     * 构造一个包含错误消息和原始异常的资源不存在异常。
     * <p>
     * 用于在捕获底层异常（如 JPA 查询异常）后包装抛出，保留原始异常堆栈，方便调试。
     * </p>
     *
     * @param message 异常描述信息
     * @param cause   原始异常对象（如 EmptyResultDataAccessException、Throwable 等）
     */
    public ResourceNotFoundException(String message, Throwable cause) {
        // 调用父类 RuntimeException 的双参构造方法，同时设置消息和原始异常
        super(message, cause);
    }
}