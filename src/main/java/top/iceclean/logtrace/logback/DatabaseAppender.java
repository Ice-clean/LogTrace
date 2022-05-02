package top.iceclean.logtrace.logback;

import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.AppenderBase;
import top.iceclean.logtrace.bean.LogTrace;
import top.iceclean.logtrace.db.LogHandler;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * 重写 logback 的 UnsynchronizedAppenderBase
 * 实现将日志保存到用户配置好的数据库中
 * @author : Ice'Clean
 * @date : 2022-04-30
 */
@Slf4j
@Component
public class DatabaseAppender extends AppenderBase<ILoggingEvent> {

    /** 注入数据库处理器 */
    private final LogHandler logHandler = new LogHandler();

    @Override
    protected void append(ILoggingEvent event) {
        // 获取日志追踪器
        LogTrace logTrace = LogTrace.getLogTrace();
        if (logTrace != null) {
            // 先插入日志头，然后插入日志信息
            int key = logHandler.insertHead(event.getLevel().levelStr,
                    event.getThreadName(), event.getLoggerName(), logTrace);
            logHandler.insertMessage(logTrace, key);
            // 完成日志
            logTrace.finish();
        }
    }
}
