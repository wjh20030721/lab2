package com.editor.application.commands;

import com.editor.application.view.tree.ConsoleTreeView;
import com.editor.application.view.tree.FileNodeAdapter;
import com.editor.application.view.tree.TreeNode;
import com.editor.interfaces.Command;
import java.io.File;

public class DirTreeCommand implements Command {
    private String rootPath;

    public DirTreeCommand(String path) {
        this.rootPath = path != null ? path : ".";
    }

    @Override
    public void execute() {
        File root = new File(rootPath);
        if (!root.exists()) {
            System.out.println("Path not found: " + rootPath);
            return;
        }

        // --- 重构后 ---
        // 1. 适配
        TreeNode treeRoot = new FileNodeAdapter(root);
        // 2. 渲染
        new ConsoleTreeView().print(treeRoot);
    }
}