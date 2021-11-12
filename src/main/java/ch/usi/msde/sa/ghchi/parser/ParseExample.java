package ch.usi.msde.sa.ghchi.parser;

import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.comments.Comment;
import com.github.javaparser.ast.stmt.BlockStmt;
import com.github.javaparser.ast.visitor.VoidVisitorAdapter;
import com.google.common.base.CharMatcher;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;
import org.apache.commons.csv.CSVRecord;
import org.apache.commons.lang3.StringUtils;

import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Reader;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

class ParseExample {
    public static void main(String[] args) throws IOException {
        Reader in = new FileReader(args[0]);
        Iterable<CSVRecord> records = CSVFormat.DEFAULT.parse(in);
        List<String> methods = new ArrayList<>();
        for (CSVRecord record : records) {
            String methodStr = record.get(3);

            new VoidVisitorAdapter<>() {
                @Override
                public void visit(MethodDeclaration declaration, Object arg) {
                    super.visit(declaration, arg);
                    Optional<BlockStmt> bodyBlock = declaration.getBody();
                    if (bodyBlock.isPresent()) {
                        String signature = declaration.getType() + " " + declaration.getNameAsString();
                        String parameters = declaration.getParameters().stream()
                                .map(Node::toString)
                                .collect(Collectors.joining(", ", "(", ") "));

                        bodyBlock.get().getAllContainedComments().forEach(Comment::remove);

                        String body = bodyBlock
                                .map(BlockStmt::toString)
                                .map(CharMatcher.ascii()::retainFrom)
                                .map(StringUtils::normalizeSpace)
                                .orElse(";");

                        methods.add(signature + parameters + body);
                    }
                }
            }.visit(StaticJavaParser.parse("public class DummyClass {\n" + methodStr + "\n}"), null);

            FileWriter writer = new FileWriter("assignment_test_cleaned.csv");
            CSVPrinter printer = new CSVPrinter(writer, CSVFormat.DEFAULT);
            for (String method : methods) {
                printer.printRecord(method);
            }

            writer.close();
        }
    }
}
