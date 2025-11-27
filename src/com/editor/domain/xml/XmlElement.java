package com.editor.domain.xml;

import java.util.*;

// 表示 XML 节点，负责存储数据（标签、属性、文本、子节点）以及将自身序列化回字符串列表。
// 代表 DOM 树中的一个节点。它包含子节点列表（体现组合模式），并提供递归生成文本的方法。
public class XmlElement {
    private String tagName;
    private String text = ""; // 文本内容
    // 使用 LinkedHashMap 保持属性顺序
    private Map<String, String> attributes = new LinkedHashMap<>();
    private List<XmlElement> children = new ArrayList<>();
    private XmlElement parent;

    public XmlElement(String tagName) {
        this.tagName = tagName;
    }

    // --- 树操作 ---
    public void addChild(XmlElement child) {
        children.add(child);
        child.setParent(this);
    }

    public void insertBefore(XmlElement newChild, XmlElement targetChild) {
        int index = children.indexOf(targetChild);
        if (index != -1) {
            children.add(index, newChild);
            newChild.setParent(this);
        }
    }

    public void removeChild(XmlElement child) {
        children.remove(child);
        child.setParent(null);
    }

    // --- 序列化 (DOM -> List<String>) ---
    public List<String> toLines(int indentLevel) {
        List<String> lines = new ArrayList<>();
        // 生成缩进
        char[] indentChars = new char[indentLevel * 4];
        Arrays.fill(indentChars, ' ');
        String indent = new String(indentChars);

        // 1. 构建开始标签
        StringBuilder sb = new StringBuilder(indent);
        sb.append("<").append(tagName);
        attributes.forEach((k, v) -> sb.append(" ").append(k).append("=\"").append(v).append("\""));
        sb.append(">");

        // 2. 处理内容
        if (children.isEmpty()) {
            // 如果没有子节点，文本直接拼在后面 (这里简化处理，假设纯文本节点不换行)
            // 实际 Lab 示例中，如果有文本且无子节点，通常在一行
            if (!text.isEmpty()) {
                sb.append(text).append("</").append(tagName).append(">");
                lines.add(sb.toString());
            } else {
                // 空节点：<tag></tag>
                sb.append("</").append(tagName).append(">");
                lines.add(sb.toString());
            }
        } else {
            // 有子节点：换行
            lines.add(sb.toString());
            // 递归添加子节点
            for (XmlElement child : children) {
                lines.addAll(child.toLines(indentLevel + 1));
            }
            // 闭合标签单独一行
            lines.add(indent + "</" + tagName + ">");
        }
        return lines;
    }

    // --- Getters / Setters ---
    public String getTagName() { return tagName; }

    public String getId() { return attributes.get("id"); }
    public void setId(String id) { attributes.put("id", id); }

    public String getText() { return text; }
    public void setText(String text) { this.text = text; }

    public void setAttribute(String key, String value) { attributes.put(key, value); }
    public String getAttribute(String key) { return attributes.get(key); }
    public Map<String, String> getAttributes() { return attributes; }

    public List<XmlElement> getChildren() { return children; }

    public XmlElement getParent() { return parent; }
    public void setParent(XmlElement parent) { this.parent = parent; }
}