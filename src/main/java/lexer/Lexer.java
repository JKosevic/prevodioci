package lexer;

import lexer.token.Token;
import lexer.token.TokenType;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class Lexer {
    private final ScannerCore sc;
    private final String source;
    private final List<Token> tokens = new ArrayList<>();

    private static final Map<String, TokenType> KEYWORDS = Map.ofEntries(

            // --- Tipovi podataka ---
            Map.entry("brojElixira", TokenType.BROJ_ELIXIRA),
            Map.entry("slovoKartice", TokenType.SLOVO_KARTICE),
            Map.entry("doubleElixir", TokenType.DOUBLE_ELIXIR),
            Map.entry("imeKartice", TokenType.IME_KARTICE),

            // --- Glavna funkcija ---
            Map.entry("battle", TokenType.BATTLE),

            // --- Povratna vrednost funkcije ---
            Map.entry("bezElixira", TokenType.BEZ_ELIXIRA),

            // --- Built-in funkcije ---
            Map.entry("ucitajKarticu", TokenType.UCITAJ_KARTICU),
            Map.entry("ispisiKarticu", TokenType.ISPISI_KARTICU),

            // --- Uslovne naredbe ---
            Map.entry("leader", TokenType.LEADER),   // if
            Map.entry("elder", TokenType.ELDER),     // else if
            Map.entry("member", TokenType.MEMBER),   // else

            // --- For / cycle ---
            Map.entry("cycle", TokenType.CYCLE),

            // --- Return ---
            Map.entry("krajBorbe", TokenType.KRAJ_BORBE)
    );

    public Lexer(String source) {
        this.source = source;
        this.sc = new ScannerCore(source);
    }

    public List<Token> scanTokens() {
        while (!sc.isAtEnd()) {
            sc.beginToken();
            scanToken();
        }
        tokens.add(new Token(TokenType.EOF, "\0", null, sc.getLine(), sc.getCol(), sc.getCol()));
        return tokens;
    }

    private void scanToken() {
        char c = sc.advance();

        switch (c) {
            // --- zagrade ---
            case '(' -> add(TokenType.LPAREN);
            case ')' -> add(TokenType.RPAREN);
            case '[' -> add(TokenType.LBRACKET);
            case ']' -> add(TokenType.RBRACKET);

            // --- blokovi i ternarni ---
            case '#' -> add(TokenType.BLOCK_START);          // početak bloka
            case '$' -> add(TokenType.BLOCK_END);            // kraj bloka
            case '{' -> add(TokenType.LBRACE_TERNARY);       // ternarni otvaranje
            case '}' -> add(TokenType.RBRACE_TERNARY);       // ternarni zatvaranje
            case '?' -> add(TokenType.TERNARY_QMARK);
            case ':' -> add(TokenType.TERNARY_COLON);

            // --- separatori / dodela ---
            case ',' -> add(TokenType.COMMA);
            case ';' -> add(TokenType.SEMICOLON);
            case '=' -> add(sc.match('=') ? TokenType.EQ : TokenType.ASSIGN); // == ili =

            // --- aritmetički ---
            case '+' -> {
                if (sc.match('+')) add(TokenType.INCREMENT);
                else if (sc.match('=')) add(TokenType.PLUS_ASSIGN);
                else add(TokenType.ADD);
            }

            case '-' -> {
                if (sc.match('-')) add(TokenType.DECREMENT);
                else if (sc.match('=')) add(TokenType.MINUS_ASSIGN);
                else add(TokenType.SUB);
            }

            case '*' -> add(TokenType.MULTIPLY);
            case '/' -> add(TokenType.DIVIDE);
            case '%' -> add(TokenType.PERCENT);

            // --- relacioni ---
            case '<' -> add(sc.match('=') ? TokenType.LE : TokenType.LT);
            case '>' -> add(sc.match('=') ? TokenType.GE : TokenType.GT);

            // --- logički ---
            case '&' -> add(TokenType.LOG_AND);
            case '|' -> add(TokenType.LOG_OR);
            case '!' -> add(sc.match('=') ? TokenType.NEQ : TokenType.LOG_NOT);

            case '"' -> {
                add(TokenType.QUOTE);

            }

            case '\'' -> {
                add(TokenType.APOSTROPHE);

            }

            // --- novi red / beline ---
            case '\n' -> tokens.add(new Token(
                    TokenType.NEWLINE, "\n", null, sc.getStartLine(), sc.getStartCol(), sc.getStartCol()
            ));
            case ' ', '\r', '\t' -> { /* skip */ }



            default -> {
                if (Character.isDigit(c)) {
                    number();     // podrži i double (npr. 20.00)
                } else if (isIdentStart(c)) {
                    identifier(); // prepoznaj keywords (battle, leader, elder, member, cycle, krajBorbe,
                    // brojElixira, slovoKartice, doubleElixir, imeKartice, ucitajKarticu, ispisiKarticu, bezElixira)
                    // + heks (HB4C) i oktal (O43) literale po potrebi
                } else {
                    throw error("Unexpected character: '" + c + "'");
                }
            }
        }
    }

    private void number() {
        while (Character.isDigit(sc.peek())) sc.advance();
        String text = source.substring(sc.getStartIdx(), sc.getCur());
        char nextChar = sc.peek();
        if (Character.isAlphabetic(nextChar)) {
            throw error("Error: Character in int literal");
        }
        addLiteralInt(text);
    }

    private void identifier() {
        while (isIdentPart(sc.peek())) sc.advance();
        String text = source.substring(sc.getStartIdx(), sc.getCur());
        TokenType type = KEYWORDS.getOrDefault(text, TokenType.IDENT);
        add(type, text);
    }

    private boolean isIdentStart(char c) { return Character.isLetter(c) || c == '_'; }
    private boolean isIdentPart(char c)  { return isIdentStart(c) || Character.isDigit(c); }

    private void add(TokenType type) {
        String lex = source.substring(sc.getStartIdx(), sc.getCur());
        tokens.add(new Token(type, lex, null,
                sc.getStartLine(), sc.getStartCol(), sc.getCol() - 1));
    }

    private void add(TokenType type, String text) {
        tokens.add(new Token(type, text, null,
                sc.getStartLine(), sc.getStartCol(), sc.getCol() - 1));
    }

    private void addLiteralInt(String literal) {
        tokens.add(new Token(TokenType.INT_LIT, literal, Integer.valueOf(literal),
                sc.getStartLine(), sc.getStartCol(), sc.getCol() - 1));
    }

    private RuntimeException error(String msg) {
        String near = source.substring(sc.getStartIdx(), Math.min(sc.getCur(), source.length()));
        return new RuntimeException("LEXER > " + msg + " at " + sc.getStartLine() + ":" + sc.getStartCol() + " near '" + near + "'");
    }
}
