package cn.nabr.personalspace.dto;

/**
 * 文章创建/更新请求体。
 */
public class ArticleRequest {
    // 分类：blog 或 chitchat。
    private String category;
    // 标题。
    private String title;
    // 正文 HTML / 富文本内容。
    private String content;
    // 摘要，可为空。
    private String summary;

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

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

    public String getSummary() {
        return summary;
    }

    public void setSummary(String summary) {
        this.summary = summary;
    }
}
