package com.editor.domain.decorator;

import com.editor.interfaces.Editor;
import com.editor.interfaces.EditorObserver;
import java.util.List;

//抽象类，实现 Editor 接口，并把所有方法调用“转发”给它包裹的 Editor 对象。这是装饰器模式的标准“样板代码”。
public abstract class EditorDecorator implements Editor {
    protected Editor wrapper; // 持有被装饰的对象

    public EditorDecorator(Editor editor) {
        this.wrapper = editor;
    }

    // 核心：转发所有方法
    @Override
    public String getDisplayName() { return wrapper.getDisplayName(); }

    // --- 下面都是原封不动的转发 ---
    @Override
    public String getPath() { return wrapper.getPath(); }
    @Override
    public List<String> getContent() { return wrapper.getContent(); }
    @Override
    public boolean isModified() { return wrapper.isModified(); }
    @Override
    public void setModified(boolean m) { wrapper.setModified(m); }
    @Override
    public void attach(EditorObserver o) { wrapper.attach(o); }
    @Override
    public void detach(EditorObserver o) { wrapper.detach(o); }
    @Override
    public void attachUnique(EditorObserver o) { wrapper.attachUnique(o); }
    @Override
    public void detach(Class<? extends EditorObserver> t) { wrapper.detach(t); }
    @Override
    public void setPath(String p) { wrapper.setPath(p); }
    @Override
    public void onSave() { wrapper.onSave(); }
    @Override
    public void onClose() { wrapper.onClose(); }
    @Override
    public void onLoad() { wrapper.onLoad(); }
}