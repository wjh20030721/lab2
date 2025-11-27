package com.editor.application.commands;

import com.editor.common.exception.EditorException;
import com.editor.domain.XmlEditor;
import com.editor.domain.xml.XmlElement;
import com.editor.interfaces.UndoableCommand;

public class EditTextCommand implements UndoableCommand {
    private XmlEditor editor;
    private String id;
    private String newText;

    // Undo 状态
    private String oldText;

    public EditTextCommand(XmlEditor editor, String id, String newText) {
        this.editor = editor;
        this.id = id;
        this.newText = newText;
    }

    @Override
    public void execute() {
        XmlElement target = editor.getElementById(id);
        if (target == null) throw new EditorException("元素不存在: " + id);

        // 1. 保存旧文本
        this.oldText = target.getText();

        // 2. 执行更新
        editor.updateText(target, newText);

        // 3. 压栈
        editor.pushUndo(this);
    }

    @Override
    public void undo() {
        XmlElement target = editor.getElementById(id);
        if (target != null) {
            editor.updateText(target, oldText);
        }
    }
}