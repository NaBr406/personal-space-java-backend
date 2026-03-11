package cn.nabr.personalspace.dto;

/**
 * 创建公告时的请求体。
 */
public class AnnouncementRequest {
    // 公告标题。
    private String title;
    // 公告正文。
    private String content;
    // 是否创建后立即置顶。
    private Boolean pinned;

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public Boolean getPinned() {
        return pinned;
    }

    public void setPinned(Boolean pinned) {
        this.pinned = pinned;
    }
}
