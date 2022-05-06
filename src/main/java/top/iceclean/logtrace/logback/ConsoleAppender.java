package top.iceclean.logtrace.logback;

import ch.qos.logback.classic.spi.ILoggingEvent;
import top.iceclean.logtrace.bean.LogTrace;
import top.iceclean.logtrace.spi.LogFormat;
import top.iceclean.logtrace.web.ViewEndPoint;

import java.io.IOException;

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
        // 判断是否为 LogTrace 日志
        if (LogFormat.isLogTrace(event)) {
            // 完成日志
            LogTrace logTrace = LogTrace.getLogTrace();
            if (logTrace != null) {
                logTrace.finish();
            }
        } else {
            // 否则生成 OTHER 类型的 LogTrace 日志
            LogTrace logTrace = LogFormat.getOtherLog(event);
            try {
                // 通知前端
                ViewEndPoint.castLogMessage(logTrace);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}
