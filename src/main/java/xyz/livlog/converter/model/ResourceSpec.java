package xyz.livlog.converter.model;

import java.util.ArrayList;
import java.util.List;

public class ResourceSpec {
    private String group;
    private String name;
    private String path;
    private String description;
    private final List<ActionSpec> actions = new ArrayList<>();

    public String getGroup() {
        return group;
    }

    public String getName() {
        return name;
    }

    public String getPath() {
        return path;
    }

    public String getDescription() {
        return description;
    }

    public List<ActionSpec> getActions() {
        return actions;
    }

    public void setGroup(String group) {
        this.group = group;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public void setDescription(String description) {
        this.description = description;
    }
}
