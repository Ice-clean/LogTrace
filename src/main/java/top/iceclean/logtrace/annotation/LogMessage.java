package top.iceclean.logtrace.annotation;

import top.iceclean.logtrace.constants.LogMode;
import top.iceclean.logtrace.constants.LogType;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * @author : Ice'Clean
 * @date : 2022-01-16
 *
 * 通用日志注解
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface LogMessage {
    String mode() default LogMode.MODE_INLINE;
    String type() default LogType.TYPE_SYSTEM;
    String value() default "";
}
