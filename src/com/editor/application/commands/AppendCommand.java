package com.editor.application.commands;

import com.editor.domain.TextEditor;
import com.editor.interfaces.UndoableCommand;

public class AppendCommand implements UndoableCommand {
    private TextEditor editor;
    private String text;
    // [新增] 记录这次操作到底增加了几行
    private int linesAdded = 0;

    public AppendCommand(TextEditor editor, String text) {
        this.editor = editor;
        this.text = text;
    }

    @Override
    public void execute() {
        editor.append(text);

        // [新增] 计算增加的行数
        // 如果 text 不含换行，linesAdded = 1
        // 如果 text 是 "A\nB"，linesAdded = 2
        // split 的 -1 参数很重要，防止 "A\n" 被算作 1 行
        if (text.contains("\n")) {
            linesAdded = text.split("\n", -1).length;
        } else {
            linesAdded = 1;
        }

        editor.pushUndo(this);
    }

    @Override
    public void undo() {
        // [修改] 根据记录的行数，从末尾依次删除
        for (int i = 0; i < linesAdded; i++) {
            // 总是删除当前内容的最后一行
            editor.removeLine(editor.getContent().size() - 1);
        }
    }
}