package top.iceclean.logtrace.config;

import top.iceclean.logtrace.constants.LogMode;
import top.iceclean.logtrace.spi.LogAdvice;
import org.springframework.aop.aspectj.AspectJExpressionPointcutAdvisor;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Component;

/**
 * 自定义日志的配置
 * @date : 2022-04-20
 * @author : Ice'Clean
 */
@Component
@ConfigurationProperties(prefix = "log-trace")
public class LogTraceConfig {
    /** 日志的显示类型，默认为记录模式 */
    public static String mode = LogMode.MODE_RECORD;
    /** 控制台输出配置 */
    public static Console console = new Console();
    /** 文件输出配置 */
    public static File file = new File();
    /** 数据库输出配置 */
    public static Database database = new Database();
    /** 输出内容控制配置 */
    public static Output output = new Output();
    /** 日志的生命周期，由 appender 的数量决定 */
    public static int lifeTime = 0;
    /** 日志为 null 值时的默认值 */
    public static String DEFAULT_VALUE = "无";

    /** TODO：需要去掉，测试用的字段 */
    public static String datasourceInfo;

    /** 控制台输出配置 */
    public static class Console {
        /** 控制台默认输出 */
        public static boolean enabled = true;
        /** 输出格式 */
        public static String pattern = "%yellow(%date{yyyy-MM-dd HH:mm:ss}) %highlight(%-5level) %magenta([%thread]) %blue(%file:%line) >>\033[39;37;0m %msg\n\033[39;37;0m";

        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            Console.enabled = enabled;
        }

        public String getPattern() {
            return pattern;
        }

        public void setPattern(String pattern) {
            Console.pattern = pattern;
        }
    }

    /** 文件输出配置 */
    public static class File {
        /** 文件默认不输出 */
        public static boolean[] enabled = {};
        /** 输出格式 */
        public static String[] pattern = {};
        /** 输出的目标文件 */
        public static String[] target = {};

        public boolean[] getEnabled() {
            return enabled;
        }

        public void setEnabled(boolean[] enabled) {
            File.enabled = enabled;
        }

        public String[] getPattern() {
            return pattern;
        }

        public void setPattern(String[] pattern) {
            File.pattern = pattern;
        }

        public String[] getTarget() {
            return target;
        }

        public void setTarget(String[] target) {
            File.target = target;
        }

        /** 检查配置是否合理 */
        public static void check() throws Exception {
            if (enabled == null || pattern == null || target == null) {
                throw new Exception("LogTrace configuration error，a null value is configured");
            } else if (!(enabled.length == pattern.length && pattern.length == target.length)) {
                throw new Exception(String.format("LogTrace configuration error，length mismatch! " +
                                "len(enable) is %d, len(pattern) is %d and len(target) is %d ",
                        enabled.length, pattern.length, target.length));
            }
        }
    }

    /** 滚动文件输出 */
    public static class RollingFile {

    }

    /** 数据库输出 */
    public static class Database {
        /** 数据库默认不输出 */
        public static boolean enabled = false;
        /** 数据库连接参数：驱动、链接、用户名和密码 */
        public static String driver, url, username, password;

        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            Database.enabled = enabled;
        }

        public String getDriver() {
            return driver;
        }

        public void setDriver(String driver) {
            Database.driver = driver;
        }

        public String getUrl() {
            return url;
        }

        public void setUrl(String url) {
            Database.url = url;
        }

        public String getUsername() {
            return username;
        }

        public void setUsername(String username) {
            Database.username = username;
        }

        public String getPassword() {
            return password;
        }

        public void setPassword(String password) {
            Database.password = password;
        }
    }

    /**
     * 自定义输出配置
     */
    public static class Output {
        /**
         * 是否记录函数入参和返回值信息
         * 详细模式下这两个配置默认开启
         */
        public static Boolean comeParam  = true;
        public static Boolean exitReturn = true;

        public Boolean getComeParam() {
            return comeParam;
        }

        public void setComeParam(Boolean comeParam) {
            Output.comeParam = comeParam;
        }

        public Boolean getExitReturn() {
            return exitReturn;
        }

        public void setExitReturn(Boolean exitReturn) {
            Output.exitReturn = exitReturn;
        }
    }


    public String getMode() {
        return mode;
    }

    public void setMode(String mode) {
        LogTraceConfig.mode = mode;
    }

    public Console getConsole() {
        return console;
    }

    public void setConsole(Console console) {
        LogTraceConfig.console = console;
    }

    public File getFile() {
        return file;
    }

    public void setFile(File file) {
        LogTraceConfig.file = file;
    }

    public Database getDataBase() {
        return database;
    }

    public void setDataBase(Database dataBase) {
        LogTraceConfig.database = dataBase;
    }

    public Output getOutput() {
        return output;
    }

    public void setOutput(Output output) {
        LogTraceConfig.output = output;
    }

    public String getDefaultValue() {
        return DEFAULT_VALUE;
    }

    public void setDefaultValue(String defaultValue) {
        DEFAULT_VALUE = defaultValue;
    }

    @Bean
    public AspectJExpressionPointcutAdvisor configAdvisor() {
        AspectJExpressionPointcutAdvisor advisor = new AspectJExpressionPointcutAdvisor();
        advisor.setAdvice(new LogAdvice());
        advisor.setExpression("@within(top.iceclean.logtrace.annotation.EnableLogTrace)");
//        advisor.setExpression("execution(* com.iceclean.siyuanpatch.controller.*.*(..)) || " +
//                "execution(* com.iceclean.siyuanpatch.service.*.*(..))");
        return advisor;
    }
}
