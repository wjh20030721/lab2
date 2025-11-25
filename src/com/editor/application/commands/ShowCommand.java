package com.editor.application.commands;

import com.editor.interfaces.Command;
import com.editor.interfaces.Editor;
import java.util.List;

public class ShowCommand implements Command {
    private Editor editor;
    private int startLine = 1;
    private int endLine = Integer.MAX_VALUE;

    public ShowCommand(Editor editor, Integer start, Integer end) {
        this.editor = editor;
        if (start != null) this.startLine = start;
        if (end != null) this.endLine = end;
    }

    @Override
    public void execute() {
        List<String> lines = editor.getContent();
        for (int i = 0; i < lines.size(); i++) {
            int lineNum = i + 1;
            if (lineNum >= startLine && lineNum <= endLine) {
                System.out.println(lineNum + ": " + lines.get(i)); //
            }
        }
    }
}