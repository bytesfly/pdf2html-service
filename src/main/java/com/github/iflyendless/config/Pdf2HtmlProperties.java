package com.github.iflyendless.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

@Data
@ConfigurationProperties(prefix = "pdf2html")
public class Pdf2HtmlProperties {

    private String command;

    private String workDir;

    private Duration commandTimeout;

    // 同时启动的最大子进程数, 需要根据系统的性能合理设置
    private int maxProcess;
}
