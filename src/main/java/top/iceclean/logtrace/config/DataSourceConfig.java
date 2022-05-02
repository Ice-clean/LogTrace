package top.iceclean.logtrace.config;

import com.alibaba.druid.pool.DruidDataSource;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.DependsOn;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;

/**
 * 数据源的配置（目前只支持 druid）
 * 依赖于项目配置 LogTraceConfig
 * 且在数据库输出开启时才生效
 * @author : Ice'Clean
 * @date : 2022-04-30
 */
@Component
@DependsOn("logTraceConfig")
@ConditionalOnProperty(name = "log-trace.database.enabled", havingValue = "true")
public class DataSourceConfig {
    @Bean
    public DataSource dataSource() {
        DruidDataSource druidDataSource = new DruidDataSource();
        druidDataSource.setDriverClassName(LogTraceConfig.Database.driver);
        druidDataSource.setUrl(LogTraceConfig.Database.url);
        druidDataSource.setUsername(LogTraceConfig.Database.username);
        druidDataSource.setPassword(LogTraceConfig.Database.password);
        return druidDataSource;
    }
}
