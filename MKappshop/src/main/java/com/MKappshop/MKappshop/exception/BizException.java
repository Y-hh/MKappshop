// 包声明：该异常类位于 exception 包下，集中存放自定义异常类
package com.MKappshop.MKappshop.exception;

// 导入全局异常处理器，用于在文档中说明该类会被谁捕获（仅用于 Javadoc 关联，非必需）
import com.MKappshop.MKappshop.handler.GlobalExceptionHandler;

/**
 * 业务逻辑异常基类。
 * <p>
 * 用于 Service 层抛出可预知的业务错误，由 {@link GlobalExceptionHandler} 统一捕获并转换为 {@link Result} 错误响应。
 * </p>
 *
 * @author MKappshop
 * @since 1.0
 */
// 定义 BizException 类，继承 RuntimeException，表示它是非受检异常（Unchecked Exception）
public class BizException extends RuntimeException {

    /**
     * 构造一个仅有错误消息的业务异常。
     *
     * @param message 异常描述信息，最终会返回给前端
     */
    public BizException(String message) {
        // 调用父类 RuntimeException 的构造方法，传入消息字符串
        super(message);
    }

    /**
     * 构造一个包含错误消息和原始异常的业务异常。
     * <p>
     * 用于捕获底层异常后包装抛出，保留异常堆栈。
     * </p>
     *
     * @param message 异常描述信息
     * @param cause   原始异常对象（如 SQLException、IOException 等）
     */
    public BizException(String message, Throwable cause) {
        // 调用父类构造方法，同时传入消息和原始异常
        super(message, cause);
    }
}