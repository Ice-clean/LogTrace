package top.iceclean.logtrace.bean;

/**
 * 自定义 logger 接口，用于对用户开放操作
 * @author : Ice'Clean
 * @date : 2022-04-27
 */
public interface Logger {
    /**
     * 添加 info 级别日志
     * @param message 日志信息
     */
    void info(String message);

    /**
     * 添加带参数的 info 级别日志，使用常规格式化符号
     * @param message 带格式化符号的日志信息
     * @param args 需要填充的值
     */
    void info(String message, Object ... args);

    /**
     * 添加 error 级别日志
     * @param message 日志信息
     */
    void error(String message);

    /**
     * 添加带参数的 error 级别日志，使用常规格式化符号
     * @param message 带格式化符号的日志信息
     * @param args 需要填充的值
     */
    void error(String message, Object ... args);
}
