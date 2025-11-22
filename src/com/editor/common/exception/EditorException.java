package com.editor.common.exception;

//异常基类
public class EditorException extends RuntimeException {
    public EditorException(String message) { super(message); }
}