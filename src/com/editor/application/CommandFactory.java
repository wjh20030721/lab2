package com.editor.application;

import com.editor.application.commands.*;
import com.editor.domain.*;
import com.editor.domain.xml.*;
import com.editor.infrastructure.spellcheck.LanguageToolHttpAdapter;
import com.editor.infrastructure.spellcheck.MockSpellChecker;
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

    // 可以在工厂里持有一个 SpellChecker 实例（单例或每次new都可以）
//    private SpellChecker spellChecker = new MockSpellChecker();
    private SpellChecker spellChecker = new LanguageToolHttpAdapter();
    public CommandFactory(Workspace workspace, FileRepository repo,Scanner scanner) {
        this.workspace = workspace;
        this.repo = repo;
        this.scanner = scanner;
    }

    public Command createCommand(String inputLine) {
        List<String> parts = parseArgs(inputLine); // 解析参数
        if (parts.isEmpty()) return null;

        String action = parts.get(0);
        // 获取通用接口 Editor，而不是强转为 TextEditor
        Editor active = workspace.getActiveEditor();
        switch (action) {
            // --- Workspace Commands ---
            case "load":
                return new LoadCommand(workspace, repo, parts.get(1));
            case "init":
                // 假设输入是: init xml with-log
                // parts.get(1) 就是 "xml"
                boolean withLog = parts.size() > 2 && "with-log".equals(parts.get(2));
                return new InitCommand(workspace, repo ,parts.get(1), withLog);
            case "save":
                // 场景 1: save all
                if (parts.size() > 1 && "all".equals(parts.get(1))) {
                    return () -> workspace.getAllEditors().forEach(e -> new SaveCommand(e, repo).execute());
                }

                // 场景 2: save <arg> (可能是目标文件名，也可能是另存为路径)
                if (parts.size() > 1) {
                    String arg = parts.get(1);

                    // A. 尝试在工作区查找名为 arg 的文件 (Lab1 逻辑)
                    Editor targetEditor = workspace.getAllEditors().stream()
                            .filter(e -> e.getPath().equals(arg))
                            .findFirst()
                            .orElse(null);

                    if (targetEditor != null) {
                        // 找到了 -> 保存那个文件
                        return new SaveCommand(targetEditor, repo);
                    } else {
                        // B. 没找到 -> 这是一个“另存为”操作 (Lab2 新逻辑)
                        // 将当前活动文件保存到 arg 指定的路径
                        if (active == null) throw new EditorException("No active file to save as " + arg);

                        // 返回一个匿名命令执行“另存为”
                        return () -> {
                            String oldPath = active.getPath();
                            // 1. 写入新路径
                            repo.writeLines(arg, active.getContent());
                            // 2. 标记未修改
                            active.setModified(false);
                            // 3. 在工作区重命名 (关键！)
                            workspace.renameEditor(oldPath, arg);
                            System.out.println("Saved as: " + arg);
                        };
                    }
                }
                // 场景 3: save (无参数，保存当前)
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
                return null;

            // --- Edit Commands ---
            case "append":
                return requireTextEditor(active, e -> new AppendCommand(e, parts.get(1)));
            case "insert":
                int[] posIns = parsePos(parts.get(1));
                return requireTextEditor(active, e -> new InsertCommand(e, posIns[0], posIns[1], parts.get(2)));
            // --- Delete (多态分发：完全替换旧代码) ---
            case "delete":
                if (active instanceof TextEditor) {
                    // Lab1 原有逻辑：删除文本范围
                    if (parts.size() < 3) throw new EditorException("参数格式错误: delete <line:col> <len>");

                    // 解析位置和长度
                    int[] posDel = parsePos(parts.get(1));
                    int len = Integer.parseInt(parts.get(2));

                    // 返回文本删除命令
                    return new DeleteCommand((TextEditor)active, posDel[0], posDel[1], len);
                }
                else if (active instanceof XmlEditor) {
                    // Lab2 新增逻辑：删除 XML 元素
                    if (parts.size() < 2) throw new EditorException("参数格式错误: delete <elementId>");

                    // 获取 ID
                    String elementId = parts.get(1);

                    // 返回元素删除命令
                    return new DeleteElementCommand((XmlEditor)active, elementId);
                }
                return null;
            case "replace":
                int[] posRep = parsePos(parts.get(1));
                int lenRep = Integer.parseInt(parts.get(2));
                return requireTextEditor(active, e -> new ReplaceCommand(e, posRep[0], posRep[1], lenRep, parts.get(3)));
            case "show":
                Integer start = null, end = null;
                if (parts.size() > 1) {
                    String[] range = parts.get(1).split(":");
                    start = Integer.parseInt(range[0]);
                    if (range.length > 1) end = Integer.parseInt(range[1]);
                }
                final Integer s = start, e = end; // lambda final capture
                return requireTextEditor(active, editor -> new ShowCommand(editor, s, e));
            case "undo":
                return () -> {
                    if (active instanceof TextEditor) ((TextEditor)active).undo();
                    else if (active instanceof XmlEditor) ((XmlEditor)active).undo();
                };
            case "redo":
                return () -> {
                    if (active instanceof TextEditor) ((TextEditor)active).redo();
                    else if (active instanceof XmlEditor) ((XmlEditor)active).redo();
                };

            // --- Log Commands ---
            case "log-on":
                // LogControlCommand 内部如果只用了 Editor 接口方法(attach/detach)则不需要强转
                // 如果用了 TextEditor 特有的，则需要强转。根据你的代码，它依赖 Editor 接口方法，
                // 但原构造函数接收 TextEditor，建议修改 LogControlCommand 接收 Editor。
                return new LogControlCommand(active, "on");
            case "log-off":
                return new LogControlCommand(active, "off");
            case "log-show":
                return new LogControlCommand(active, "show");
            case "edit":
                if (parts.size() < 2) throw new EditorException("Missing filename");
                String targetFile = parts.get(1);
                return () -> {
                    boolean exists = workspace.getAllEditors().stream()
                            .anyMatch(ed -> ed.getPath().equals(targetFile));
                    if (!exists) {
                        System.out.println("File not open: " + targetFile);
                    } else {
                        workspace.setActive(targetFile);
                        System.out.println("Switched to: " + targetFile);
                    }
                };

                // --- XML Commands ---
            case "xml-tree":
                XmlEditor targetXmlEditor = null; // 声明最终要用的变量

                // 情况 1: 用户指定了文件名 "xml-tree test.xml"
                if (parts.size() > 1) {
                    String targetPath = parts.get(1);

                    // --- 修改点：变量名从 'e' 改为 'foundEditor' ---
                    Editor foundEditor = workspace.getAllEditors().stream()
                            .filter(ed -> ed.getPath().equals(targetPath))
                            .findFirst()
                            .orElse(null);

                    if (foundEditor == null) throw new EditorException("File not open: " + targetPath);
                    if (!(foundEditor instanceof XmlEditor)) throw new EditorException("Not an XML file: " + targetPath);
                    targetXmlEditor = (XmlEditor) foundEditor;
                }
                // 情况 2: 没指定，默认用当前激活的
                else {
                    if (active instanceof XmlEditor) {
                        targetXmlEditor = (XmlEditor) active;
                    } else {
                        throw new EditorException("Current file is not XML.");
                    }
                }
                return new XmlTreeCommand(targetXmlEditor);
            case "insert-before":
                // 参数检查: 命令名 + 3个必填参数 = 4 parts
                if (parts.size() < 4) {
                    throw new EditorException("参数不足: insert-before <tag> <newId> <targetId> [text]");
                }

                // 使用辅助方法确保当前是 XmlEditor
                return requireXmlEditor(active, editor -> {
                    String tag = parts.get(1);
                    String newId = parts.get(2);
                    String targetId = parts.get(3);
                    // 文本是可选的第 4 个参数
                    String text = parts.size() > 4 ? parts.get(4) : "";

                    return new InsertBeforeCommand(editor, tag, newId, targetId, text);
                });
            case "append-child":
                // 参数检查: 命令名 + 3个必填参数 = 4 parts
                if (parts.size() < 4) {
                    throw new EditorException("参数不足: append-child <tag> <newId> <parentId> [text]");
                }

                return requireXmlEditor(active, editor -> {
                    String tag = parts.get(1);
                    String newId = parts.get(2);
                    String parentId = parts.get(3);
                    // 文本是可选的第 4 个参数
                    String text = parts.size() > 4 ? parts.get(4) : "";

                    return new AppendChildCommand(editor, tag, newId, parentId, text);
                });

            // --- XML Edit Commands (新增) ---
            // --- XML Edit Commands ---
            case "edit-id":
                if (parts.size() < 3) throw new EditorException("Args: edit-id <oldId> <newId>");
                return requireXmlEditor(active, xmlEditor -> new EditIdCommand(xmlEditor, parts.get(1), parts.get(2)));

            case "edit-text":
                if (parts.size() < 2) throw new EditorException("Args: edit-text <elementId> [text]");
                String txt = parts.size() > 2 ? parts.get(2) : "";
                return requireXmlEditor(active, xmlEditor -> new EditTextCommand(xmlEditor, parts.get(1), txt));
            // --- Spell Check ---
            case "spell-check":
                // 支持 text 和 xml，直接传 active editor 即可
                if (active == null) throw new EditorException("No active editor to check.");
                return new SpellCheckCommand(active, spellChecker);
            default:
                throw new EditorException("Unknown command: " + action);
        }
    }

    // --- 辅助方法：类型检查 ---

    // 定义一个简单的函数式接口，用于创建命令
    private interface CommandCreator<T> {
        Command create(T editor);
    }


    private Command requireTextEditor(Editor active, CommandCreator<TextEditor> creator) {
        if (active instanceof TextEditor) {
            return creator.create((TextEditor) active);
        }
        throw new EditorException("Command not supported for current editor type (Text only).");
    }

    private Command requireXmlEditor(Editor active, CommandCreator<XmlEditor> creator) {
        if (active instanceof XmlEditor) {
            return creator.create((XmlEditor) active);
        }
        throw new EditorException("Command not supported for current editor type (XML only).");
    }

    private int[] parsePos(String arg) {
        String[] s = arg.split(":");
        return new int[]{Integer.parseInt(s[0]), Integer.parseInt(s[1])};
    }

    private List<String> parseArgs(String input) {
        List<String> list = new ArrayList<>();
        Matcher m = Pattern.compile("([^\"\\s]\\S*|\".+?\")\\s*").matcher(input);
        while (m.find()) {
            String s = m.group(1).replace("\"", "");
            s = s.replace("\\n", "\n");
            list.add(s);
        }
        return list;
    }
}