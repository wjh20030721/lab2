package com.editor.application.view.tree;

import com.editor.domain.xml.XmlElement;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class XmlNodeAdapter implements TreeNode {
    private XmlElement element;

    public XmlNodeAdapter(XmlElement element) {
        this.element = element;
    }

    @Override
    public String getContent() {
        // 复用原 XmlTreeCommand 的逻辑：拼接标签名和属性
        StringBuilder sb = new StringBuilder();
        sb.append(element.getTagName());

        Map<String, String> attrs = element.getAttributes();
        if (!attrs.isEmpty()) {
            sb.append(" [");
            attrs.forEach((k, v) -> sb.append(k).append("=\"").append(v).append("\", "));
            sb.setLength(sb.length() - 2); // 移除最后的 ", "
            sb.append("]");
        }
        return sb.toString();
    }

    @Override
    public List<TreeNode> getChildren() {
        List<TreeNode> result = new ArrayList<>();

        // 1. 处理 XML 子元素
        for (XmlElement child : element.getChildren()) {
            result.add(new XmlNodeAdapter(child));
        }

        // 2. 处理文本内容 (为了保持原有的显示效果，将文本作为特殊的叶子节点)
        // 原逻辑是：如果有文本，且不为空，则打印它
        String text = element.getText();
        if (text != null && !text.trim().isEmpty()) {
            // 这里我们创建一个匿名内部类或者简单的 TextNode 来表示纯文本
            result.add(new TreeNode() {
                @Override
                public String getContent() { return "\"" + text + "\""; }
                @Override
                public List<TreeNode> getChildren() { return new ArrayList<>(); }
                @Override
                public boolean isLeaf() { return true; }
            });
        }

        return result;
    }

    @Override
    public boolean isLeaf() {
        String text = element.getText();
        boolean hasText = text != null && !text.trim().isEmpty();
        return element.getChildren().isEmpty() && !hasText;
    }
}