package com.editor.application.commands;

import com.editor.interfaces.Command;
import com.editor.domain.*;
import java.util.Scanner;

public class CloseCommand implements Command {
    private Workspace workspace;
    private String path;
    private Scanner scanner; // 需要交互

    public CloseCommand(Workspace ws, String path) {
        this.workspace = ws;
        this.path = path;
        this.scanner = new Scanner(System.in);
    }

    @Override
    public void execute() {
        TextEditor target = (TextEditor) workspace.getAllEditors().stream()
                .filter(e -> e.getPath().equals(path)).findFirst().orElse(null);

        if (target == null) {
            System.out.println("File not open: " + path);
            return;
        }

        if (target.isModified()) {
            System.out.print("File modified. Save? (y/n): ");
            String choice = scanner.nextLine().trim();
            if ("y".equalsIgnoreCase(choice)) {
                // 触发保存逻辑 (这里可以复用 SaveCommand 或直接调用 Repo，为了简单直接调用 Workspace 逻辑)
                System.out.println("Please run 'save' command manually."); // 简化处理，或注入 Repo 执行保存
                return; // 中断关闭
            }
        }

        workspace.close(path);
        System.out.println("Closed: " + path);
    }
}