package xyz.livlog.converter;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.fasterxml.jackson.dataformat.yaml.YAMLGenerator;
import xyz.livlog.converter.model.BlueprintDocument;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

public class BlueprintConverterApplication {

    public static void main(String[] args) throws Exception {
        if (args.length < 2) {
            System.out.println("Usage: java -jar converter.jar <input.apib> <output.yaml>");
            System.exit(1);
        }

        Path input = Path.of(args[0]);
        Path output = Path.of(args[1]);

        String content = Files.readString(input, StandardCharsets.UTF_8);

        ApiBlueprintParser parser = new ApiBlueprintParser();
        BlueprintDocument document = parser.parse(content);

        OpenApiBuilder builder = new OpenApiBuilder();
        Map<String, Object> openapi = builder.build(document);

        ObjectMapper yamlMapper = new ObjectMapper(
                new YAMLFactory().disable(YAMLGenerator.Feature.WRITE_DOC_START_MARKER)
        );
        String yaml = yamlMapper.writerWithDefaultPrettyPrinter().writeValueAsString(openapi);

        if (output.getParent() != null) {
            Files.createDirectories(output.getParent());
        }
        Files.writeString(output, yaml, StandardCharsets.UTF_8);

        System.out.println("Converted: " + input);
        System.out.println("Output   : " + output);
    }
}
