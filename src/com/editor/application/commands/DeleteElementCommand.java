package com.editor.application.commands;

import com.editor.common.exception.EditorException;
import com.editor.domain.XmlEditor;
import com.editor.domain.xml.XmlElement;
import com.editor.interfaces.UndoableCommand;

import java.util.List;

public class DeleteElementCommand implements UndoableCommand {
    private XmlEditor editor;
    private String id;

    // Undo 状态：记住被删的元素、它的父节点、以及它在父节点中的位置
    private XmlElement deletedElement;
    private XmlElement parent;
    private int originalIndex;

    public DeleteElementCommand(XmlEditor editor, String id) {
        this.editor = editor;
        this.id = id;
    }

    @Override
    public void execute() {
        // 1. 查找元素
        deletedElement = editor.getElementById(id);
        if (deletedElement == null) {
            throw new EditorException("元素不存在: " + id);
        }

        // 2. 根元素检查
        if (deletedElement == editor.getRoot()) {
            throw new EditorException("不能删除根元素");
        }

        // 3. 记录状态用于 Undo
        parent = deletedElement.getParent();
        if (parent != null) {
            originalIndex = parent.getChildren().indexOf(deletedElement);
        }

        // 4. 执行删除
        editor.delete(deletedElement);

        // 5. 压栈
        editor.pushUndo(this);
    }

    @Override
    public void undo() {
        if (deletedElement != null && parent != null) {
            // 恢复逻辑：根据索引插回原位置
            List<XmlElement> siblings = parent.getChildren();

            // 如果原位置现在是列表末尾，直接 append
            if (originalIndex >= siblings.size()) {
                editor.appendChild(parent, deletedElement);
            } else {
                // 否则，插在当前那个位置的元素之前
                XmlElement refNode = siblings.get(originalIndex);
                editor.insertBefore(refNode, deletedElement);
            }
        }
    }
}