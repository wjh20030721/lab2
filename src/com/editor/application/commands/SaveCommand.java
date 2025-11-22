package com.editor.application.commands;

import com.editor.interfaces.Command;
import com.editor.interfaces.Editor;
import com.editor.interfaces.FileRepository;

public class SaveCommand implements Command {
    private Editor editor;
    private FileRepository repo; // 依赖接口

    public SaveCommand(Editor editor, FileRepository repo) {
        this.editor = editor;
        this.repo = repo;
    }

    @Override
    public void execute() {
        if (editor != null) {
            repo.writeLines(editor.getPath(), editor.getContent());
            System.out.println("Saved: " + editor.getPath());
        }
    }
}