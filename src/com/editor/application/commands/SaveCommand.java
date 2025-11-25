package com.editor.application.commands;

import com.editor.interfaces.Command;
import com.editor.interfaces.Editor;
import com.editor.interfaces.FileRepository;
import com.editor.interfaces.Editor; // [新增] 需要引入具体类

public class SaveCommand implements Command {
    private Editor editor;
    private FileRepository repo;

    public SaveCommand(Editor editor, FileRepository repo) {
        this.editor = editor;
        this.repo = repo;
    }

    @Override
    public void execute() {
        if (editor != null) {
            // 1. 执行 IO 写操作
            repo.writeLines(editor.getPath(), editor.getContent());

            // 2. [修复] 调用接口方法清除修改标记
            // 以前这里需要强转 ((TextEditor)editor).setModified(false)
            // 现在不需要了，因为 setModified 已经是 Editor 接口的一部分
            editor.setModified(false);

            System.out.println("Saved: " + editor.getPath());
        }
    }
}