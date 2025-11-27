package com.editor.application.commands;

import com.editor.common.exception.EditorException;
import com.editor.domain.XmlEditor;
import com.editor.domain.xml.XmlElement;
import com.editor.interfaces.UndoableCommand;

public class AppendChildCommand implements UndoableCommand {
    private XmlEditor editor;
    private String tagName;
    private String newId;
    private String parentId;
    private String text;

    // Undo 状态：持有被追加的新元素
    private XmlElement appendedElement;

    public AppendChildCommand(XmlEditor editor, String tagName, String newId, String parentId, String text) {
        this.editor = editor;
        this.tagName = tagName;
        this.newId = newId;
        this.parentId = parentId;
        this.text = text;
    }

    @Override
    public void execute() {
        // 1. 检查 ID 冲突
        if (editor.getElementById(newId) != null) {
            throw new EditorException("元素ID已存在: " + newId);
        }

        // 2. 检查父元素是否存在
        XmlElement parent = editor.getElementById(parentId);
        if (parent == null) {
            throw new EditorException("父元素不存在: " + parentId);
        }

        // 3. 构建新节点
        appendedElement = new XmlElement(tagName);
        appendedElement.setId(newId);
        if (text != null && !text.isEmpty()) {
            appendedElement.setText(text);
        }

        // 4. 执行追加
        // XmlEditor.appendChild 负责处理 DOM 挂载、ID 注册、Modified 标记和日志通知
        editor.appendChild(parent, appendedElement);

        // 5. 压入撤销栈
        editor.pushUndo(this);
    }

    @Override
    public void undo() {
        // 撤销追加 = 删除该元素
        if (appendedElement != null) {
            editor.delete(appendedElement);
        }
    }
}