package com.editor.domain.statistics;

import com.editor.infrastructure.memento.WorkspaceMemento;
import com.editor.interfaces.WorkspaceObserver;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

public class TimeTracker implements WorkspaceObserver {
    // 1. 单例模式
    private static final TimeTracker INSTANCE = new TimeTracker();

    // 2. 存储每个文件的累计时长（秒）
    private Map<String, Long> fileDurations = new HashMap<>();

    // 3. 当前状态
    private String currentFilePath;
    private LocalDateTime startTime;

    private TimeTracker() {}

    public static TimeTracker getInstance() {
        return INSTANCE;
    }

    // [新增] 接口方法的实现
    @Override
    public void onActiveFileChanged(String newPath) {
        this.switchFile(newPath);
    }

    // [核心逻辑] 切换文件时调用
    public void switchFile(String newFilePath) {
        // A. 如果之前有打开的文件，先结算它的时间
        if (currentFilePath != null && startTime != null) {
            long seconds = Duration.between(startTime, LocalDateTime.now()).getSeconds();
            // 将时间累加到 map 中
            fileDurations.merge(currentFilePath, seconds, Long::sum);
        }

        // B. 开始记录新文件
        this.currentFilePath = newFilePath;
        this.startTime = LocalDateTime.now();
    }

    // 停止当前计时（用于关闭文件或退出程序时结算）
    // 但前的设计中并没有调用这个方法的地方，可以视为预留
    // 因为，文档中提到了，工作区状态恢复不会恢复编辑时长，每次重新启动都是新的计时，也就是不需要持久化
    public void stop() {
        if (currentFilePath != null && startTime != null) {
            long seconds = Duration.between(startTime, LocalDateTime.now()).getSeconds();
            fileDurations.merge(currentFilePath, seconds, Long::sum);
            startTime = null; // 停止计时
            currentFilePath = null;
        }
    }

    // 获取格式化的时长字符串 (给 editor-list 用)
    public String getFormattedDuration(String path) {
        long totalSeconds = fileDurations.getOrDefault(path, 0L);

        // 如果查询的是当前正在编辑的文件，还要加上“当前这段还没结算”的时间
        if (path.equals(currentFilePath) && startTime != null) {
            totalSeconds += Duration.between(startTime, LocalDateTime.now()).getSeconds();
        }

        return formatTime(totalSeconds);
    }

    // 辅助方法：把秒转换成 "X小时Y分钟" 或 "X秒"
    private String formatTime(long seconds) {
        if (seconds < 60) {
            return seconds + "秒";
        } else if (seconds < 3600) {
            return (seconds / 60) + "分钟";
        } else if (seconds < 86400) {
            // 1-23小时: X小时Y分钟 (86400秒 = 24小时)
            long hours = seconds / 3600;
            long minutes = (seconds % 3600) / 60;
            return hours + "小时" + minutes + "分钟";
        } else {
            // ≥ 24小时: X天Y小时
            long days = seconds / 86400;
            long hours = (seconds % 86400) / 3600;
            return days + "天" + hours + "小时";
        }
    }
}