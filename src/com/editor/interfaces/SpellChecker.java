package com.editor.interfaces;

import java.util.List;

public interface SpellChecker {
    /**
     * 检查文本中的拼写错误
     * @param text 需要检查的完整文本
     * @return 错误报告列表 (每项为一个字符串，描述错误详情)
     */
    List<String> check(String text);
}