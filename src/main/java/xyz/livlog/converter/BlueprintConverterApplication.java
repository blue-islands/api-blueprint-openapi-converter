package xyz.livlog.converter;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.fasterxml.jackson.dataformat.yaml.YAMLGenerator;
import xyz.livlog.converter.model.BlueprintDocument;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Properties;
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

        ConverterConfig config = loadConfig(parsed.configPath);
        runWithConfig(config);
    }

    private static void runWithConfig(ConverterConfig config) throws Exception {
        if ("single".equals(config.mode)) {
            if (config.singleInputFile == null || config.singleOutputFile == null) {
                throw new IllegalArgumentException("single mode requires single.input and single.output");
            }
            convertOne(config.singleInputFile, config.singleOutputFile);
            return;
        }

        if ("batch".equals(config.mode)) {
            if (config.batchInputDir == null || config.batchOutputDir == null) {
                throw new IllegalArgumentException("batch mode requires batch.inputDir and batch.outputDir");
            }

            List<Path> inputFiles = listApibFiles(config.batchInputDir, config.batchRecursive);
            if (inputFiles.isEmpty()) {
                System.out.println("No .apib files found: " + config.batchInputDir.toAbsolutePath());
                return;
            }

            int converted = 0;
            for (Path inputFile : inputFiles) {
                Path relative = config.batchInputDir.relativize(inputFile);
                String outputFileName = replaceExtension(relative.getFileName().toString(), ".yaml");
                Path outputRelative = relative.resolveSibling(outputFileName);
                Path outputFile = config.batchOutputDir.resolve(outputRelative);
                convertOne(inputFile, outputFile);
                converted++;
            }

            System.out.println("Batch converted: " + converted + " file(s)");
            System.out.println("Input dir      : " + config.batchInputDir.toAbsolutePath());
            System.out.println("Output dir     : " + config.batchOutputDir.toAbsolutePath());
            return;
        }

        throw new IllegalArgumentException("Unknown mode in config: " + config.mode + " (allowed: single, batch)");
    }

    private static ConverterConfig loadConfig(Path configPath) throws IOException {
        if (!Files.exists(configPath)) {
            throw new IllegalArgumentException("Config file not found: " + configPath.toAbsolutePath());
        }

        Properties props = new Properties();
        try (InputStream in = Files.newInputStream(configPath)) {
            props.load(in);
        }

        String mode = props.getProperty("mode", "batch").trim().toLowerCase(Locale.ROOT);
        Path singleInput = pathOrNull(props.getProperty("single.input"));
        Path singleOutput = pathOrNull(props.getProperty("single.output"));
        Path batchInputDir = pathOrNull(props.getProperty("batch.inputDir", "input"));
        Path batchOutputDir = pathOrNull(props.getProperty("batch.outputDir", "output"));
        boolean batchRecursive = Boolean.parseBoolean(props.getProperty("batch.recursive", "false"));

        return new ConverterConfig(mode, singleInput, singleOutput, batchInputDir, batchOutputDir, batchRecursive);
    }

    private static Path pathOrNull(String raw) {
        if (raw == null) {
            return null;
        }
        String trimmed = raw.trim();
        if (trimmed.isEmpty()) {
            return null;
        }
        return Path.of(trimmed);
    }

    private static void printUsage() {
        System.out.println("Usage:");
        System.out.println("  java -jar converter.jar <input.apib> <output.yaml>");
        System.out.println("  java -jar converter.jar [--config <file>] ");
        System.out.println("  java -jar converter.jar --help");
        System.out.println();
        System.out.println("Config defaults:");
        System.out.println("  --config converter.properties");
        System.out.println("  mode=batch");
        System.out.println("  batch.inputDir=input");
        System.out.println("  batch.outputDir=output");
        System.out.println("  batch.recursive=false");
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
        private final Path configPath;
        private final boolean showHelp;

        private Arguments(Path configPath, boolean showHelp) {
            this.configPath = configPath;
            this.showHelp = showHelp;
        }

        private static Arguments parse(String[] args) {
            Path configPath = Path.of("converter.properties");
            boolean showHelp = false;
            List<String> errors = new ArrayList<>();

            for (int i = 0; i < args.length; i++) {
                String arg = args[i];
                switch (arg) {
                    case "--config":
                        if (i + 1 >= args.length) {
                            errors.add("Missing value for --config");
                        } else {
                            configPath = Path.of(args[++i]);
                        }
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

            return new Arguments(configPath, showHelp);
        }
    }

    private static final class ConverterConfig {
        private final String mode;
        private final Path singleInputFile;
        private final Path singleOutputFile;
        private final Path batchInputDir;
        private final Path batchOutputDir;
        private final boolean batchRecursive;

        private ConverterConfig(
                String mode,
                Path singleInputFile,
                Path singleOutputFile,
                Path batchInputDir,
                Path batchOutputDir,
                boolean batchRecursive
        ) {
            this.mode = mode;
            this.singleInputFile = singleInputFile;
            this.singleOutputFile = singleOutputFile;
            this.batchInputDir = batchInputDir;
            this.batchOutputDir = batchOutputDir;
            this.batchRecursive = batchRecursive;
        }
    }
}
