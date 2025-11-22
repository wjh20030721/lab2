package com.editor.application;

import com.editor.common.config.AppConfig;
import com.editor.common.exception.EditorException;
import com.editor.domain.Workspace;
import com.editor.infrastructure.memento.WorkspaceMemento;
import com.editor.infrastructure.persistence.LocalFileRepository;
import com.editor.interfaces.Command;

import java.io.File;
import java.util.Scanner;
import java.util.stream.Collectors;

public class Main {
    public static void main(String[] args) {
        // 1. 组装应用
        Workspace workspace = Workspace.getInstance();
        LocalFileRepository repo = new LocalFileRepository();
        CommandFactory factory = new CommandFactory(workspace, repo);

        // 2. 恢复上次关闭的工作区状态 (Memento)
        if (new File(AppConfig.MEMENTO_FILE).exists()) {
            WorkspaceMemento m = WorkspaceMemento.load(AppConfig.MEMENTO_FILE);
            if (m != null) {
                for (String f : m.openFiles) factory.createCommand("load " + f).execute();
                if (m.activeFile != null) workspace.setActive(m.activeFile);
            }
        }

        Scanner scanner = new Scanner(System.in);
        System.out.println("Editor Ready. Type 'exit' to quit.");

        // 3. 事件循环
        while (true) {
            System.out.print("> ");
            String input = scanner.nextLine();
            if (input.trim().isEmpty()) continue;

            try {
                //注：这里并不是说createCommand命令中未得到的命令就直接退出
                //这里使用的设计是将所有命令包括exit命令都交给工厂去创建，如果工厂返回null则表示exit命令
                //如果未获取到命令则会抛出异常并被捕获
                Command cmd = factory.createCommand(input);
                if (cmd == null) { // Exit
                    handleExit(workspace);
                    break;
                }
                cmd.execute();
            } catch (EditorException e) {
                System.out.println("Error: " + e.getMessage());
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        scanner.close();
    }

    private static void handleExit(Workspace ws) {
        WorkspaceMemento m = new WorkspaceMemento();
        m.openFiles = ws.getAllEditors().stream().map(e -> e.getPath()).collect(Collectors.toList());
        if (ws.getActiveEditor() != null) m.activeFile = ws.getActiveEditor().getPath();
        WorkspaceMemento.save(AppConfig.MEMENTO_FILE, m);
        System.out.println("State saved. Bye!");
    }
}