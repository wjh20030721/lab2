package com.editor.interfaces;
import java.util.List;

public interface Editor {
    String getPath();
    List<String> getContent();
    void attach(EditorObserver observer);
    void detach(EditorObserver observer);
}