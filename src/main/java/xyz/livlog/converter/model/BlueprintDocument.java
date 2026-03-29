package xyz.livlog.converter.model;

import java.util.ArrayList;
import java.util.List;

public class BlueprintDocument {
    private String format;
    private String host;
    private String title;
    private String description;
    private String authSection;
    private final List<ResourceSpec> resources = new ArrayList<>();

    public String getFormat() {
        return format;
    }

    public String getHost() {
        return host;
    }

    public String getTitle() {
        return title;
    }

    public String getDescription() {
        return description;
    }

    public String getAuthSection() {
        return authSection;
    }

    public List<ResourceSpec> getResources() {
        return resources;
    }

    public void setFormat(String format) {
        this.format = format;
    }

    public void setHost(String host) {
        this.host = host;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public void setAuthSection(String authSection) {
        this.authSection = authSection;
    }
}
