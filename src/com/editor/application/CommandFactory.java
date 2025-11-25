package com.editor.application;

import com.editor.application.commands.*;
import com.editor.domain.*;
import com.editor.interfaces.*;
import com.editor.common.exception.EditorException;

import java.util.Scanner;
import java.util.regex.*;
import java.util.ArrayList;
import java.util.List;

public class CommandFactory {
    private Workspace workspace;
    private FileRepository repo;
    private Scanner scanner;

    public CommandFactory(Workspace workspace, FileRepository repo,Scanner scanner) {
        this.workspace = workspace;
        this.repo = repo;
        this.scanner = scanner;
    }

    public Command createCommand(String inputLine) {
        List<String> parts = parseArgs(inputLine); // 解析参数
        if (parts.isEmpty()) return null;

        String action = parts.get(0);
        TextEditor active = (TextEditor) workspace.getActiveEditor();

        switch (action) {
            // --- Workspace Commands ---
            case "load":
                return new LoadCommand(workspace, repo, parts.get(1));
            case "init":
                boolean withLog = parts.size() > 2 && "with-log".equals(parts.get(2));
                return new InitCommand(workspace, parts.get(1), withLog);
            case "save":
                // 处理 save all
                if (parts.size() > 1 && "all".equals(parts.get(1))) {
                    return () -> workspace.getAllEditors().forEach(e ->
                            new SaveCommand(e, repo).execute());
                }
                // 处理 save <filename>
                if (parts.size() > 1) {
                    String targetName = parts.get(1);
                    // 在工作区查找指定名称的编辑器
                    TextEditor targetEditor = (TextEditor) workspace.getAllEditors().stream()
                            .filter(e -> e.getPath().equals(targetName))
                            .findFirst()
                            .orElse(null);

                    if (targetEditor == null) {
                        // 如果没找到（没 load），抛出异常或打印错误
                        throw new EditorException("File not found in workspace: " + targetName);
                    }
                    return new SaveCommand(targetEditor, repo);
                }
                // 保存当前活动文件
                if (active == null) throw new EditorException("No active file to save");
                return new SaveCommand(active, repo);
            case "close":
                String fileToClose = parts.size() > 1 ? parts.get(1) : (active != null ? active.getPath() : null);
                if (fileToClose == null) throw new EditorException("No file to close");
                return new CloseCommand(workspace, repo, fileToClose, scanner);
            case "editor-list":
                return new EditorListCommand(workspace);
            case "dir-tree":
                return new DirTreeCommand(parts.size() > 1 ? parts.get(1) : null);
            case "exit":
                return null; // Exit signal

            // --- Edit Commands ---
            case "append":
                return new AppendCommand(active, parts.get(1));
            case "insert":
                // insert 1:4 "text"
                int[] posIns = parsePos(parts.get(1)); //
                return new InsertCommand(active, posIns[0], posIns[1], parts.get(2));
            case "delete":
                // delete 1:4 5
                int[] posDel = parsePos(parts.get(1));
                int len = Integer.parseInt(parts.get(2));
                return new DeleteCommand(active, posDel[0], posDel[1], len);
            case "replace":
                // replace 1:4 5 "text"
                int[] posRep = parsePos(parts.get(1));
                int lenRep = Integer.parseInt(parts.get(2));
                return new ReplaceCommand(active, posRep[0], posRep[1], lenRep, parts.get(3));
            case "show":
                // show 1:5
                Integer start = null, end = null;
                if (parts.size() > 1) {
                    String[] range = parts.get(1).split(":");
                    start = Integer.parseInt(range[0]);
                    if (range.length > 1) end = Integer.parseInt(range[1]);
                }
                return new ShowCommand(active, start, end);
            case "undo":
                return () -> { if(active!=null) active.undo(); };
            case "redo":
                return () -> { if(active!=null) active.redo(); };

            // --- Log Commands ---
            case "log-on":
                return new LogControlCommand(active, "on");
            case "log-off":
                return new LogControlCommand(active, "off");
            case "log-show":
                return new LogControlCommand(active, "show");
            case "edit":
                if (parts.size() < 2) throw new EditorException("Missing filename");
                String targetFile = parts.get(1);

                // 返回一个匿名 Command (或者 Lambda)
                return () -> {
                    // 1. 检查文件是否已打开 (复用 Workspace 查找逻辑，或者直接利用 setActive 的静默失败特性)
                    // 但为了打印错误提示，我们需要先检查
                    boolean exists = workspace.getAllEditors().stream()
                            .anyMatch(e -> e.getPath().equals(targetFile));

                    if (!exists) {
                        System.out.println("File not open: " + targetFile);
                    } else {
                        workspace.setActive(targetFile);
                        System.out.println("Switched to: " + targetFile);
                    }
                };
            default:
                throw new EditorException("Unknown command: " + action);
        }
    }

    // 解析 "line:col" -> [line, col]
    private int[] parsePos(String arg) {
        String[] s = arg.split(":");
        return new int[]{Integer.parseInt(s[0]), Integer.parseInt(s[1])};
    }

    // 解析命令行参数，支持双引号 (正则)
    private List<String> parseArgs(String input) {
        List<String> list = new ArrayList<>();
        Matcher m = Pattern.compile("([^\"]\\S*|\".+?\")\\s*").matcher(input);
        while (m.find()) {
            String s = m.group(1).replace("\"", ""); // 去除引号

            // [新增] 处理转义字符：把用户输入的字面量 "\n" 变成真正的换行符
            s = s.replace("\\n", "\n");
            // 如果还需要支持 tab，可以加: s = s.replace("\\t", "\t");

            list.add(s);
        }
        return list;
    }
}