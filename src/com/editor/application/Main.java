package com.editor.application;

import com.editor.application.commands.SaveCommand;
import com.editor.common.config.AppConfig;
import com.editor.common.exception.EditorException;
import com.editor.domain.TextEditor;
import com.editor.domain.Workspace;
import com.editor.domain.statistics.TimeTracker;
import com.editor.infrastructure.memento.WorkspaceMemento;
import com.editor.infrastructure.persistence.LocalFileRepository;
import com.editor.interfaces.Command;
import com.editor.interfaces.Editor;
import com.editor.interfaces.FileRepository;

import java.io.File;
import java.util.Scanner;
import java.util.stream.Collectors;

public class Main {
    public static void main(String[] args) {
        // 1. 组装应用
        // wjh
        Workspace workspace = Workspace.getInstance();
        LocalFileRepository repo = new LocalFileRepository();
        Scanner scanner = new Scanner(System.in);
        CommandFactory factory = new CommandFactory(workspace, repo,scanner);

        // 核心解耦点：注册统计模块观察者
        // 这样 Workspace 依然不知道 TimeTracker 的存在，只知道发通知
        workspace.addObserver(TimeTracker.getInstance());

        // 2. 恢复上次关闭的工作区状态 (Memento)
        if (new File(AppConfig.MEMENTO_FILE).exists()) {
            WorkspaceMemento m = WorkspaceMemento.load(AppConfig.MEMENTO_FILE);
            if (m != null) {
                for (String f : m.openFiles) factory.createCommand("load " + f).execute();
                if (m.activeFile != null) workspace.setActive(m.activeFile);
            }
        }
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
                // 1. 接收到退出信号 (这里沿用你现有的 null 作为退出信号的设计)
                if (cmd == null) {
                    // 2. [关键] 在退出前，由 Main 负责处理“善后工作”
                    performExitChecks(workspace, repo, scanner);

                    // 3. 真正的退出动作
                    handleExit(workspace); // 保存 memento
                    break; // 打断循环，程序结束
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

    // 新增：退出检查逻辑
    // 这里的逻辑是：遍历所有文件 -> 发现未保存 -> 问用户 -> 用户定 -> (保存/跳过) -> 继续下一个 -> 最后一定返回
    // 新增：退出检查逻辑
    private static void performExitChecks(Workspace workspace, FileRepository repo, Scanner scanner) {
        for (Editor e : workspace.getAllEditors()) {
            // [修复] 删除这行强制转换: TextEditor editor = (TextEditor) e;
            // 直接使用接口引用 e 即可，因为 isModified() 和 getPath() 都在 Editor 接口里

            // 只处理已修改的文件
            if (e.isModified()) {
                System.out.print("File '" + e.getPath() + "' is modified. Save before exit? (y/n): ");
                String choice = scanner.nextLine().trim().toLowerCase();

                // 只有用户明确输入 y 才保存
                if ("y".equals(choice)) {
                    // SaveCommand 的构造函数已经支持 Editor 接口，直接传入 e 即可
                    new SaveCommand(e, repo).execute();
                }
            }
        }
    }
}