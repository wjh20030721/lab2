package com.editor.application.commands;

import com.editor.interfaces.Command;
import com.editor.domain.*;
import com.editor.interfaces.FileRepository;

import java.util.Scanner;

public class CloseCommand implements Command {
    private Workspace workspace;
    private String path;
    private FileRepository repo; // 需要 Repo 来执行保存
    private Scanner scanner; // 复用外部 Scanner

    // 构造函数接收 scanner 和 repo
    public CloseCommand(Workspace ws, FileRepository repo, String path, Scanner scanner) {
        this.workspace = ws;
        this.repo = repo;
        this.path = path;
        this.scanner = scanner;
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
                // [修复] 真正执行保存
                new SaveCommand(target, repo).execute();
                // 保存后继续执行下面的关闭逻辑
            } else {
                // 如果选 n，直接关闭（丢弃修改）；
                // 如果选了其他奇怪的键（如取消），可能需要 return
            }
        }

        workspace.close(path);
        System.out.println("Closed: " + path);
    }
}