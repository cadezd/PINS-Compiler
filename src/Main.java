/**
 * @Author: turk
 * @Description: Vhodna točka prevajalnika.
 */

import java.io.IOException;
import java.io.PrintStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Optional;

import cli.PINS;
import cli.PINS.Phase;
import compiler.common.PrettyPrintVisitor3;
import compiler.lexer.Lexer;
import compiler.parser.Parser;
import compiler.parser.ast.def.Def;
import compiler.seman.common.NodeDescription;
import compiler.seman.name.NameChecker;
import compiler.seman.name.env.FastSymbolTable;
import compiler.seman.name.env.SymbolTable;
import compiler.seman.type.TypeChecker;
import compiler.seman.type.type.Type;

public class Main {
    /**
     * Metoda, ki izvede celotni proces prevajanja.
     * 
     * @param args parametri ukazne vrstice.
     */
    public static void main(String[] args) throws Exception {
        var cli = PINS.parse(args);
        run(cli);
    }


    // -------------------------------------------------------------------


    private static void run(PINS cli) throws IOException {
        var sourceCode = Files.readString(Paths.get(cli.sourceFile));
        run(cli, sourceCode);
    }

    private static void run(PINS cli, String sourceCode) {
        /**
         * Izvedi leksikalno analizo.
         */
        var symbols = new Lexer(sourceCode).scan();
        if (cli.dumpPhases.contains(Phase.LEX)) {
            for (var symbol : symbols) {
                System.out.println(symbol.toString());
            }
        }
        if (cli.execPhase == Phase.LEX) {
            return;
        }
        /**
         * Izvedi sintaksno analizo.
         */
        Optional<PrintStream> out = cli.dumpPhases.contains(Phase.SYN) 
                ? Optional.of(System.out)
                : Optional.empty();
        var parser = new Parser(symbols, out);
        var ast = parser.parse();
        if (cli.execPhase == Phase.SYN) {
            return;
        }
        /**
         * Abstraktna sintaksa.
         */
        var prettyPrint = new PrettyPrintVisitor3(2, System.out);
        if (cli.dumpPhases.contains(Phase.AST)) {
            ast.accept(prettyPrint);
        }
        if (cli.execPhase == Phase.AST) {
            return;
        }
        /**
         * Izvedi razreševanje imen.
         */
        SymbolTable symbolTable = new FastSymbolTable();
        var definitions = new NodeDescription<Def>();
        var nameChecker = new NameChecker(definitions, symbolTable);
        ast.accept(nameChecker);
        if (cli.dumpPhases.contains(Phase.NAME)) {
            prettyPrint.definitions = Optional.of(definitions);
            ast.accept(prettyPrint);
        }
        if (cli.execPhase == Phase.NAME) {
            return;
        }
        /**
         * Izvedi preverjanje tipov.
         */
        var types = new NodeDescription<Type>();
        var typeChecker = new TypeChecker(definitions, types);
        ast.accept(typeChecker);
        
        if (cli.dumpPhases.contains(Phase.TYP)) {
            prettyPrint.definitions = Optional.of(definitions);
            prettyPrint.types = Optional.of(types);
            ast.accept(prettyPrint);
        }
        if (cli.execPhase == Phase.TYP) {
            return;
        }
    }
}
