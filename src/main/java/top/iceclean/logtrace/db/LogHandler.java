package top.iceclean.logtrace.db;

import ch.qos.logback.classic.spi.ILoggingEvent;
import top.iceclean.logtrace.bean.LogData;
import top.iceclean.logtrace.bean.LogTrace;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

import static top.iceclean.logtrace.spi.LogFormat.*;

/**
 * 数据库的日志处理器
 * @author : Ice'Clean
 * @date : 2022-05-01
 */
@Slf4j
@Component
public class LogHandler {
    /** 使用静态注入 */
    public static DataSource dataSource;

    /** 插入日志头语句 */
    public static final String INSERT_HEAD_LOG_SQL = "insert into t_log_head (" +
            "log_level, log_thread, log_site, log_mode, log_type, " +
            "log_url, log_parent, log_method, log_params, log_return, log_stack ) " +
            "values(?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";

    /** 插入日志信息语句 */
    public static final String INSERT_MESSAGE_LOG_SQL = "insert into t_log_message (" +
            "head_id, log_level, log_site, log_message ) values(?, ?, ?, ?)";

    @Autowired
    public void setDataSource(DataSource dataSource) {
        LogHandler.dataSource = dataSource;
    }

    /**
     * 插入日志头
     * @param event logback 日志
     * @param logTrace logTrace 日志
     * @return 日志头主键（插入失败返回 0）
     */
    public int insertHead(ILoggingEvent event, LogTrace logTrace) {
        int key = 0;
        try {
            PreparedStatement insertHead = dataSource.getConnection().prepareStatement(INSERT_HEAD_LOG_SQL, Statement.RETURN_GENERATED_KEYS);
            insertHead.setString(1, event.getLevel().levelStr);
            insertHead.setString(2, event.getThreadName());
            insertHead.setString(3, event.getLoggerName());
            insertHead.setString(4, logTrace.getMode());
            insertHead.setString(5, logTrace.getType());
            insertHead.setString(6, logTrace.getRequestPath());
            insertHead.setString(7, logTrace.getClassName());
            insertHead.setString(8, logTrace.getMethodName());
            insertHead.setString(9, logTrace.getParameters());
            insertHead.setString(10, logTrace.getReturnString());
            insertHead.setString(11, logTrace.getStackString());
            if (insertHead.executeUpdate() == 1) {
                ResultSet generatedKeys = insertHead.getGeneratedKeys();
                if (generatedKeys.next()) {
                    key = generatedKeys.getInt(1);
                }
            }
            insertHead.getConnection().close();
        } catch (SQLException e) {
            log.error("插入日志头失败：" + e.toString());
        }

        return key;
    }

    /**
     * 插入日志信息
     * @param logTrace 日志
     * @param key 日志头主键
     */
    public void insertMessage(LogTrace logTrace, int key) {
        // 检查主键
        if (key == 0) {
            log.error("插入日志信息失败，主键获取不到");
            return;
        }

        // 预编译
        PreparedStatement insertMessage;
        try {
            insertMessage = dataSource.getConnection().prepareStatement(INSERT_MESSAGE_LOG_SQL);
        } catch (SQLException e) {
            log.error("预编译语句失败：" + e.toString());
            return;
        }

        // 依次次插入日志信息
        List<LogData> logDataList = logTrace.getLogDataList();
        for (LogData logData : logDataList) {
            try {
                insertMessage.setInt(1, key);
                insertMessage.setString(2, logData.getLevel());
                insertMessage.setString(3, logData.getSite());
                // 如果是函数入参记录的话，转化成入参
                if (logData.getContent() == null) {
                    insertMessage.setString(4, plainInlineParams(logData.getParamList()));
                } else {
                    insertMessage.setString(4, logData.getContent());
                }
                insertMessage.executeUpdate();
            } catch (SQLException e) {
                log.error("插入日志信息失败：" + e.toString());
            }
        }

        try {
            insertMessage.getConnection().close();
        } catch (SQLException sqlException) {
            sqlException.printStackTrace();
        }
    }

    /**
     * 从数据库中获取日志
     * @param level 日志等级过滤
     * @param type 日志类型过滤
     * @param last 倒数第 n 页
     * @param max 每一页的数量
     * @param offset 分页偏移量
     * @return 日志列表
     */
    public List<LogTrace> getLogTraceList(String level, String type, int last, int max, int offset) {
        // 存储结果
        List<LogTrace> logTraceList = new ArrayList<>();
        // 获取预编译语句
        PreparedStatement getLogHead = getLogHeadPreparedStatement(level, type, last, max, offset);
        PreparedStatement getLogData = getLogDataPreparedStatement();

        if (getLogHead != null && getLogData != null) {
            try {
                // 把所有日志取出来
                ResultSet resultSet = getLogHead.executeQuery();
                while (resultSet.next()) {
                    // 获取日志头信息
                    LogTrace logTrace = new LogTrace();
                    logTrace.setLevel(resultSet.getString(2));
                    logTrace.setThread(resultSet.getString(3));
                    logTrace.setSite(resultSet.getString(4));
                    logTrace.setMode(resultSet.getString(5));
                    logTrace.setType(resultSet.getString(6));
                    logTrace.setRequestPath(resultSet.getString(7));
                    logTrace.setClassName(resultSet.getString(8));
                    logTrace.setMethodName(resultSet.getString(9));
                    logTrace.setParamList(getParamList(resultSet.getString(10)));
                    logTrace.setReturnString(resultSet.getString(11));
                    logTrace.setStackList(getStackList(resultSet.getString(12)));
                    logTrace.setRead(resultSet.getInt(13));
                    logTrace.setCreateTime(resultSet.getString(14));

                    // 获取具体日志信息列表
                    logTrace.setLogDataList(getLogDataList(resultSet.getInt(1), getLogData));
                    logTraceList.add(logTrace);
                }
                resultSet.close();
                return logTraceList;
            } catch (SQLException e) {
                log.error("查询日志头失败：" + e.toString());
            } finally {
                try {
                    getLogData.getConnection().close();
                    getLogHead.getConnection().close();
                } catch (SQLException e) {
                    log.error("关闭连接失败：" + e.toString());
                }
            }
        }

        return logTraceList;
    }

    private List<LogData> getLogDataList(int headId, PreparedStatement getLogData) {
        // 存放结果
        List<LogData> logDataList = new ArrayList<>();
        // 获取预编译语句
        if (getLogData != null) {
            try {
                // 将日志信息取出来
                getLogData.setInt(1, headId);
                ResultSet resultSet = getLogData.executeQuery();
                while (resultSet.next()) {
                    // 判断是否为为入参，是的话将参数列表串转化为参数列表
                    if ("COME".equals(resultSet.getString(3))) {
                        logDataList.add(new LogData(
                                resultSet.getString(3),
                                resultSet.getString(4),
                                getParamList(resultSet.getString(5))));
                    } else {
                        logDataList.add(new LogData(
                                resultSet.getString(3),
                                resultSet.getString(4),
                                resultSet.getString(5)));
                    }
                }
                return logDataList;
            } catch (SQLException e) {
                log.error("查蓄日志信息失败：" + e.toString());
            }
        }
        return null;
    }

    private PreparedStatement getLogHeadPreparedStatement(String level, String type, int last, int max, int offset) {
        // 拼接 sql 语句
        int num = 0;
        StringBuilder sql = new StringBuilder();
        sql.append("select * from t_log_head where 1 = 1 ");
        if (level != null && !"ALL".equals(level)) {
            num += 1;
            sql.append("and log_level = ? ");
        }
        if (type != null && !"ALL".equals(type)) {
            num += 2;
            sql.append("and log_type = ? ");
        }
        sql.append("order by head_id desc ").append("limit ").append(offset + max * last).append(",").append(max);

        System.out.println(sql.toString());
        try {
            Connection connection = dataSource.getConnection();
            PreparedStatement getLogHead = connection.prepareStatement(sql.toString());
            switch (num) {
                case 1: getLogHead.setString(1, level); break;
                case 2: getLogHead.setString(1, type); break;
                case 3:
                    getLogHead.setString(1, level);
                    getLogHead.setString(2, type);
                    break;
                default:
            }
            return getLogHead;
        } catch (SQLException e) {
            log.error("查询日志头失败：" + e.toString());
        }

        return null;
    }

    private PreparedStatement getLogDataPreparedStatement() {
        try {
            return dataSource.getConnection().prepareStatement("select * from t_log_message where head_id = ?");
        } catch (SQLException e) {
            log.error("查询日志信息失败：" + e.toString());
        }
        return null;
    }
}
