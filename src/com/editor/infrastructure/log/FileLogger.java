package com.editor.infrastructure.log;

import com.editor.common.config.AppConfig;
import com.editor.interfaces.EditorObserver;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.time.LocalDateTime;
import java.util.Collections;

public class FileLogger implements EditorObserver {
    private String logFilePath;

    public FileLogger(String sourceFile) {
        // 自动生成 log 文件名
        this.logFilePath = "." + sourceFile + AppConfig.LOG_SUFFIX;
        write("session start at " + LocalDateTime.now());
    }

    @Override
    public void update(String event) {
        write(LocalDateTime.now() + " " + event);
    }

    private void write(String content) {
        try {
            Files.write(Paths.get(logFilePath),
                    Collections.singletonList(content),
                    StandardOpenOption.CREATE, StandardOpenOption.APPEND);
        } catch (IOException e) {
            System.err.println("Log Error: " + e.getMessage());
        }
    }
}