package com.editor.application.commands;

import com.editor.application.view.tree.ConsoleTreeView;
import com.editor.application.view.tree.TreeNode;
import com.editor.application.view.tree.XmlNodeAdapter;
import com.editor.domain.XmlEditor;
import com.editor.domain.xml.XmlElement;
import com.editor.interfaces.Command;

public class XmlTreeCommand implements Command {
    private XmlEditor editor;

    public XmlTreeCommand(XmlEditor editor) {
        this.editor = editor;
    }

    @Override
    public void execute() {
        if (editor == null) return;
        XmlElement root = editor.getRoot();
        if (root == null) {
            System.out.println("Empty XML Tree.");
            return;
        }

        // --- 重构后 ---
        // 1. 适配
        TreeNode treeRoot = new XmlNodeAdapter(root);
        // 2. 渲染
        new ConsoleTreeView().print(treeRoot);
    }
}