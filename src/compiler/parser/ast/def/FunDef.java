/**
 * @Author: turk
 * @Description: Definicija funkcije.
 */

package compiler.parser.ast.def;

import static common.RequireNonNull.requireNonNull;

import compiler.common.Visitor;
import compiler.lexer.Position;

import java.util.List;

import compiler.parser.ast.type.Type;
import compiler.parser.ast.expr.Expr;

public class FunDef extends Def {
    /**
     * Parametri.
     */
    public final List<Parameter> parameters;

    /**
     * Tip rezultata.
     */
    public final Type type;

    /**
     * Jedro funkcije.
     */
    public final Expr body;

    public FunDef(Position position, String name, List<Parameter> parameters, Type type, Expr body) {
        super(position, name);
        requireNonNull(parameters);
        requireNonNull(type);
        requireNonNull(body);
        this.parameters = parameters;
        this.type = type;
        this.body = body;
    }

	@Override public void accept(Visitor visitor) { visitor.visit(this); }

    /**
     * Parameter funkcije.
     */
    public static class Parameter extends Def {
        /**
         * Tip parametra.
         */
        public final Type type;
    
        public Parameter(Position position, String name, Type type) {
            super(position, name);
            requireNonNull(type);
            this.type = type;
        }
    
        @Override public void accept(Visitor visitor) { visitor.visit(this); }
    }
}
