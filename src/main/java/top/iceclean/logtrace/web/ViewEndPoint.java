package top.iceclean.logtrace.web;

import com.fasterxml.jackson.databind.ObjectMapper;
import javafx.util.Pair;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import top.iceclean.logtrace.bean.LogTrace;
import top.iceclean.logtrace.config.LogTraceConfig;
import top.iceclean.logtrace.db.LogHandler;
import top.iceclean.logtrace.spi.LogFormat;

import javax.websocket.*;
import javax.websocket.server.ServerEndpoint;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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

    /** 日志操作 */
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

    @ResponseBody
    @PostMapping("/log/code")
    public Object getCode(@RequestBody Map<String, String> className) {
        try {
            // 响应实体
            Map<String, Object> response = new HashMap<>(4);
            response.put("className", className.get("className"));
            response.put("line", -1);
            response.put("method", "");
            response.put("code", "");

            // 取出信息，格式如 LogFormat.java:279 getOtherLog
            String[] info = className.get("className").split(":");
            if (info.length == 2) {
                String[] lineAndMethod = info[1].split(" ");
                response.put("className", info[0]);
                response.put("line", Integer.parseInt(lineAndMethod[0]));
                response.put("method", lineAndMethod[1]);

                // 获取字节码文件目录
                File file = new File("src/main/java");
                List<Pair<Long, String>> classNameList = LogFormat.findFile(file, info[0]);
                if (classNameList.size() > 0) {
                    // 读取类内容（置获取到第一个找到的）
                    byte[] content = new byte[classNameList.get(0).getKey().intValue()];
                    String path = classNameList.get(0).getValue();
                    InputStream inputStream = new FileInputStream(path);
                    int read = inputStream.read(content);
                    inputStream.close();
                    response.put("code", new String(content, StandardCharsets.UTF_8));
                }
            }

            return response;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

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
