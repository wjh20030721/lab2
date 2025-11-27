package com.editor.interfaces;

public interface WorkspaceObserver {
    void onActiveFileChanged(String newPath);
}