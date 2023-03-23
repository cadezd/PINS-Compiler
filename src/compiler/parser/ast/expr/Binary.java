/**
 * @Author: turk
 * @Description: Binarni izraz.
 */

package compiler.parser.ast.expr;

import static common.RequireNonNull.requireNonNull;

import compiler.common.Visitor;
import compiler.lexer.Position;

public class Binary extends Expr {
    /**
     * Levi podizraz.
     */
    public final Expr left;

    /**
     * Operacija.
     */
    public final Operator operator;

    /**
     * Desni podizraz.
     */
    public final Expr right;

    public Binary(Position position, Expr left, Operator operator, Expr right) {
        super(position);
        requireNonNull(left);
        requireNonNull(operator);
        requireNonNull(right);
        this.left = left;
        this.operator = operator;
        this.right = right;
    }

	@Override public void accept(Visitor visitor) { 
        visitor.visit(this); 
    }

    public static enum Operator {
        ADD,       // +
        SUB,       // -
        MUL,       // *
        DIV,       // /
        MOD,       // %
    
        AND,       // &
        OR,        // |
        
        EQ,        // ==
        NEQ,       // !=
        LT,        // <
        GT,        // >
        LEQ,       // <=
        GEQ,       // >=
    
        ASSIGN,     // =

        ARR;        // a[indeks]

        public boolean isAndOr() {
            return this == AND || this == OR;
        }

        public boolean isArithmetic() {
            return this == ADD || this == SUB || this == MUL || this == DIV || this == MOD;
        }

        public boolean isComparison() {
            return this == EQ || this == NEQ || this == LT || this == GT || this == GEQ || this == LEQ;
        }
    }
}
