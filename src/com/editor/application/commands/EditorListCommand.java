package com.editor.application.commands;

import com.editor.domain.decorator.TimeDisplayDecorator;
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

            // --- 核心修改：使用装饰器 ---

            // 1. 将普通的 Editor 包装成“带时间显示的 Editor”
            Editor decoratedEditor = new TimeDisplayDecorator(e);

            // 2. 调用装饰后的方法获取显示名称
            // 此时它会自动带上时长信息
            System.out.println(prefix + decoratedEditor.getDisplayName());
        }
    }
}