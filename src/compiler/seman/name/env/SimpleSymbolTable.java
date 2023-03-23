/**
 * @ Author: turk
 * @ Description: Preprosta implementacija simbolne tabele.
 */

package compiler.seman.name.env;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Optional;

import compiler.parser.ast.def.Def;

public class SimpleSymbolTable implements SymbolTable {
    private ArrayList<Env> stack;

    public SimpleSymbolTable() {
        this.stack = new ArrayList<>();
        this.stack.add(new Env());
    }

    /**
     * @complexity O(1)
     */
    @Override
    public void insert(Def definition) throws DefinitionAlreadyExistsException {
        if (stack.isEmpty()) { throw new RuntimeException(); }
        if (stack.get(stack.size() - 1).mapping.containsKey(definition.name)) {
            throw new DefinitionAlreadyExistsException(definition);
        }
        stack.get(stack.size() - 1).mapping.put(definition.name, definition);
    }

    /**
     * @complexity O(k), k ... globina gnezdenja
     */
    @Override
    public Optional<Def> definitionFor(String name) {
        if (stack.isEmpty()) { throw new RuntimeException(); }
        for (int i = stack.size() - 1; i >= 0; i--) {
            var env = stack.get(i);
            if (env.mapping.containsKey(name)) {
                return Optional.of(env.mapping.get(name));
            }
        }
        return Optional.empty();
    }

    /**
     * @complexity O(1)
     */
    @Override
    public void pushScope() {
        stack.add(new Env());
    }

    /**
     * @complexity O(1)
     */
    @Override
    public void popScope() {
        if (stack.isEmpty()) { throw new RuntimeException(); }
        stack.remove(stack.size() - 1);
    }

    private static class Env {
        HashMap<String, Def> mapping = new HashMap<>();
    }
}
