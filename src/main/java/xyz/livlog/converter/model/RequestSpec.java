package xyz.livlog.converter.model;

import java.util.LinkedHashMap;
import java.util.Map;

public class RequestSpec {
    private String description;
    private final Map<String, String> headers = new LinkedHashMap<>();

    public String getDescription() {
        return description;
    }

    public Map<String, String> getHeaders() {
        return headers;
    }

    public void setDescription(String description) {
        this.description = description;
    }
}
