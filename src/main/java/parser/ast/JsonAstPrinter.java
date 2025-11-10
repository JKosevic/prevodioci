package parser.ast;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

public final class JsonAstPrinter
        implements Ast.Expr.Visitor<JsonNode>, Ast.Stmt.Visitor<JsonNode> {

    private static final ObjectMapper M = new ObjectMapper();

    public String print(Ast.Program p) {
        try {
            ObjectNode root = M.createObjectNode();
            root.put("type", "program");
            root.put("hasBattleMain", p.hasBattleMain);

            ArrayNode items = M.createArrayNode();
            for (Ast.TopItem it : p.items) {
                items.add(printTopItem(it));
            }
            root.set("items", items);

            return M.writerWithDefaultPrettyPrinter().writeValueAsString(root);
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }
    }

    private JsonNode printTopItem(Ast.TopItem it) {
        if (it instanceof Ast.TopVarDecl v) {
            ObjectNode o = M.createObjectNode();
            o.put("kind", "topVarDecl");
            o.set("decl", v.decl.accept(this));
            return o;
        }

        if (it instanceof Ast.TopStmt s) {
            ObjectNode o = M.createObjectNode();
            o.put("kind", "topStmt");
            o.set("stmt", s.stmt.accept(this));
            return o;
        }

        if (it instanceof Ast.FuncDef f) {
            ObjectNode o = M.createObjectNode();
            o.put("kind", "funcDef");
            o.put("name", f.name.lexeme);

            ObjectNode rt = M.createObjectNode();
            rt.put("kind", f.returnType.kind.toString());
            rt.put("rank", f.returnType.rank);
            if (f.returnType.baseTypeTok != null)
                rt.put("baseLexeme", f.returnType.baseTypeTok.lexeme);
            else
                rt.put("baseLexeme", "void");
            o.set("returnType", rt);

            ArrayNode params = M.createArrayNode();
            for (Ast.Param p : f.params) {
                ObjectNode pp = M.createObjectNode();
                pp.put("name", p.name.lexeme);

                ObjectNode t = M.createObjectNode();
                t.put("kind", p.type.kind.toString());
                t.put("rank", p.type.rank);
                if (p.type.baseTypeTok != null)
                    t.put("baseLexeme", p.type.baseTypeTok.lexeme);
                pp.set("type", t);

                params.add(pp);
            }
            o.set("params", params);

            ArrayNode body = M.createArrayNode();
            for (Ast.Stmt st : f.body) body.add(st.accept(this));
            o.set("body", body);

            return o;
        }

        ObjectNode u = M.createObjectNode();
        u.put("kind", "unknownTopItem");
        return u;
    }

    @Override
    public JsonNode visitLiteralInt(Ast.Expr.LiteralInt e) {
        ObjectNode o = M.createObjectNode();
        o.put("type", "literalInt");
        o.put("value", e.value);
        return o;
    }

    @Override
    public JsonNode visitLiteralDouble(Ast.Expr.LiteralDouble e) {
        ObjectNode o = M.createObjectNode();
        o.put("type", "literalDouble");
        o.put("value", e.value);
        return o;
    }

    @Override
    public JsonNode visitLiteralChar(Ast.Expr.LiteralChar e) {
        ObjectNode o = M.createObjectNode();
        o.put("type", "literalChar");
        o.put("codePoint", e.codePoint);
        return o;
    }

    @Override
    public JsonNode visitLiteralString(Ast.Expr.LiteralString e) {
        ObjectNode o = M.createObjectNode();
        o.put("type", "literalString");
        o.put("value", e.value);
        return o;
    }

    @Override
    public JsonNode visitIdent(Ast.Expr.Ident e) {
        ObjectNode o = M.createObjectNode();
        o.put("type", "ident");
        o.put("name", e.name.lexeme);
        return o;
    }

    @Override
    public JsonNode visitIndex(Ast.Expr.Index e) {
        ObjectNode o = M.createObjectNode();
        o.put("type", "index");
        o.put("name", e.name.lexeme);

        ArrayNode idx = M.createArrayNode();
        for (Ast.Expr ex : e.indices) idx.add(ex.accept(this));
        o.set("indices", idx);

        return o;
    }

    @Override
    public JsonNode visitGrouping(Ast.Expr.Grouping e) {
        ObjectNode o = M.createObjectNode();
        o.put("type", "group");
        o.set("inner", e.inner.accept(this));
        return o;
    }

    @Override
    public JsonNode visitCall(Ast.Expr.Call e) {
        ObjectNode o = M.createObjectNode();
        o.put("type", "call");
        o.put("name", e.callee.lexeme);

        ArrayNode args = M.createArrayNode();
        for (Ast.Expr ex : e.args) args.add(ex.accept(this));
        o.set("args", args);

        return o;
    }

    @Override
    public JsonNode visitUnary(Ast.Expr.Unary e) {
        ObjectNode o = M.createObjectNode();
        o.put("type", "unary");
        o.put("op", e.op.lexeme);
        o.set("expr", e.expr.accept(this));
        return o;
    }

    @Override
    public JsonNode visitBinary(Ast.Expr.Binary e) {
        ObjectNode o = M.createObjectNode();
        o.put("type", "binary");
        o.put("op", e.op.lexeme);
        o.set("left", e.left.accept(this));
        o.set("right", e.right.accept(this));
        return o;
    }

    @Override
    public JsonNode visitTernary(Ast.Expr.Ternary e) {
        ObjectNode o = M.createObjectNode();
        o.put("type", "ternary");
        o.set("cond", e.condition.accept(this));
        o.set("then", e.thenExpr.accept(this));
        o.set("elseExpr", e.elseExpr.accept(this));
        return o;
    }

    @Override
    public JsonNode visitVarDecl(Ast.Stmt.VarDecl s) {
        ObjectNode o = M.createObjectNode();
        o.put("stmt", "varDecl");

        ObjectNode tt = M.createObjectNode();
        tt.put("kind", s.type.kind.toString());
        tt.put("rank", s.type.rank);
        if (s.type.baseTypeTok != null)
            tt.put("baseLexeme", s.type.baseTypeTok.lexeme);
        o.set("type", tt);

        ArrayNode dims = M.createArrayNode();
        for (Ast.Expr d : s.dims) dims.add(d.accept(this));
        o.set("dims", dims);

        ArrayNode names = M.createArrayNode();
        for (var t : s.names) names.add(t.lexeme);
        o.set("names", names);

        return o;
    }

    @Override
    public JsonNode visitAssign(Ast.Stmt.Assign s) {
        ObjectNode o = M.createObjectNode();
        o.put("stmt", "assign");

        o.set("left", s.left.accept(this));

        ObjectNode lv = M.createObjectNode();
        lv.put("name", s.lvalue.name.lexeme);

        ArrayNode idx = M.createArrayNode();
        for (Ast.Expr e : s.lvalue.indices) idx.add(e.accept(this));
        lv.set("indices", idx);

        o.set("lvalue", lv);

        return o;
    }

    @Override
    public JsonNode visitCallStmt(Ast.Stmt.CallStmt s) {
        ObjectNode o = M.createObjectNode();
        o.put("stmt", "callStmt");
        o.set("call", s.call.accept(this));
        return o;
    }

    @Override
    public JsonNode visitReturn(Ast.Stmt.Return s) {
        ObjectNode o = M.createObjectNode();
        o.put("stmt", "return");
        if (s.expr != null)
            o.set("expr", s.expr.accept(this));
        else
            o.putNull("expr");
        return o;
    }

    @Override
    public JsonNode visitBeginIf(Ast.Stmt.BeginIf s) {
        ObjectNode o = M.createObjectNode();
        o.put("stmt", "if");

        ObjectNode ifo = M.createObjectNode();
        ifo.set("cond", s.ifArm.cond.accept(this));

        ArrayNode fb = M.createArrayNode();
        for (Ast.Stmt st : s.ifArm.block) fb.add(st.accept(this));
        ifo.set("block", fb);

        o.set("if", ifo);


        ArrayNode elders = M.createArrayNode();
        for (Ast.Stmt.BeginIf.Arm a : s.elderArms) {
            ObjectNode eo = M.createObjectNode();
            eo.set("cond", a.cond.accept(this));

            ArrayNode bb = M.createArrayNode();
            for (Ast.Stmt st : a.block) bb.add(st.accept(this));
            eo.set("block", bb);

            elders.add(eo);
        }
        o.set("elder", elders);

        if (s.elseBlock != null) {
            ArrayNode eb = M.createArrayNode();
            for (Ast.Stmt st : s.elseBlock) eb.add(st.accept(this));
            o.set("member", eb);
        }

        return o;
    }

    @Override
    public JsonNode visitBeginCycle(Ast.Stmt.BeginCycle s) {
        ObjectNode o = M.createObjectNode();
        o.put("stmt", "cycle");

        if (s.init != null) o.set("init", s.init.accept(this));
        else o.putNull("init");

        if (s.cond != null) o.set("cond", s.cond.accept(this));
        else o.putNull("cond");

        if (s.step != null) o.set("step", s.step.accept(this));
        else o.putNull("step");

        ArrayNode bod = M.createArrayNode();
        for (Ast.Stmt st : s.body) bod.add(st.accept(this));
        o.set("body", bod);

        return o;
    }
}
