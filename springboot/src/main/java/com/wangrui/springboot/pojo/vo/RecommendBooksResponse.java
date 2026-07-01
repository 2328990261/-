package com.wangrui.springboot.pojo.vo;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * GET /api/recommend/books 返回体：书单 + 供前端 console 的调试载荷。
 */
public class RecommendBooksResponse {
    private List<Map<String, Object>> books = new ArrayList<>();
    private Map<String, Object> debug = new LinkedHashMap<>();

    public List<Map<String, Object>> getBooks() {
        return books;
    }

    public void setBooks(List<Map<String, Object>> books) {
        this.books = books != null ? books : new ArrayList<>();
    }

    public Map<String, Object> getDebug() {
        return debug;
    }

    public void setDebug(Map<String, Object> debug) {
        this.debug = debug != null ? debug : new LinkedHashMap<>();
    }
}
