package com.editor.application;

import com.editor.application.commands.*;
import com.editor.common.exception.*;
import com.editor.domain.*;
import com.editor.interfaces.*;

public class CommandFactory {
    private Workspace workspace;
    private FileRepository repo;

    public CommandFactory(Workspace workspace, FileRepository repo) {
        this.workspace = workspace;
        this.repo = repo;
    }

    public Command createCommand(String input) {
        String[] parts = input.trim().split(" ", 2);
        String action = parts[0];

        switch (action) {
            case "load":
                if (parts.length < 2) throw new EditorException("Path required");
                return new LoadCommand(workspace, repo, parts[1]);

            case "save":
                return new SaveCommand(workspace.getActiveEditor(), repo);

            case "append":
                if (!(workspace.getActiveEditor() instanceof TextEditor))
                    throw new EditorException("No active text editor");
                // 去除引号简单处理
                String text = parts[1].replace("\"", "");
                return new AppendCommand((TextEditor) workspace.getActiveEditor(), text);

            case "undo":
                // 简单的 Undo 触发器
                return () -> {
                    if (workspace.getActiveEditor() instanceof TextEditor) {
                        ((TextEditor) workspace.getActiveEditor()).undo();
                    }
                };

            case "show":
                return () -> {
                    Editor e = workspace.getActiveEditor();
                    if(e != null) e.getContent().forEach(System.out::println);
                };

            case "exit": return null; // Exit signal

            default:
                throw new EditorException("Unknown command: " + action);
        }
    }
}