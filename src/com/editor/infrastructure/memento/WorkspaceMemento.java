package com.editor.infrastructure.memento;

import java.io.*;
import java.util.ArrayList;
import java.util.List;

// 简单的序列化实现备忘录
public class WorkspaceMemento implements Serializable {
    public List<String> openFiles = new ArrayList<>();
    public String activeFile;

    public static void save(String path, WorkspaceMemento m) {
        try (ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(path))) {
            oos.writeObject(m);
        } catch (IOException e) { e.printStackTrace(); }
    }

    public static WorkspaceMemento load(String path) {
        try (ObjectInputStream ois = new ObjectInputStream(new FileInputStream(path))) {
            return (WorkspaceMemento) ois.readObject();
        } catch (Exception e) { return null; }
    }
}