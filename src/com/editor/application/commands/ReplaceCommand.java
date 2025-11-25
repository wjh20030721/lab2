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
        // [修复] 使用 deleteRange 处理可能包含换行的新文本
        if (newText.contains("\n")) {
            // 计算多行文本的结束位置
            String[] parts = newText.split("\n", -1);
            int endLine = line + parts.length - 1;
            int endCol = parts[parts.length - 1].length();

            // 1. 先删掉新插入的（多行）文本
            editor.deleteRange(line, col, endLine, endCol);
        } else {
            // 单行情况，用老方法即可
            editor.delete(line, col, newText.length());
        }

        // 2. 把旧文本（原本就在一行里）插回去
        editor.insert(line, col, oldText);
    }
}