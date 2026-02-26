package com.MKappshop.MKappshop.annotation;  // 声明当前类所在的包路径

import java.lang.annotation.*;  // 导入 Java 语言提供的所有注解基础类和元注解

/**
 * 注入当前登录的管理员对象。
 * <p>
 * 在 Controller 方法参数上使用此注解，拦截器会自动从 JWT 解析出管理员 ID，
 * 并从数据库查询完整的管理员实体并注入。
 * </p>
 *
 * @author MKappshop
 * @since 1.0
 */
// 以上是 Javadoc 文档注释，描述该注解的用途、工作原理、作者及版本信息

@Target(ElementType.PARAMETER)      // 元注解：指定 CurrentAdmin 只能用于方法参数上
@Retention(RetentionPolicy.RUNTIME) // 元注解：指定 CurrentAdmin 在运行时仍然保留，可通过反射机制读取
@Documented                         // 元注解：指定使用该注解的元素会被 javadoc 工具生成到文档中
public @interface CurrentAdmin {    // 使用 @interface 关键字定义一个注解类型，名称为 CurrentAdmin
}