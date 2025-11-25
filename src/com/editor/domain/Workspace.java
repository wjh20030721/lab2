package com.editor.domain;

import com.editor.interfaces.Editor;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;

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


    private Workspace() {}

    //私有构造函数，防止外部用 new 创建 Workspace 实例。
    public static Workspace getInstance() { return INSTANCE; }

    // 注册新的编辑器，并将其设为当前激活编辑器
    public void register(Editor editor) {
        editorMap.put(editor.getPath(), editor);
        this.activeEditor = editor;
    }
    //get当前激活的编辑器
    public Editor getActiveEditor() { return activeEditor; }
    //获取所有已打开的编辑器集合
    public Collection<Editor> getAllEditors() { return editorMap.values(); }

    //设置当前激活的编辑器，通过路径查找
    public void setActive(String path) {
        if (editorMap.containsKey(path)) {
            this.activeEditor = editorMap.get(path);
        }
    }

    // 在 Workspace 类中添加
    public void close(String path) {
        editorMap.remove(path); //
        // 如果关闭的是当前文件，切换到最近的一个（简单策略：取第一个）
        if (activeEditor != null && activeEditor.getPath().equals(path)) {
            if (!editorMap.isEmpty()) {
                activeEditor = editorMap.values().iterator().next();
            } else {
                activeEditor = null;
            }
        }
    }
}