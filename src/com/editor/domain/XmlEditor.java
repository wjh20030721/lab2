package com.editor.domain;

import com.editor.common.exception.EditorException;
import com.editor.domain.xml.XmlElement;
import com.editor.interfaces.Editor;
import com.editor.interfaces.EditorObserver;
import com.editor.interfaces.UndoableCommand;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

// 实现 Editor 接口，负责解析文本构建 DOM 树、维护 ID 索引表（Map）以支持 O(1) 查找，并提供具体的编辑方法。
/*
    XmlEditor 需要具备以下关键特性：
解析器：构造函数中需要将 List<String> 解析为 XmlElement 树。
ID 索引：维护 Map<String, XmlElement>，并在增删改 ID 时同步更新，这对于 Lab2 的性能和便利性至关重要。
栈与观察者：为了与 Lab1 架构兼容，需要复制或继承 TextEditor 中的 undoStack 和 observers 逻辑
（这里为了解耦和清晰，我们暂时采用独立实现，如果可以修改 TextEditor，提取 AbstractEditor 会更好）。
 */
public class XmlEditor implements Editor {
    private String path;
    private XmlElement root;
    private boolean modified = false;

    // ID 索引映射：O(1) 查找元素
    private Map<String, XmlElement> idMap = new HashMap<>();

    // 复用 Lab1 的栈和观察者机制
    private Stack<UndoableCommand> undoStack = new Stack<>();
    private Stack<UndoableCommand> redoStack = new Stack<>();
    private boolean isRedoing = false;
    private List<EditorObserver> observers = new ArrayList<>();

    public XmlEditor(String path, List<String> lines) {
        this.path = path;
        parse(lines);
    }

    // --- 核心逻辑：解析 (Parser) ---
    private void parse(List<String> lines) {
        idMap.clear();
        Stack<XmlElement> stack = new Stack<>();

        // 正则匹配标签：<tag attr="val"> 或 </tag>
        // 简化版正则，假设属性格式良好
        Pattern startTagPattern = Pattern.compile("<([a-zA-Z0-9_-]+)(.*?)>");
        Pattern endTagPattern = Pattern.compile("</([a-zA-Z0-9_-]+)>");
        Pattern attrPattern = Pattern.compile("\\s+([a-zA-Z0-9_-]+)=\"([^\"]*)\"");

        for (String line : lines) {
            line = line.trim();
            if (line.isEmpty() || line.startsWith("<?xml") || line.startsWith("# log")) continue;

            Matcher endMatcher = endTagPattern.matcher(line);
            if (endMatcher.find()) {
                // 这是一个闭合标签行（可能是单独的 </tag>，也可能是 <tag>text</tag> 的一部分）
                // 简单起见，我们假设 Lab 的 XML 格式比较规整：
                // 1. <tag>text</tag> 在一行 -> 处理开始，处理文本，处理结束
                // 2. </tag> 单独一行 -> 弹出栈

                // 检查是否包含开始标签 (例如 <title>Text</title>)
                if (line.startsWith("<") && !line.startsWith("</")) {
                    // 既有开始又有结束，属于叶子节点行
                    // 提取内容
                    String content = line.substring(line.indexOf(">") + 1, line.lastIndexOf("<"));

                    // 解析开始标签部分
                    Matcher startMatcher = startTagPattern.matcher(line);
                    if (startMatcher.find()) {
                        XmlElement leaf = createElementFromMatch(startMatcher, attrPattern);
                        leaf.setText(content);

                        if (!stack.isEmpty()) stack.peek().addChild(leaf);
                        else this.root = leaf;
                    }
                } else {
                    // 纯闭合标签
                    if (!stack.isEmpty()) {
                        stack.pop();
                    }
                }
                continue;
            }

            Matcher startMatcher = startTagPattern.matcher(line);
            if (startMatcher.find()) {
                XmlElement element = createElementFromMatch(startMatcher, attrPattern);

                if (!stack.isEmpty()) stack.peek().addChild(element);
                else this.root = element; // 第一个节点是根

                stack.push(element);
            }
        }
    }

    private XmlElement createElementFromMatch(Matcher startMatcher, Pattern attrPattern) {
        String tagName = startMatcher.group(1);
        String attrsStr = startMatcher.group(2);

        XmlElement element = new XmlElement(tagName);

        // 解析属性
        Matcher attrMatcher = attrPattern.matcher(attrsStr);
        while (attrMatcher.find()) {
            String key = attrMatcher.group(1);
            String val = attrMatcher.group(2);
            element.setAttribute(key, val);

            // 维护 ID 索引
            if ("id".equals(key)) {
                idMap.put(val, element);
            }
        }
        return element;
    }

    // --- Editor 接口实现 ---

    @Override
    public List<String> getContent() {
        List<String> lines = new ArrayList<>();
        // 加上 XML 声明
        lines.add("<?xml version=\"1.0\" encoding=\"UTF-8\"?>");
        if (root != null) {
            lines.addAll(root.toLines(0));
        }
        return lines;
    }

    @Override
    public String getPath() { return path; }

    // --- 领域特定业务方法 (供 Command 调用) ---

    public XmlElement getElementById(String id) {
        return idMap.get(id);
    }

    public XmlElement getRoot() { return root; }

    public boolean hasId(String id) {
        return idMap.containsKey(id);
    }

    // 插入元素
    public void insertBefore(XmlElement target, XmlElement newElement) {
        if (target == root) throw new EditorException("不能在根元素前插入元素");
        XmlElement parent = target.getParent();
        if (parent == null) throw new EditorException("目标元素无父节点");

        parent.insertBefore(newElement, target);
        registerIdRecursive(newElement); // 递归注册新元素的ID
        markModified("insert-before");
    }

    // 追加子元素
    public void appendChild(XmlElement parent, XmlElement newChild) {
        parent.addChild(newChild);
        registerIdRecursive(newChild);
        markModified("append-child");
    }

    // 删除元素
    public void delete(XmlElement target) {
        if (target == root) throw new EditorException("不能删除根元素");
        XmlElement parent = target.getParent();
        if (parent != null) {
            parent.removeChild(target);
            unregisterIdRecursive(target); // 递归移除ID索引
            markModified("delete");
        }
    }

    // 修改 ID
    public void updateId(XmlElement element, String newId) {
        if (idMap.containsKey(newId)) throw new EditorException("目标ID已存在: " + newId);
        String oldId = element.getId();

        idMap.remove(oldId);
        element.setId(newId);
        idMap.put(newId, element);

        markModified("edit-id");
    }

    // 修改文本
    public void updateText(XmlElement element, String newText) {
        element.setText(newText);
        markModified("edit-text");
    }

    // --- 辅助方法 ---
    private void registerIdRecursive(XmlElement node) {
        if (node.getId() != null) idMap.put(node.getId(), node);
        for (XmlElement child : node.getChildren()) registerIdRecursive(child);
    }

    private void unregisterIdRecursive(XmlElement node) {
        if (node.getId() != null) idMap.remove(node.getId());
        for (XmlElement child : node.getChildren()) unregisterIdRecursive(child);
    }

    // --- 基础架构 (Observer, Undo, etc.) 复用 TextEditor 逻辑 ---
    // 为了代码简洁，这里复制了 TextEditor 的部分基础设施代码
    // 在实际重构中，建议将这部分提取到 AbstractEditor

    @Override
    public boolean isModified() { return modified; }
    @Override
    public void setModified(boolean m) { this.modified = m; }

    @Override
    public void attach(EditorObserver observer) { observers.add(observer); }
    @Override
    public void detach(EditorObserver observer) { observers.remove(observer); }
    @Override
    public void attachUnique(EditorObserver observer) {
        boolean exists = observers.stream().anyMatch(o -> o.getClass().equals(observer.getClass()));
        if (!exists) observers.add(observer);
    }
    @Override
    public void detach(Class<? extends EditorObserver> type) {
        observers.removeIf(o -> type.isInstance(o));
    }

    private void notifyObservers(String event) {
        for (EditorObserver o : observers) o.update(event);
    }

    private void markModified(String event) {
        this.modified = true;
        notifyObservers(event);
    }

    public void pushUndo(UndoableCommand cmd) {
        undoStack.push(cmd);
        if (!isRedoing) redoStack.clear();
    }

    public void undo() {
        if (!undoStack.isEmpty()) {
            UndoableCommand cmd = undoStack.pop();
            cmd.undo();
            redoStack.push(cmd);
        }
    }

    public void redo() {
        if (!redoStack.isEmpty()) {
            UndoableCommand cmd = redoStack.pop();
            try {
                isRedoing = true;
                cmd.execute();
            } finally {
                isRedoing = false;
            }
        }
    }

    @Override
    public void setPath(String newPath) {
        this.path = newPath;
    }
    // 复用已有的通知逻辑
    @Override
    public void onSave() {
        // save 不会改变 modified 为 true，所以不能调 markModified
        // 直接调用 notifyObservers
        notifyObservers("save");
    }
    @Override
    public void onClose() {
        // 通知日志观察者记录 "close"
        notifyObservers("close");
    }

    @Override
    public void onLoad() {
        // 通知日志观察者记录 "load filename"
        // 注意：Log 格式要求是 "load lab.txt"，所以这里最好带上文件名
        // 或者直接传 "load " + this.path
        notifyObservers("load " + this.path);
    }
}