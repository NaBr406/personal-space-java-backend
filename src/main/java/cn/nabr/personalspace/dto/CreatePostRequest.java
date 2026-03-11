package cn.nabr.personalspace.dto;

/**
 * 纯 JSON 发动态请求。
 */
public class CreatePostRequest {
    // 动态文字内容。
    private String content;

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }
}
