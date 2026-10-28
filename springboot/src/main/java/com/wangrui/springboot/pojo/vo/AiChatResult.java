package com.wangrui.springboot.pojo.vo;

import java.util.ArrayList;
import java.util.List;

/** AI聊天返回结果体：思考过程 + 回复文本 + 推荐书籍列表 */
public class AiChatResult {
    private String thinking;
    private String text;
    private List<AiBookItem> books = new ArrayList<>();

    public String getThinking() { return thinking; }
    public void setThinking(String thinking) { this.thinking = thinking; }

    public String getText() { return text; }
    public void setText(String text) { this.text = text; }

    public List<AiBookItem> getBooks() { return books; }
    public void setBooks(List<AiBookItem> books) { this.books = books != null ? books : new ArrayList<>(); }
}