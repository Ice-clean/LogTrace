package top.iceclean.logtrace.logback;

import ch.qos.logback.classic.spi.ILoggingEvent;
import top.iceclean.logtrace.bean.LogTrace;

/**
 * 重写 logback 的 ConsoleAppender
 * 对输出到控制台的日志进行颜色处理
 * @author : Ice'Clean
 * @date : 2022-04-30
 */
public class ConsoleAppender extends ch.qos.logback.core.ConsoleAppender<ILoggingEvent> {
    @Override
    protected void subAppend(ILoggingEvent event) {
        super.subAppend(event);
        // 完成日志
        LogTrace logTrace = LogTrace.getLogTrace();
        if (logTrace != null) {
            logTrace.finish();
        }
    }
}
