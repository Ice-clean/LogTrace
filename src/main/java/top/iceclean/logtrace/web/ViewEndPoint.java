package top.iceclean.logtrace.web;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import top.iceclean.logtrace.bean.LogTrace;
import top.iceclean.logtrace.config.LogTraceConfig;
import top.iceclean.logtrace.db.LogHandler;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.ResponseBody;

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
    private static final ArrayList<Session> CONNECT = new ArrayList<>();
    private static int incrementNum = 0;

    /** 当前会话 */
    private Session session;

    /** 日志操作 */
    @Autowired
    private LogHandler logHandler;

    @GetMapping("/log")
    public String logView() {
        return "log.html";
    }

    @ResponseBody
    @GetMapping("/log/{level}/{type}/{last}/{max}")
    public Object getLogMessage(@PathVariable String level, @PathVariable String type,
                                @PathVariable int last, @PathVariable int max) {
        if (LogTraceConfig.database.isEnabled()) {
            return logHandler.getLogTraceList(level, type, last, max);
        }
        return null;
    }

//    @GetMapping("/log/read/{logId}")
//    public Object readLogMessage() {
//
//    }

    @OnOpen
    public void onOpen(Session session) {
        incrementNum++;
        log.info("新日志请求连接 id=" + incrementNum);
        this.session = session;
        CONNECT.add(session);
    }

    @OnMessage
    public void onMessage(String message) {
        log.info("请求连接 id=" + incrementNum + " 发来数据：" + message);
    }

    @OnClose
    public void onClose() {
        CONNECT.remove(session);
        log.info("请求连接 id=" + incrementNum + " 断开");
    }

    @OnError
    public void onError(Throwable e) {
        CONNECT.remove(session);
        log.info("捕获到异常" + e.getMessage());
    }

    /** 将新的日志信息广播出去 */
    public static void castLogMessage(LogTrace logTrace) throws IOException {
        ObjectMapper objectMapper = new ObjectMapper();
        String message = objectMapper.writeValueAsString(logTrace);
        for (Session session : CONNECT) {
            session.getBasicRemote().sendText(message);
        }
    }
}
