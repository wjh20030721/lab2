package com.editor.application.commands;

import com.editor.interfaces.UndoableCommand;
import com.editor.domain.TextEditor;

public class InsertCommand implements UndoableCommand {
    private TextEditor editor;
    private int line, col;
    private String text;

    public InsertCommand(TextEditor editor, int line, int col, String text) {
        this.editor = editor;
        this.line = line - 1;
        this.col = col - 1;
        this.text = text;
    }

    @Override
    public void execute() {
        editor.insert(line, col, text);
        editor.pushUndo(this);
    }

    @Override
    public void undo() {
        // 撤销插入 = 删除插入的文本
        editor.delete(line, col, text.length());
    }
}