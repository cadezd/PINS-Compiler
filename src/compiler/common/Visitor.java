/**
 * @ Author: turk
 * @ Description: Obiskovalec vozlišč AST.
 */

package compiler.common;

import compiler.parser.ast.def.*;
import compiler.parser.ast.expr.*;
import compiler.parser.ast.type.*;

public interface Visitor {
    /**
     * Izrazi.
     */
    void visit(Call call);
    void visit(Binary binary);
    void visit(Block block);
    void visit(For forLoop);
    void visit(Name name);
    void visit(IfThenElse ifThenElse);
    void visit(Literal literal);
    void visit(Unary unary);
    void visit(While whileLoop);
    void visit(Where where);

    /**
     * Definicije.
     */
    void visit(Defs defs);
    void visit(FunDef funDef);
    void visit(TypeDef typeDef);
    void visit(VarDef varDef);
    void visit(FunDef.Parameter parameter);

    /**
     * Tipi.
     */
    void visit(Array array);
    void visit(Atom atom);
    void visit(TypeName name);
}
