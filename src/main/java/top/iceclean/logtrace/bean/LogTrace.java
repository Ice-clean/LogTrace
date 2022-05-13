package top.iceclean.logtrace.bean;

import com.fasterxml.jackson.annotation.JsonIgnore;
import top.iceclean.logtrace.config.LogTraceConfig;
import top.iceclean.logtrace.constants.LogLevel;
import top.iceclean.logtrace.constants.LogMode;
import top.iceclean.logtrace.constants.LogStyle;
import top.iceclean.logtrace.constants.LogType;
import top.iceclean.logtrace.spi.LogFormat;
import javafx.util.Pair;

import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 *
 * @date : 2022/4/21
 * @author Ice'Clean
 */
public class LogTrace implements Logger {
    /** 存放每次业务所属的自定义日志 */
    @JsonIgnore
    private static final ConcurrentMap<String, LogTrace> LOG_TRACE_MAP = new ConcurrentHashMap<>();

    /** 原日志头信息 */
    private String thread = LogTraceConfig.DEFAULT_VALUE;
    private String site = LogTraceConfig.DEFAULT_VALUE;
    private Integer read = 0;
    private String createTime = LogTraceConfig.DEFAULT_VALUE;

    /** 日志级别（取所有日志信息的最高级别，默认为 INFO） */
    private String level = LogLevel.LEVEL_INFO;
    /** 日志记录（默认为单行模式） */
    private String mode = LogMode.MODE_INLINE;
    /** 日志类型（默认为其他依赖包日志，本依赖包创建的日志会自动设置类型） */
    private String type = LogType.TYPE_OTHER;
    /** 日志信息（一整条调用链的信息集合） */
    private List<LogData> logDataList = new ArrayList<>(5);

    /** 【记录模式】 日志的请求路径（针对于 Controller 层） */
    private String requestPath = LogTraceConfig.DEFAULT_VALUE;
    /** 【记录模式】 产生日志的类名和方法名 */
    private String className = LogTraceConfig.DEFAULT_VALUE, methodName = LogTraceConfig.DEFAULT_VALUE;
    /** 【记录模式】 产生日志的方法中的参数个数和具体值 */
    private List<Pair<String, Object>> paramList;
    /** 【记录模式】 生日志方法中的返回值 */
    private String returnString = LogTraceConfig.DEFAULT_VALUE;
    /** 【记录模式】 堆栈调用链信息（针对于 ERROR 日志） */
    private List<String> stackList;

    /** 调用链层次，每一次进入方法时 +1，退出方法时 -1 */
    @JsonIgnore
    private int layer = 0;
    /** 该日志的生命周期，每完成一个 appender 任务就 -1，全部完成则将该日志销毁 */
    @JsonIgnore
    private int lifeTime = LogTraceConfig.lifeTime;

    /**
     * 获取一个和当前线程绑定的自定义日志
     * @return 自定义日志
     */
    public static LogTrace getLogTrace() {
        Thread currentThread = Thread.currentThread();
        if (LOG_TRACE_MAP.containsKey(currentThread.getName())) {
            return LOG_TRACE_MAP.get(currentThread.getName());
        }
        return null;
    }

    /**
     * 为当前线程绑定自定义日志
     * @param logTrace 给定的自定义日志
     * @return 将传入的自定义日志返回出去
     */
    public static LogTrace bindLogTrace(LogTrace logTrace) {
        LOG_TRACE_MAP.put(Thread.currentThread().getName(), logTrace);
        return logTrace;
    }

    /**
     * 移除当前线程的自定义日志
     * 用于将日志输出后，清除日志实体
     */
    public static void removeLogTrace() {
        LOG_TRACE_MAP.remove(Thread.currentThread().getName());
    }

    public void come() {
        layer++;
    }

    public void exit() {
        layer--;
    }

    public int getLayer() {
        return layer;
    }

    public void finish() {
        if (--lifeTime == 0) {
            removeLogTrace();
        }
    }

    public LogTrace() {}

    public LogTrace(String mode, String type) {
        this.mode = mode;
        this.type = type;
    }

    public LogTrace(String mode, String type, String logLevel, String logMessage) {
        this.mode = mode;
        this.type = type;
        addMessage(logLevel, logMessage, getOutCaller());
    }

    /**
     * 获取调用本类方法的外部方法信息
     */
    private String getOutCaller() {
        // 数组中的 3 表示外部方法，1 表示本层方法，2 表示上一层方法（即在本类调用），所以 3 才是外部
        StackTraceElement caller = Thread.currentThread().getStackTrace()[3];
        return Objects.requireNonNull(caller.getFileName()) + ":" + caller.getLineNumber() + " "
                + caller.getMethodName();
    }

    /** 添加日志信息，内部方法 */
    private void addMessage(String level, String message, String site) {
        // 提升等级为 ERROR
        if (LogLevel.LEVEL_ERROR.equals(level)) {
            this.level = LogLevel.LEVEL_ERROR;
        }
        logDataList.add(new LogData(level, site, message));
    }

    /**
     * 记录进入方法日志
     * @param method 方法
     * @param parameters 方法参数
     * @param datum 参数具体值
     */
    public void inMethod(Method method, Parameter[] parameters, Object[] datum) {
        logDataList.add(new LogData(method, LogFormat.collectParams(parameters, datum)));
    }

    /**
     * 创建跳出方法日志
     * @param method 方法
     * @param returnString 方法返回值字符串
     */
    public void outMethod(Method method, String returnString) {
        logDataList.add(new LogData(method, returnString));
    }

    /**
     * 记录异常日志
     * @param fileName 发生的文件
     * @param line 发生的行数
     * @param methodName 发生的方法名
     * @param content 异常内容
     */
    public void exception(String fileName, int line, String methodName, String content) {
        addMessage(LogLevel.LEVEL_ERROR, content, String.format("%s:%d %s", fileName, line, methodName));
    }

    public String getThread() {
        return thread;
    }

    public void setThread(String thread) {
        this.thread = thread;
    }

    public String getSite() {
        return site;
    }

    public void setSite(String site) {
        this.site = site;
    }

    public Integer getRead() {
        return read;
    }

    public void setRead(Integer read) {
        this.read = read;
    }

    public String getCreateTime() {
        return createTime;
    }

    public void setCreateTime(String createTime) {
        this.createTime = createTime;
    }

    public String getLevel() {
        return level;
    }

    public void setLevel(String level) {
        this.level = level;
    }

    public String getMode() {
        return mode;
    }

    public void setMode(String mode) {
        this.mode = mode;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public void setClassName(String className) {
        this.className = className;
    }

    public String getClassName() {
        return className;
    }

    public void setParameter(Parameter[] parameters, Object[] datum) {
        paramList = LogFormat.collectParams(parameters, datum);
    }

    public void setParamList(List<Pair<String, Object>> paramList) {
        this.paramList = paramList;
    }

    @JsonIgnore
    public String getParameters() {
        return LogFormat.plainInlineParams(paramList);
    }

    public List<Pair<String, Object>> getParamList() {
        return paramList;
    }

    public void setMethodName(String methodName) {
        this.methodName = methodName;
    }

    public String getMethodName() {
        return methodName;
    }

    public void setRequestPath(String requestPath) {
        this.requestPath = requestPath;
    }

    public String getRequestPath() {
        return requestPath;
    }

    public void setReturnString(String returnString) {
        this.returnString = returnString;
    }

    public String getReturnString() {
        return returnString;
    }

    public void setLogDataList(List<LogData> logDataList) {
        this.logDataList = logDataList;
    }

    public List<LogData> getLogDataList() {
        return logDataList;
    }

    public void setStackTrace(StackTraceElement[] elements) {
        stackList = new ArrayList<>(elements.length);
        for (StackTraceElement element : elements) {
            if ("LogAdvice.java".equals(element.getFileName())) {
                break;
            }
            stackList.add(element.toString());
        }
    }

    public void setStackList(List<String> stackList) {
        this.stackList = stackList;
    }

    public List<String> getStackList() {
        return stackList;
    }

    @JsonIgnore
    public String getStackString() {
        return LogFormat.plainInlineStackTrace(stackList);
    }



    /**
     * 可手动调用获取带颜色的格式化字符串
     * @return 带颜色的格式化字符串
     */
    public String toColorString() {
        return LogFormat.listLogTrace(this, LogStyle.STYLE_COLOR);
    }

    /**
     * 默认输出无颜色格式化字符串
     * @return 无颜色格式化字符串
     */
    @Override
    public String toString() {
        return LogFormat.listLogTrace(this, LogStyle.STYLE_PLAIN);
    }

    @Override
    public void info(String message) {
        addMessage(LogLevel.LEVEL_INFO, message, getOutCaller());
    }

    @Override
    public void info(String message, Object... args) {
        addMessage(LogLevel.LEVEL_INFO, String.format(message, args), getOutCaller());
    }

    @Override
    public void error(String message) {
        addMessage(LogLevel.LEVEL_ERROR, message, getOutCaller());
    }

    @Override
    public void error(String message, Object... args) {
        addMessage(LogLevel.LEVEL_ERROR, String.format(message, args), getOutCaller());
    }
}
