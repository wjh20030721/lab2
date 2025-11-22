package com.editor.application.commands;

import com.editor.domain.TextEditor;
import com.editor.interfaces.UndoableCommand;

public class AppendCommand implements UndoableCommand {
    private TextEditor editor;
    private String text;

    public AppendCommand(TextEditor editor, String text) {
        this.editor = editor;
        this.text = text;
    }

    @Override
    public void execute() {
        editor.append(text);
        editor.pushUndo(this); // 记录到 Domain 的栈中
    }

    @Override
    public void undo() {
        // 简单的撤销逻辑：删除最后一行
        editor.removeLine(editor.getContent().size() - 1);
    }
}