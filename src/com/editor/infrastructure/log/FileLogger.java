package com.editor.infrastructure.log;

import com.editor.common.config.AppConfig;
import com.editor.interfaces.EditorObserver;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

public class FileLogger implements EditorObserver {
    private String logFilePath;
    // [新增] 存储需要屏蔽的命令
    private Set<String> excludedCommands = new HashSet<>();

    // [修改] 构造函数增加 configLine 参数
    public FileLogger(String sourceFile, String configLine) {
        this.logFilePath = "." + sourceFile + AppConfig.LOG_SUFFIX;

        // 解析配置
        parseConfig(configLine);

        write("session start at " + LocalDateTime.now());
    }

    // 保持兼容性的构造函数 (默认无配置)
    public FileLogger(String sourceFile) {
        this(sourceFile, null);
    }

    // [新增] 解析逻辑
    // 格式示例: # log -e append -e delete
    private void parseConfig(String configLine) {
        if (configLine == null || !configLine.startsWith("# log")) {
            return;
        }

        String[] parts = configLine.split("\\s+");
        for (int i = 0; i < parts.length; i++) {
            // 找到 -e 参数
            if ("-e".equals(parts[i])) {
                if (i + 1 < parts.length) {
                    String cmdToExclude = parts[i + 1];
                    excludedCommands.add(cmdToExclude);
                    i++; // 跳过参数值
                } else {
                    System.err.println("Log Warning: Missing argument for -e in config.");
                }
            }
        }

        if (!excludedCommands.isEmpty()) {
            System.out.println("Log filtering enabled for: " + excludedCommands);
        }
    }

    @Override
    public void update(String event) {
        // [新增] 过滤逻辑
        // event 通常是 "append" 或 "insert 1:2 ..."
        // 我们取第一个单词作为命令名进行比对
        String cmdName = event.split("\\s+")[0];

        if (excludedCommands.contains(cmdName)) {
            return; // 命中屏蔽列表，不记录
        }

        write(LocalDateTime.now() + " " + event);
    }

    private void write(String content) {
        try {
            Files.write(Paths.get(logFilePath),
                    Collections.singletonList(content),
                    StandardOpenOption.CREATE, StandardOpenOption.APPEND);
        } catch (IOException e) {
            // [Lab2 要求] 日志写入失败仅提示警告，不阻断流程
            System.err.println("Log Warning: Write failed - " + e.getMessage());
        }
    }
}