package xyz.livlog.converter;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.fasterxml.jackson.dataformat.yaml.YAMLGenerator;
import xyz.livlog.converter.model.BlueprintDocument;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class BlueprintConverterApplication {

    public static void main(String[] args) throws Exception {
        if (args.length == 2 && !args[0].startsWith("--") && !args[1].startsWith("--")) {
            convertOne(Path.of(args[0]), Path.of(args[1]));
            return;
        }

        Arguments parsed = Arguments.parse(args);
        if (parsed.showHelp) {
            printUsage();
            return;
        }

        Path inputDir = parsed.inputDir;
        Path outputDir = parsed.outputDir;
        List<Path> inputFiles = listApibFiles(inputDir, parsed.recursive);

        if (inputFiles.isEmpty()) {
            System.out.println("No .apib files found: " + inputDir.toAbsolutePath());
            return;
        }

        int converted = 0;
        for (Path inputFile : inputFiles) {
            Path relative = inputDir.relativize(inputFile);
            String outputFileName = replaceExtension(relative.getFileName().toString(), ".yaml");
            Path outputRelative = relative.resolveSibling(outputFileName);
            Path outputFile = outputDir.resolve(outputRelative);
            convertOne(inputFile, outputFile);
            converted++;
        }

        System.out.println("Batch converted: " + converted + " file(s)");
        System.out.println("Input dir      : " + inputDir.toAbsolutePath());
        System.out.println("Output dir     : " + outputDir.toAbsolutePath());
    }

    private static void printUsage() {
        System.out.println("Usage:");
        System.out.println("  java -jar converter.jar <input.apib> <output.yaml>");
        System.out.println("  java -jar converter.jar [--input-dir <dir>] [--output-dir <dir>] [--recursive]");
        System.out.println("  java -jar converter.jar --help");
        System.out.println();
        System.out.println("Defaults (batch mode):");
        System.out.println("  input-dir  = -Dconverter.inputDir or ./input");
        System.out.println("  output-dir = -Dconverter.outputDir or ./output");
    }

    private static void convertOne(Path input, Path output) throws Exception {
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

    private static List<Path> listApibFiles(Path inputDir, boolean recursive) throws IOException {
        if (!Files.exists(inputDir)) {
            return List.of();
        }
        int depth = recursive ? Integer.MAX_VALUE : 1;
        try (Stream<Path> stream = Files.walk(inputDir, depth)) {
            return stream
                    .filter(Files::isRegularFile)
                    .filter(path -> path.getFileName().toString().toLowerCase(Locale.ROOT).endsWith(".apib"))
                    .sorted(Comparator.naturalOrder())
                    .collect(Collectors.toList());
        }
    }

    private static String replaceExtension(String filename, String newExtension) {
        int dot = filename.lastIndexOf('.');
        if (dot < 0) {
            return filename + newExtension;
        }
        return filename.substring(0, dot) + newExtension;
    }

    private static final class Arguments {
        private final Path inputDir;
        private final Path outputDir;
        private final boolean recursive;
        private final boolean showHelp;

        private Arguments(Path inputDir, Path outputDir, boolean recursive, boolean showHelp) {
            this.inputDir = inputDir;
            this.outputDir = outputDir;
            this.recursive = recursive;
            this.showHelp = showHelp;
        }

        private static Arguments parse(String[] args) {
            Path inputDir = Path.of(System.getProperty("converter.inputDir", "input"));
            Path outputDir = Path.of(System.getProperty("converter.outputDir", "output"));
            boolean recursive = false;
            boolean showHelp = false;

            List<String> errors = new ArrayList<>();

            for (int i = 0; i < args.length; i++) {
                String arg = args[i];
                switch (arg) {
                    case "--input-dir":
                        if (i + 1 >= args.length) {
                            errors.add("Missing value for --input-dir");
                        } else {
                            inputDir = Path.of(args[++i]);
                        }
                        break;
                    case "--output-dir":
                        if (i + 1 >= args.length) {
                            errors.add("Missing value for --output-dir");
                        } else {
                            outputDir = Path.of(args[++i]);
                        }
                        break;
                    case "--recursive":
                        recursive = true;
                        break;
                    case "--help":
                    case "-h":
                        showHelp = true;
                        break;
                    default:
                        errors.add("Unknown option: " + arg);
                        break;
                }
            }

            if (!errors.isEmpty()) {
                for (String error : errors) {
                    System.err.println(error);
                }
                printUsage();
                System.exit(1);
            }

            return new Arguments(inputDir, outputDir, recursive, showHelp);
        }
    }
}
