package com.editor.application.commands;

import com.editor.interfaces.Command;
import com.editor.interfaces.Editor;
import com.editor.interfaces.SpellChecker;

import java.util.List;

public class SpellCheckCommand implements Command {
    private Editor editor;
    private SpellChecker spellChecker;

    public SpellCheckCommand(Editor editor, SpellChecker spellChecker) {
        this.editor = editor;
        this.spellChecker = spellChecker;
    }

    @Override
    public void execute() {
        if (editor == null) return;

        System.out.println("Checking spelling for " + editor.getPath() + "...");

        // 1. 获取全文
        List<String> lines = editor.getContent();
        String fullText = String.join("\n", lines);

        // 2. 调用检查
        List<String> report = spellChecker.check(fullText);

        // 3. 输出结果
        System.out.println("----------------------------------------");
        for (String line : report) {
            System.out.println(line);
        }
        System.out.println("----------------------------------------");
    }
}