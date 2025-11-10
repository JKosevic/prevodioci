// src/main/java/parser/ast/Stmt.java
package parser.ast;

import java.util.List;
import lexer.token.Token;

public abstract class Stmt {

    // ===== Visitor =====
    public interface Visitor<R> {
        R visitVarDecl(VarDecl s);
        R visitAssign(Assign s);
        R visitCallStmt(CallStmt s);
        R visitReturn(Return s);
        R visitBeginIf(BeginIf s);
        R visitBeginCycle(BeginCycle s);
    }

    public abstract <R> R accept(Visitor<R> v);

    // varDecl: tip dims? names
    public static final class VarDecl extends Stmt {
        public final Ast.Type type;     // bazni tip + rank (broj []), popunjava parser
        public final List<Expr> dims;   // konst dimenzije ako se navode uz tip (npr. [10][n])
        public final List<Token> names; // lista imena promenljivih
        public VarDecl(Ast.Type type, List<Expr> dims, List<Token> names) {
            this.type = type; this.dims = dims; this.names = names;
        }
        @Override public <R> R accept(Visitor<R> v) { return v.visitVarDecl(this); }
    }

    // leftExpr -> lvalue
    public static final class Assign extends Stmt {
        public final Expr left;
        public final LValue lvalue;
        public Assign(Expr left, LValue lvalue) { this.left = left; this.lvalue = lvalue; }
        @Override public <R> R accept(Visitor<R> v) { return v.visitAssign(this); }
    }

    // poziv kao naredba (bez dodele)
    public static final class CallStmt extends Stmt {
        public final Expr.Call call;
        public CallStmt(Expr.Call call) { this.call = call; }
        @Override public <R> R accept(Visitor<R> v) { return v.visitCallStmt(this); }
    }

    // krajBorbe [expr]
    public static final class Return extends Stmt {
        public final Expr expr; // može biti null za bezElixira/void
        public Return(Expr expr) { this.expr = expr; }
        @Override public <R> R accept(Visitor<R> v) { return v.visitReturn(this); }
    }

    // leader / elder* / member
    public static final class BeginIf extends Stmt {
        public static final class Arm {
            public final Expr cond;
            public final List<Stmt> block;
            public Arm(Expr cond, List<Stmt> block) { this.cond = cond; this.block = block; }
        }
        public final Arm ifArm;
        public final List<Arm> elderArms;   // else-if grane
        public final List<Stmt> elseBlock;  // member blok (može biti null)
        public BeginIf(Arm ifArm, List<Arm> elderArms, List<Stmt> elseBlock) {
            this.ifArm = ifArm; this.elderArms = elderArms; this.elseBlock = elseBlock;
        }
        @Override public <R> R accept(Visitor<R> v) { return v.visitBeginIf(this); }
    }

    // cycle(init; cond; step) # body $
    public static final class BeginCycle extends Stmt {
        public final Expr init;   // može biti null
        public final Expr cond;   // može biti null
        public final Expr step;   // može biti null
        public final List<Stmt> body;
        public BeginCycle(Expr init, Expr cond, Expr step, List<Stmt> body) {
            this.init = init; this.cond = cond; this.step = step; this.body = body;
        }
        @Override public <R> R accept(Visitor<R> v) { return v.visitBeginCycle(this); }
    }

    // pomoćna struktura za dodelu u niz/element
    public static final class LValue {
        public final Token name;          // IDENT
        public final List<Expr> indices;  // []...[]
        public LValue(Token name, List<Expr> indices) { this.name = name; this.indices = indices; }
    }
}
