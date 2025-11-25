package com.editor.application.commands;

import com.editor.interfaces.UndoableCommand;
import com.editor.domain.TextEditor;

public class InsertCommand implements UndoableCommand {
    private TextEditor editor;
    private int line, col; // 起始位置 (0-based)
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
        // [修改] 计算结束位置，调用 deleteRange
        if (!text.contains("\n")) {
            // 简单情况：单行插入，直接用老方法或者 deleteRange 都可以
            editor.deleteRange(line, col, line, col + text.length());
        } else {
            // 复杂情况：跨行插入
            String[] parts = text.split("\n", -1);

            // 1. 结束行号 = 起始行 + 增加的行数
            int endLine = line + parts.length - 1;

            // 2. 结束列号
            // 最后一行插入的内容长度是 parts[parts.length-1].length()
            // 如果是多行插入，最后一段是从行首(或接续位置)开始算的吗？
            // 不，因为中间行是新开的，最后一行也是被切断后的新行的一部分。
            // 实际上：
            // - 第一行插入位置在 col
            // - 中间行全是新内容
            // - 最后一行是：[插入的最后一段] + [原行后缀]
            // 所以要删除的范围在最后一行的结束列就是：插入的最后一段的长度
            int endCol = parts[parts.length - 1].length();

            editor.deleteRange(line, col, endLine, endCol);
        }
    }
}