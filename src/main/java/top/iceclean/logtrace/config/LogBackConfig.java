package top.iceclean.logtrace.config;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.PatternLayout;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.LayoutBase;
import ch.qos.logback.core.encoder.LayoutWrappingEncoder;
import top.iceclean.logtrace.logback.ConsoleAppender;
import top.iceclean.logtrace.logback.ConsoleLayout;
import top.iceclean.logtrace.logback.DatabaseAppender;
import top.iceclean.logtrace.filter.LogTraceFilter;
import top.iceclean.logtrace.logback.FileAppender;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.impl.StaticLoggerBinder;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;

/**
 * 使用代码配置 logback，而不是 xml 配置
 * @author : Ice'Clean
 * @date : 2022-04-26
 */
@Slf4j
@Component
public class LogBackConfig implements ApplicationRunner {
    @Override
    public void run(ApplicationArguments args) throws Exception{
        // 获取 context 和根日志配置
        LoggerContext context = (LoggerContext) StaticLoggerBinder.getSingleton().getLoggerFactory();
        Logger rootLogger = context.getLogger(Logger.ROOT_LOGGER_NAME);

        // 添加控制台输出、文件输出和数据库输出
        setCustomConsoleAppender(context, rootLogger);
        setCustomFileAppender(context, rootLogger);
        setDatabaseAppender(context, rootLogger);
    }

    private void setCustomConsoleAppender(LoggerContext context, Logger rootLogger) {
        if (LogTraceConfig.Console.enabled) {
            // 定义样式
            ConsoleLayout layout = new ConsoleLayout();
            layout.setPattern(LogTraceConfig.Console.pattern);
            layout.setContext(context);
            layout.start();

            // 定义控制台输出
            ConsoleAppender consoleAppender = new ConsoleAppender();
            consoleAppender.setContext(context);
            consoleAppender.setName("CUSTOM_CONSOLE");
            consoleAppender.setEncoder(getEncoder(context, layout));
            consoleAppender.addFilter(new LogTraceFilter());
            consoleAppender.start();

            // 添加控制台输出并取消原控制台输出
            rootLogger.addAppender(consoleAppender);
            rootLogger.detachAppender("STDOUT");
            log.info("add console appender successfully：CUSTOM_CONSOLE");

            // 生命周期自增
            LogTraceConfig.lifeTime++;
        }
    }

    private void setCustomFileAppender(LoggerContext context, Logger rootLogger) throws Exception {
        // 判断文件配置是否合理，不合理将抛出异常
        LogTraceConfig.File.check();

        // 合理的话，则遍历数组添加文件输出
        boolean[] enabled = LogTraceConfig.File.enabled;
        for (int i = 0; i < enabled.length; i++) {
            if (enabled[i]) {
                // 定义样式
                PatternLayout layout = new PatternLayout();
                layout.setPattern(LogTraceConfig.File.pattern[i]);
                layout.setContext(context);
                layout.start();

                // 定义文件输出
                FileAppender fileAppender = new FileAppender();
                fileAppender.setContext(context);
                fileAppender.setName("CUSTOM_FILE");
                fileAppender.setEncoder(getEncoder(context, layout));
                fileAppender.setFile(LogTraceConfig.File.target[i]);

                // 添加文件输出
                rootLogger.addAppender(fileAppender);
                log.info("add file appender successfully：{}", LogTraceConfig.File.target[i]);

                // 生命周期自增
                LogTraceConfig.lifeTime++;
            }
        }
    }

    private void setRollingFileAppender(LoggerContext context, Logger rootLogger) {

    }

    private void setDatabaseAppender(LoggerContext context, Logger rootLogger) {
        if (LogTraceConfig.Database.enabled) {
            // 定义数据库输出
            DatabaseAppender databaseAppender = new DatabaseAppender();
            databaseAppender.setContext(context);
            databaseAppender.setName("CUSTOM_DATABASE");
            databaseAppender.start();

            // 添加数据库输出
            rootLogger.addAppender(databaseAppender);
            log.info("add database appender successfully：CUSTOM_DATABASE");

            // 生命周期自增
            LogTraceConfig.lifeTime++;
        }
    }

    /** 获取一个编码器 */
    private LayoutWrappingEncoder<ILoggingEvent> getEncoder(LoggerContext context, LayoutBase<ILoggingEvent> layout) {
        LayoutWrappingEncoder<ILoggingEvent> encoder = new LayoutWrappingEncoder<>();
        encoder.setContext(context);
        encoder.setLayout(layout);
        encoder.setCharset(StandardCharsets.UTF_8);
        encoder.start();
        return encoder;
    }
}
