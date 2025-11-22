package com.editor.interfaces;

public interface UndoableCommand extends Command {
    void undo();
}