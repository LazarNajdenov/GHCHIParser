package ch.usi.msde.sa.ghchi.parser;

import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.comments.Comment;
import com.github.javaparser.ast.comments.JavadocComment;
import com.github.javaparser.ast.stmt.BlockStmt;
import com.github.javaparser.ast.visitor.VoidVisitorAdapter;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Scanner;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import com.google.common.base.CharMatcher;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.StringUtils;

public class ParseMethods {

    static String bin = "/bin/sh";
    static String directory = "/tmp/clonedRepo";

    public static void main(String[] args) throws Exception {
        String csvFileName = args[0];
        Scanner scanner = new Scanner(new File("./repolist.txt"));
        while (scanner.hasNextLine()) {
            String repositoryName = scanner.nextLine();
            cloneRepository(repositoryName);
            File file = new File(directory + "/" + repositoryName);
            List<List<String>> methodLines = new ArrayList<>();
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

    private static void deleteClone(String repositoryName) throws IOException {
        File repoDir = new File(getRepositoryPath(repositoryName.split("/")[0]));
        FileUtils.deleteDirectory(repoDir);
    }


    /**
     * Methods used to iterate through all the java files in the directory and parse them successively
     * */
    public static void parseJavaFiles(File dir, List<List<String>> methodLines) throws IOException {
        String dirName = dir.getName();
        if (dir.isDirectory()) {
            File[] files = dir.listFiles();
            File[] validFiles = Objects.requireNonNull(files);
            for (File file : validFiles) {
                if (!canSkip(dirName, ".*(test).*")) parseJavaFiles(file, methodLines);
            }
        } else {
            boolean canSkip = canSkip(dirName, ".*(main)|(test).*");
            String extension = FilenameUtils.getExtension(dirName);
            if (!canSkip && extension.equals("java")) {
                parseClassMethods(dir, methodLines);
            }
        }
    }

    private static boolean canSkip(String name, String regex) {
        return Pattern.compile(regex, Pattern.CASE_INSENSITIVE).matcher(name).matches();
    }

    /**
     * Methods used for parsing the methods for each class
     * */
    private static void parseClassMethods(File file, List<List<String>> dataLines) throws FileNotFoundException {
        new VoidVisitorAdapter<>() {
            @Override
            public void visit(MethodDeclaration n, Object arg) {
                super.visit(n, arg);
                String name = n.getNameAsString();
                boolean isTestMethod = name.toLowerCase().contains("test");
                Optional<BlockStmt> methodBody = n.getBody();
                if (methodBody.isPresent() && !isTestMethod) {
                    String javaDoc = n.getJavadocComment()
                            .map(JavadocComment::toString)
                            .map(CharMatcher.ascii()::retainFrom)
                            .map(StringUtils::normalizeSpace)
                            // Apply stopword removal
                            // Apply * removal
                            .map(comment -> comment + " <SEP> ")
                            .orElse("");

                    String signature = n.getType() + " " + n.getNameAsString();
                    String parameters = n.getParameters().stream()
                            .map(Node::toString)
                            .collect(Collectors.joining(", ", "(", ") "));

                    methodBody.get().getAllContainedComments().forEach(Comment::remove);

                    String body = methodBody
                            .map(BlockStmt::toString)
                            .map(CharMatcher.ascii()::retainFrom)
                            .map(StringUtils::normalizeSpace)
                            .orElse(";");

                    String method = javaDoc + signature + parameters + body;

                    dataLines.add(List.of(name, method));
                }
            }
        }.visit(StaticJavaParser.parse(file), null);
    }

    /**
     * Used for saving the extracted methods to a CSV file.
     *
     * @param filePath The path of the CSV file.
     */
    private static void saveMethods(String filePath, List<List<String>> dataLines) throws IOException {
        FileWriter writer = new FileWriter(filePath, true);
        Collections.shuffle(dataLines);
        dataLines = dataLines.stream().limit(1000).collect(Collectors.toList());

        CSVPrinter printer = new CSVPrinter(writer, CSVFormat.DEFAULT);
        for (List<String> line : dataLines) {
            printer.printRecord(line.get(0), line.get(1));
        }

        writer.close();
    }
}