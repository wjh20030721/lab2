package com.editor.application.commands;

import com.editor.domain.*;
import com.editor.infrastructure.log.FileLogger;
import com.editor.interfaces.*;

import java.util.List;

public class LoadCommand implements Command {
    private Workspace workspace;
    private FileRepository repo;
    private String path;

    public LoadCommand(Workspace workspace, FileRepository repo, String path) {
        this.workspace = workspace;
        this.repo = repo;
        this.path = path;
    }

    @Override
    public void execute() {
        // 1. Infra 读数据
        if (!repo.exists(path)) repo.createEmpty(path);
        List<String> lines = repo.readLines(path);

        // 2. 组装 Domain 对象
        // 文件加载之后产生一个 TextEditor 实例作为一个编辑器页面，管理对这个文件的编辑
        TextEditor editor = new TextEditor(path, lines);

        // 3. 配置 Observer (Log)，将日志观察者注册到编辑器
        // 但这个不意味着
        if (!lines.isEmpty() && lines.get(0).trim().equals("# log")) {
            editor.attach(new FileLogger(path));
        }

        // 4. 注册到 Workspace
        workspace.register(editor);
        System.out.println("Loaded: " + path);
    }
}