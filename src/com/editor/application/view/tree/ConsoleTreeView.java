package com.editor.application.view.tree;

import java.util.List;

/**
 * 通用树形视图渲染器
 * 负责将任何实现了 TreeNode 接口的结构打印到控制台
 */
public class ConsoleTreeView {

    public void print(TreeNode root) {
        if (root == null) return;
        // 打印根节点，或者从根节点的子节点开始打印，取决于你的需求。
        // 通常如果是目录树，根目录本身也要打印吗？Lab 示例通常是的。
        // 这里为了效果统一，我们把 root 当作第一层打印。
        printNode(root, "", true);
    }

    public void print(TreeNode root, String prefix, boolean showChildren) {

    }

    // 递归打印逻辑
    private void printNode(TreeNode node, String prefix, boolean isLast) {
        // 1. 打印当前节点
        System.out.println(prefix + (isLast ? "└── " : "├── ") + node.getContent());

        // 2. 准备子节点的前缀
        String childPrefix = prefix + (isLast ? "    " : "│   ");

        // 3. 获取并打印子节点
        List<TreeNode> children = node.getChildren();
        for (int i = 0; i < children.size(); i++) {
            boolean isLastChild = (i == children.size() - 1);
            printNode(children.get(i), childPrefix, isLastChild);
        }
    }
}