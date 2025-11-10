package parser.ast;

// src/main/java/parser/ast/Expr.java

import java.util.List;
import lexer.token.Token;

public abstract class Expr {

    // ===== Visitor =====
    public interface Visitor<R> {
        R visitLiteralInt(LiteralInt e);
        R visitLiteralDouble(LiteralDouble e);
        R visitLiteralChar(LiteralChar e);
        R visitLiteralString(LiteralString e);
        R visitIdent(Ident e);
        R visitIndex(Index e);
        R visitGrouping(Grouping e);
        R visitCall(Call e);
        R visitUnary(Unary e);
        R visitBinary(Binary e);
        R visitTernary(Ternary e);
    }

    public abstract <R> R accept(Visitor<R> v);

    // ===== Atomi / Literali =====
    public static final class LiteralInt extends Expr {
        public final Token token;   // INT_LIT
        public final int value;
        public LiteralInt(Token token, int value) { this.token = token; this.value = value; }
        @Override public <R> R accept(Visitor<R> v) { return v.visitLiteralInt(this); }
    }

    public static final class LiteralDouble extends Expr {
        public final Token token;   // DOUBLE_LIT
        public final double value;
        public LiteralDouble(Token token, double value) { this.token = token; this.value = value; }
        @Override public <R> R accept(Visitor<R> v) { return v.visitLiteralDouble(this); }
    }

    public static final class LiteralChar extends Expr {
        public final Token token;   // CHAR_LIT
        public final int codePoint;
        public LiteralChar(Token token, int codePoint) { this.token = token; this.codePoint = codePoint; }
        @Override public <R> R accept(Visitor<R> v) { return v.visitLiteralChar(this); }
    }

    public static final class LiteralString extends Expr {
        public final Token token;   // STRING_LIT
        public final String value;
        public LiteralString(Token token, String value) { this.token = token; this.value = value; }
        @Override public <R> R accept(Visitor<R> v) { return v.visitLiteralString(this); }
    }

    public static final class Ident extends Expr {
        public final Token name; // IDENT
        public Ident(Token name) { this.name = name; }
        @Override public <R> R accept(Visitor<R> v) { return v.visitIdent(this); }
    }

    // IDENT [expr] [expr] ...
    public static final class Index extends Expr {
        public final Token name; // IDENT
        public final List<Expr> indices;
        public Index(Token name, List<Expr> indices) { this.name = name; this.indices = indices; }
        @Override public <R> R accept(Visitor<R> v) { return v.visitIndex(this); }
    }

    public static final class Grouping extends Expr {
        public final Expr inner;
        public Grouping(Expr inner) { this.inner = inner; }
        @Override public <R> R accept(Visitor<R> v) { return v.visitGrouping(this); }
    }

    // f(args...)
    public static final class Call extends Expr {
        public final Token callee;       // IDENT
        public final List<Expr> args;
        public final Token callTok;      // npr. UCITAJ/ISPISI token ako ga čuvaš
        public Call(Token callTok, Token callee, List<Expr> args) {
            this.callTok = callTok; this.callee = callee; this.args = args;
        }
        @Override public <R> R accept(Visitor<R> v) { return v.visitCall(this); }
    }

    public static final class Unary extends Expr {
        public final Token op;   // LOG_NOT / +/- itd.
        public final Expr expr;
        public Unary(Token op, Expr expr) { this.op = op; this.expr = expr; }
        @Override public <R> R accept(Visitor<R> v) { return v.visitUnary(this); }
    }

    public static final class Binary extends Expr {
        public final Expr left;
        public final Token op; // ADD/SUB/MULTIPLY/DIVIDE/PERCENT ili LT/LE/GT/GE/EQ/NEQ
        public final Expr right;
        public Binary(Expr left, Token op, Expr right) { this.left = left; this.op = op; this.right = right; }
        @Override public <R> R accept(Visitor<R> v) { return v.visitBinary(this); }
    }

    // {cond ? thenExpr : elseExpr}
    public static final class Ternary extends Expr {
        public final Expr condition;
        public final Expr thenExpr;
        public final Expr elseExpr;
        public Ternary(Expr condition, Expr thenExpr, Expr elseExpr) {
            this.condition = condition; this.thenExpr = thenExpr; this.elseExpr = elseExpr;
        }
        @Override public <R> R accept(Visitor<R> v) { return v.visitTernary(this); }
    }
}

