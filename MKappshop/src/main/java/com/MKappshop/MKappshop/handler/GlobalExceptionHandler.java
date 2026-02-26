package com.MKappshop.MKappshop.handler;
// 包声明：当前类位于 handler 包下，专门存放各类处理器（如全局异常处理器、拦截器等）

// ========== 项目内部依赖 ==========
import com.MKappshop.MKappshop.common.Result;               // 导入统一响应结果类，用于封装错误信息返回给前端
import com.MKappshop.MKappshop.exception.ResourceNotFoundException; // 导入自定义资源不存在异常

// ========== Jakarta Bean Validation 异常 ==========
import jakarta.validation.ConstraintViolationException;    // 导入 Jakarta 校验框架的约束违反异常（参数校验失败时抛出）

// ========== Lombok ==========
import lombok.extern.slf4j.Slf4j;                           // 导入 Lombok 日志注解，自动生成 log 常量

// ========== Spring 相关 ==========
import org.springframework.http.HttpStatus;                 // 导入 HTTP 状态码枚举
import org.springframework.web.bind.annotation.ExceptionHandler; // 导入异常处理器注解，标记方法处理特定异常
import org.springframework.web.bind.annotation.ResponseStatus;   // 导入响应状态码注解，用于指定 HTTP 响应状态
import org.springframework.web.bind.annotation.RestControllerAdvice; // 导入 REST 控制器增强注解，结合了 @ControllerAdvice 和 @ResponseBody

/**
 * 全局异常处理器。
 * <p>
 * 使用 @RestControllerAdvice 捕获所有控制器抛出的异常，
 * 转换为统一的 Result 错误响应格式。
 * </p>
 *
 * @author MKappshop
 * @since 1.0
 */
@Slf4j                                                      // Lombok 注解：自动生成一个名为 log 的 Logger 对象（org.slf4j.Logger）
@RestControllerAdvice                                       // Spring 注解：声明该类是全局 REST 控制器增强类，能捕获所有 @RestController 抛出的异常，并将返回值自动包装为 JSON
public class GlobalExceptionHandler {

    /**
     * 处理资源不存在异常（404）。
     *
     * @param e 异常对象
     * @return 统一错误响应，状态码 404
     */
    @ExceptionHandler(ResourceNotFoundException.class)       // 声明该方法处理 ResourceNotFoundException 异常
    @ResponseStatus(HttpStatus.NOT_FOUND)                   // 设置 HTTP 响应状态码为 404 NOT FOUND（会覆盖正常的 200 状态）
    public Result<Void> handleResourceNotFound(ResourceNotFoundException e) {
        // 方法返回 Result<Void>，表示没有响应数据，仅包含状态码和消息
        log.warn("资源不存在: {}", e.getMessage());          // 使用 Lombok 生成的 log 对象打印警告日志，记录异常信息
        return Result.error(404, e.getMessage());           // 调用 Result.error 静态方法，返回状态码 404 和异常消息
    }

    /**
     * 处理请求参数校验失败异常（如 @Positive、@NotNull）。
     *
     * @param e 异常对象
     * @return 统一错误响应，状态码 400
     */
    @ExceptionHandler(ConstraintViolationException.class)    // 声明该方法处理 ConstraintViolationException（参数校验异常）
    @ResponseStatus(HttpStatus.BAD_REQUEST)                 // 设置 HTTP 响应状态码为 400 BAD REQUEST
    public Result<Void> handleConstraintViolation(ConstraintViolationException e) {
        log.warn("参数校验失败: {}", e.getMessage());        // 打印警告日志，记录参数校验失败详情
        return Result.error(400, "请求参数错误: " + e.getMessage()); // 返回 400 错误，并拼接异常信息
    }

    /**
     * 处理所有未捕获的异常（500 服务器内部错误）。
     *
     * @param e 异常对象
     * @return 统一错误响应，状态码 500
     */
    @ExceptionHandler(Exception.class)                      // 声明该方法处理所有 Exception 及其子类（兜底异常处理）
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)      // 设置 HTTP 响应状态码为 500 INTERNAL SERVER ERROR
    public Result<Void> handleGlobalException(Exception e) {
        log.error("系统内部异常", e);                       // 打印错误日志，并输出完整堆栈跟踪（使用 log.error 的重载版本）
        return Result.error(500, "服务器开小差了，请稍后重试"); // 返回统一的友好提示，不暴露具体异常细节（防止信息泄露）
    }
}