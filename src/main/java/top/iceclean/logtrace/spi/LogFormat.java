package top.iceclean.logtrace.spi;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.spi.ILoggingEvent;
import javafx.util.Pair;
import top.iceclean.logtrace.bean.LogData;
import top.iceclean.logtrace.bean.LogTrace;
import top.iceclean.logtrace.config.LogTraceConfig;
import top.iceclean.logtrace.constants.LogLevel;
import top.iceclean.logtrace.constants.LogMode;
import top.iceclean.logtrace.constants.LogStyle;
import top.iceclean.logtrace.constants.LogType;

import java.io.File;
import java.lang.reflect.Parameter;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Pattern;

/**
 * 日志格式处理器
 * @author : Ice'Clean
 * @date : 2022-04-27
 */
public class LogFormat {

    /**
     * 颜色常量
     */
    public static final Integer BLACK   = 30;
    public static final Integer RED     = 31;
    public static final Integer GREEN   = 32;
    public static final Integer YELLOW  = 33;
    public static final Integer BLUE    = 34;
    public static final Integer PURPLE  = 35;
    public static final Integer CYAN    = 36;
    public static final Integer WHITE   = 37;

    /** 匹配 LogTrace 格式 */
    public static final Pattern logTracePattern = Pattern.compile("((INLINE)|(RECORD)|(DETAIL)) \\S+\\n");

    /**
     * 为内容更改显示颜色
     * @param content 目标内容
     * @return 带颜色的内容
     */
    public static String changeColor(String content, int frontColor) {
        return "\033[" + frontColor + "m" + content + "\033[0;39m";
    }

    /**
     * 将参数名和参数值收集为参数对列表
     * @param parameters 参数名数组
     * @param datum 参数值数组
     * @return 参数对列表
     */
    public static List<Pair<String, Object>> collectParams(Parameter[] parameters, Object[] datum) {
        List<Pair<String, Object>> paramList = new ArrayList<>(parameters.length);
        // 将参数名和对应的参数值对上
        for (int i = 0; i < parameters.length; i++) {
            paramList.add(new Pair<>(parameters[i].getName(), datum[i]));
        }
        return paramList;
    }

    /**
     * 将参数列表格式化为列表字符串
     * @param paramList 参数列表
     * @param color 参数名颜色，传入 -1 表示无颜色
     * @param layer 缩进层级
     * @return 格式化后的参数字符串
     */
    public static String listParams(List<Pair<String, Object>> paramList, int color, int layer) {
        if (paramList == null) {
            return LogTraceConfig.DEFAULT_VALUE;
        }
        // 参数数据建造器
        StringBuilder paramDataBuilder = new StringBuilder();
        // 提取出参数列表
        for (Pair<String, Object> paramPair : paramList) {
            paramDataBuilder.append("\n");
            for (int i = 0; i < layer; i++) {
                paramDataBuilder.append("\t");
            }
            paramDataBuilder.append(" \uF09F ")
                    .append(color > 0 ? changeColor(paramPair.getKey(), color) : paramPair.getKey())
                    .append(" : ").append(paramPair.getValue());
        }
        return paramDataBuilder.length() > 0 ? paramDataBuilder.toString() : LogTraceConfig.DEFAULT_VALUE;
    }

    /**
     * 将参数列表格式化为无颜色的行内参数字符串
     * @param paramList 参数列表
     * @return 格式化后的参数字符串
     */
    public static String plainInlineParams(List<Pair<String, Object>> paramList) {
        if (paramList != null && paramList.size() > 0) {
            StringBuilder builder = new StringBuilder();
            for (Pair<String, Object> paramPair : paramList) {
                builder.append(paramPair.getKey()).append("=")
                        .append(paramPair.getValue()).append("|");
            }
            return builder.toString();
        }

        return LogTraceConfig.DEFAULT_VALUE;
    }

    /**
     * 将堆栈调用信息格式化为行内字符串
     * @param stackList 堆栈调用信息
     * @return 格式化后的行内字符串
     */
    public static String plainInlineStackTrace(List<String> stackList) {
        if (stackList != null && stackList.size() > 0) {
            StringBuilder builder = new StringBuilder();
            for (String stack: stackList) {
                builder.append(stack).append("|");
            }
            return builder.toString();
        }
        return LogTraceConfig.DEFAULT_VALUE;
    }

    /**
     * 将日志格式化为列表字符串
     * @param logTrace 目标日志
     * @param style 格式化的风格
     * @return 格式化完毕的日志字符串
     */
    public static String listLogTrace(LogTrace logTrace, int style) {
        StringBuilder builder = new StringBuilder();
        String mode = logTrace.getMode();
        String type = logTrace.getType();

        // 格式化参数
        String listParams;
        if (LogStyle.STYLE_COLOR == style) {
            listParams = listParams(logTrace.getParamList(), BLUE, 1);
        } else {
            listParams = listParams(logTrace.getParamList(), -1, 1);
        }

        // 根据模式输出信息
        if (LogMode.MODE_RECORD.equals(mode) || LogMode.MODE_DETAIL.equals(mode)) {
            // 将详细信息格式化
            builder.append("\n\t|请求路径 : ").append(logTrace.getRequestPath());
            builder.append("\n\t|所属父类 : ").append(logTrace.getClassName());
            builder.append("\n\t|所属方法 : ").append(logTrace.getMethodName());
            builder.append("\n\t|传入参数 : ").append(listParams);
            builder.append("\n\t|请求返回 : ").append(logTrace.getReturnString());
            builder.append("\n\t|信息输出 : ");
        }

        // 输出信息
        if (LogStyle.STYLE_COLOR == style) {
            logTrace.getLogDataList().forEach(logData -> builder.append(logData.toColorString()));
        } else {
            logTrace.getLogDataList().forEach(builder::append);
        }

        // 将堆栈信息列表格式化
        List<String> stackMsg = logTrace.getStackList();
        if (stackMsg != null) {
            builder.append("\n\t|堆栈调用 : ");
            for (String stack : stackMsg) {
                builder.append("\n\t \uF09F ").append(stack);
            }
        }

        return mode + " " + type + builder.toString();
    }

    /**
     * 将日志数据格式化为列表字符串
     * @param logData 日志数据
     * @param style 格式化的风格
     * @return 格式化完毕的日志数据字符串
     */
    public static String listLogData(LogData logData, int style) {
        String level = logData.getLevel();
        String site = logData.getSite();
        String content = logData.getContent();
        List<Pair<String, Object>> paramList = logData.getParamList();

        if (LogStyle.STYLE_COLOR == style) {
            // 为产生位置和方法添加颜色
            if (site != null) {
                if (LogLevel.LEVEL_IN.equals(level) || LogLevel.LEVEL_OUT.equals(level)) {
                    site = changeColor(site, PURPLE);
                } else {
                    site = changeColor(site, BLUE);
                }
            }

            // 当为方法入参记录时，为入参添加颜色
            if (content == null) {
                content = listParams(paramList, BLUE, 2);
            }

            // 为等级添加颜色
            switch (level) {
                case LogLevel.LEVEL_INFO: level = changeColor(level, GREEN); break;
                case LogLevel.LEVEL_ERROR:
                    level = changeColor(level, RED);
                    content = changeColor(content, RED);
                    break;
                default: level = changeColor(level, YELLOW); break;
            }
        }

        // 当为方法入参记录且没有加上颜色时，进行无颜色格式化
        if (content == null) {
            content = listParams(paramList, -1, 2);
        }

        return "\n\t \uF09F " + (site == null ?
                String.format("%s : %s", level, content) :
                String.format("%s %s : %s", level, site, content));
    }

    /**
     * 解析获取参数列表
     * @param paramString 参数数组字符串
     * @return 目标参数列表
     */
    public static List<Pair<String, Object>> getParamList(String paramString) {
        if (!LogTraceConfig.DEFAULT_VALUE.equals(paramString)) {
            String[] paramArray = paramString.split("\\|");
            List<Pair<String, Object>> paramList = new ArrayList<>(paramArray.length);
            for (String paramPair : paramArray) {
                String[] split = paramPair.split("=", 2);
                paramList.add(new Pair<>(split[0], split[1]));
            }
            return paramList;
        }
        return null;
    }

    /**
     * 解析获取堆栈列表
     * @param stackString 堆栈字符串
     * @return 目标堆栈列表
     */
    public static List<String> getStackList(String stackString) {
        if (!LogTraceConfig.DEFAULT_VALUE.equals(stackString)) {
            String[] stackArray = stackString.split("\\|");
            return new ArrayList<>(Arrays.asList(stackArray));
        }
        return null;
    }

    /**
     * 判断一个日志是否为 LogTrace 日志
     * @param event logback 的日志事件
     * @return 是 LogTrace 日志则返回 true，否则 false
     */
    public static boolean isLogTrace(ILoggingEvent event) {
       return logTracePattern.matcher(event.getMessage()).find();
    }

    /**
     * 获取其他包的日志
     * @param event logback 的日志事件
     * @return LogTrace 日志
     */
    public static LogTrace getOtherLog(ILoggingEvent event) {
        LogTrace logTrace = new LogTrace();
        logTrace.setCreateTime(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss").format(LocalDateTime.now()));
        logTrace.setLevel(event.getLevel().levelStr);
        logTrace.setThread(Thread.currentThread().getName());
        logTrace.setSite(event.getLoggerName());
        logTrace.setMode(LogMode.MODE_INLINE);
        logTrace.setType(LogType.TYPE_OTHER);
        if (event.getLevel().equals(Level.INFO)) {
            logTrace.info(event.getFormattedMessage());
        } else {
            logTrace.error(event.getFormattedMessage());
        }
        return logTrace;
    }

    /**
     * 在指定文件夹下查找包含指定名称的文件
     * @param file 目标文件夹
     * @param fileName 目标文件名称
     * @return 文件大小和文件全路径对列表
     */
    public static List<Pair<Long, String>> findFile(File file, String fileName) {
        File[] files = file.listFiles();
        List<Pair<Long, String>> classNameList = new ArrayList<>();
        if (files == null) {
            return classNameList;
        }

        for (File f : files) {
            if (f.isDirectory()) {
                classNameList.addAll(findFile(f, fileName));
            }
            if (f.isFile() && f.getName().contains(fileName)) {
                classNameList.add(new Pair<>(f.length(), f.getPath()));
            }
        }

        return classNameList;
    }
}
