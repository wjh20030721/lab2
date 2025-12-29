package com.editor.application.view.tree;

import java.util.List;

/**
 * 树节点接口 (Target)
 * 对应 VS Code API 中的 TreeItem + TreeDataProvider 的概念
 */
public interface TreeNode {
    // 获取当前节点显示的文本 (例如文件名，或者 XML 标签+属性)
    String getContent();

    // 获取子节点列表
    List<TreeNode> getChildren();

    // 是否是叶子节点 (用于优化渲染逻辑)
    boolean isLeaf();
}