package xyz.livlog.converter;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import xyz.livlog.converter.model.ActionSpec;
import xyz.livlog.converter.model.BlueprintDocument;
import xyz.livlog.converter.model.Parameter;
import xyz.livlog.converter.model.RequestSpec;
import xyz.livlog.converter.model.ResourceSpec;
import xyz.livlog.converter.model.ResponseSpec;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@SuppressWarnings("unchecked")
public class OpenApiBuilder {

    private final ObjectMapper objectMapper = new ObjectMapper();

    public Map<String, Object> build(BlueprintDocument document) {
        Map<String, Object> openapi = new LinkedHashMap<>();
        openapi.put("openapi", "3.1.0");

        Map<String, Object> info = new LinkedHashMap<>();
        info.put("title", document.getTitle());
        info.put("version", "1.0.0");
        if (document.getDescription() != null && !document.getDescription().isBlank()) {
            String description = document.getDescription();
            if (document.getAuthSection() != null && !document.getAuthSection().isBlank()) {
                description += "\n\n## 認証\n" + document.getAuthSection();
            }
            info.put("description", description);
        }
        openapi.put("info", info);

        if (document.getHost() != null && !document.getHost().isBlank()) {
            openapi.put("servers", List.of(Map.of("url", document.getHost())));
        }

        Map<String, Object> components = new LinkedHashMap<>();
        components.put("securitySchemes", Map.of(
                "bearerAuth",
                Map.of(
                        "type", "http",
                        "scheme", "bearer",
                        "bearerFormat", "JWT"
                )
        ));
        openapi.put("components", components);

        Map<String, Object> paths = new LinkedHashMap<>();
        for (ResourceSpec resource : document.getResources()) {
            for (ActionSpec action : resource.getActions()) {
                String fullPath = normalizePath(action.getPath());
                String pathOnly = pathOnly(fullPath);

                Map<String, Object> pathItem = (Map<String, Object>) paths.computeIfAbsent(pathOnly, k -> new LinkedHashMap<>());
                Map<String, Object> operation = new LinkedHashMap<>();

                operation.put("summary", action.getName());
                operation.put("operationId", operationId(action.getMethod(), pathOnly, action.getName()));

                String description = buildDescription(resource, action);
                if (!description.isBlank()) {
                    operation.put("description", description);
                }

                if (resource.getGroup() != null && !resource.getGroup().isBlank()) {
                    operation.put("tags", List.of(resource.getGroup()));
                }

                List<Map<String, Object>> parameters = buildParameters(fullPath, action.getParameters());
                if (!parameters.isEmpty()) {
                    operation.put("parameters", parameters);
                }

                if (action.getRequest() != null && hasAuthorizationHeader(action.getRequest())) {
                    operation.put("security", List.of(Map.of("bearerAuth", List.of())));
                }

                Map<String, Object> responses = new LinkedHashMap<>();
                for (ResponseSpec response : action.getResponses()) {
                    Map<String, Object> responseMap = new LinkedHashMap<>();
                    responseMap.put("description", response.getDescription() != null && !response.getDescription().isBlank()
                            ? response.getDescription()
                            : "HTTP " + response.getStatusCode() + " response");

                    if (response.getBody() != null && !response.getBody().isBlank()) {
                        Map<String, Object> content = new LinkedHashMap<>();
                        Map<String, Object> mediaType = new LinkedHashMap<>();
                        mediaType.put("schema", inferSchema(response.getBody()));
                        mediaType.put("example", parseJsonOrRaw(response.getBody()));
                        content.put(response.getContentType(), mediaType);
                        responseMap.put("content", content);
                    }

                    responses.put(String.valueOf(response.getStatusCode()), responseMap);
                }

                if (responses.isEmpty()) {
                    responses.put("default", Map.of("description", "No response documented"));
                }

                operation.put("responses", responses);
                pathItem.put(action.getMethod().toLowerCase(), operation);
            }
        }

        openapi.put("paths", paths);
        return openapi;
    }

    private List<Map<String, Object>> buildParameters(String fullPath, List<Parameter> actionParameters) {
        List<Map<String, Object>> parameters = new ArrayList<>();
        String pathOnly = pathOnly(fullPath);
        List<String> queryParams = extractQueryParams(fullPath);

        for (Parameter parameter : actionParameters) {
            Map<String, Object> map = new LinkedHashMap<>();
            map.put("name", parameter.getName());
            map.put("in", queryParams.contains(parameter.getName()) ? "query" : (pathOnly.contains("{" + parameter.getName() + "}") ? "path" : "query"));
            boolean required = "path".equals(map.get("in")) || parameter.isRequired();
            map.put("required", required);

            Map<String, Object> schema = new LinkedHashMap<>();
            schema.put("type", parameter.getType());
            if (parameter.getExample() != null && !parameter.getExample().isBlank()) {
                schema.put("example", coerceExample(parameter.getExample(), parameter.getType()));
            }
            map.put("schema", schema);

            if (parameter.getDescription() != null && !parameter.getDescription().isBlank()) {
                map.put("description", parameter.getDescription());
            }
            parameters.add(map);
        }

        return parameters;
    }

    private String buildDescription(ResourceSpec resource, ActionSpec action) {
        StringBuilder sb = new StringBuilder();
        if (resource.getDescription() != null && !resource.getDescription().isBlank()) {
            sb.append(resource.getDescription());
        }
        if (action.getDescription() != null && !action.getDescription().isBlank()) {
            if (sb.length() > 0) {
                sb.append("\n\n");
            }
            sb.append(action.getDescription());
        }
        RequestSpec request = action.getRequest();
        if (request != null && request.getDescription() != null && !request.getDescription().isBlank()) {
            if (sb.length() > 0) {
                sb.append("\n\n");
            }
            sb.append("認証要件: ").append(request.getDescription());
        }
        return sb.toString().trim();
    }

    private boolean hasAuthorizationHeader(RequestSpec request) {
        return request.getHeaders().keySet().stream()
                .anyMatch(h -> "authorization".equalsIgnoreCase(h));
    }

    private String normalizePath(String path) {
        return path == null ? "/" : path.trim();
    }

    private String pathOnly(String fullPath) {
        int idx = fullPath.indexOf("{?");
        return idx >= 0 ? fullPath.substring(0, idx) : fullPath;
    }

    private List<String> extractQueryParams(String fullPath) {
        int start = fullPath.indexOf("{?");
        int end = fullPath.indexOf("}");
        if (start < 0 || end < 0 || end <= start) {
            return List.of();
        }
        String csv = fullPath.substring(start + 2, end);
        String[] parts = csv.split(",");
        List<String> list = new ArrayList<>();
        for (String part : parts) {
            String trimmed = part.trim();
            if (!trimmed.isBlank()) {
                list.add(trimmed);
            }
        }
        return list;
    }

    private String operationId(String method, String path, String actionName) {
        String merged = method.toLowerCase() + "_" + path + "_" + actionName;
        return merged.replaceAll("\\{", "_")
                .replaceAll("}", "_")
                .replaceAll("[^a-zA-Z0-9_]+", "_")
                .replaceAll("_+", "_")
                .replaceAll("^_|_$", "")
                .toLowerCase();
    }

    private Object inferSchema(String body) {
        Object example = parseJsonOrRaw(body);
        return schemaFromExample(example);
    }

    private Object schemaFromExample(Object example) {
        if (example instanceof Map<?, ?> map) {
            Map<String, Object> schema = new LinkedHashMap<>();
            schema.put("type", "object");
            Map<String, Object> properties = new LinkedHashMap<>();
            for (Map.Entry<?, ?> entry : map.entrySet()) {
                properties.put(String.valueOf(entry.getKey()), schemaFromExample(entry.getValue()));
            }
            schema.put("properties", properties);
            return schema;
        }
        if (example instanceof List<?> list) {
            Map<String, Object> schema = new LinkedHashMap<>();
            schema.put("type", "array");
            Object first = list.isEmpty() ? "string" : list.get(0);
            schema.put("items", schemaFromExample(first));
            return schema;
        }
        if (example instanceof Integer || example instanceof Long) {
            return Map.of("type", "integer");
        }
        if (example instanceof Float || example instanceof Double) {
            return Map.of("type", "number");
        }
        if (example instanceof Boolean) {
            return Map.of("type", "boolean");
        }
        if (example == null) {
            return Map.of("type", "string", "nullable", true);
        }
        return Map.of("type", "string");
    }

    private Object parseJsonOrRaw(String body) {
        try {
            return objectMapper.readValue(body, Object.class);
        } catch (JsonProcessingException e) {
            return body;
        }
    }

    private Object coerceExample(String value, String type) {
        try {
            return switch (type) {
                case "integer" -> Integer.parseInt(value);
                case "number" -> Double.parseDouble(value);
                case "boolean" -> Boolean.parseBoolean(value);
                default -> value;
            };
        } catch (Exception e) {
            return value;
        }
    }
}
