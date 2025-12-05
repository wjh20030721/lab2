package com.editor.infrastructure.spellcheck;

import com.editor.interfaces.SpellChecker;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class LanguageToolHttpAdapter implements SpellChecker {
    private static final String API_URL = "https://api.languagetool.org/v2/check";

    @Override
    public List<String> check(String text) {
        List<String> errors = new ArrayList<>();
        if (text == null || text.trim().isEmpty()) return errors;

        try {
            // 1. 建立连接
            URL url = new URL(API_URL);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("POST");
            conn.setDoOutput(true);

            // 2. 构造参数 (language=en-US, text=...)
            String params = "language=en-US&text=" + URLEncoder.encode(text, StandardCharsets.UTF_8);

            // 3. 发送请求
            try (OutputStream os = conn.getOutputStream()) {
                byte[] input = params.getBytes(StandardCharsets.UTF_8);
                os.write(input, 0, input.length);
            }

            // 4. 读取响应
            StringBuilder response = new StringBuilder();
            try (BufferedReader br = new BufferedReader(new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8))) {
                String responseLine;
                while ((responseLine = br.readLine()) != null) {
                    response.append(responseLine.trim());
                }
            }

            // 5. 解析结果 (简单解析 JSON 中的 "message" 字段)
            // 真实响应示例: {"matches":[{"message":"Possible spelling mistake", ...}, ...]}
            // 这里用正则简单提取 message，生产环境请用 Jackson/Gson
            errors.addAll(parseErrorsFromJson(response.toString()));

        } catch (Exception e) {
            errors.add("Spell check failed: " + e.getMessage());
            e.printStackTrace();
        }

        if (errors.isEmpty()) {
            errors.add("No spelling errors found (Online Check).");
        }
        return errors;
    }

    // 简易 JSON 解析器 (仅用于演示，提取 message 字段)
    private List<String> parseErrorsFromJson(String json) {
        List<String> foundErrors = new ArrayList<>();
        // 匹配 "message":"..." 模式
        Pattern pattern = Pattern.compile("\"message\":\"([^\"]+)\"");
        Matcher matcher = pattern.matcher(json);

        // 匹配 "replacements":[{"value":"..."}] 模式以获取建议
        Pattern suggestPattern = Pattern.compile("\"value\":\"([^\"]+)\"");

        // 注意：这里简单的正则无法精确匹配对应的建议，仅作演示
        // 实际开发应引入 org.json 或 com.google.gson
        while (matcher.find()) {
            foundErrors.add("API Suggestion: " + matcher.group(1));
        }
        return foundErrors;
    }
}