/**
 * @ Author: turk
 * @ Description: Kompleksnejša, a hitrejša, implementacija simbolne tabele.
 */

package compiler.seman.name.env;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import compiler.parser.ast.def.Def;

public class FastSymbolTable implements SymbolTable {
    private int currentScope = 0;
    private Map<Integer, ArrayList<String>> scopes = new HashMap<>();
    private Map<String, ArrayList<Pair>> env = new HashMap<>();

    public FastSymbolTable() {
        scopes.put(currentScope, new ArrayList<>());
    }

    @Override
    public void insert(Def definition) throws DefinitionAlreadyExistsException {
        var definitions = env.get(definition.name);
        if (definitions != null) {
            if (!definitions.isEmpty() && definitions.get(definitions.size() - 1).scope == currentScope) {
                throw new DefinitionAlreadyExistsException(definition);
            }
            definitions.add(new Pair(currentScope, definition));
        } else {
            var stack = new ArrayList<Pair>();
            stack.add(new Pair(currentScope, definition));
            env.put(definition.name, stack);
        }
        var existingScope = scopes.get(currentScope);
        if (existingScope == null) {
            var list = new ArrayList<String>();
            list.add(definition.name);
            scopes.put(currentScope, list);
        } else {
            existingScope.add(definition.name);
        }
    }

    @Override
    public Optional<Def> definitionFor(String name) {
        var definitions = env.get(name);
        if (definitions == null || definitions.isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(definitions.get(definitions.size() - 1).def);
    }

    @Override
    public void pushScope() {
        currentScope++;
    }

    @Override
    public void popScope() {
        var scope = scopes.get(currentScope);
        if (scope == null) { return; }
        for (var name : scope) {
            var definitions = env.get(name);
            if (definitions.get(definitions.size() - 1).scope != currentScope) {
                throw new RuntimeException("Interna napaka prevajalnika.");
            }
            definitions.remove(definitions.size() - 1);
        }
        scopes.remove(currentScope);
        currentScope--;
    }

    private static class Pair {
        final int scope;
        final Def def;

        public Pair(int scope, Def def) {
            this.scope = scope;
            this.def = def;
        }
    }
}
