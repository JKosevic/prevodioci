package parser;

import java.util.List;
import lexer.token.Token;
import lexer.token.TokenType;


import java.util.List;
import lexer.token.Token;
import lexer.token.TokenType;

public final class RecognizerParser {

    private final List<Token> tokens;
    private int current = 0;

    public RecognizerParser(List<Token> tokens) { this.tokens = tokens; }


    public void parseProgram() {
        skipNewlines();
        while (!check(TokenType.EOF)) {
            parseFunDecl();
            skipNewlines();
        }
        consume(TokenType.EOF, "očekivao sam kraj fajla");
    }


    private void parseFunDecl() {
        skipNewlines();
        if (match(TokenType.AT_TYPE)) {
            consume(TokenType.IDENT, "očekivao sam ime funkcije");
            consume(TokenType.LPAREN, "očekivao sam '('");
            if (!check(TokenType.RPAREN)) parseParamList();
            consume(TokenType.RPAREN, "očekivao sam ')'");
            parseBlock();
            return;
        }
        consume(TokenType.BATTLE, "očekivao sam battle");
        consume(TokenType.LPAREN, "očekivao sam '('");
        consume(TokenType.RPAREN, "očekivao sam ')'");
        parseBlock();
    }


    private void parseParamList() {
        parseType();
        consume(TokenType.IDENT, "očekivao sam ime parametra");
        while (match(TokenType.COMMA)) {
            parseType();
            consume(TokenType.IDENT, "očekivao sam ime parametra");
        }
    }


    private void parseType() {
        if (!match(TokenType.BROJ_ELIXIRA, TokenType.SLOVO_KARTICE,
                TokenType.DOUBLE_ELIXIR, TokenType.IME_KARTICE,
                TokenType.BEZ_ELIXIRA)) {
            error(peek(), "očekivao sam tip");
        }
    }


    private void parseBlock() {
        consume(TokenType.BLOCK_START, "očekivao sam '#'");
        skipNewlines();
        while (!check(TokenType.BLOCK_END)) {
            parseStmt();
            skipNewlines();
        }
        consume(TokenType.BLOCK_END, "očekivao sam '$'");
    }


    private void parseStmt() {
        if (match(TokenType.NEWLINE)) return;


        if (check(TokenType.ISPISI_KARTICU) || check(TokenType.UCITAJ_KARTICU)) {
            advance();
            consume(TokenType.LPAREN, "očekivao sam '('");
            if (!check(TokenType.RPAREN)) {
                parseExpr();
                while (match(TokenType.COMMA)) parseExpr();
            }
            consume(TokenType.RPAREN, "očekivao sam ')'");
            consume(TokenType.SEMICOLON, "očekivao sam ';'");
            return;
        }


        if (startsType()) { parseVarDeclOrInit(); return; }

        // if/elif/member
        if (check(TokenType.LEADER)) { parseIf(); return; }
        if (check(TokenType.ELDER))  { error(peek(), "elder bez leader-a"); }
        if (check(TokenType.MEMBER)) { parseMember(); return; }

        // cycle
        if (check(TokenType.CYCLE)) { parseCycle(); return; }

        // return
        if (check(TokenType.KRAJ_BORBE)) { parseReturn(); return; }

        // ++i/--i
        if (match(TokenType.INCREMENT, TokenType.DECREMENT)) {
            consume(TokenType.IDENT, "očekivao sam identifikator");
            consume(TokenType.SEMICOLON, "očekivao sam ';'");
            return;
        }

        // i++ / i--
        if (check(TokenType.IDENT) && checkNext(TokenType.INCREMENT)) {
            consume(TokenType.IDENT, "očekivao sam ime");
            consume(TokenType.INCREMENT, "očekivao sam ++");
            consume(TokenType.SEMICOLON, "očekivao sam ';'");
            return;
        }
        if (check(TokenType.IDENT) && checkNext(TokenType.DECREMENT)) {
            consume(TokenType.IDENT, "očekivao sam ime");
            consume(TokenType.DECREMENT, "očekivao sam --");
            consume(TokenType.SEMICOLON, "očekivao sam ';'");
            return;
        }


        if (check(TokenType.IDENT)) {
            if (checkNext(TokenType.LPAREN)) {
                parseFuncCall();
                consume(TokenType.SEMICOLON, "očekivao sam ';'");
                return;
            }
            if (checkNext(TokenType.LBRACKET)) { parseArrayAssign(); return; }
            parseAssign(); return;
        }

        error(peek(), "očekivao sam izjavu");
    }

    private void parseVarDeclOrInit() {
        parseType();
        consume(TokenType.IDENT, "očekivao sam ime promenljive");
        if (match(TokenType.SEMICOLON)) return;
        if (match(TokenType.ASSIGN)) {
            parseExpr();
            consume(TokenType.SEMICOLON, "očekivao sam ';'");
            return;
        }
        error(peek(), "neispravna deklaracija");
    }

    private void parseAssign() {
        consume(TokenType.IDENT, "očekivao sam ime");
        consume(TokenType.ASSIGN, "očekivao sam '='");
        parseExpr();
        consume(TokenType.SEMICOLON, "očekivao sam ';'");
    }

    private void parseArrayAssign() {
        consume(TokenType.IDENT, "očekivao sam ime niza");
        consume(TokenType.LBRACKET, "očekivao sam '['");
        parseExpr();
        consume(TokenType.RBRACKET, "očekivao sam ']'");
        consume(TokenType.ASSIGN, "očekivao sam '='");
        parseExpr();
        consume(TokenType.SEMICOLON, "očekivao sam ';'");
    }

    private void parseFuncCall() {
        consume(TokenType.IDENT, "očekivao sam ime funkcije");
        consume(TokenType.LPAREN, "očekivao sam '('");
        if (!check(TokenType.RPAREN)) {
            parseExpr();
            while (match(TokenType.COMMA)) parseExpr();
        }
        consume(TokenType.RPAREN, "očekivao sam ')'");
    }

    private void parseIf() {
        consume(TokenType.LEADER, "očekivao sam leader");
        consume(TokenType.LPAREN, "očekivao sam '('");
        parseExpr();
        consume(TokenType.RPAREN, "očekivao sam ')'");
        parseBlock();
        while (match(TokenType.ELDER)) {
            consume(TokenType.LPAREN, "očekivao sam '('");
            parseExpr();
            consume(TokenType.RPAREN, "očekivao sam ')'");
            parseBlock();
        }
        if (match(TokenType.MEMBER)) parseBlock();
    }

    private void parseMember() {
        consume(TokenType.MEMBER, "očekivao sam member");
        parseBlock();
    }

    private void parseCycle() {
        consume(TokenType.CYCLE, "očekivao sam cycle");
        consume(TokenType.LPAREN, "očekivao sam '('");

        if (startsType()) parseVarDeclOrInit();
        else parseSimpleStmt();

        parseExpr();
        consume(TokenType.SEMICOLON, "očekivao sam ';'");

        parseSimpleStmt();

        consume(TokenType.RPAREN, "očekivao sam ')'");
        parseBlock();
    }

    private void parseSimpleStmt() {
        if (match(TokenType.INCREMENT, TokenType.DECREMENT)) {
            consume(TokenType.IDENT, "očekivao sam identifikator posle ++/--");
            return;
        }
        if (check(TokenType.IDENT)) {
            if (checkNext(TokenType.INCREMENT)) { consume(TokenType.IDENT,"očekivao sam ime"); consume(TokenType.INCREMENT,"očekivao sam ++"); return; }
            if (checkNext(TokenType.DECREMENT)) { consume(TokenType.IDENT,"očekivao sam ime"); consume(TokenType.DECREMENT,"očekivao sam --"); return; }
            if (checkNext(TokenType.LPAREN)) { parseFuncCall(); return; }
            parseAssign(); return;
        }
        error(peek(), "očekivao sam izraz u step delu cycle()");
    }

    private void parseReturn() {
        consume(TokenType.KRAJ_BORBE, "očekivao sam krajBorbe");
        if (!check(TokenType.SEMICOLON)) parseExpr();
        consume(TokenType.SEMICOLON, "očekivao sam ';'");
    }


    private void parseExpr() { parseTernary(); }

    private void parseTernary() {
        parseOr();
        if (match(TokenType.LBRACE_TERNARY)) {
            parseExpr();
            consume(TokenType.TERNARY_QMARK, "očekivao sam '?'");
            parseExpr();
            consume(TokenType.TERNARY_COLON, "očekivao sam ':'");
            parseExpr();
            consume(TokenType.RBRACE_TERNARY, "očekivao sam '}'");
        }
    }

    private void parseOr() {
        parseAnd();
        while (match(TokenType.LOG_OR)) parseAnd();
    }
    private void parseAnd() {
        parseEquality();
        while (match(TokenType.LOG_AND)) parseEquality();
    }
    private void parseEquality() {
        parseComparison();
        while (match(TokenType.EQ, TokenType.NEQ)) parseComparison();
    }
    private void parseComparison() {
        parseTerm();
        while (match(TokenType.LT, TokenType.LE, TokenType.GT, TokenType.GE)) parseTerm();
    }
    private void parseTerm() {
        parseFactor();
        while (match(TokenType.ADD, TokenType.SUB)) parseFactor();
    }
    private void parseFactor() {
        parseUnary();
        while (match(TokenType.MULTIPLY, TokenType.DIVIDE, TokenType.PERCENT)) parseUnary();
    }
    private void parseUnary() {
        if (match(TokenType.ADD, TokenType.SUB, TokenType.LOG_NOT,
                TokenType.INCREMENT, TokenType.DECREMENT)) {
            parseUnary(); return;
        }
        parsePrimary();
    }
    private void parsePrimary() {
        if (match(TokenType.INT_LIT, TokenType.DOUBLE_LIT, TokenType.STRING_LIT,
                TokenType.CHAR_LIT, TokenType.HEX_LIT, TokenType.OCT_LIT)) return;
        if (check(TokenType.IDENT) && checkNext(TokenType.LPAREN)) { parseFuncCall(); return; }
        if (match(TokenType.IDENT)) return;
        if (match(TokenType.LPAREN)) { parseExpr(); consume(TokenType.RPAREN, "očekivao sam ')'"); return; }
        error(peek(), "očekivao sam izraz");
    }


    private boolean startsType() {
        return check(TokenType.BROJ_ELIXIRA, TokenType.SLOVO_KARTICE,
                TokenType.DOUBLE_ELIXIR, TokenType.IME_KARTICE, TokenType.BEZ_ELIXIRA);
    }
    private boolean check(TokenType... types) {

        if (isAtEnd()) {
            for (TokenType t : types) if (t == TokenType.EOF) return true;
            return false;
        }
        for (TokenType t : types) if (peek().type == t) return true;
        return false;
    }
    private boolean checkNext(TokenType type) {
        if (isAtEnd() || current + 1 >= tokens.size()) return false;
        return tokens.get(current + 1).type == type;
    }
    private boolean match(TokenType... types) {
        for (TokenType t : types) if (check(t)) { advance(); return true; }
        return false;
    }
    private Token advance() { if (!isAtEnd()) current++; return previous(); }
    private boolean isAtEnd() { return peek().type == TokenType.EOF; }
    private Token peek() { return tokens.get(current); }
    private Token previous() { return tokens.get(current - 1); }
    private Token consume(TokenType type, String msg) {
        if (type == TokenType.EOF) {
            while (!isAtEnd()) {
                if (peek().type == TokenType.NEWLINE) { advance(); continue; }

                if (peek().lexeme != null && peek().lexeme.isBlank()) { advance(); continue; }
                break;
            }
        }
        if (check(type)) return advance();
        error(peek(), msg);
        return null;
    }
    private void skipNewlines() {
        while (match(TokenType.NEWLINE)) {

        }
    }


    private void error(Token t, String msg) {
        throw new RuntimeException("PARSER ERROR kod '" + t.lexeme + "' – " + msg +
                " (linija " + t.line + ", kol " + t.colStart + ")");
    }
}


