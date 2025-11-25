package com.editor.interfaces;
import java.util.List;

public interface Editor {
    String getPath();
    List<String> getContent();
    void attach(EditorObserver observer);
    void detach(EditorObserver observer);
    boolean isModified();
    void setModified(boolean m);
    // [新增] 专门用于去重挂载：如果已存在同类观察者，则不添加
    void attachUnique(EditorObserver observer);

    // [新增] 专门用于按类型移除：移除所有该类型的观察者(用于 log-off)
    void detach(Class<? extends EditorObserver> type);
}