package xyz.livlog.converter;

import xyz.livlog.converter.model.ActionSpec;
import xyz.livlog.converter.model.BlueprintDocument;
import xyz.livlog.converter.model.Parameter;
import xyz.livlog.converter.model.RequestSpec;
import xyz.livlog.converter.model.ResourceSpec;
import xyz.livlog.converter.model.ResponseSpec;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ApiBlueprintParser {

    private static final Pattern TITLE_PATTERN = Pattern.compile("^#\\s+(.+)$");
    private static final Pattern GROUP_PATTERN = Pattern.compile("^#\\s+Group\\s+(.+)$");
    private static final Pattern RESOURCE_PATTERN = Pattern.compile("^##\\s+(.+?)\\s*\\[(.+)]\\s*$");
    private static final Pattern SIMPLE_SECTION_PATTERN = Pattern.compile("^##\\s+(.+?)\\s*$");
    private static final Pattern ACTION_PATTERN = Pattern.compile("^###\\s+(.+?)\\s*\\[(GET|POST|PUT|PATCH|DELETE|OPTIONS|HEAD)]\\s*$", Pattern.CASE_INSENSITIVE);
    private static final Pattern ACTION_WITH_PATH_PATTERN = Pattern.compile("^###\\s+(.+?)\\s*\\[(GET|POST|PUT|PATCH|DELETE|OPTIONS|HEAD)\\s+(.+)]\\s*$", Pattern.CASE_INSENSITIVE);
    private static final Pattern RESPONSE_PATTERN = Pattern.compile("^\\+\\s+Response\\s+(\\d{3})(?:\\s*\\(([^)]+)\\))?.*$");
    private static final Pattern PARAMETER_PATTERN = Pattern.compile("^\\s*\\+\\s+([A-Za-z0-9_\\-]+):\\s*`?([^`(]*)`?\\s*\\(([^)]*)\\)\\s*-\\s*(.+)\\s*$");
    private static final Pattern BULLET_URL_PATTERN = Pattern.compile("^\\+\\s+URL:\\s+(.+)$");

    public BlueprintDocument parse(String content) {
        List<String> lines = normalize(content);
        BlueprintDocument document = new BlueprintDocument();

        String currentGroup = null;
        ResourceSpec currentResource = null;
        ActionSpec currentAction = null;

        StringBuilder introBuffer = new StringBuilder();
        StringBuilder authBuffer = new StringBuilder();

        boolean inIntro = false;
        boolean inAuth = false;

        int i = 0;
        while (i < lines.size()) {
            String line = lines.get(i);
            String trimmed = line.trim();

            if (trimmed.isEmpty()) {
                i++;
                continue;
            }

            if (trimmed.startsWith("FORMAT:")) {
                document.setFormat(trimmed.substring("FORMAT:".length()).trim());
                i++;
                continue;
            }

            if (trimmed.startsWith("HOST:")) {
                document.setHost(trimmed.substring("HOST:".length()).trim());
                i++;
                continue;
            }

            Matcher groupMatcher = GROUP_PATTERN.matcher(trimmed);
            if (groupMatcher.matches()) {
                currentGroup = groupMatcher.group(1).trim();
                currentResource = null;
                currentAction = null;
                inIntro = false;
                inAuth = false;
                i++;
                continue;
            }

            Matcher titleMatcher = TITLE_PATTERN.matcher(trimmed);
            if (titleMatcher.matches() && !trimmed.startsWith("##") && !trimmed.startsWith("###")) {
                document.setTitle(titleMatcher.group(1).trim());
                i++;
                continue;
            }

            if ("## はじめに".equals(trimmed)) {
                inIntro = true;
                inAuth = false;
                i++;
                continue;
            }

            if ("## 認証".equals(trimmed)) {
                inIntro = false;
                inAuth = true;
                i++;
                continue;
            }

            Matcher resourceMatcher = RESOURCE_PATTERN.matcher(trimmed);
            if (resourceMatcher.matches()) {
                currentResource = new ResourceSpec();
                currentResource.setGroup(currentGroup);
                currentResource.setName(resourceMatcher.group(1).trim());
                currentResource.setPath(resourceMatcher.group(2).trim());
                document.getResources().add(currentResource);
                currentAction = null;
                inIntro = false;
                inAuth = false;
                i++;
                continue;
            }

            Matcher actionWithPathMatcher = ACTION_WITH_PATH_PATTERN.matcher(trimmed);
            if (actionWithPathMatcher.matches()) {
                if (currentResource == null) {
                    currentResource = new ResourceSpec();
                    currentResource.setGroup(currentGroup);
                    currentResource.setName(actionWithPathMatcher.group(1).trim());
                    currentResource.setPath(actionWithPathMatcher.group(3).trim());
                    document.getResources().add(currentResource);
                }
                currentAction = new ActionSpec();
                currentAction.setName(actionWithPathMatcher.group(1).trim());
                currentAction.setMethod(actionWithPathMatcher.group(2).toUpperCase());
                currentAction.setPath(actionWithPathMatcher.group(3).trim());
                currentResource.getActions().add(currentAction);
                inIntro = false;
                inAuth = false;
                i++;
                continue;
            }

            Matcher actionMatcher = ACTION_PATTERN.matcher(trimmed);
            if (actionMatcher.matches()) {
                if (currentResource == null) {
                    throw new IllegalStateException("Action found before resource: " + trimmed);
                }
                currentAction = new ActionSpec();
                currentAction.setName(actionMatcher.group(1).trim());
                currentAction.setMethod(actionMatcher.group(2).toUpperCase());
                currentAction.setPath(currentResource.getPath());
                currentResource.getActions().add(currentAction);
                inIntro = false;
                inAuth = false;
                i++;
                continue;
            }

            Matcher simpleSectionMatcher = SIMPLE_SECTION_PATTERN.matcher(trimmed);
            if (simpleSectionMatcher.matches() && !trimmed.startsWith("## ")) {
                i++;
                continue;
            }

            if (trimmed.startsWith("+ Parameters")) {
                i = parseParameters(lines, i + 1, currentAction);
                continue;
            }

            if (trimmed.startsWith("+ Request")) {
                i = parseRequest(lines, i, currentAction);
                continue;
            }

            Matcher responseMatcher = RESPONSE_PATTERN.matcher(trimmed);
            if (responseMatcher.matches()) {
                i = parseResponse(lines, i, currentAction, responseMatcher);
                continue;
            }

            Matcher urlBulletMatcher = BULLET_URL_PATTERN.matcher(trimmed);
            if (urlBulletMatcher.matches() && inAuth) {
                authBuffer.append(urlBulletMatcher.group(1).trim()).append("\n");
                i++;
                continue;
            }

            if (inIntro) {
                introBuffer.append(trimmed).append("\n");
            } else if (inAuth) {
                authBuffer.append(trimmed).append("\n");
            } else if (currentAction != null) {
                appendActionDescription(currentAction, trimmed);
            } else if (currentResource != null) {
                appendResourceDescription(currentResource, trimmed);
            }

            i++;
        }

        document.setDescription(introBuffer.toString().trim());
        document.setAuthSection(authBuffer.toString().trim());
        return document;
    }

    private int parseParameters(List<String> lines, int start, ActionSpec action) {
        int i = start;
        while (i < lines.size()) {
            String line = lines.get(i);
            String trimmed = line.trim();
            if (trimmed.isEmpty()) {
                i++;
                continue;
            }
            if (!trimmed.startsWith("+")) {
                break;
            }
            if (trimmed.startsWith("+ Request") || trimmed.startsWith("+ Response")) {
                break;
            }

            Matcher matcher = PARAMETER_PATTERN.matcher(line);
            if (matcher.matches()) {
                Parameter parameter = new Parameter();
                parameter.setName(matcher.group(1).trim());
                parameter.setExample(matcher.group(2).trim());
                String[] meta = matcher.group(3).split(",");
                String type = "string";
                boolean required = false;
                for (String m : meta) {
                    String token = m.trim().toLowerCase();
                    if ("required".equals(token)) {
                        required = true;
                    } else if (!token.isEmpty()) {
                        type = mapType(token);
                    }
                }
                parameter.setType(type);
                parameter.setRequired(required);
                parameter.setDescription(matcher.group(4).trim());
                action.getParameters().add(parameter);
            }
            i++;
        }
        return i;
    }

    private int parseRequest(List<String> lines, int start, ActionSpec action) {
        String requestLine = lines.get(start).trim();
        RequestSpec request = new RequestSpec();
        request.setDescription(requestLine.replaceFirst("^\\+\\s+Request\\s*", "").trim());

        int i = start + 1;
        while (i < lines.size()) {
            String line = lines.get(i);
            String trimmed = line.trim();

            if (trimmed.isEmpty()) {
                i++;
                continue;
            }
            if (trimmed.startsWith("+ Response") || trimmed.startsWith("### ") || trimmed.startsWith("## ")) {
                break;
            }
            if (trimmed.startsWith("+ Headers")) {
                i++;
                while (i < lines.size()) {
                    String headerLine = lines.get(i);
                    String headerTrimmed = headerLine.trim();
                    if (headerTrimmed.isEmpty()) {
                        i++;
                        continue;
                    }
                    if (!headerLine.startsWith("            ") && !headerLine.startsWith("\t")) {
                        break;
                    }
                    int idx = headerTrimmed.indexOf(':');
                    if (idx > 0) {
                        String key = headerTrimmed.substring(0, idx).trim();
                        String value = headerTrimmed.substring(idx + 1).trim();
                        request.getHeaders().put(key, value);
                    }
                    i++;
                }
                continue;
            }
            i++;
        }

        action.setRequest(request);
        return i;
    }

    private int parseResponse(List<String> lines, int start, ActionSpec action, Matcher responseMatcher) {
        ResponseSpec response = new ResponseSpec();
        response.setStatusCode(Integer.parseInt(responseMatcher.group(1)));
        if (responseMatcher.group(2) != null && !responseMatcher.group(2).isBlank()) {
            response.setContentType(responseMatcher.group(2).trim());
        }

        int i = start + 1;
        StringBuilder body = new StringBuilder();

        while (i < lines.size()) {
            String line = lines.get(i);
            String trimmed = line.trim();

            if (trimmed.startsWith("+ Response") || trimmed.startsWith("### ") || trimmed.startsWith("## ")) {
                break;
            }

            if (line.startsWith("        ")) {
                body.append(line.substring(8)).append("\n");
            } else if (line.startsWith("\t")) {
                body.append(line.substring(1)).append("\n");
            } else if (trimmed.isEmpty()) {
                if (body.length() > 0) {
                    body.append("\n");
                }
            }
            i++;
        }

        response.setBody(body.toString().trim());
        action.getResponses().add(response);
        return i;
    }

    private static void appendActionDescription(ActionSpec action, String text) {
        if (action.getDescription() == null || action.getDescription().isBlank()) {
            action.setDescription(text);
        } else {
            action.setDescription(action.getDescription() + "\n" + text);
        }
    }

    private static void appendResourceDescription(ResourceSpec resource, String text) {
        if (resource.getDescription() == null || resource.getDescription().isBlank()) {
            resource.setDescription(text);
        } else {
            resource.setDescription(resource.getDescription() + "\n" + text);
        }
    }

    private static String mapType(String token) {
        return switch (token) {
            case "number" -> "number";
            case "integer" -> "integer";
            case "boolean" -> "boolean";
            case "array" -> "array";
            case "object" -> "object";
            default -> "string";
        };
    }

    private static List<String> normalize(String content) {
        String normalized = content.replace("\r\n", "\n").replace("\r", "\n");
        String[] split = normalized.split("\n", -1);
        List<String> lines = new ArrayList<>();
        for (String s : split) {
            lines.add(s);
        }
        return lines;
    }
}
