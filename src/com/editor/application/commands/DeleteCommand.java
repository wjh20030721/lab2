package com.editor.application.commands;

import com.editor.interfaces.UndoableCommand;
import com.editor.domain.TextEditor;

public class DeleteCommand implements UndoableCommand {
    private TextEditor editor;
    private int line, col, len;
    private String deletedText; // [关键] 保存状态用于撤销

    public DeleteCommand(TextEditor editor, int line, int col, int len) {
        this.editor = editor;
        this.line = line - 1; // 转换为 0-based
        this.col = col - 1;
        this.len = len;
    }

    @Override
    public void execute() {
        // 先保存要删除的文本
        this.deletedText = editor.getTextSegment(line, col, len);
        editor.delete(line, col, len);
        editor.pushUndo(this);
    }

    @Override
    public void undo() {
        // 撤销删除 = 插入回原来的文本
        editor.insert(line, col, deletedText);
    }
}