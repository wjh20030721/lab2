package com.editor.application.commands;

import com.editor.domain.XmlEditor;
import com.editor.domain.xml.XmlElement;
import com.editor.interfaces.Command;
import java.util.Map;

public class XmlTreeCommand implements Command {
    private XmlEditor editor;

    // 构造函数接收 XmlEditor
    public XmlTreeCommand(XmlEditor editor) {
        this.editor = editor;
    }

    @Override
    public void execute() {
        if (editor == null) return;

        XmlElement root = editor.getRoot();
        if (root == null) {
            System.out.println("Empty XML Tree.");
            return;
        }

        // 开始递归打印
        printNode(root, "", true);
    }

    // 递归打印树形结构
    // prefix: 前缀字符串 (如 "│   ")
    // isLast: 当前节点是否是父节点的最后一个子节点 (决定用 └── 还是 ├──)
    private void printNode(XmlElement node, String prefix, boolean isLast) {
        // 1. 打印当前节点本身
        StringBuilder sb = new StringBuilder();
        sb.append(prefix);
        sb.append(isLast ? "└── " : "├── ");
        sb.append(node.getTagName());

        // 打印属性 (例如 [id="1", val="2"])
        Map<String, String> attrs = node.getAttributes();
        if (!attrs.isEmpty()) {
            sb.append(" [");
            attrs.forEach((k, v) -> sb.append(k).append("=\"").append(v).append("\", "));
            sb.setLength(sb.length() - 2); // 移除最后的 ", "
            sb.append("]");
        }
        System.out.println(sb.toString());

        // 2. 准备子节点的前缀
        // 如果当前节点是最后一个，子节点就不需要继承竖线 "│"
        String childPrefix = prefix + (isLast ? "    " : "│   ");

        // 3. 处理文本内容 (作为特殊的子节点显示)
        boolean hasText = node.getText() != null && !node.getText().trim().isEmpty();
        int childCount = node.getChildren().size();

        // 如果有文本，打印文本
        if (hasText) {
            // 如果没有子元素，文本就是唯一的“子节点”
            boolean textIsLast = (childCount == 0);
            System.out.println(childPrefix + (textIsLast ? "└── " : "├── ") + "\"" + node.getText() + "\"");
        }

        // 4. 递归打印子元素
        for (int i = 0; i < childCount; i++) {
            boolean isLastChild = (i == childCount - 1);
            printNode(node.getChildren().get(i), childPrefix, isLastChild);
        }
    }
}