package parser.ast;

import lexer.token.Token;
import lexer.token.TokenType;

import java.util.ArrayList;
import java.util.List;

public final class ParserAst {

    private final List<Token> tokens;
    private int current = 0;

    public ParserAst(List<Token> tokens) {
        this.tokens = tokens;
    }


    public Ast.Program parseProgram() {
        boolean hasBattleMain = false;
        List<Ast.TopItem> items = new ArrayList<>();

        while (!isAtEnd()) {
            if (match(TokenType.BATTLE)) {
                items.add(parseFunction());
                hasBattleMain = true;
            } else if (checkTypeKeyword()) {
                items.add(new Ast.TopVarDecl(parseVarDecl()));
            } else {
                items.add(new Ast.TopStmt(parseStatement()));
            }
        }

        return new Ast.Program(hasBattleMain, items);
    }


    private Ast.FuncDef parseFunction() {
        Token name = consume(TokenType.IDENT, "čekao sam ime funkcije");

        consume(TokenType.LPAREN, "čekao sam '('");

        List<Ast.Param> params = new ArrayList<>();
        if (!check(TokenType.RPAREN)) {
            do {
                Ast.Type t = parseType();
                Token pname = consume(TokenType.IDENT, "čekao sam ime parametra");
                params.add(new Ast.Param(pname, t));
            } while (match(TokenType.COMMA));
        }

        consume(TokenType.RPAREN, "čekao sam ')'");

        // @tip@
        consume(TokenType.AT_TYPE, "čekao sam '@'");
        Ast.Type returnType = parseType();
        consume(TokenType.AT_TYPE, "čekao sam '@'");

        List<Ast.Stmt> body = parseBlock();

        return new Ast.FuncDef(name, params, returnType, body);
    }


    // Block
    private List<Ast.Stmt> parseBlock() {
        consume(TokenType.BLOCK_START, "čekao sam '#'");

        List<Ast.Stmt> stmts = new ArrayList<>();
        while (!check(TokenType.BLOCK_END) && !isAtEnd()) {
            stmts.add(parseStatement());
        }

        consume(TokenType.BLOCK_END, "čekao sam '$'");
        return stmts;
    }

    // STATEMENT
    private Ast.Stmt parseStatement() {
        if (match(TokenType.KRAJ_BORBE)) return parseReturn();
        if (match(TokenType.LEADER)) return parseIf();
        if (match(TokenType.ELDER)) error(peek(), "elder bez leader-a");
        if (match(TokenType.MEMBER)) error(peek(), "member bez leader-a");
        if (match(TokenType.CYCLE)) return parseCycle();

        if (checkTypeKeyword()) return parseVarDecl();

        return parseAssignOrCall();
    }


    // RETURN
    private Ast.Stmt.Return parseReturn() {
        if (match(TokenType.SEMICOLON)) {
            return new Ast.Stmt.Return(null);
        }

        Ast.Expr expr = parseExpression();
        consume(TokenType.SEMICOLON, "čekao sam ';'");
        return new Ast.Stmt.Return(expr);
    }


    // IF / ELDER / MEMBER
    private Ast.Stmt.BeginIf parseIf() {
        consume(TokenType.LPAREN, "čekao sam '('");
        Ast.Expr cond = parseExpression();
        consume(TokenType.RPAREN, "čekao sam ')'");
        List<Ast.Stmt> block = parseBlock();
        Ast.Stmt.BeginIf.Arm ifArm = new Ast.Stmt.BeginIf.Arm(cond, block);

        List<Ast.Stmt.BeginIf.Arm> elders = new ArrayList<>();

        while (match(TokenType.ELDER)) {
            consume(TokenType.LPAREN, "čekao sam '('");
            Ast.Expr ec = parseExpression();
            consume(TokenType.RPAREN, "čekao sam ')'");
            List<Ast.Stmt> eb = parseBlock();
            elders.add(new Ast.Stmt.BeginIf.Arm(ec, eb));
        }

        List<Ast.Stmt> elseBlock = null;
        if (match(TokenType.MEMBER)) {
            elseBlock = parseBlock();
        }

        return new Ast.Stmt.BeginIf(ifArm, elders, elseBlock);
    }


    // CYCLE
    private Ast.Stmt.BeginCycle parseCycle() {
        consume(TokenType.LPAREN, "čekao sam '('");

        Ast.Stmt init = null;
        if (!check(TokenType.SEMICOLON)) init = parseStatement();
        consume(TokenType.SEMICOLON, "");

        Ast.Expr cond = null;
        if (!check(TokenType.SEMICOLON)) cond = parseExpression();
        consume(TokenType.SEMICOLON, "");

        Ast.Stmt step = null;
        if (!check(TokenType.RPAREN)) step = parseStatement();
        consume(TokenType.RPAREN, "čekao sam ')'");

        List<Ast.Stmt> body = parseBlock();

        return new Ast.Stmt.BeginCycle(init, cond, step, body);
    }


    // VAR DECL
    private Ast.Stmt.VarDecl parseVarDecl() {
        Ast.Type t = parseType();

        List<Ast.Expr> dims = new ArrayList<>();
        while (match(TokenType.LBRACKET)) {
            Ast.Expr d = parseExpression();
            consume(TokenType.RBRACKET, "čekao sam ']'");
            dims.add(d);
        }

        List<Token> names = new ArrayList<>();
        names.add(consume(TokenType.IDENT, "čekao sam ime promenljive"));

        while (match(TokenType.COMMA)) {
            names.add(consume(TokenType.IDENT, "čekao sam ime promenljive"));
        }

        consume(TokenType.SEMICOLON, "čekao sam ';'");

        return new Ast.Stmt.VarDecl(t, dims, names);
    }


    // ASSIGN OR CALL
    private Ast.Stmt parseAssignOrCall() {
        Ast.Expr left = parseExpression();

        if (match(TokenType.ASSIGN)) {
            Ast.Stmt.LValue lv = parseLValue();
            consume(TokenType.SEMICOLON, "čekao sam ';'");
            return new Ast.Stmt.Assign(left, lv);
        }

        consume(TokenType.SEMICOLON, "čekao sam ';'");
        return new Ast.Stmt.CallStmt((Ast.Expr.Call) left);
    }

    private Ast.Stmt.LValue parseLValue() {
        Token name = consume(TokenType.IDENT, "čekao sam ime");

        List<Ast.Expr> indices = new ArrayList<>();
        while (match(TokenType.LBRACKET)) {
            Ast.Expr e = parseExpression();
            consume(TokenType.RBRACKET, "čekao sam ']'");
            indices.add(e);
        }

        return new Ast.Stmt.LValue(name, indices);
    }


    // TYPE
    private Ast.Type parseType() {
        Token base = null;
        Ast.Type.Kind kind;

        if (match(TokenType.BROJ_ELIXIRA)) { kind = Ast.Type.Kind.INT; base = previous(); }
        else if (match(TokenType.DOUBLE_ELIXIR)) { kind = Ast.Type.Kind.DOUBLE; base = previous(); }
        else if (match(TokenType.SLOVO_KARTICE)) { kind = Ast.Type.Kind.CHAR; base = previous(); }
        else if (match(TokenType.IME_KARTICE)) { kind = Ast.Type.Kind.STRING; base = previous(); }
        else if (match(TokenType.BEZ_ELIXIRA)) { kind = Ast.Type.Kind.VOID; base = previous(); }
        else throw error(peek(), "čekao sam tip");

        int rank = 0;
        while (match(TokenType.LBRACKET)) {
            consume(TokenType.RBRACKET, "čekao sam ']'");
            rank++;
        }

        return new Ast.Type(kind, base, rank);
    }

    private boolean checkTypeKeyword() {
        return check(TokenType.BROJ_ELIXIRA) ||
                check(TokenType.DOUBLE_ELIXIR) ||
                check(TokenType.SLOVO_KARTICE) ||
                check(TokenType.IME_KARTICE) ||
                check(TokenType.BEZ_ELIXIRA);
    }


    // EXPRESSIONS — kompletna hijerarhija
    private Ast.Expr parseExpression() { return parseTernary(); }

    private Ast.Expr parseTernary() {
        Ast.Expr cond = parseOr();
        if (match(TokenType.TERNARY_QMARK)) {
            Ast.Expr thenExpr = parseExpression();
            consume(TokenType.TERNARY_COLON, "čekao sam ':'");
            Ast.Expr elseExpr = parseExpression();
            return new Ast.Expr.Ternary(cond, thenExpr, elseExpr);
        }
        return cond;
    }

    private Ast.Expr parseOr() {
        Ast.Expr e = parseAnd();
        while (match(TokenType.LOG_OR)) {
            Token op = previous();
            Ast.Expr right = parseAnd();
            e = new Ast.Expr.Binary(e, op, right);
        }
        return e;
    }

    private Ast.Expr parseAnd() {
        Ast.Expr e = parseEquality();
        while (match(TokenType.LOG_AND)) {
            Token op = previous();
            Ast.Expr right = parseEquality();
            e = new Ast.Expr.Binary(e, op, right);
        }
        return e;
    }

    private Ast.Expr parseEquality() {
        Ast.Expr e = parseComparison();
        while (match(TokenType.EQ, TokenType.NEQ)) {
            Token op = previous();
            Ast.Expr r = parseComparison();
            e = new Ast.Expr.Binary(e, op, r);
        }
        return e;
    }

    private Ast.Expr parseComparison() {
        Ast.Expr e = parseTerm();
        while (match(TokenType.LT, TokenType.LE, TokenType.GT, TokenType.GE)) {
            Token op = previous();
            Ast.Expr r = parseTerm();
            e = new Ast.Expr.Binary(e, op, r);
        }
        return e;
    }

    private Ast.Expr parseTerm() {
        Ast.Expr e = parseFactor();
        while (match(TokenType.ADD, TokenType.SUB)) {
            Token op = previous();
            Ast.Expr r = parseFactor();
            e = new Ast.Expr.Binary(e, op, r);
        }
        return e;
    }

    private Ast.Expr parseFactor() {
        Ast.Expr e = parseUnary();
        while (match(TokenType.MULTIPLY, TokenType.DIVIDE, TokenType.PERCENT)) {
            Token op = previous();
            Ast.Expr r = parseUnary();
            e = new Ast.Expr.Binary(e, op, r);
        }
        return e;
    }

    private Ast.Expr parseUnary() {
        if (match(TokenType.LOG_NOT, TokenType.ADD, TokenType.SUB)) {
            Token op = previous();
            Ast.Expr r = parseUnary();
            return new Ast.Expr.Unary(op, r);
        }
        return parsePrimary();
    }

    private Ast.Expr parsePrimary() {

        // literal int
        if (match(TokenType.INT_LIT)) {
            Token tok = previous();
            long val = Long.parseLong(tok.lexeme);
            return new Ast.Expr.LiteralInt(tok, val);
        }

        // char
        if (match(TokenType.CHAR_LIT)) {
            Token tok = previous();
            return new Ast.Expr.LiteralChar(tok, tok.lexeme.codePointAt(0));
        }

        // string
        if (match(TokenType.STRING_LIT)) {
            Token tok = previous();
            return new Ast.Expr.LiteralString(tok, tok.literal.toString());
        }

        // ident + call/index
        if (match(TokenType.IDENT)) {
            Token name = previous();

            // CALL ime(...)
            if (match(TokenType.LPAREN)) {
                List<Ast.Expr> args = new ArrayList<>();
                if (!check(TokenType.RPAREN)) {
                    do args.add(parseExpression());
                    while (match(TokenType.COMMA));
                }
                consume(TokenType.RPAREN, "čekao sam ')'");
                return new Ast.Expr.Call(null, name, args);
            }

            // index
            List<Ast.Expr> idx = new ArrayList<>();
            while (match(TokenType.LBRACKET)) {
                Ast.Expr e = parseExpression();
                consume(TokenType.RBRACKET, "");
                idx.add(e);
            }
            if (!idx.isEmpty()) return new Ast.Expr.Index(name, idx);

            return new Ast.Expr.Ident(name);
        }

        // ( expr )
        if (match(TokenType.LPAREN)) {
            Ast.Expr e = parseExpression();
            consume(TokenType.RPAREN, "čekao sam ')'");
            return new Ast.Expr.Grouping(e);
        }

        throw error(peek(), "neočekivan token u izrazu");
    }


    // TOKEN helpers
    private boolean match(TokenType... types) {
        for (TokenType t : types) {
            if (check(t)) { advance(); return true; }
        }
        return false;
    }

    private boolean check(TokenType t) {
        if (isAtEnd()) return false;
        return peek().type == t;
    }

    private Token consume(TokenType t, String msg) {
        if (check(t)) return advance();
        throw error(peek(), msg);
    }

    private Token advance() {
        if (!isAtEnd()) current++;
        return previous();
    }

    private boolean isAtEnd() { return peek().type == TokenType.EOF; }
    private Token peek() { return tokens.get(current); }
    private Token previous() { return tokens.get(current - 1); }

    private RuntimeException error(Token t, String msg) {
        return new RuntimeException("Parser error at line " + t.line + ": " + msg);
    }
}
