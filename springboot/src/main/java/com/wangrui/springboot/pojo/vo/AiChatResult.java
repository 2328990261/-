package com.wangrui.springboot.pojo.vo;

import java.util.ArrayList;
import java.util.List;

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