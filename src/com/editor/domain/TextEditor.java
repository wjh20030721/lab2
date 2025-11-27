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
        if (text.contains("\n")) {
            // 使用 -1 参数防止末尾空行被吞 (例如 "A\n" -> ["A", ""])
            String[] parts = text.split("\n", -1);
            for (String part : parts) {
                lines.add(part);
            }
        } else {
            lines.add(text);
        }
        markModified("append");
    }
    // 插入逻辑
    public void insert(int lineIdx, int colIdx, String text) {
        // 1. [修复] 空文件检查
        if (lines.isEmpty()) {
            if (lineIdx != 0 || colIdx != 0) {
                throw new EditorException("空文件只能在1:1位置插入");
            }
            // 空文件插入时需要初始化一行
            lines.add("");
        }

        // 2. [修复] 行号越界检查 (禁止自动扩容)
        // 允许在最后一行之后紧接着插入(追加行)，即 lineIdx == lines.size() 是合法的
        // 但不允许跳行插入，即 lineIdx > lines.size() 是非法的
        if (lineIdx > lines.size()) {
            throw new EditorException("行号越界");
        }

        // 如果是追加新行，先填一个空串占位，防止 get(lineIdx) 越界
        if (lineIdx == lines.size()) {
            lines.add("");
        }

        String line = lines.get(lineIdx);

        // 3. 列号越界检查 (保持原样)
        if (colIdx > line.length()) {
            throw new EditorException("列号越界");
        }
//        ensureCapacity(lineIdx);

//        if (colIdx > line.length()) throw new EditorException("Column out of bounds");

        // 如果不含换行，走老逻辑（效率高一点）
        if (!text.contains("\n")) {
            String newLine = line.substring(0, colIdx) + text + line.substring(colIdx);
            lines.set(lineIdx, newLine);
        } else {
            // 1. 拆分插入文本
            String[] parts = text.split("\n", -1);

            // 2.以此点为界，保存原行内容的“前半截”和“后半截”
            String prefix = line.substring(0, colIdx);
            String suffix = line.substring(colIdx);

            // 3. 修改当前行 = 原前半截 + 插入文本的第一段
            lines.set(lineIdx, prefix + parts[0]);

            // 4. 插入中间的行 (如果有)
            // 注意：每次插入后，list大小变了，后续插入位置要跟着变
            for (int i = 1; i < parts.length - 1; i++) {
                lines.add(lineIdx + i, parts[i]);
            }

            // 5. 插入最后一行 = 插入文本的最后一段 + 原后半截
            // 只有当 parts 长度 > 1 时才需要这一步，否则都在第3步处理了
            if (parts.length > 1) {
                lines.add(lineIdx + parts.length - 1, parts[parts.length - 1] + suffix);
            }
        }
        markModified("insert");
    }

    // 删除逻辑(不支持跨行)
    public void delete(int lineIdx, int colIdx, int len) {
        if (lineIdx >= lines.size()) throw new EditorException("Line number out of bounds");
        String line = lines.get(lineIdx);

        // 2. [新增] 纯粹的列号越界检查 (更精准的提示)
        if (colIdx > line.length()) {
            throw new EditorException("Column out of bounds");
        }

        // 边界检查：删除长度不可超出行尾
        if (colIdx + len > line.length()) {
            throw new EditorException("Delete length exceeds line end");
        }

        String newLine = line.substring(0, colIdx) + line.substring(colIdx + len);
        lines.set(lineIdx, newLine);
        markModified("delete");
    }

    // [新增] 通用区间删除方法，支持跨行（为了undo和redo）
    // startLine/startCol: 删除起始位置
    // endLine/endCol: 删除结束位置（不包含该位置字符）
    public void deleteRange(int startLine, int startCol, int endLine, int endCol) {
        if (startLine >= lines.size() || endLine >= lines.size())
            throw new EditorException("Line range out of bounds");

        String startLineText = lines.get(startLine);
        String endLineText = lines.get(endLine);

        // 1. 获取保留的前缀（第一行被删处之前的内容）
        String prefix = startLineText.substring(0, startCol);

        // 2. 获取保留的后缀（最后一行被删处之后的内容）
        // 注意检查越界，虽然理论上由 Command 保证正确性
        String suffix = "";
        if (endCol < endLineText.length()) {
            suffix = endLineText.substring(endCol);
        }

        // 3. 合并：将后缀拼接到前缀后面，更新到起始行
        lines.set(startLine, prefix + suffix);

        // 4. 删除中间的行（包括原来的 endLine）
        // 注意：要从后往前删，或者每次都删 startLine + 1，删 endLine - startLine 次
        int linesToRemove = endLine - startLine;
        for (int i = 0; i < linesToRemove; i++) {
            lines.remove(startLine + 1);
        }

        markModified("delete range");
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

//    // 辅助方法：扩容
//    private void ensureCapacity(int targetIndex) {
//        while (lines.size() <= targetIndex) {
//            lines.add("");
//        }
//    }

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

    //这个方法耦合性太高了，解了耦合以后，就废弃了
//    // [新增 1] 安全挂载 Logger：防止重复添加
//    public void attachLogger(EditorObserver observer) {
//        // 检查观察者列表中是否已经存在相同类型(class)的观察者
//        boolean exists = observers.stream()
//                .anyMatch(o -> o.getClass().equals(observer.getClass()));
//
//        // 只有不存在时才添加
//        if (!exists) {
//            observers.add(observer);
//        }
//    }

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
    @Override
    public boolean isModified() { return modified; }
    //设置 modified 标志（外部可用以手动清除或标记修改状态）
    @Override
    public void setModified(boolean m) { this.modified = m; }

    @Override
    public void attachUnique(EditorObserver observer) {
        // 检查是否存在同类观察者
        boolean exists = observers.stream()
                .anyMatch(o -> o.getClass().equals(observer.getClass()));
        if (!exists) {
            observers.add(observer);
        }
    }

    @Override
    public void detach(Class<? extends EditorObserver> type) {
        // 移除所有指定类型的观察者
        observers.removeIf(o -> type.isInstance(o));
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