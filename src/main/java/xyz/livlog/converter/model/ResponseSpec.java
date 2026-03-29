package xyz.livlog.converter.model;

public class ResponseSpec {
    private int statusCode;
    private String contentType = "application/json";
    private String body;
    private String description;

    public int getStatusCode() {
        return statusCode;
    }

    public String getContentType() {
        return contentType;
    }

    public String getBody() {
        return body;
    }

    public String getDescription() {
        return description;
    }

    public void setStatusCode(int statusCode) {
        this.statusCode = statusCode;
    }

    public void setContentType(String contentType) {
        this.contentType = contentType;
    }

    public void setBody(String body) {
        this.body = body;
    }

    public void setDescription(String description) {
        this.description = description;
    }
}
