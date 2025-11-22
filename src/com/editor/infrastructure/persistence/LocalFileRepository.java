package com.editor.infrastructure.persistence;

import com.editor.common.exception.EditorException;
import com.editor.interfaces.FileRepository;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;

public class LocalFileRepository implements FileRepository {
    @Override
    public List<String> readLines(String path) {
        try {
            return Files.readAllLines(Paths.get(path));
        } catch (IOException e) {
            throw new EditorException("IO Error reading " + path);
        }
    }

    @Override
    public void writeLines(String path, List<String> lines) {
        try {
            Files.write(Paths.get(path), lines);
        } catch (IOException e) {
            throw new EditorException("IO Error writing " + path);
        }
    }

    @Override
    public boolean exists(String path) {
        return Files.exists(Paths.get(path));
    }

    @Override
    public void createEmpty(String path) {
        try {
            Files.createFile(Paths.get(path));
        } catch (IOException e) {
            throw new EditorException("Cannot create file " + path);
        }
    }
}