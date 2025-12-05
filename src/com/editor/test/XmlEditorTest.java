package com.editor.test;

import com.editor.application.commands.*;
import com.editor.common.exception.EditorException;
import com.editor.domain.XmlEditor;
import com.editor.domain.xml.XmlElement;

import java.util.ArrayList;
import java.util.List;

public class XmlEditorTest {

    // 统计变量
    private static int testsPassed = 0;
    private static int testsFailed = 0;

    public static void main(String[] args) {
        System.out.println("========== 开始自动化测试 ==========");

        // 运行所有测试用例
        runTest("testInsertBefore", XmlEditorTest::testInsertBefore);
        runTest("testAppendChild", XmlEditorTest::testAppendChild);
        runTest("testDeleteElement", XmlEditorTest::testDeleteElement);
        runTest("testUndoRedo", XmlEditorTest::testUndoRedo);
        runTest("testIdConflict", XmlEditorTest::testIdConflict);

        System.out.println("==================================");
        System.out.println(String.format("测试结果: %d 通过, %d 失败", testsPassed, testsFailed));

        if (testsFailed > 0) {
            System.exit(1); // 只要有失败，返回非0状态码
        }
    }

    // --- 具体测试用例 ---

    /**
     * 测试场景：insert-before
     * 目标：验证节点是否正确插入到了目标节点的前面
     */
    private static void testInsertBefore() {
        // 1. 准备环境 (Setup)
        XmlEditor editor = createBasicXmlEditor();
        // 初始: <root><book id="b1"/></root>

        // 2. 执行操作 (Action)
        // 在 b1 前插入 b2
        new InsertBeforeCommand(editor, "book", "b2", "b1", "Book 2").execute();

        // 3. 验证结果 (Assert)
        XmlElement root = editor.getRoot();
        assertEquals(2, root.getChildren().size(), "根节点应该有2个子节点");
        assertEquals("b2", root.getChildren().get(0).getId(), "第一个子节点应该是 b2");
        assertEquals("b1", root.getChildren().get(1).getId(), "第二个子节点应该是 b1");
    }

    /**
     * 测试场景：append-child
     * 目标：验证节点是否正确追加到了父节点的末尾
     */
    private static void testAppendChild() {
        XmlEditor editor = createBasicXmlEditor();
        // 初始: <root><book id="b1"/></root>

        // 在 b1 下追加 title
        new AppendChildCommand(editor, "title", "t1", "b1", "Java Guide").execute();

        XmlElement b1 = editor.getElementById("b1");
        assertEquals(1, b1.getChildren().size(), "b1 应该有1个子节点");
        assertEquals("t1", b1.getChildren().get(0).getId(), "子节点ID应该是 t1");
        assertEquals("Java Guide", b1.getChildren().get(0).getText(), "子节点文本验证");
    }

    /**
     * 测试场景：delete
     * 目标：验证节点是否被物理删除，且 ID 映射也被清理
     */
    private static void testDeleteElement() {
        XmlEditor editor = createBasicXmlEditor();
        // 初始: <root><book id="b1"/></root>

        new DeleteElementCommand(editor, "b1").execute();

        assertEquals(0, editor.getRoot().getChildren().size(), "根节点应该为空");
        assertNull(editor.getElementById("b1"), "ID 映射中不应再查到 b1");
    }

    /**
     * 测试场景：Undo/Redo
     * 目标：验证撤销删除操作后，节点和 ID 映射是否完全恢复
     */
    private static void testUndoRedo() {
        XmlEditor editor = createBasicXmlEditor();
        // 初始: <root><book id="b1"/></root>

        // 执行删除
        new DeleteElementCommand(editor, "b1").execute();
        assertNull(editor.getElementById("b1"), "删除后查不到 ID");

        // 执行 Undo
        editor.undo();
        assertNotNull(editor.getElementById("b1"), "撤销后 ID 应该恢复");
        assertEquals(1, editor.getRoot().getChildren().size(), "撤销后树结构应该恢复");

        // 执行 Redo
        editor.redo();
        assertNull(editor.getElementById("b1"), "重做后 ID 应该再次消失");
    }

    /**
     * 测试场景：ID 冲突检测
     * 目标：验证插入重复 ID 时是否抛出异常
     */
    private static void testIdConflict() {
        XmlEditor editor = createBasicXmlEditor();
        // 初始已有 b1

        try {
            // 尝试再插一个 b1
            new InsertBeforeCommand(editor, "book", "b1", "b1", "").execute();
            fail("应该抛出 ID 重复异常，但没有抛出");
        } catch (EditorException e) {
            // 预期内的异常
            assertTrue(e.getMessage().contains("已存在"), "异常信息应包含'已存在'");
        }
    }

    // --- 辅助方法 ---

    // 创建一个基础的 XML 编辑器环境： <root id="root"><book id="b1"/></root>
    private static XmlEditor createBasicXmlEditor() {
        List<String> lines = new ArrayList<>();
        lines.add("<?xml version=\"1.0\"?>");
        lines.add("<root id=\"root\">");
        lines.add("    <book id=\"b1\"></book>");
        lines.add("</root>");
        return new XmlEditor("test.xml", lines);
    }

    private static void runTest(String testName, Runnable test) {
        try {
            test.run();
            System.out.println("[PASS] " + testName);
            testsPassed++;
        } catch (Throwable e) {
            System.out.println("[FAIL] " + testName);
            e.printStackTrace(); // 打印堆栈以便调试
            testsFailed++;
        }
    }

    // --- 自定义断言方法 (仿 JUnit) ---

    private static void assertEquals(Object expected, Object actual, String message) {
        if (!expected.equals(actual)) {
            throw new RuntimeException(String.format("%s. Expected: <%s>, Actual: <%s>", message, expected, actual));
        }
    }

    private static void assertNull(Object actual, String message) {
        if (actual != null) {
            throw new RuntimeException(String.format("%s. Expected: null, Actual: <%s>", message, actual));
        }
    }

    private static void assertNotNull(Object actual, String message) {
        if (actual == null) {
            throw new RuntimeException(String.format("%s. Expected: not null", message));
        }
    }

    private static void assertTrue(boolean condition, String message) {
        if (!condition) {
            throw new RuntimeException(message);
        }
    }

    private static void fail(String message) {
        throw new RuntimeException(message);
    }
}