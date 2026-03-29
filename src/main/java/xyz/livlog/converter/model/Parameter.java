package xyz.livlog.converter.model;

public class Parameter {
    private String name;
    private String type = "string";
    private boolean required;
    private String description;
    private String example;

    public Parameter() {
    }

    public Parameter(String name, String type, boolean required, String description, String example) {
        this.name = name;
        this.type = type;
        this.required = required;
        this.description = description;
        this.example = example;
    }

    public String getName() {
        return name;
    }

    public String getType() {
        return type;
    }

    public boolean isRequired() {
        return required;
    }

    public String getDescription() {
        return description;
    }

    public String getExample() {
        return example;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setType(String type) {
        this.type = type;
    }

    public void setRequired(boolean required) {
        this.required = required;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public void setExample(String example) {
        this.example = example;
    }
}
