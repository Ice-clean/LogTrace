package top.iceclean.logtrace.spi;

import lombok.extern.slf4j.Slf4j;
import org.aopalliance.intercept.MethodInterceptor;
import org.aopalliance.intercept.MethodInvocation;
import org.aspectj.lang.annotation.AfterThrowing;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import top.iceclean.logtrace.annotation.LogMessage;
import top.iceclean.logtrace.bean.LogTrace;
import top.iceclean.logtrace.config.LogTraceConfig;
import top.iceclean.logtrace.constants.LogLevel;
import top.iceclean.logtrace.constants.LogMode;
import top.iceclean.logtrace.constants.LogType;
import top.iceclean.logtrace.web.ViewEndPoint;

import java.io.IOException;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Optional;

/**
 * @author : Ice'Clean
 * @date : 2022-04-20
 */
@Slf4j
@Component
@Aspect
public class LogAdvice implements MethodInterceptor {

    @Override
    public Object invoke(MethodInvocation invocation) throws Throwable {
        // 获取方法的参数类型和参数值
        Method method = invocation.getMethod();
        Parameter[] parameters = method.getParameters();
        Object[] args = invocation.getArguments();

        System.out.println("受理方法：" + method.getName());

        // 获取该业务线（线程）绑定的系统自定义日志
        LogTrace threadLog = LogTrace.getLogTrace();
        LogTrace systemLog = null;
        if (threadLog == null) {
            // 如果线程系统日志为空，说明该方法是起始方法，应该创建新的系统日志
            systemLog = LogTrace.bindLogTrace(bindSystemLog(method, parameters, args));
        } else if (LogMode.MODE_DETAIL.equals(LogTraceConfig.mode) && LogTraceConfig.Output.comeParam) {
            // 如果非空，说明是调用链中的方法，系统日志保留为空，使用起始的系统日志，并在详细模式下时添加入参日志
            threadLog.inMethod(method, parameters, args);
        }

        // 绑定操作日志
        // bindGeneralLog(method, parameters, args);

        // 执行并获取结果，方法进入和退出都有相应记录，便于异常处理时回推到最初的方法
        LogTrace.getLogTrace().come();
        Object result = invocation.proceed();
        LogTrace.getLogTrace().exit();

        // 将一整条调用链的日志写出（到控制台、文件、数据库等，并通过 websocket 通知前端）
        // 系统日志为空说明调用链还没结束，不执行写出操作
        if (systemLog != null) {
            // 设置返回值
            systemLog.setReturnString(Optional.ofNullable(result).orElse("null").toString());

            // 判断级别并写入日志
            if (LogLevel.LEVEL_ERROR.equals(systemLog.getLevel())){
                log.error(systemLog.toString());
            } else {
                log.info(systemLog.toString());
            }

            // 将日志同步到前端
            systemLog.setCreateTime(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss").format(LocalDateTime.now()));
            systemLog.setThread(Thread.currentThread().getName());
            systemLog.setSite("iceclean.customlog.spi.LogAdvice");
            ViewEndPoint.castLogMessage(systemLog);
        } else if (threadLog != null && LogMode.MODE_DETAIL.equals(LogTraceConfig.mode) && LogTraceConfig.Output.exitReturn){
            // 否则在详细模式下添加中间函数的返回值日志
            threadLog.outMethod(method, Optional.ofNullable(result).orElse(LogTraceConfig.DEFAULT_VALUE).toString());
        }

        // 返回结果
        return result;
    }

    /** 绑定系统日志 */
    public LogTrace bindSystemLog(Method method, Parameter[] parameters, Object[] args) {
        // 有设置自定义日志格式的话，则为系统日志
        LogTrace logTrace = new LogTrace(LogTraceConfig.mode, LogType.TYPE_SYSTEM);
        setLogTrace(logTrace, method, parameters, args);
        return logTrace;
    }

    /** 为自定义日志格式赋予详细值 */
    public void setLogTrace(LogTrace logTrace, Method method, Parameter[] parameters, Object[] args) {
        // 给自定义日志格式赋值
        logTrace.setClassName(method.getDeclaringClass().toString());
        logTrace.setMethodName(method.getName());
        logTrace.setParameter(parameters, args);

        // 获取请求连接
        GetMapping getMapping = method.getAnnotation(GetMapping.class);
        PostMapping postMapping = method.getAnnotation(PostMapping.class);
        RequestMapping requestMapping = method.getAnnotation(RequestMapping.class);
        String requestUrl = getMapping != null ? getMapping.value()[0] :
                        postMapping != null ? postMapping.value()[0] :
                        requestMapping != null ? requestMapping.value()[0] : null;
        logTrace.setRequestPath(requestUrl);
    }

    /** 绑定通用日志 */
    public void bindGeneralLog(Method method, Parameter[] parameters, Object[] args) {
        // 有日志信息注解的话，记录通用日志
        LogMessage logMessage = method.getAnnotation(LogMessage.class);
        if (logMessage != null) {
            // 有该注解的话，得到注解的内容，即日志信息
            String mode = logMessage.mode();
            String type = logMessage.type();
            String value = logMessage.value();

            // 进行参数值的替换
            String realData;
            for (int i = 0; i < args.length; i++) {
                if (!parameters[i].getType().equals(LogTrace.class)) {
                    // 解析出参数值，并替换占位符
                    realData = parseParam(parameters[i], args[i]);
                    value = value.replaceAll("#arg" + i, realData);
                }
            }

            // 根据注解情况，判断日志的具体类型
            log.info(new LogTrace(mode, type, LogLevel.LEVEL_INFO, value).toString());
        }
    }

    /** 解析参数值为需要替换的值 */
    public String parseParam(Parameter param, Object arg) {
        // 解析后的数据
        StringBuilder parsedData = new StringBuilder();
        // 进行解析，有必要的话在这个地方做参数转换
        parsedData.append(arg.toString());
        return parsedData.toString();
    }

    /**
     * 记录异常信息
     * @param exception 捕获到的异常
     */
    @AfterThrowing(value = "@within(top.iceclean.logtrace.annotation.EnableLogTrace)", throwing = "exception", argNames = "exception")
    public void exceptionLog(Exception exception) throws IOException {
        // 获取异常发生的方法
        StackTraceElement caller = exception.getStackTrace()[0];

        // 获取线程日志
        LogTrace threadLog = LogTrace.getLogTrace();
        if (threadLog != null) {
            // 在调用链退出到最初一层时，写入日志
            threadLog.exit();
            if (threadLog.getLayer() == 0) {
                threadLog.exception(caller.getFileName(), caller.getLineNumber(), caller.getMethodName(), exception.toString());
                threadLog.setStackTrace(exception.getStackTrace());
                // 添加异常信息
                log.error(threadLog.toString());

                // 将日志同步到前端
                threadLog.setCreateTime(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss").format(LocalDateTime.now()));
                threadLog.setThread(Thread.currentThread().getName());
                threadLog.setSite("iceclean.customlog.spi.LogAdvice");
                ViewEndPoint.castLogMessage(threadLog);
                System.out.println("通知完前端了");
            }
        }
    }
}
