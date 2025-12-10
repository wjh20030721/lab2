package com.editor.domain.decorator;

import com.editor.interfaces.Editor;
import com.editor.domain.statistics.TimeTracker;

public class TimeDisplayDecorator extends EditorDecorator {

    public TimeDisplayDecorator(Editor editor) {
        super(editor);
    }

    @Override
    public String getDisplayName() {
        // 1. 获取原始名称 (比如 "test.txt*")
        String originalName = super.getDisplayName();

        // 2. 获取时长数据
        String duration = TimeTracker.getInstance().getFormattedDuration(getPath());

        // 3. 装饰！(拼接在一起)
        return originalName + " (" + duration + ")";
    }
}