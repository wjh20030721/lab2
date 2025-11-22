package com.editor.interfaces;
import java.util.List;

public interface FileRepository {
    List<String> readLines(String path);
    void writeLines(String path, List<String> lines);
    boolean exists(String path);
    void createEmpty(String path);
}