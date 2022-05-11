package top.iceclean.logtrace.web;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.ResponseBody;
import top.iceclean.logtrace.bean.LogTrace;
import top.iceclean.logtrace.config.LogTraceConfig;
import top.iceclean.logtrace.db.LogHandler;

import javax.websocket.*;
import javax.websocket.server.ServerEndpoint;
import java.io.IOException;
import java.util.ArrayList;

/**
 * @author : Ice'Clean
 * @date : 2022-04-28
 */
@Slf4j
@Component
@Controller
@ServerEndpoint("/log")
public class ViewEndPoint {
    /** 保存全部会话 */
    private static final ArrayList<ViewEndPoint> CONNECT = new ArrayList<>();
    private static int incrementNum = 0;

    /** 当前会话 */
    private Session session;

    /** 日志操作
     *  */
    @Autowired
    private LogHandler logHandler;

    /** 保存筛选状态（筛选的等级和类别） */
    private String level = "ALL", type = "ALL";
    /** 记录筛选偏移量，分页从 offset 开始进行，在一次筛选中，若有新的日志插入到数据库，则 offset+1 */
    private int offset;

    @GetMapping("/log")
    public String logView() {
        return "index.html";
    }

    @ResponseBody
    @GetMapping("/log/{level}/{type}/{last}/{max}")
    public Object getLogMessage(@PathVariable String level, @PathVariable String type,
                                @PathVariable int last, @PathVariable int max) {
        if (LogTraceConfig.database.isEnabled()) {
            offset = 0;
            this.level = level;
            this.type = type;
            return logHandler.getLogTraceList(level, type, last, max, offset);
        }
        return new ArrayList<>();
    }

//    @GetMapping("/log/read/{logId}")
//    public Object readLogMessage(@PathVariable int logId) {
//
//    }


    public Session getSession() {
        return session;
    }

    public void incrementOffset() {
        offset++;
    }

    public boolean needSend(LogTrace logTrace) {
        return ("ALL".equals(level) || level.equals(logTrace.getLevel())) &&
                "ALL".equals(type) || type.equals(logTrace.getType());
    }

    @OnOpen
    public void onOpen(Session session) {
        incrementNum++;
        log.info("新日志请求连接 id=" + incrementNum);
        this.session = session;
        CONNECT.add(this);
    }

    @OnMessage
    public void onMessage(String message) {
        log.info("请求连接 id=" + incrementNum + " 发来数据：" + message);
    }

    @OnClose
    public void onClose() {
        CONNECT.remove(this);
        log.info("请求连接 id=" + incrementNum + " 断开");
    }

    @OnError
    public void onError(Throwable e) {
        CONNECT.remove(this);
        log.info("捕获到异常" + e.getMessage());
    }

    /** 将新的日志信息广播出去 */
    public static void castLogMessage(LogTrace logTrace) throws IOException {
        ObjectMapper objectMapper = new ObjectMapper();
        String message = objectMapper.writeValueAsString(logTrace);
        synchronized (CONNECT) {
            for (ViewEndPoint endPoint : CONNECT) {
                // 偏移量自增
                endPoint.incrementOffset();
                // 判断是否发送发送会话
                if (endPoint.needSend(logTrace)) {
                    endPoint.getSession().getBasicRemote().sendText(message);
                }
            }
        }
    }
}
