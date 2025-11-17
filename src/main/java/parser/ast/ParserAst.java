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


        skipTrivia();


        while (!isAtEnd()) {

            if (check(TokenType.EOF)) break;

            if (check(TokenType.BATTLE) || check(TokenType.AT_TYPE)) {
                Ast.FuncDef f = parseFunction();
                if ("battle".equals(f.name.lexeme)) hasBattleMain = true;
                items.add(f);
            } else {

                skipTrivia();
                if (check(TokenType.EOF)) break;

                if (!(check(TokenType.BATTLE) || check(TokenType.AT_TYPE))) {
                    throw error(peek(), "očekivao sam definiciju funkcije (battle ili @tip@)");
                }
            }


            skipTrivia();
        }


        skipTrivia();
        consume(TokenType.EOF, "čekao sam kraj fajla");

        return new Ast.Program(hasBattleMain, items);
    }



    private Ast.FuncDef parseFunction() {
        if (match(TokenType.AT_TYPE)) {
            Token atTok = previous();
            Ast.Type returnType = atTypeToAstType(atTok);

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
            List<Ast.Stmt> body = parseBlock();
            return new Ast.FuncDef(name, params, returnType, body);
        }

        consume(TokenType.BATTLE, "čekao sam battle");

        Token name = new Token(TokenType.IDENT, "battle", null, peek().line, peek().colStart, peek().colEnd);
        consume(TokenType.LPAREN, "čekao sam '('");
        consume(TokenType.RPAREN, "čekao sam ')'");
        List<Ast.Stmt> body = parseBlock();

        Ast.Type retVoid = new Ast.Type(
                Ast.Type.Kind.VOID,
                new Token(TokenType.BEZ_ELIXIRA, "bezElixira", null, name.line, name.colStart, name.colEnd),
                0
        );
        return new Ast.FuncDef(name, List.of(), retVoid, body);
    }

    private Ast.Type atTypeToAstType(Token atTok) {
        String lx = atTok.lexeme;
        String inner = lx.substring(1, lx.length() - 1);
        Ast.Type.Kind kind;
        Token baseTok;
        switch (inner) {
            case "brojElixira" -> { kind = Ast.Type.Kind.INT;    baseTok = new Token(TokenType.BROJ_ELIXIRA, inner, null, atTok.line, atTok.colStart, atTok.colEnd); }
            case "doubleElixir" -> { kind = Ast.Type.Kind.DOUBLE; baseTok = new Token(TokenType.DOUBLE_ELIXIR, inner, null, atTok.line, atTok.colStart, atTok.colEnd); }
            case "slovoKartice" -> { kind = Ast.Type.Kind.CHAR;   baseTok = new Token(TokenType.SLOVO_KARTICE, inner, null, atTok.line, atTok.colStart, atTok.colEnd); }
            case "imeKartice" -> { kind = Ast.Type.Kind.STRING;   baseTok = new Token(TokenType.IME_KARTICE, inner, null, atTok.line, atTok.colStart, atTok.colEnd); }
            case "bezElixira" -> { kind = Ast.Type.Kind.VOID;     baseTok = new Token(TokenType.BEZ_ELIXIRA, inner, null, atTok.line, atTok.colStart, atTok.colEnd); }
            default -> throw error(atTok, "nepoznat tip u @type@");
        }
        return new Ast.Type(kind, baseTok, 0);
    }



    private List<Ast.Stmt> parseBlock() {
        consume(TokenType.BLOCK_START, "čekao sam '#'");
        skipNewlines();

        List<Ast.Stmt> stmts = new ArrayList<>();
        while (!check(TokenType.BLOCK_END) && !isAtEnd()) {

            if (match(TokenType.NEWLINE)) continue;

            stmts.add(parseStatement());
            skipNewlines();
        }

        consume(TokenType.BLOCK_END, "čekao sam '$'");
        return stmts;
    }


    private Ast.Stmt parseStatement() {

        if (match(TokenType.KRAJ_BORBE)) return parseReturn();

        // leader / elder / member / cycle
        if (match(TokenType.LEADER)) return parseIf();
        if (match(TokenType.ELDER))  throw error(peek(), "elder bez leader-a");
        if (match(TokenType.MEMBER)) throw error(peek(), "member bez leader-a");
        if (match(TokenType.CYCLE))  return parseCycle();

        // deklaracija
        if (checkTypeKeyword()) return parseVarDecl();
        if (check(TokenType.ISPISI_KARTICU) || check(TokenType.UCITAJ_KARTICU)) {
            return parseBuiltinCallStmt();
        }

        // IDENT
        if (check(TokenType.IDENT)) {

            if (checkNext(TokenType.LPAREN))  return parseFuncCallStmt();
            if (checkNext(TokenType.LBRACKET)) return parseArrayAssign();
            if (checkNext(TokenType.ASSIGN))   return parseAssignStmt();
            throw error(peek(), "posle imena očekujem '(', '[' ili '='");
        }

        throw error(peek(), "očekivao sam izjavu");
    }

    private Ast.Stmt parseFuncCallStmt() {
        Token name = consume(TokenType.IDENT, "čekao sam ime funkcije");
        consume(TokenType.LPAREN, "čekao sam '('");
        List<Ast.Expr> args = new ArrayList<>();
        if (!check(TokenType.RPAREN)) {
            do { args.add(parseExpression()); }
            while (match(TokenType.COMMA));
        }
        consume(TokenType.RPAREN, "čekao sam ')'");
        consume(TokenType.SEMICOLON, "čekao sam ';'");
        return new Ast.Stmt.CallStmt(new Ast.Expr.Call(null, name, args));
    }


    private Ast.Stmt parseArrayAssign() {
        Token name = consume(TokenType.IDENT, "čekao sam ime niza");
        List<Ast.Expr> indices = new ArrayList<>();
        do {
            consume(TokenType.LBRACKET, "čekao sam '['");
            Ast.Expr idx = parseExpression();
            consume(TokenType.RBRACKET, "čekao sam ']'");
            indices.add(idx);
        } while (check(TokenType.LBRACKET));

        consume(TokenType.ASSIGN, "čekao sam '='");
        Ast.Expr rhs = parseExpression();
        consume(TokenType.SEMICOLON, "čekao sam ';'");


        Ast.Stmt.LValue lv = new Ast.Stmt.LValue(name, indices);

        return new Ast.Stmt.Assign(rhs, lv);
    }


    private Ast.Stmt parseAssignStmt() {
        Token name = consume(TokenType.IDENT, "čekao sam ime");
        consume(TokenType.ASSIGN, "čekao sam '='");
        Ast.Expr rhs = parseExpression();
        consume(TokenType.SEMICOLON, "čekao sam ';'");

        Ast.Stmt.LValue lv = new Ast.Stmt.LValue(name, List.of());
        return new Ast.Stmt.Assign(rhs, lv);
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
        // LEADER je već match-ovan u parseStatement()

        // leader
        consume(TokenType.LPAREN, "čekao sam '('");
        Ast.Expr cond = parseExpression();
        consume(TokenType.RPAREN, "čekao sam ')'");
        List<Ast.Stmt> ifBlock = parseBlock();

        Ast.Stmt.BeginIf.Arm ifArm = new Ast.Stmt.BeginIf.Arm(cond, ifBlock);
        List<Ast.Stmt.BeginIf.Arm> elders = new ArrayList<>();
        List<Ast.Stmt> elseBlock = null;


        skipNewlines();


        while (match(TokenType.ELDER)) {
            consume(TokenType.LPAREN, "čekao sam '('");
            Ast.Expr ec = parseExpression();
            consume(TokenType.RPAREN, "čekao sam ')'");
            List<Ast.Stmt> eb = parseBlock();
            elders.add(new Ast.Stmt.BeginIf.Arm(ec, eb));


            skipNewlines();
        }


        if (match(TokenType.MEMBER)) {
            elseBlock = parseBlock();

        }

        return new Ast.Stmt.BeginIf(ifArm, elders, elseBlock);
    }


    // CYCLE
    private Ast.Stmt.BeginCycle parseCycle() {
        consume(TokenType.LPAREN, "čekao sam '('");

        // INIT
        Ast.Stmt init = null;
        if (!check(TokenType.SEMICOLON)) {
            init = parseCycleInitOrStep();
        }
        consume(TokenType.SEMICOLON, "čekao sam ';' posle init dela cycle()");

        // COND
        Ast.Expr cond = null;
        if (!check(TokenType.SEMICOLON)) {
            cond = parseExpression();
        }
        consume(TokenType.SEMICOLON, "čekao sam ';' posle cond dela cycle()");

        // STEP
        Ast.Stmt step = null;
        if (!check(TokenType.RPAREN)) {
            step = parseCycleInitOrStep();  // ← vraća Stmt; bez ';'
        }
        consume(TokenType.RPAREN, "čekao sam ')'");

        List<Ast.Stmt> body = parseBlock();
        return new Ast.Stmt.BeginCycle(init, cond, step, body);
    }


    private void parseCycleInit() {
        if (checkTypeKeyword()) {
            parseVarDecl();
            return;
        }

        Token id = consume(TokenType.IDENT, "čekao sam ime u init delu cycle()");
        consume(TokenType.ASSIGN, "čekao sam '=' u init delu cycle()");
        parseExpression();
        consume(TokenType.SEMICOLON, "čekao sam ';' posle init dela cycle()");
    }

    private Ast.Stmt parseCycleStep() {
        // prefix ++/--
        if (match(TokenType.INCREMENT, TokenType.DECREMENT)) {
            consume(TokenType.IDENT, "čekao sam identifikator posle ++/-- u step delu");
            return new Ast.Stmt.CallStmt(null);
        }

        if (check(TokenType.IDENT)) {
            // postfix i++ / i--
            if (checkNext(TokenType.INCREMENT) || checkNext(TokenType.DECREMENT)) {
                consume(TokenType.IDENT, "čekao sam ime u postfix ++/--");
                if (check(TokenType.INCREMENT)) consume(TokenType.INCREMENT, "očekivao sam '++'");
                else consume(TokenType.DECREMENT, "očekivao sam '--'");
                return new Ast.Stmt.CallStmt(null);
            }


            if (checkNext(TokenType.LPAREN)) {

                Token name = consume(TokenType.IDENT, "čekao sam ime funkcije");
                consume(TokenType.LPAREN, "čekao sam '('");
                if (!check(TokenType.RPAREN)) {
                    parseExpression();
                    while (match(TokenType.COMMA)) parseExpression();
                }
                consume(TokenType.RPAREN, "čekao sam ')'");
                return new Ast.Stmt.CallStmt(null);
            }


            consume(TokenType.IDENT, "čekao sam ime u step delu");
            consume(TokenType.ASSIGN, "čekao sam '=' u step delu");
            parseExpression();
            return new Ast.Stmt.CallStmt(null);
        }

        throw error(peek(), "očekivao sam izraz (++/--, poziv ili dodelu) u step delu cycle()");
    }

    // VAR DECL
    private Ast.Stmt.VarDecl parseVarDecl() {
        Ast.Type t = parseType();

        List<Ast.Expr> dims = new ArrayList<>();
        Token name = consume(TokenType.IDENT, "čekao sam ime promenljive");


        if (match(TokenType.ASSIGN)) {
            parseExpression();
        }

        consume(TokenType.SEMICOLON, "čekao sam ';'");
        List<Token> names = new ArrayList<>();
        names.add(name);

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


    // TIP
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


    // EXPRESSION
    private Ast.Expr parseExpression() { return parseTernary(); }


    private Ast.Expr parseTernary() {
        Ast.Expr base = parseOr();

        if (match(TokenType.LBRACE_TERNARY)) {
            Ast.Expr cond = parseExpression();
            consume(TokenType.TERNARY_QMARK, "čekao sam '?'");
            Ast.Expr thenE = parseExpression();
            consume(TokenType.TERNARY_COLON, "čekao sam ':'");
            Ast.Expr elseE = parseExpression();
            consume(TokenType.RBRACE_TERNARY, "čekao sam '}'");
            return new Ast.Expr.Ternary(cond, thenE, elseE);
        }
        return base;
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

        if (match(TokenType.DOUBLE_LIT)) {
            Token tok = previous();
            double val = Double.parseDouble(tok.lexeme);
            return new Ast.Expr.LiteralDouble(tok, val);
        }


        if (match(TokenType.HEX_LIT))  return new Ast.Expr.LiteralInt(previous(), 0L);
        if (match(TokenType.OCT_LIT))  return new Ast.Expr.LiteralInt(previous(), 0L);

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

            // CALL
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

        // expr
        if (match(TokenType.LPAREN)) {
            Ast.Expr e = parseExpression();
            consume(TokenType.RPAREN, "čekao sam ')'");
            return new Ast.Expr.Grouping(e);
        }

        throw error(peek(), "neočekivan token u izrazu");
    }
    private Ast.Expr.Call parseBuiltinCall() {
        Token callee = advance();
        consume(TokenType.LPAREN, "čekao sam '('");
        List<Ast.Expr> args = new ArrayList<>();
        if (!check(TokenType.RPAREN)) {
            do { args.add(parseExpression()); } while (match(TokenType.COMMA));
        }
        consume(TokenType.RPAREN, "čekao sam ')'");
        return new Ast.Expr.Call(null, callee, args);
    }



    private boolean match(TokenType... types) {
        for (TokenType t : types) {
            if (check(t)) { advance(); return true; }
        }
        return false;
    }

    private boolean check(TokenType... types) {
        TokenType cur = peek().type;
        for (TokenType t : types) if (cur == t) return true;
        return false;
    }
    private Token consume(TokenType t, String msg) {
        if (t == TokenType.EOF) {
            while (match(TokenType.NEWLINE)) {  }
        }
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
    private void skipNewlines() {
        while (match(TokenType.NEWLINE)) {  }
    }
    private Ast.Stmt parseInitOrSimpleInCycle() {
        if (checkTypeKeyword()) {
            return parseVarDecl();
        }

        //  prefiks ++/--
        if (match(TokenType.INCREMENT, TokenType.DECREMENT)) {
            Token op = previous();
            Token id = consume(TokenType.IDENT, "čekao sam identifikator posle " + op.lexeme);
            consume(TokenType.SEMICOLON, "čekao sam ';'");
            return new Ast.Stmt.CallStmt(new Ast.Expr.Call(null, id, List.of()));
        }

        //  postfix i++ / i--
        if (check(TokenType.IDENT) && (checkNext(TokenType.INCREMENT) || checkNext(TokenType.DECREMENT))) {
            Token id = consume(TokenType.IDENT, "čekao sam ime");
            if (check(TokenType.INCREMENT)) {
                consume(TokenType.INCREMENT, "očekivao sam '++'");
            } else {
                consume(TokenType.DECREMENT, "očekivao sam '--'");
            }
            consume(TokenType.SEMICOLON, "čekao sam ';'");
            return new Ast.Stmt.CallStmt(new Ast.Expr.Call(null, id, List.of()));
        }


        if (check(TokenType.IDENT) && checkNext(TokenType.LPAREN)) {
            Token name = consume(TokenType.IDENT, "čekao sam ime funkcije");
            consume(TokenType.LPAREN, "čekao sam '('");
            List<Ast.Expr> args = new ArrayList<>();
            if (!check(TokenType.RPAREN)) {
                do { args.add(parseExpression()); } while (match(TokenType.COMMA));
            }
            consume(TokenType.RPAREN, "čekao sam ')'");
            consume(TokenType.SEMICOLON, "čekao sam ';'");
            return new Ast.Stmt.CallStmt(new Ast.Expr.Call(null, name, args));
        }


        if (check(TokenType.IDENT)) {
            Token name = consume(TokenType.IDENT, "čekao sam ime");
            List<Ast.Expr> indices = new ArrayList<>();
            while (match(TokenType.LBRACKET)) {
                Ast.Expr e = parseExpression();
                consume(TokenType.RBRACKET, "čekao sam ']'");
                indices.add(e);
            }
            consume(TokenType.ASSIGN, "čekao sam '='");
            Ast.Expr rhs = parseExpression();
            consume(TokenType.SEMICOLON, "čekao sam ';'");
            Ast.Stmt.LValue lv = new Ast.Stmt.LValue(name, indices);
            return new Ast.Stmt.Assign(rhs, lv);
        }

        throw error(peek(), "očekivao sam init/step naredbu u cycle()");
    }
    private boolean checkNext(TokenType type) {
        if (isAtEnd()) return false;
        if (current + 1 >= tokens.size()) return false;
        return tokens.get(current + 1).type == type;
    }


    private Token peekNext() {
        if (current + 1 >= tokens.size()) return tokens.get(tokens.size() - 1);
        return tokens.get(current + 1);
    }
    private Ast.Stmt parseBuiltinCallStmt() {
        Token callee = advance();
        consume(TokenType.LPAREN, "čekao sam '('");
        List<Ast.Expr> args = new ArrayList<>();
        if (!check(TokenType.RPAREN)) {
            do { args.add(parseExpression()); }
            while (match(TokenType.COMMA));
        }
        consume(TokenType.RPAREN, "čekao sam ')'");
        consume(TokenType.SEMICOLON, "čekao sam ';'");


        return new Ast.Stmt.CallStmt(new Ast.Expr.Call(callee, callee, args));
    }
    private void skipTrivia() {
        while (true) {
            if (match(TokenType.NEWLINE)) continue;
            if (!isAtEnd()) {
                String lx = peek().lexeme;
                if (lx != null && lx.isBlank()) { advance(); continue; }
            }
            break;
        }
    }
    private Ast.Stmt parseCycleInitOrStep() {

        if (checkTypeKeyword()) {
            Ast.Type t = parseType();


            List<Ast.Expr> dims = new ArrayList<>();

            Token name = consume(TokenType.IDENT, "čekao sam ime promenljive");


            Ast.Expr initExpr = null;
            if (match(TokenType.ASSIGN)) {
                initExpr = parseExpression();
            }
            if (initExpr == null) {

                List<Token> names = new ArrayList<>();
                names.add(name);
                return new Ast.Stmt.VarDecl(t, dims, names);
            } else {

                Ast.Stmt.LValue lv = new Ast.Stmt.LValue(name, List.of());
                return new Ast.Stmt.Assign(initExpr, lv);
            }
        }


        if (match(TokenType.INCREMENT, TokenType.DECREMENT)) {
            Token op = previous();
            Token name = consume(TokenType.IDENT, "čekao sam identifikator posle " + op.lexeme);
            return makeIncDecAssign(name, op.type == TokenType.INCREMENT);
        }


        if (check(TokenType.IDENT)) {
            Token name = advance();

            // postfix i++ / i--
            if (match(TokenType.INCREMENT)) return makeIncDecAssign(name, true);
            if (match(TokenType.DECREMENT)) return makeIncDecAssign(name, false);

            if (match(TokenType.LPAREN)) {
                List<Ast.Expr> args = new ArrayList<>();
                if (!check(TokenType.RPAREN)) {
                    do { args.add(parseExpression()); } while (match(TokenType.COMMA));
                }
                consume(TokenType.RPAREN, "čekao sam ')'");
                return new Ast.Stmt.CallStmt(new Ast.Expr.Call(null, name, args));
            }


            List<Ast.Expr> idx = new ArrayList<>();
            while (match(TokenType.LBRACKET)) {
                Ast.Expr e = parseExpression();
                consume(TokenType.RBRACKET, "čekao sam ']'");
                idx.add(e);
            }


            if (match(TokenType.ASSIGN)) {
                Ast.Expr rhs = parseExpression();
                Ast.Stmt.LValue lv = new Ast.Stmt.LValue(name, idx);
                return new Ast.Stmt.Assign(rhs, lv);
            }

            throw error(peek(), "očekivao sam ++/--, poziv ili dodelu u init/step delu cycle()");
        }

        throw error(peek(), "očekivao sam naredbu u init/step delu cycle()");
    }
    private Ast.Stmt makeIncDecAssign(Token name, boolean increment) {
        Ast.Expr id = new Ast.Expr.Ident(name);
        Token opTok = increment
                ? new Token(TokenType.ADD, "+", null, name.line, name.colStart, name.colEnd)
                : new Token(TokenType.SUB, "-", null, name.line, name.colStart, name.colEnd);
        Token oneTok = new Token(TokenType.INT_LIT, "1", 1L, name.line, name.colStart, name.colEnd);

        Ast.Expr one = new Ast.Expr.LiteralInt(oneTok, 1L);
        Ast.Expr rhs = new Ast.Expr.Binary(id, opTok, one);

        Ast.Stmt.LValue lv = new Ast.Stmt.LValue(name, List.of());
        return new Ast.Stmt.Assign(rhs, lv);
    }


}
