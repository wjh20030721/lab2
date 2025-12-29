package com.editor.application.view.tree;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class FileNodeAdapter implements TreeNode {
    private File file;

    public FileNodeAdapter(File file) {
        this.file = file;
    }

    @Override
    public String getContent() {
        // 根目录可能没有名字(例如 ".")，处理一下
        String name = file.getName();
        return (name == null || name.isEmpty()) ? file.getPath() : name;
    }

    @Override
    public List<TreeNode> getChildren() {
        List<TreeNode> children = new ArrayList<>();
        if (file.isDirectory()) {
            File[] files = file.listFiles();
            if (files != null) {
                for (File f : files) {
                    children.add(new FileNodeAdapter(f));
                }
            }
        }
        return children;
    }

    @Override
    public boolean isLeaf() {
        return file.isFile();
    }
}