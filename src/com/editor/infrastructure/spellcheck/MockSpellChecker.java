package com.editor.infrastructure.spellcheck;

import com.editor.interfaces.SpellChecker;

import java.util.ArrayList;
import java.util.List;

public class MockSpellChecker implements SpellChecker {
    @Override
    public List<String> check(String text) {
        List<String> errors = new ArrayList<>();
        if (text == null || text.isEmpty()) return errors;

        // 简单的模拟逻辑：按行扫描，查找硬编码的几个错别字
        String[] lines = text.split("\n");
        for (int i = 0; i < lines.length; i++) {
            String line = lines[i];
            int lineNum = i + 1;

            // 模拟检测 "recieve" -> "receive"
            if (line.contains("recieve")) {
                errors.add(String.format("第%d行: Found 'recieve', did you mean 'receive'?", lineNum));
            }

            // 模拟检测 "teh" -> "the"
            if (line.contains("teh")) {
                errors.add(String.format("第%d行: Found 'teh', did you mean 'the'?", lineNum));
            }

            // 模拟检测 "Itallian" -> "Italian"
            if (line.contains("Itallian")) {
                errors.add(String.format("第%d行: Found 'Itallian', did you mean 'Italian'?", lineNum));
            }
        }

        if (errors.isEmpty()) {
            errors.add("No spelling errors found.");
        }

        return errors;
    }
}