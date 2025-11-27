package com.editor.domain;

//import com.editor.domain.statistics.TimeTracker;
import com.editor.interfaces.Editor;
import com.editor.interfaces.WorkspaceObserver; // [新增] 引入接口

import java.util.*;

public class Workspace {
    // 单例模式
    /*
    单例模式
    含义：整个应用中只有一个类的实例，提供全局访问点。
    在 Workspace 中的实现：私有构造函数、静态且唯一的 INSTANCE 字段、通过 getInstance() 获取。
    这里采用饿汉式（类加载时创建），天然线程安全且简单。
     */
    private static final Workspace INSTANCE = new Workspace();

    // 领域状态
    /*
    领域状态
    含义：表示业务/领域模型的核心数据和当前状态（与 UI/持久化细节分离），用于表示系统在某一时刻的业务语义。
    在 Workspace 中表现为 editorMap（已打开的编辑器集合）和 activeEditor（当前激活的编辑器）。
    这些字段保存了编辑器子系统的领域状态，方法如 register、setActive 用来变更该状态。
     */
    //使用 LinkedHashMap 保持插入顺序
    //已打开的编辑器集合,保存路径到编辑器的映射
    private Map<String, Editor> editorMap = new LinkedHashMap<>();
    private Editor activeEditor;    //当前激活的编辑器

    // 观察者列表
    // 使用 CopyOnWriteArrayList 防止遍历时修改抛出异常（虽然后续 Main 是单线程，但这更安全）
    private List<WorkspaceObserver> observers = new ArrayList<>();

    private Workspace() {}

    //私有构造函数，防止外部用 new 创建 Workspace 实例。
    public static Workspace getInstance() { return INSTANCE; }

    // 注册观察者的方法
    public void addObserver(WorkspaceObserver observer) {
        observers.add(observer);
    }

    // 注册新的编辑器，并将其设为当前激活编辑器
    public void register(Editor editor) {
        editorMap.put(editor.getPath(), editor);
        this.activeEditor = editor;
        // 注册新文件时，通常也视为切换到了新文件，需要通知
        notifyObservers(editor.getPath());
    }
    //get当前激活的编辑器
    public Editor getActiveEditor() { return activeEditor; }
    //获取所有已打开的编辑器集合
    public Collection<Editor> getAllEditors() { return editorMap.values(); }

    // 设置当前激活的编辑器
    public void setActive(String path) {
        if (editorMap.containsKey(path)) {
            this.activeEditor = editorMap.get(path);

//            // 通知统计模块切换了文件
//            TimeTracker.getInstance().switchFile(path);
            // 不再直接调用 TimeTracker，而是通知观察者
            notifyObservers(path);
        }
    }

    // 通知逻辑
    private void notifyObservers(String path) {
        for (WorkspaceObserver observer : observers) {
            try {
                observer.onActiveFileChanged(path);
            } catch (Exception e) {
                // [修复] 捕获所有异常，仅打印警告，确保循环继续，且不影响 Workspace 主逻辑
                System.err.println("Warning: Observer failed to update: " + e.getMessage());
                // e.printStackTrace(); // 可选：调试时打开
            }
        }
    }

    // 在 Workspace 类中添加
    public void close(String path) {
        editorMap.remove(path); //
        // 如果关闭的是当前文件，切换到最近的一个（简单策略：取第一个）
        if (activeEditor != null && activeEditor.getPath().equals(path)) {
            if (!editorMap.isEmpty()) {
                activeEditor = editorMap.values().iterator().next();
                // 关闭并切换后，也需要通知
                notifyObservers(activeEditor.getPath());
            } else {
                activeEditor = null;
            }
        }
    }
    // 用于在“另存为”时更新 Map 索引
    public void renameEditor(String oldPath, String newPath) {
        if (editorMap.containsKey(oldPath)) {
            Editor editor = editorMap.remove(oldPath); // 移除旧 Key
            editor.setPath(newPath);                   // 更新 Editor 内部状态
            editorMap.put(newPath, editor);            // 放入新 Key

            // 如果它是活动文件，更新 activeEditor 引用（虽然引用没变，但为了保险）
            if (activeEditor == editor) {
                activeEditor = editor;
            }
        }
    }
}