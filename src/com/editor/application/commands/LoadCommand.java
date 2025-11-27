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
        // 1. 读取文件
        if (!repo.exists(path)) repo.createEmpty(path);
        List<String> lines = repo.readLines(path);

        // 2. 创建编辑器 (Text/XML 判断逻辑)
        Editor editor;
        if (path.endsWith(".xml")) {
            editor = new XmlEditor(path, lines);
        } else {
            editor = new TextEditor(path, lines);
        }

        // 3. [修改] 配置 Log，传入第一行作为配置参数
        if (!lines.isEmpty() && lines.get(0).trim().startsWith("# log")) {
            // 传入 lines.get(0) 以便 Logger 解析 -e 参数
            editor.attach(new FileLogger(path, lines.get(0)));
        }

        // 在挂载完 Logger 后，立即触发加载日志
        editor.onLoad();

        // 4. 注册
        workspace.register(editor);
        System.out.println("Loaded: " + path);
    }
}