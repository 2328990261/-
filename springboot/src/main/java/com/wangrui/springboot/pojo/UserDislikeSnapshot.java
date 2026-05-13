package com.wangrui.springboot.pojo;

import java.util.ArrayList;
import java.util.List;

/** GET /dislike 返回当前用户的屏蔽与降权标签列表 */
public class UserDislikeSnapshot {

    private List<Integer> novelIds = new ArrayList<>();
    private List<String> authors = new ArrayList<>();
    private List<String> tags = new ArrayList<>();

    public List<Integer> getNovelIds() {
        return novelIds;
    }

    public void setNovelIds(List<Integer> novelIds) {
        this.novelIds = novelIds != null ? novelIds : new ArrayList<>();
    }

    public List<String> getAuthors() {
        return authors;
    }

    public void setAuthors(List<String> authors) {
        this.authors = authors != null ? authors : new ArrayList<>();
    }

    public List<String> getTags() {
        return tags;
    }

    public void setTags(List<String> tags) {
        this.tags = tags != null ? tags : new ArrayList<>();
    }
}
