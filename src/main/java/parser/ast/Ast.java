package parser.ast;

import java.util.List;
import java.util.ArrayList;
import lexer.token.Token;


public final class Ast {


    public static final class Program {

        public final boolean hasBattleMain;
        public final List<TopItem> items;

        public Program(boolean hasBattleMain, List<TopItem> items) {
            this.hasBattleMain = hasBattleMain;
            this.items = items;
        }
    }

    public interface TopItem {}


    public static final class TopVarDecl implements TopItem {
        public final Stmt.VarDecl decl;
        public TopVarDecl(Stmt.VarDecl decl) { this.decl = decl; }
    }


    public static final class TopStmt implements TopItem {
        public final Stmt stmt;
        public TopStmt(Stmt stmt) { this.stmt = stmt; }
    }


    public static final class FuncDef implements TopItem {
        public final Token name;
        public final List<Param> params;
        public final Type returnType;
        public final List<Stmt> body;

        public FuncDef(Token name, List<Param> params, Type returnType, List<Stmt> body) {
            this.name = name;
            this.params = params;
            this.returnType = returnType;
            this.body = body;
        }
    }

    public static final class Param {
        public final Token name;
        public final Type type;
        public Param(Token name, Type type) { this.name = name; this.type = type; }
    }


    public static final class Type {
        public enum Kind { INT, DOUBLE, CHAR, STRING, VOID }
        public final Kind kind;
        public final Token baseTypeTok;
        public final int rank;

        public Type(Kind kind, Token baseTypeTok, int rank) {
            this.kind = kind;
            this.baseTypeTok = baseTypeTok;
            this.rank = rank;
        }
    }


    public static abstract class Expr {
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

        public static final class LiteralInt extends Expr {
            public final Token token;
            public final long value;
            public LiteralInt(Token token, long value) { this.token = token; this.value = value; }
            @Override public <R> R accept(Visitor<R> v) { return v.visitLiteralInt(this); }
        }

        public static final class LiteralDouble extends Expr {
            public final Token token;
            public final double value;
            public LiteralDouble(Token token, double value) { this.token = token; this.value = value; }
            @Override public <R> R accept(Visitor<R> v) { return v.visitLiteralDouble(this); }
        }

        public static final class LiteralChar extends Expr {
            public final Token token;
            public final int codePoint;
            public LiteralChar(Token token, int codePoint) { this.token = token; this.codePoint = codePoint; }
            @Override public <R> R accept(Visitor<R> v) { return v.visitLiteralChar(this); }
        }

        public static final class LiteralString extends Expr {
            public final Token token;
            public final String value;
            public LiteralString(Token token, String value) { this.token = token; this.value = value; }
            @Override public <R> R accept(Visitor<R> v) { return v.visitLiteralString(this); }
        }

        public static final class Ident extends Expr {
            public final Token name;
            public Ident(Token name) { this.name = name; }
            @Override public <R> R accept(Visitor<R> v) { return v.visitIdent(this); }
        }


        public static final class Index extends Expr {
            public final Token name;
            public final List<Expr> indices;
            public Index(Token name, List<Expr> indices) { this.name = name; this.indices = indices; }
            @Override public <R> R accept(Visitor<R> v) { return v.visitIndex(this); }
        }

        public static final class Grouping extends Expr {
            public final Expr inner;
            public Grouping(Expr inner) { this.inner = inner; }
            @Override public <R> R accept(Visitor<R> v) { return v.visitGrouping(this); }
        }


        public static final class Call extends Expr {
            public final Token callTok;
            public final Token callee;
            public final List<Expr> args;
            public Call(Token callTok, Token callee, List<Expr> args) {
                this.callTok = callTok; this.callee = callee; this.args = args;
            }
            @Override public <R> R accept(Visitor<R> v) { return v.visitCall(this); }
        }

        public static final class Unary extends Expr {
            public final Token op;
            public final Expr expr;
            public Unary(Token op, Expr expr) { this.op = op; this.expr = expr; }
            @Override public <R> R accept(Visitor<R> v) { return v.visitUnary(this); }
        }

        public static final class Binary extends Expr {
            public final Expr left;
            public final Token op;
            public final Expr right;
            public Binary(Expr left, Token op, Expr right) { this.left = left; this.op = op; this.right = right; }
            @Override public <R> R accept(Visitor<R> v) { return v.visitBinary(this); }
        }

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


    public static abstract class Stmt {
        public interface Visitor<R> {
            R visitVarDecl(VarDecl s);
            R visitAssign(Assign s);
            R visitCallStmt(CallStmt s);
            R visitReturn(Return s);
            R visitBeginIf(BeginIf s);
            R visitBeginCycle(BeginCycle s);
        }
        public abstract <R> R accept(Visitor<R> v);


        public static final class VarDecl extends Stmt {
            public final Type type;
            public final List<Expr> dims;
            public final List<Token> names;
            public VarDecl(Type type, List<Expr> dims, List<Token> names) {
                this.type = type; this.dims = dims; this.names = names;
            }
            @Override public <R> R accept(Visitor<R> v) { return v.visitVarDecl(this); }
        }


        public static final class LValue {
            public final Token name;
            public final List<Expr> indices;
            public LValue(Token name, List<Expr> indices) { this.name = name; this.indices = indices; }
        }


        public static final class Assign extends Stmt {
            public final Expr left;
            public final LValue lvalue;
            public Assign(Expr left, LValue lvalue) { this.left = left; this.lvalue = lvalue; }
            @Override public <R> R accept(Visitor<R> v) { return v.visitAssign(this); }
        }


        public static final class CallStmt extends Stmt {
            public final Expr.Call call;
            public CallStmt(Expr.Call call) { this.call = call; }
            @Override public <R> R accept(Visitor<R> v) { return v.visitCallStmt(this); }
        }


        public static final class Return extends Stmt {
            public final Expr expr;
            public Return(Expr expr) { this.expr = expr; }
            @Override public <R> R accept(Visitor<R> v) { return v.visitReturn(this); }
        }


        public static final class BeginIf extends Stmt {
            public static final class Arm {
                public final Expr cond;
                public final List<Stmt> block;
                public Arm(Expr cond, List<Stmt> block) { this.cond = cond; this.block = block; }
            }
            public final Arm ifArm;
            public final List<Arm> elderArms;
            public final List<Stmt> elseBlock;

            public BeginIf(Arm ifArm, List<Arm> elderArms, List<Stmt> elseBlock) {
                this.ifArm = ifArm; this.elderArms = elderArms; this.elseBlock = elseBlock;
            }
            @Override public <R> R accept(Visitor<R> v) { return v.visitBeginIf(this); }
        }


        public static final class BeginCycle extends Stmt {
            public final Stmt init;
            public final Expr cond;
            public final Stmt step;
            public final List<Stmt> body;

            public BeginCycle(Stmt init, Expr cond, Stmt step, List<Stmt> body) {
                this.init = init; this.cond = cond; this.step = step; this.body = body;
            }
            @Override public <R> R accept(Visitor<R> v) { return v.visitBeginCycle(this); }
        }
    }
}
