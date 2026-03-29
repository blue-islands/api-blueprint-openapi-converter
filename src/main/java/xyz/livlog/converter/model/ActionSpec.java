package xyz.livlog.converter.model;

import java.util.ArrayList;
import java.util.List;

public class ActionSpec {
    private String name;
    private String method;
    private String path;
    private String description;
    private final List<Parameter> parameters = new ArrayList<>();
    private RequestSpec request;
    private final List<ResponseSpec> responses = new ArrayList<>();

    public String getName() {
        return name;
    }

    public String getMethod() {
        return method;
    }

    public String getPath() {
        return path;
    }

    public String getDescription() {
        return description;
    }

    public List<Parameter> getParameters() {
        return parameters;
    }

    public RequestSpec getRequest() {
        return request;
    }

    public List<ResponseSpec> getResponses() {
        return responses;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setMethod(String method) {
        this.method = method;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public void setRequest(RequestSpec request) {
        this.request = request;
    }
}
