package ch.usi.msde.sa.ghchi.parser;

import com.github.javaparser.ParseProblemException;
import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.NodeList;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.comments.Comment;
import com.github.javaparser.ast.comments.JavadocComment;
import com.github.javaparser.ast.stmt.BlockStmt;
import com.github.javaparser.ast.visitor.VoidVisitorAdapter;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.tuple.Pair;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Scanner;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class ParseMethods {

    static String bin = "/bin/sh";
    static String directory = "/tmp/clonedRepo";

    static int max_project_methods = 1000;

    public static void main(String[] args) throws Exception {
        String csvFileName = args[0];
        Scanner scanner = new Scanner(new File("./repolist.txt"));
        while (scanner.hasNextLine()) {
            String repositoryName = scanner.nextLine();
            cloneRepository(repositoryName);
            File file = new File(directory + "/" + repositoryName);
            List<Pair<String, String>> methodLines = new ArrayList<>();
            parseJavaFiles(file, methodLines);
            saveMethods(csvFileName, methodLines);
            deleteClone(repositoryName);
        }
    }

    /**
     * Helper methods that we used also in our original GHCHI
     * */
    private static String getRepositoryPath(String repositoryName) {
        return directory + "/" + repositoryName;
    }

    private static void cloneRepository(String repositoryName) throws IOException, InterruptedException {
        File repoDir = new File(getRepositoryPath(repositoryName.split("/")[0]));
        Files.createDirectories(repoDir.toPath());
        String[] cmd = new String[]{
                bin, "-c", String.format("git clone https://github.com/%s.git", repositoryName)
        };
        Process process = Runtime.getRuntime().exec(cmd, null, repoDir);
        process.waitFor();
    }

    private static void deleteClone(String repositoryName) {
        File repoDir = new File(getRepositoryPath(repositoryName.split("/")[0]));
        try {
            FileUtils.deleteDirectory(repoDir);
        } catch (Exception ignored) {
            //if clone deletion fails, just move on
        }
    }


    /**
     * Methods used to iterate through all the java files in the directory and parse them successively
     */
    @SuppressWarnings("ConstantConditions")
    public static void parseJavaFiles(File dir, List<Pair<String, String>> methodLines) {
        String dirName = dir.getName();
        if (dir.isDirectory()) {
            for (File file : dir.listFiles()) {
                if (!canSkip(dirName, ".*(test).*")) parseJavaFiles(file, methodLines);
            }
        } else {
            boolean canSkip = canSkip(dirName, ".*(main)|(test).*");
            String extension = FilenameUtils.getExtension(dirName);
            if (!canSkip && extension.equals("java")) {
                try {
                    parseClassMethods(dir, methodLines);
                } catch (ParseProblemException ignored) {
                    // ignore files that can not be parsed
                } catch (FileNotFoundException ignored) {
                    // ignore files that could not be located
                }
            }
        }
    }

    private static boolean canSkip(String name, String regex) {
        return Pattern.compile(regex, Pattern.CASE_INSENSITIVE).matcher(name).matches();
    }

    /**
     * Methods used for parsing the methods for each class
     */
    private static void parseClassMethods(File file, List<Pair<String, String>> dataLines) throws FileNotFoundException {
        new VoidVisitorAdapter<>() {
            @Override
            public void visit(MethodDeclaration declaration, Object arg) {
                super.visit(declaration, arg);
                String name = declaration.getNameAsString();
                Optional<BlockStmt> bodyBlock = declaration.getBody();
                int statements = bodyBlock.map(BlockStmt::getStatements).map(NodeList::size).orElse(0);
                boolean correctSize = statements > 2 && statements < 20;
                boolean isTestMethod = name.toLowerCase().contains("test");
                if (bodyBlock.isPresent() && !isTestMethod && correctSize) {
                    String javaDoc = declaration.getJavadocComment()
                            .map(JavadocComment::toString)
                            .map(StringProcessors::processJavadocString)
                            .orElse("");

                    String signature = declaration.getType() + " " + declaration.getNameAsString();
                    String parameters = declaration.getParameters().stream()
                            .map(Node::toString)
                            .collect(Collectors.joining(", ", "(", ") "));

                    bodyBlock.get().getAllContainedComments().forEach(Comment::remove);

                    String body = bodyBlock
                            .map(BlockStmt::toString)
                            .map(StringProcessors::processMethodString)
                            .orElse(";");

                    dataLines.add(Pair.of(name, javaDoc + signature + parameters + body));
                }
            }
        }.visit(StaticJavaParser.parse(file), null);
    }

    /**
     * Used for saving the extracted methods to a CSV file.
     *
     * @param filePath The path of the CSV file.
     */
    private static void saveMethods(String filePath, List<Pair<String, String>> dataLines) throws IOException {
        FileWriter writer = new FileWriter(filePath, true);
        Collections.shuffle(dataLines);
        dataLines = dataLines.stream().limit(max_project_methods).collect(Collectors.toList());

        CSVPrinter printer = new CSVPrinter(writer, CSVFormat.DEFAULT);
        for (Pair<String, String> line : dataLines) {
            printer.printRecord(line.getKey(), line.getValue());
        }

        writer.close();
    }
}