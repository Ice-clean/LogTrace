package top.iceclean.logtrace.constants;

/**
 * @author : Ice'Clean
 * @date : 2022-04-21
 * 日志的记录模式
 */
public class LogMode {
    /**
     * 记录模式<br>
     * 将常规日志记录的类信息、方法信息、行信息去除，转而记录每条日志的<br>
     * 请求路径、所属父类、所属方法、传入参数、请求返回、信息输出、堆栈调用这 7 中信息
     */
    public static final String MODE_RECORD = "RECORD";
    /**
     * 详细模式<br/>
     * 在记录模式的基础上，记录整条方法调用链的所有入参和返回值
     */
    public static final String MODE_DETAIL = "DETAIL";
    /**
     * 单行模式<br/>
     * 按照常规日志记录的形式（类、方法和行信息）只记录日志值
     */
    public static final String MODE_INLINE = "INLINE";
}
