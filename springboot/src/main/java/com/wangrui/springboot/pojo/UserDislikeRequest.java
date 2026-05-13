package com.wangrui.springboot.pojo;

import java.util.List;

/**
 * 提交「不感兴趣」：可组合屏蔽本书、作者，以及对标签降权。
 */
public class UserDislikeRequest {

    private Integer novelId;
    private Boolean blockNovel;
    private Boolean blockAuthor;
    /** 手动勾选的标签（trim 后入库） */
    private List<String> tagNames;
    /** 为 true 时将本书 label 拆分后的全部标签并入降权列表 */
    private Boolean allBookTags;

    public Integer getNovelId() {
        return novelId;
    }

    public void setNovelId(Integer novelId) {
        this.novelId = novelId;
    }

    public Boolean getBlockNovel() {
        return blockNovel;
    }

    public void setBlockNovel(Boolean blockNovel) {
        this.blockNovel = blockNovel;
    }

    public Boolean getBlockAuthor() {
        return blockAuthor;
    }

    public void setBlockAuthor(Boolean blockAuthor) {
        this.blockAuthor = blockAuthor;
    }

    public List<String> getTagNames() {
        return tagNames;
    }

    public void setTagNames(List<String> tagNames) {
        this.tagNames = tagNames;
    }

    public Boolean getAllBookTags() {
        return allBookTags;
    }

    public void setAllBookTags(Boolean allBookTags) {
        this.allBookTags = allBookTags;
    }
}
