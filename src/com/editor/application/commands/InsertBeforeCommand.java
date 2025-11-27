package com.editor.application.commands;

import com.editor.common.exception.EditorException;
import com.editor.domain.XmlEditor;
import com.editor.domain.xml.XmlElement;
import com.editor.interfaces.UndoableCommand;

public class InsertBeforeCommand implements UndoableCommand {
    private XmlEditor editor;
    private String tagName;
    private String newId;
    private String targetId;
    private String text;

    // [Undo状态] 持有被插入的元素对象，以便撤销时直接删除
    private XmlElement insertedElement;

    public InsertBeforeCommand(XmlEditor editor, String tagName, String newId, String targetId, String text) {
        this.editor = editor;
        this.tagName = tagName;
        this.newId = newId;
        this.targetId = targetId;
        this.text = text;
    }

    @Override
    public void execute() {
        // 1. 检查 ID 冲突 (Lab2 要求)
        if (editor.getElementById(newId) != null) {
            throw new EditorException("元素ID已存在: " + newId);
        }

        // 2. 检查目标是否存在
        XmlElement target = editor.getElementById(targetId);
        if (target == null) {
            throw new EditorException("目标元素不存在: " + targetId);
        }

        // 3. 构建新节点
        insertedElement = new XmlElement(tagName);
        insertedElement.setId(newId); // ID 是必填且唯一的
        if (text != null && !text.isEmpty()) {
            insertedElement.setText(text);
        }

        // 4. 执行插入 (委托给 XmlEditor 处理 DOM 挂载和日志通知)
        // 注意：XmlEditor.insertBefore 内部会检查“是否根节点前插入”的异常
        editor.insertBefore(target, insertedElement);

        // 5. 压入撤销栈
        editor.pushUndo(this);
    }

    @Override
    public void undo() {
        // 撤销逻辑：插入的反向操作是删除
        if (insertedElement != null) {
            editor.delete(insertedElement);
        }
    }
}