package com.editor.application.commands;

import com.editor.interfaces.Command;
import com.editor.domain.*;
import com.editor.interfaces.Editor;

public class EditorListCommand implements Command {
    private Workspace workspace;

    public EditorListCommand(Workspace ws) {
        this.workspace = ws;
    }

    @Override
    public void execute() {
        Editor active = workspace.getActiveEditor();
        for (Editor e : workspace.getAllEditors()) {
            String prefix = (e == active) ? "> " : "  "; //
            String suffix = ((TextEditor)e).isModified() ? "*" : "";
            System.out.println(prefix + e.getPath() + suffix);
        }
    }
}