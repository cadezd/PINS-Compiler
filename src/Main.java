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
import compiler.common.PrettyPrintVisitor1;
import compiler.lexer.Lexer;
import compiler.parser.Parser;

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
        var prettyPrint = new PrettyPrintVisitor1(2, System.out);
        if (cli.dumpPhases.contains(Phase.AST)) {
            ast.accept(prettyPrint);
        }
        if (cli.execPhase == Phase.AST) {
            return;
        }
    }
}
