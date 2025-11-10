package application;

import lexer.Lexer;
import lexer.token.Token;
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

        // 1) UČITAJ PROGRAM (resources -> filesystem fallback)
        String source = readFromResourcesOrFs(inputName);

        // 2) LEXING
        Lexer lexer = new Lexer(source);
        List<Token> tokens = lexer.scanTokens();
        System.out.println("LEXING OK");

         //3) SINTAKSA
        RecognizerParser recognizer = new RecognizerParser(tokens);
        recognizer.parseProgram();
        System.out.println("SYNTAX OK");

        // 4) AST PARSER
        ParserAst parser = new ParserAst(tokens);
        Ast.Program program = parser.parseProgram();
        System.out.println("AST OK");

        // 5) JSON AST
        JsonAstPrinter printer = new JsonAstPrinter();
        String json = printer.print(program);

        System.out.println("JSON AST:");
        System.out.println(json);
    }

    private static String readFromResourcesOrFs(String name) throws Exception {
        // 1) Pokušaj kao resource (classpath) — radi za src/main/resources
        String normalized = name.startsWith("/") ? name : "/" + name;
        try (InputStream is = Application.class.getResourceAsStream(normalized)) {
            if (is != null) {
                System.out.println("Učitavam sa classpath-a: " + normalized);
                return new String(is.readAllBytes(), StandardCharsets.UTF_8);
            }
        }

        // 2) Fallback: pokušaj sa fajl sistema (relative/absolute path)
        Path p = Path.of(name);
        if (Files.exists(p)) {
            System.out.println("Učitavam sa fajl sistema: " + p.toAbsolutePath());
            return Files.readString(p, StandardCharsets.UTF_8);
        }

        // 3) Nije nađeno
        throw new java.nio.file.NoSuchFileException(
                "Nije pronađeno ni kao resource (" + normalized + ") ni na fajl sistemu (" + p.toAbsolutePath() + ")");
    }
}