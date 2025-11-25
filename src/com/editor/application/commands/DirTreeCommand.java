package com.editor.application.commands;

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
        printTree(root, "", true);
    }

    // 递归打印树
    private void printTree(File node, String prefix, boolean isLast) {
        System.out.println(prefix + (isLast ? "└── " : "├── ") + node.getName());

        File[] children = node.listFiles();
        if (children == null) return;

        for (int i = 0; i < children.length; i++) {
            printTree(children[i], prefix + (isLast ? "    " : "│   "), i == children.length - 1);
        }
    }
}