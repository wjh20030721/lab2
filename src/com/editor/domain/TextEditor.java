package com.editor.domain;

import com.editor.interfaces.*;

import java.util.*;
import com.editor.common.exception.EditorException;

//管理 List<String> 和 Undo栈
public class TextEditor implements Editor {
    private String path;
    private List<String> lines;
    private boolean modified = false;
    // 增加一个标记位，用来指示当前是否正在执行 redo 操作
    private boolean isRedoing = false;

    // 独立的撤销栈
    private Stack<UndoableCommand> undoStack = new Stack<>();
    private Stack<UndoableCommand> redoStack = new Stack<>();

    // 观察者列表
    private List<EditorObserver> observers = new ArrayList<>();

    //设置编辑器路径并对传入的初始行做保护性拷贝（若为 null 则使用空列表），初始化内部内容列表
    public TextEditor(String path, List<String> initialLines) {
        this.path = path;
        // 保护性拷贝，确保 Domain 数据独立
        this.lines = new ArrayList<>(initialLines != null ? initialLines : new ArrayList<>());
    }

    // --- 纯内存业务逻辑 ---
    //在内容末尾追加一行（把 text 当作一行添加到 lines），并调用 markModified("append") 标记为已修改并通知观察者。
    public void append(String text) {
        lines.add(text);
        markModified("append");
    }

    // 插入逻辑
    public void insert(int lineIdx, int colIdx, String text) {
        ensureCapacity(lineIdx);
        String line = lines.get(lineIdx);

        // 边界检查
        if (colIdx > line.length()) throw new EditorException("Column out of bounds");

        String newLine = line.substring(0, colIdx) + text + line.substring(colIdx);
        lines.set(lineIdx, newLine);
        markModified("insert");
    }

    // 删除逻辑
    public void delete(int lineIdx, int colIdx, int len) {
        if (lineIdx >= lines.size()) throw new EditorException("Line number out of bounds");
        String line = lines.get(lineIdx);

        // 边界检查：删除长度不可超出行尾
        if (colIdx + len > line.length()) {
            throw new EditorException("Delete length exceeds line end");
        }

        String newLine = line.substring(0, colIdx) + line.substring(colIdx + len);
        lines.set(lineIdx, newLine);
        markModified("delete");
    }

    // 替换逻辑：组合删除和插入，或者直接操作
    public void replace(int lineIdx, int colIdx, int len, String text) {
        delete(lineIdx, colIdx, len);
        insert(lineIdx, colIdx, text);
        // 注意：这里可能会触发两次 notify，如果介意可以单独写逻辑
    }

    // 获取指定位置的文本（用于 Undo 时恢复删除的内容）
    public String getTextSegment(int lineIdx, int colIdx, int len) {
        if (lineIdx >= lines.size()) return "";
        String line = lines.get(lineIdx);
        if (colIdx + len > line.length()) return line.substring(colIdx);
        return line.substring(colIdx, colIdx + len);
    }

    // 辅助方法：扩容
    private void ensureCapacity(int targetIndex) {
        while (lines.size() <= targetIndex) {
            lines.add("");
        }
    }

    //删除指定索引的一行（在索引合法时），并调用 markModified("delete line")
    public void removeLine(int index) {
        if(index >= 0 && index < lines.size()) {
            lines.remove(index);
            markModified("delete line");
        }
    }

    //内部方法。将 modified 标志置为 true，并通过 notifyObservers 将事件通知所有注册的观察者。
    private void markModified(String event) {
        this.modified = true;
        notifyObservers(event);
    }

    // --- 栈管理 ---
    //将可撤销命令压入 undoStack，并清空 redoStack（新操作后无法再重做旧的重做栈）
    public void pushUndo(UndoableCommand cmd) {
        undoStack.push(cmd);

        // 关键修复：只有当是"新操作"(非重做)时，才清空 redoStack
        // 如果是 redo 触发的 execute() -> pushUndo()，则保留 redoStack 里的其他命令
        if (!isRedoing) {
            redoStack.clear();
        }
    }

    //如果 undoStack 非空，弹出栈顶命令并调用其 undo() 方法，再将该命令推入 redoStack（支持重做）
    public void undo() {
        if (!undoStack.isEmpty()) {
            UndoableCommand cmd = undoStack.pop();
            cmd.undo();
            redoStack.push(cmd);
        }
    }

    //如果 redoStack 非空，弹出栈顶命令并调用其 execute() 方法，再将该命令推入 undoStack（恢复到可撤销状态）
    // 修改redo 方法
    public void redo() {
        if (!redoStack.isEmpty()) {
            UndoableCommand cmd = redoStack.pop();

            try {
                // 标记状态：告诉 pushUndo 这是一个重做操作，不要清空 redoStack
                isRedoing = true;

                // 执行命令 (execute 内部会调用 pushUndo，从而把命令放回 undoStack)
                cmd.execute();
            } finally {
                // 还原状态
                isRedoing = false;
            }

            // undoStack.push(cmd);
            // 删掉，因为 cmd.execute() -> pushUndo() 已经把命令压入 undoStack 了。
            // 如果保留，会导致同一个命令在 undoStack 里出现两次。
        }
    }

    // --- 接口实现 ---
    //注册一个观察者（将其添加到 observers 列表）
    @Override
    public void attach(EditorObserver observer) { observers.add(observer); }

    //注销一个观察者（从 observers 列表移除）
    @Override
    public void detach(EditorObserver observer) { observers.remove(observer); }

    //内部方法。遍历 observers 列表并调用每个观察者的 update(event)，用于传播事件通知
    private void notifyObservers(String event) {
        for (EditorObserver o : observers) o.update(event);
    }

    //返回编辑器关联的文件路径 path
    @Override
    public String getPath() { return path; }

    //返回当前内容列表 lines（注意：返回的是内部列表的引用，而非不可变拷贝）
    @Override
    public List<String> getContent() { return lines; }

    //返回当前的已修改标志 modified
    public boolean isModified() { return modified; }
    //设置 modified 标志（外部可用以手动清除或标记修改状态）
    public void setModified(boolean m) { this.modified = m; }
}