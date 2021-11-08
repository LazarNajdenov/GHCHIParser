import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.visitor.VoidVisitorAdapter;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.stream.Stream;


public class ParseMethods {

    public static void listClasses(String filePath) {
        new VoidVisitorAdapter<Object>() {
            @Override
            public void visit(MethodDeclaration n, Object arg) {
                super.visit(n, arg);
                System.out.println(" * " + n.getJavadocComment());
                System.out.println(" * " + n.getNameAsString());
                System.out.println(" * " + n.getDeclarationAsString());
                System.out.println(" * " + n.getBody());
                System.out.println();
            }
        }.visit(StaticJavaParser.parse(filePath), null);
        System.out.println();
    }

    public static void main(String[] args) throws Exception {
        File file = new File("/path/to/java/file.java");
        StringBuilder text = new StringBuilder();
        Stream<String> stream = Files.lines(Paths.get(String.valueOf(file)));
        stream.forEach(text::append);
        listClasses(text.toString());
    }
}