package top.iceclean.logtrace.logback;

import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.UnsynchronizedAppenderBase;
import org.springframework.stereotype.Component;
import top.iceclean.logtrace.bean.LogTrace;
import top.iceclean.logtrace.db.LogHandler;
import top.iceclean.logtrace.spi.LogFormat;

/**
 * 重写 logback 的 UnsynchronizedAppenderBase
 * 实现将日志保存到用户配置好的数据库中
 * @author : Ice'Clean
 * @date : 2022-04-30
 */
@Component
public class DatabaseAppender extends UnsynchronizedAppenderBase<ILoggingEvent> {

    /** 注入数据库处理器 */
    private final LogHandler logHandler = new LogHandler();

    @Override
    protected void append(ILoggingEvent event) {
        // 判断日志类型
        if (LogFormat.isLogTrace(event)) {
            // 获取日志追踪器
            LogTrace logTrace = LogTrace.getLogTrace();
            if (logTrace != null) {
                // 先插入日志头，然后插入日志信息
                int key = logHandler.insertHead(event, logTrace);
                logHandler.insertMessage(logTrace, key);
                // 完成日志
                logTrace.finish();
            }
        } else {
            // 生成 OTHER 类型的日志
            LogTrace logTrace = LogFormat.getOtherLog(event);
            int key = logHandler.insertHead(event, logTrace);
            logHandler.insertMessage(logTrace, key);
        }

    }
}
