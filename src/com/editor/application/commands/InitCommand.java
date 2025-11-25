package com.editor.application.commands;

import com.editor.interfaces.*;
import com.editor.domain.*;
import com.editor.infrastructure.log.FileLogger;
import java.util.ArrayList;

public class InitCommand implements Command {
    private Workspace workspace;
    private String path;
    private boolean withLog;

    public InitCommand(Workspace ws, String path, boolean withLog) {
        this.workspace = ws;
        this.path = path;
        this.withLog = withLog;
    }

    @Override
    public void execute() {
        // 创建新缓冲区，标记为 modified
        TextEditor editor = new TextEditor(path, new ArrayList<>());

        if (withLog) {
            editor.append("# log");
            editor.attach(new FileLogger(path));
        }

        editor.setModified(true); // 新建的未保存
        workspace.register(editor);
        System.out.println("Initialized new buffer: " + path);
    }
}