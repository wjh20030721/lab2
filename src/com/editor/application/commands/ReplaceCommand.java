package com.editor.application.commands;

import com.editor.interfaces.UndoableCommand;
import com.editor.domain.TextEditor;

public class ReplaceCommand implements UndoableCommand {
    private TextEditor editor;
    private int line, col, len;
    private String newText;
    private String oldText; // 用于 Undo

    public ReplaceCommand(TextEditor editor, int line, int col, int len, String newText) {
        this.editor = editor;
        this.line = line - 1;
        this.col = col - 1;
        this.len = len;
        this.newText = newText;
    }

    @Override
    public void execute() {
        // 1. 保存旧文本
        this.oldText = editor.getTextSegment(line, col, len);
        // 2. 执行替换 (先删后插，或者直接 TextEditor.replace)
        editor.delete(line, col, len);
        editor.insert(line, col, newText);
        // 3. 压栈
        editor.pushUndo(this);
    }

    @Override
    public void undo() {
        // 撤销替换 = 把新文本删掉，把旧文本插回去
        editor.delete(line, col, newText.length());
        editor.insert(line, col, oldText);
    }
}