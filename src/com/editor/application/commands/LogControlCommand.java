package com.editor.application.commands;

import com.editor.interfaces.*;
import com.editor.domain.*;
import com.editor.infrastructure.log.FileLogger;
import java.io.File;
import java.nio.file.Files;

public class LogControlCommand implements Command {
    private TextEditor editor;
    private String action; // "on", "off", "show"

    public LogControlCommand(TextEditor editor, String action) {
        this.editor = editor;
        this.action = action;
    }

    @Override
    public void execute() {
        if (editor == null) { System.out.println("No active editor"); return; }

        switch (action) {
            case "on": //
                editor.attach(new FileLogger(editor.getPath()));
                System.out.println("Logging enabled for " + editor.getPath());
                break;
            case "off": //
                // 实际需要找到对应的 Logger 对象并 detach。
                // 简化实现：清空所有 observer 或者在 Editor 中实现按类型移除
                // 这里假设我们简单地把 Logger 作为唯一 Observer 移除
                // (更完善的实现需要在 Editor 里管理 Observer Map)
                System.out.println("Logging disabled (implementation pending detach logic)");
                break;
            case "show": //
                String logPath = "." + editor.getPath() + ".log";
                try {
                    File f = new File(logPath);
                    if(f.exists()) Files.lines(f.toPath()).forEach(System.out::println);
                    else System.out.println("No log file found.");
                } catch (Exception e) { e.printStackTrace(); }
                break;
        }
    }
}