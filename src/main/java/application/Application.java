package application;

import lexer.Lexer;
import lexer.token.Token;
import lexer.token.TokenFormatter;
import parser.RecognizerParser;
import parser.ast.ParserAst;
import parser.ast.Ast;
import parser.ast.JsonAstPrinter;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

public class Application {
    public static void main(String[] args) throws Exception {

        if (args.length != 1) {
            System.err.println("Usage: java application.Application <input-file-or-resource>");
            System.err.println("Primer: java application.Application test.txt  (u src/main/resources)");
            System.exit(1);
        }

        String inputName = args[0];


        String source = readFromResourcesOrFs(inputName);


        Lexer lexer = new Lexer(source);
        List<Token> tokens = lexer.scanTokens();
        System.out.println("LEXING OK");
        System.out.println(TokenFormatter.formatList(tokens));


        RecognizerParser recognizer = new RecognizerParser(tokens);
        recognizer.parseProgram();
        System.out.println("SYNTAX OK");


        ParserAst parser = new ParserAst(tokens);
        Ast.Program program = parser.parseProgram();
        System.out.println("AST OK");


        JsonAstPrinter printer = new JsonAstPrinter();
        String json = printer.print(program);

        System.out.println("JSON AST:");
        System.out.println(json);
    }

    private static String readFromResourcesOrFs(String name) throws Exception {

        String normalized = name.startsWith("/") ? name : "/" + name;
        try (InputStream is = Application.class.getResourceAsStream(normalized)) {
            if (is != null) {
                System.out.println("Učitavam sa classpath-a: " + normalized);
                return new String(is.readAllBytes(), StandardCharsets.UTF_8);
            }
        }


        Path p = Path.of(name);
        if (Files.exists(p)) {
            System.out.println("Učitavam sa fajl sistema: " + p.toAbsolutePath());
            return Files.readString(p, StandardCharsets.UTF_8);
        }


        throw new java.nio.file.NoSuchFileException(
                "Nije pronađeno ni kao resource (" + normalized + ") ni na fajl sistemu (" + p.toAbsolutePath() + ")");
    }
}