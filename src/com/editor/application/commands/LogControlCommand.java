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
            case "on":
                // 使用接口方法：去重挂载
                editor.attachUnique(new FileLogger(editor.getPath()));
                System.out.println("Logging enabled.");
                break;
            case "off":
                // 使用接口方法：按类型移除 FileLogger
                editor.detach(FileLogger.class);
                System.out.println("Logging disabled.");
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