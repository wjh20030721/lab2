package com.editor.application.commands;

import com.editor.interfaces.*;
import com.editor.infrastructure.log.FileLogger;
import java.io.File;
import java.nio.file.Files;
import java.util.List;

public class LogControlCommand implements Command {
    // 使用接口类型
    private Editor editor;
    private String action; // "on", "off", "show"

    // 构造函数接收 Editor 接口
    public LogControlCommand(Editor editor, String action) {
        this.editor = editor;
        this.action = action;
    }

    @Override
    public void execute() {
        if (editor == null) { System.out.println("No active editor"); return; }

        switch (action) {
            case "on":
                // [修改] 尝试从编辑器内容中获取第一行配置
                String config = null;
                List<String> content = editor.getContent();
                if (!content.isEmpty() && content.get(0).startsWith("# log")) {
                    config = content.get(0);
                }
                // 接口方法：去重挂载
                editor.attachUnique(new FileLogger(editor.getPath(), config));
                System.out.println("Logging enabled.");
                break;
            case "off":
                // 接口方法：按类型移除
                editor.detach(FileLogger.class);
                System.out.println("Logging disabled.");
                break;
            case "show":
                // 查看日志文件内容，这是文件系统操作，和编辑器类型无关，可以直接用
                String logPath = "." + editor.getPath() + ".log";
                try {
                    File f = new File(logPath);
                    if(f.exists()) {
                        System.out.println("--- Log Content ---");
                        Files.lines(f.toPath()).forEach(System.out::println);
                        System.out.println("-------------------");
                    } else {
                        System.out.println("No log file found.");
                    }
                } catch (Exception e) { e.printStackTrace(); }
                break;
        }
    }
}