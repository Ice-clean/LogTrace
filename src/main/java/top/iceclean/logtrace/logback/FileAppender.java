package top.iceclean.logtrace.logback;

import ch.qos.logback.classic.spi.ILoggingEvent;
import top.iceclean.logtrace.bean.LogTrace;

/**
 * @author : Ice'Clean
 * @date : 2022-05-01
 */
public class FileAppender extends ch.qos.logback.core.FileAppender<ILoggingEvent> {
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
