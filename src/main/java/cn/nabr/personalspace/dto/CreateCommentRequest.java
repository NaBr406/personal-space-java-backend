package cn.nabr.personalspace.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public class CreateCommentRequest {
    private String content;

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
