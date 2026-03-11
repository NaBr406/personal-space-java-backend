package cn.nabr.personalspace.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * 评论发布请求。
 */
public class CreateCommentRequest {
    // 评论正文。
    private String content;

    // 如果是回复评论，这里带上被回复的父评论 id。
    @JsonProperty("parent_id")
    private Long parentId;

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public Long getParentId() {
        return parentId;
    }

    public void setParentId(Long parentId) {
        this.parentId = parentId;
    }
}
