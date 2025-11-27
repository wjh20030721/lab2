package com.editor.application.commands;

import com.editor.interfaces.Command;
import com.editor.domain.*;
import com.editor.interfaces.Editor; // [修改] 引入接口
import com.editor.interfaces.FileRepository;

import java.util.Scanner;

public class CloseCommand implements Command {
    private Workspace workspace;
    private String path;
    private FileRepository repo;
    private Scanner scanner;

    public CloseCommand(Workspace ws, FileRepository repo, String path, Scanner scanner) {
        this.workspace = ws;
        this.repo = repo;
        this.path = path;
        this.scanner = scanner;
    }

    @Override
    public void execute() {
        // [修复] 使用 Editor 接口，移除 (TextEditor) 强制转换
        Editor target = workspace.getAllEditors().stream()
                .filter(e -> e.getPath().equals(path)).findFirst().orElse(null);

        if (target == null) {
            System.out.println("File not open: " + path);
            return;
        }

        // isModified() 是 Editor 接口的方法，所以可以直接调用
        if (target.isModified()) {
            System.out.print("File modified. Save? (y/n): ");
            String choice = scanner.nextLine().trim();
            if ("y".equalsIgnoreCase(choice)) {
                // SaveCommand 的构造函数已经接受 Editor 接口，所以这里也没问题
                new SaveCommand(target, repo).execute();
            } else {
                // 如果选 n，直接关闭
            }
        }

        // 在从工作区移除之前，触发关闭日志
        // 这样 Logger 还能收到最后一条消息
        target.onClose();

        workspace.close(path);
        System.out.println("Closed: " + path);
    }
}