package ch.usi.msde.sa.ghchi.parser;

import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.comments.Comment;
import com.github.javaparser.ast.stmt.BlockStmt;
import com.github.javaparser.ast.type.Type;
import com.github.javaparser.ast.visitor.VoidVisitorAdapter;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;
import org.apache.commons.csv.CSVRecord;

import java.io.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

class ParseExample {
    public static void main(String[] args) throws IOException {
        Reader in = new FileReader(args[0]);
        Iterable<CSVRecord> records = CSVFormat.DEFAULT.parse(in);
        List<String> methods = new ArrayList<>();
        List<String> methodsNames = new ArrayList<>();
        for (CSVRecord record : records) {
            String methodStr = record.get(3);

            new VoidVisitorAdapter<>() {
                @Override
                public void visit(MethodDeclaration declaration, Object arg) {
                    super.visit(declaration, arg);
                    Optional<BlockStmt> bodyBlock = declaration.getBody();
                    if (bodyBlock.isPresent()) {
                        declaration.getAnnotations().clear();
                        Type type = declaration.getType();
                        type.getAnnotations().clear();
                        type.getComment().ifPresent(type::remove);

                        String signature = declaration.getType() + " <extra_id_0>";
                        String parameters = declaration.getParameters().stream()
                                .peek(parameter -> {
                                    parameter.getAnnotations().clear();
                                    parameter.getComment().ifPresent(parameter::remove);
                                })
                                .map(Node::toString)
                                .collect(Collectors.joining(", ", "(", ") "));

                        bodyBlock.get().getAllContainedComments().forEach(Comment::remove);

                        String body = bodyBlock
                                .map(BlockStmt::toString)
                                .map(StringProcessors::processMethodString)
                                .orElse(";");

                        methods.add("<SEP> " + signature + parameters + body);
                        methodsNames.add(declaration.getNameAsString());
                    }
                }
            }.visit(StaticJavaParser.parse("public class DummyClass {\n" + methodStr + "\n}"), null);

            FileWriter writerInput = new FileWriter("challenge_inputs.txt");
            FileWriter writerTarget = new FileWriter("challenge_targets.txt");
            BufferedWriter bufferedWriterInput = new BufferedWriter(writerInput);
            BufferedWriter bufferedWriterTarget = new BufferedWriter(writerTarget);
            for (String method : methods) {
                bufferedWriterInput.write(method);
                bufferedWriterInput.newLine();
             }
            for (String methodName: methodsNames) {
                bufferedWriterTarget.write(methodName);
                bufferedWriterTarget.newLine();
            }
            bufferedWriterInput.close();
            bufferedWriterTarget.close();
            writerInput.close();
            writerTarget.close();
        }
    }
}
