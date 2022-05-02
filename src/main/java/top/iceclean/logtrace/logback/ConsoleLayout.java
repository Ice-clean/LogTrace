package top.iceclean.logtrace.logback;

import ch.qos.logback.classic.PatternLayout;
import ch.qos.logback.classic.spi.ILoggingEvent;
import top.iceclean.logtrace.bean.LogTrace;

/**
 * 重写 logback 的布局，针对控制台的颜色显示
 * @author : Ice'Clean
 * @date : 2022-04-30
 */
public class ConsoleLayout extends PatternLayout {
    @Override
    public String doLayout(ILoggingEvent event) {
        // layout 开启时输出
        if (isStarted()) {
            // 先拿到第一阶段格式化的内容
            String content = this.writeLoopOnConverters(event);

            // 如果能拿到日志追踪器的话，则进行第二阶段格式化，给控制台输出加上颜色
            LogTrace logTrace = LogTrace.getLogTrace();
            if (logTrace != null) {
                String[] split = content.split("(INLINE)|(RECORD)|(DETAIL)");
                if (split.length > 1) {
                    return split[0] + logTrace.toColorString() + "\n";
                }
            }

            // 否则返回原格式化结果
            return content;
        }

        // 关闭时则输出空串
        return "";
    }
}
