package top.iceclean.logtrace.bean;

import top.iceclean.logtrace.constants.LogLevel;
import top.iceclean.logtrace.constants.LogStyle;
import top.iceclean.logtrace.spi.LogFormat;
import javafx.util.Pair;

import java.lang.reflect.Method;
import java.util.List;

/**
 * @author : Ice'Clean
 * @date : 2022-04-21
 * 单条日志数据
 */
public class LogData {
    /** 日志级别 */
    private final String level;
    /** 日志产生位置 */
    private final String site;
    /** 日志内容 */
    private final String content;
    /** 传入参数 */
    private final List<Pair<String, Object>> paramList;

    /**
     * 创建普通日志
     * @param level 日志级别
     * @param content 日志内容
     * @param site 日志产生位置
     */
    public LogData(String level, String site, String content) {
        this.level = level;
        this.site = site;
        this.content = content;
        this.paramList = null;
    }

    public LogData(String level, String site, List<Pair<String, Object>> paramList) {
        this.level = level;
        this.site = site;
        this.content = null;
        this.paramList = paramList;
    }

    /**
     * 创建进入方法日志
     * @param paramList 方法参数列表
     */
    public LogData(Method method, List<Pair<String, Object>> paramList) {
        this.level = LogLevel.LEVEL_IN;
        this.site = String.format("%s_%s", method.getDeclaringClass().getSimpleName(), method.getName());
        this.content = null;
        this.paramList = paramList;
    }

    /**
     * 创建跳出方法日志
     * @param returnString 方法的返回值
     */
    public LogData(Method method, String returnString) {
        this.level = LogLevel.LEVEL_OUT;
        this.site = String.format("%s_%s", method.getDeclaringClass().getSimpleName(), method.getName());
        this.content = returnString == null || returnString.isEmpty() ? "" : returnString;
        this.paramList = null;
    }

    public String getLevel() {
        return level;
    }

    public String getContent() {
        return content;
    }

    public String getSite() {
        return site;
    }

    public List<Pair<String, Object>> getParamList() {
        return paramList;
    }

    /**
     * 可手动调用获取带颜色的格式化字符串
     * @return 带颜色的格式化字符串
     */
    public String toColorString() {
        return LogFormat.listLogData(this, LogStyle.STYLE_COLOR);
    }

    /**
     * 默认输出无颜色格式化字符串
     * @return 无颜色格式化字符串
     */
    @Override
    public String toString() {
        return LogFormat.listLogData(this, LogStyle.STYLE_PLAIN);
    }
}
