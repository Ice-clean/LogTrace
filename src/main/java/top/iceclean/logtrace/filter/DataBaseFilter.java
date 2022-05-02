package top.iceclean.logtrace.filter;

import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.filter.Filter;
import ch.qos.logback.core.spi.FilterReply;

/**
 * 数据库输出过滤器
 * @author : Ice'Clean
 * @date : 2022-04-30
 */
public class DataBaseFilter extends Filter<ILoggingEvent> {
    @Override
    public FilterReply decide(ILoggingEvent event) {
        return FilterReply.ACCEPT;
    }
}
