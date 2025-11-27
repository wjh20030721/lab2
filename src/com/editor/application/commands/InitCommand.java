package com.editor.application.commands;

import com.editor.interfaces.*;
import com.editor.domain.*;
import com.editor.infrastructure.log.FileLogger;
import java.util.ArrayList;

public class InitCommand implements Command {
    private Workspace workspace;
    private String arg; // 可能是类型 "xml"/"text"，也可能是文件名 "test.xml"
    private boolean withLog;
    private FileRepository repo;

    public InitCommand(Workspace ws, FileRepository repo, String arg, boolean withLog) {
        this.workspace = ws;
        this.repo = repo;
        this.arg = arg;
        this.withLog = withLog;
    }

    @Override
    public void execute() {
        String path;
        boolean isXml;

        // [改进逻辑] 智能判断参数是“类型”还是“文件名”
        if ("xml".equalsIgnoreCase(arg)) {
            // 场景 1: init xml -> 自动命名，类型 XML
            path = findAvailableName("Untitled", ".xml");
            isXml = true;
        } else if ("text".equalsIgnoreCase(arg)) {
            // 场景 2: init text -> 自动命名，类型 Text
            path = findAvailableName("Untitled", ".txt");
            isXml = false;
        } else {
            // 场景 3: init filename.xml -> 指定文件名，根据后缀判断类型
            path = arg;
            isXml = arg.toLowerCase().endsWith(".xml");
        }

        // 检查文件是否已存在（针对场景 3）
        if (workspace.getAllEditors().stream().anyMatch(e -> e.getPath().equals(path))) {
            System.out.println("File already open: " + path);
            return;
        }

        Editor editor;
        ArrayList<String> lines = new ArrayList<>();

        if (isXml) {
            // 初始化 XML
            if (withLog) {
                lines.add("# log");
            } else {
                lines.add("<?xml version=\"1.0\" encoding=\"UTF-8\"?>");
            }
            lines.add("<root id=\"root\">");
            lines.add("</root>");
            editor = new XmlEditor(path, lines);
        } else {
            // 初始化 Text
            if (withLog) lines.add("# log");
            editor = new TextEditor(path, lines);
        }

        editor.setModified(true);
        if (withLog) {
            // [修改] 如果是 XML 且没有第一行配置（标准XML头），则传 null (无过滤)
            // 如果是 Text 且写入了 "# log"，则传入该字符串
            String config = null;
            if (!lines.isEmpty() && lines.get(0).startsWith("# log")) {
                config = lines.get(0);
            }
            editor.attach(new FileLogger(path, config));
        }

        workspace.register(editor);

        // 打印更友好的提示信息
        System.out.println("Initialized new buffer: " + path + " (" + (isXml ? "XML" : "Text") + ")");
    }

    private String findAvailableName(String prefix, String suffix) {
        int i = 1;
        while (true) {
            String candidate = prefix + "-" + i + suffix;
            boolean existsOnDisk = repo.exists(candidate);
            boolean isOpen = workspace.getAllEditors().stream()
                    .anyMatch(e -> e.getPath().equals(candidate));
            if (!existsOnDisk && !isOpen) return candidate;
            i++;
        }
    }
}