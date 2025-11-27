package com.editor.application.commands;

import com.editor.common.exception.EditorException;
import com.editor.domain.XmlEditor;
import com.editor.domain.xml.XmlElement;
import com.editor.interfaces.UndoableCommand;

public class EditIdCommand implements UndoableCommand {
    private XmlEditor editor;
    private String oldId;
    private String newId;

    // 状态保持不需要额外字段，因为 oldId 和 newId 都在属性里

    public EditIdCommand(XmlEditor editor, String oldId, String newId) {
        this.editor = editor;
        this.oldId = oldId;
        this.newId = newId;
    }

    @Override
    public void execute() {
        // 1. 检查
        XmlElement target = editor.getElementById(oldId);
        if (target == null) throw new EditorException("元素不存在: " + oldId);

        if (editor.hasId(newId)) throw new EditorException("目标ID已存在: " + newId);

        if (target == editor.getRoot()) {
            // Lab2 虽然说"不建议"，但如果严格按文档示例是 Warning，这里简单处理为允许或提示
            System.out.println("Warning: 修改根元素ID可能影响结构识别");
        }

        // 2. 执行
        editor.updateId(target, newId);

        // 3. 压栈
        editor.pushUndo(this);
    }

    @Override
    public void undo() {
        // 撤销：把 newId 改回 oldId
        // 注意：此时元素 ID 已经是 newId 了
        XmlElement target = editor.getElementById(newId);
        if (target != null) {
            editor.updateId(target, oldId);
        }
    }
}