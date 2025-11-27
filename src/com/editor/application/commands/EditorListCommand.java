package com.editor.application.commands;

import com.editor.interfaces.Command;
import com.editor.domain.*;
import com.editor.interfaces.Editor;
import com.editor.domain.statistics.TimeTracker; // 现在有这个类了，可以导包了

public class EditorListCommand implements Command {
    private Workspace workspace;

    public EditorListCommand(Workspace ws) {
        this.workspace = ws;
    }

    @Override
    public void execute() {
        Editor active = workspace.getActiveEditor();
        for (Editor e : workspace.getAllEditors()) {
            String prefix = (e == active) ? "> " : "  ";
            String suffix = e.isModified() ? "*" : "";

            // [Lab2] 获取时长字符串
//            String timeStr = " (" + TimeTracker.getInstance().getFormattedDuration(e.getPath()) + ")";
            String timeStr = "";
            try {
                // 就算这里报错，也不应该影响文件名列表的打印
                timeStr = " (" + TimeTracker.getInstance().getFormattedDuration(e.getPath()) + ")";
            } catch (Exception ex) {
                // 统计模块挂了，哪怕显示成空字符串，也不能让 editor-list 命令崩溃
                System.err.println("Warning: Failed to retrieve stats for " + e.getPath());
            }

            System.out.println(prefix + e.getPath() + suffix + timeStr);
        }
    }
}