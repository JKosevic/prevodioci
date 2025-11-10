package parser.ast;

import java.util.List;
import java.util.ArrayList;
import lexer.token.Token;

/**
 * AST za Clash Royale jezik.
 * Korak 1/4: samo definicije čvorova + Visitor interfejsi.
 */
public final class Ast {

    // ===== Program / Top-level =====
    public static final class Program {
        // Da li je pronađena battle() funkcija (nije obavezno za AST,
        // ali korisno za semantiku / validaciju)
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
        public final Token name;     // IDENT (npr. battle, vracaVeciBroj)
        public final List<Param> params;
        public final Type returnType; // @bezElixira@ => VOID
        public final List<Stmt> body; // statements iz bloka # ... $

        public FuncDef(Token name, List<Param> params, Type returnType, List<Stmt> body) {
            this.name = name;
            this.params = params;
            this.returnType = returnType;
            this.body = body;
        }
    }

    public static final class Param {
        public final Token name; // IDENT
        public final Type type;
        public Param(Token name, Type type) { this.name = name; this.type = type; }
    }


    public static final class Type {
        public enum Kind { INT, DOUBLE, CHAR, STRING, VOID }
        public final Kind kind;
        public final Token baseTypeTok; // token za "brojElixira"/"doubleElixir"/"slovoKartice"/"imeKartice"/"bezElixira" (za VOID može biti null)
        public final int rank;          // broj [] (nizova); 0 je skalar

        public Type(Kind kind, Token baseTypeTok, int rank) {
            this.kind = kind;
            this.baseTypeTok = baseTypeTok;
            this.rank = rank;
        }
    }

    // Izrazi
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
            public final Token token; // INT_LIT / heks / oktal ako ih tokenizuješ posebno
            public final long value;
            public LiteralInt(Token token, long value) { this.token = token; this.value = value; }
            @Override public <R> R accept(Visitor<R> v) { return v.visitLiteralInt(this); }
        }

        public static final class LiteralDouble extends Expr {
            public final Token token; // DOUBLE_LIT
            public final double value;
            public LiteralDouble(Token token, double value) { this.token = token; this.value = value; }
            @Override public <R> R accept(Visitor<R> v) { return v.visitLiteralDouble(this); }
        }

        public static final class LiteralChar extends Expr {
            public final Token token; // CHAR_LIT
            public final int codePoint;
            public LiteralChar(Token token, int codePoint) { this.token = token; this.codePoint = codePoint; }
            @Override public <R> R accept(Visitor<R> v) { return v.visitLiteralChar(this); }
        }

        public static final class LiteralString extends Expr {
            public final Token token; // STRING_LIT
            public final String value;
            public LiteralString(Token token, String value) { this.token = token; this.value = value; }
            @Override public <R> R accept(Visitor<R> v) { return v.visitLiteralString(this); }
        }

        public static final class Ident extends Expr {
            public final Token name; // IDENT
            public Ident(Token name) { this.name = name; }
            @Override public <R> R accept(Visitor<R> v) { return v.visitIdent(this); }
        }

        /** IDENT [expr] [ [expr] ... ] */
        public static final class Index extends Expr {
            public final Token name;      // IDENT
            public final List<Expr> indices; // >=1
            public Index(Token name, List<Expr> indices) { this.name = name; this.indices = indices; }
            @Override public <R> R accept(Visitor<R> v) { return v.visitIndex(this); }
        }

        public static final class Grouping extends Expr {
            public final Expr inner;
            public Grouping(Expr inner) { this.inner = inner; }
            @Override public <R> R accept(Visitor<R> v) { return v.visitGrouping(this); }
        }

        /** CALL ime(args...)  — koristi se i za ucitajKarticu/ispisiKarticu */
        public static final class Call extends Expr {
            public final Token callTok; // sam CALL token (ako postoji), može biti null ako parser nema poseban CALL token
            public final Token callee;  // IDENT
            public final List<Expr> args;
            public Call(Token callTok, Token callee, List<Expr> args) {
                this.callTok = callTok; this.callee = callee; this.args = args;
            }
            @Override public <R> R accept(Visitor<R> v) { return v.visitCall(this); }
        }

        /** Unarni: +x, -x, !x, ++x, --x (prefiks varijanta) */
        public static final class Unary extends Expr {
            public final Token op;
            public final Expr expr;
            public Unary(Token op, Expr expr) { this.op = op; this.expr = expr; }
            @Override public <R> R accept(Visitor<R> v) { return v.visitUnary(this); }
        }

        /** Binarni: + - * / %  i  < <= > >= == !=  i  & | (logički)  i += -= (ako ih zadržiš kao izraze) */
        public static final class Binary extends Expr {
            public final Expr left;
            public final Token op;
            public final Expr right;
            public Binary(Expr left, Token op, Expr right) { this.left = left; this.op = op; this.right = right; }
            @Override public <R> R accept(Visitor<R> v) { return v.visitBinary(this); }
        }

        /** Ternarni: { cond ? thenExpr : elseExpr } */
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

    // ===== Izjave =====
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

        //Deklaracija: tip [dims] listaImena ;  (dims opcioni: npr. [N] [M]...) */
        public static final class VarDecl extends Stmt {
            public final Type type;            // bazni tip + rank (rank se može + dims kombinovati u semantici)
            public final List<Expr> dims;      // konkretne dimenzije (ako su date eksplicitno pre imena)
            public final List<Token> names;    // jedna ili više promenljivih
            public VarDecl(Type type, List<Expr> dims, List<Token> names) {
                this.type = type; this.dims = dims; this.names = names;
            }
            @Override public <R> R accept(Visitor<R> v) { return v.visitVarDecl(this); }
        }

        //L-value: ime i niz indeksa (0..k) */
        public static final class LValue {
            public final Token name;         // IDENT
            public final List<Expr> indices; // može biti prazna lista
            public LValue(Token name, List<Expr> indices) { this.name = name; this.indices = indices; }
        }

        //Dodela: expr -> lvalue (po tvom primeru) ili lvalue = expr (ako tako želiš). */
        public static final class Assign extends Stmt {
            public final Expr left;  // ono što dodeljujemo (izraz ili poziv)
            public final LValue lvalue;
            public Assign(Expr left, LValue lvalue) { this.left = left; this.lvalue = lvalue; }
            @Override public <R> R accept(Visitor<R> v) { return v.visitAssign(this); }
        }

        //Poziv kao izjava (bez korišćenja rezultata). */
        public static final class CallStmt extends Stmt {
            public final Expr.Call call;
            public CallStmt(Expr.Call call) { this.call = call; }
            @Override public <R> R accept(Visitor<R> v) { return v.visitCallStmt(this); }
        }

        //krajBorbe [expr] ; — ako je @bezElixira@ onda expr može biti null */
        public static final class Return extends Stmt {
            public final Expr expr; // može biti null
            public Return(Expr expr) { this.expr = expr; }
            @Override public <R> R accept(Visitor<R> v) { return v.visitReturn(this); }
        }

        //leader (...) # ... $  [ elder (...) # ... $ ]*  [ member # ... $ ] */
        public static final class BeginIf extends Stmt {
            public static final class Arm {
                public final Expr cond;
                public final List<Stmt> block;
                public Arm(Expr cond, List<Stmt> block) { this.cond = cond; this.block = block; }
            }
            public final Arm ifArm;
            public final List<Arm> elderArms;
            public final List<Stmt> elseBlock; // member blok (može biti null)

            public BeginIf(Arm ifArm, List<Arm> elderArms, List<Stmt> elseBlock) {
                this.ifArm = ifArm; this.elderArms = elderArms; this.elseBlock = elseBlock;
            }
            @Override public <R> R accept(Visitor<R> v) { return v.visitBeginIf(this); }
        }

        /** cycle( init ; cond ; step ) # body $  — zadržavamo sva tri dela kao izraze/izjave */
        public static final class BeginCycle extends Stmt {
            public final Stmt init;    // može biti VarDecl ili Assign ili CallStmt, ili null (u parseru dozvoli varijante)
            public final Expr cond;    // može biti null (beskonačna petlja)
            public final Stmt step;    // obično inkrement/dekrement/poziv; može biti null
            public final List<Stmt> body;

            public BeginCycle(Stmt init, Expr cond, Stmt step, List<Stmt> body) {
                this.init = init; this.cond = cond; this.step = step; this.body = body;
            }
            @Override public <R> R accept(Visitor<R> v) { return v.visitBeginCycle(this); }
        }
    }
}
