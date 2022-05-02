-- 日志头表
create table t_log_head(
    head_id     int       auto_increment comment '日志头 ID' primary key,
    log_level   char(10)                 comment '日志级别',
    log_thread  varchar(255)             comment '日志线程',
    log_site    varchar(255)             comment '日志产生位置',
    log_mode    varchar(20)              comment '日志模式',
    log_type    varchar(20)              comment '日志类型',
    log_url     varchar(255)             comment '请求路径',
    log_parent  varchar(255)             comment '所属父类',
    log_method  varchar(255)             comment '所属方法',
    log_params  varchar(255)             comment '传入参数',
    log_return  varchar(255)             comment '请求返回',
    log_stack   text                     comment '日志的堆栈调用信息',
    log_read    int       default 0      comment '是否已读',
    create_time timestamp default CURRENT_TIMESTAMP not null comment '创建时间'
);

-- 日志信息表
create table t_log_message(
    message_id  int       auto_increment comment '日志信息 ID' primary key,
    head_id     int                      comment '日志头 ID',
    log_level   char(10)                 comment '日志级别',
    log_site    varchar(255)             comment '日志产生位置',
    log_message text                     comment '日志信息',
    create_time timestamp default CURRENT_TIMESTAMP not null comment '创建时间'
)
