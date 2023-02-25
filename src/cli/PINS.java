/**
 * @Author: turk
 * @Description: Uporabniški vmesnik (CLI).
 */

package cli;

import java.util.EnumSet;

import ArgPar.Annotation.ParsableArgument;
import ArgPar.Annotation.ParsableCommand;
import ArgPar.Annotation.ParsableOption;
import ArgPar.Exception.ParseException;
import ArgPar.Parser.ArgumentParser;

@ParsableCommand(commandName = "PINS", description = "Prevajalnik za programski jezik PINS")
public class PINS {
    /**
     * Pot do izvorne datoteke.
     */
    @ParsableArgument
    public String sourceFile;

    /**
     * Faze prevajanja, ki izpišejo vmesne rezultate.
     */
    @ParsableOption(name = "--dump")
    public PhasesEnumSet dumpPhases = PhasesEnumSet.empty();

    /**
     * Faza, ki se bo izvedla nazadnje.
     */
    @ParsableOption(name = "--exec")
    public Phase execPhase = Phase.LEX;

    @ParsableOption(name = "--memory")
    public int memory = 1024;

    /**
     * Razčleni argumente.
     */
    public static PINS parse(String[] args) {
        try {
            var parser = new ArgumentParser<PINS>(PINS.class);
            return parser.parse(args);
        } catch (ParseException __) {
            System.exit(2);
            return null;
        }
    }

    // --------------------------------------------------------------

    /**
     * Faze prevajanja.
     */
    public static enum Phase {
        LEX, SYN, AST, NAME, TYP, FRM, IMC, INT
    }

    /**
     * Razred, ki hrani faze prevajanja.
     */
    public static class PhasesEnumSet extends ForwardingSet<Phase> {
        PhasesEnumSet(EnumSet<Phase> set) {
            super(set);
        }

        /**
         * Ustvari prazno možico.
         */
        static PhasesEnumSet empty() {
            return new PhasesEnumSet(EnumSet.noneOf(Phase.class));
        }

        /**
         * Razčleni argument in kreira novo množico.
         * 
         * @param arg Argument, ki ga metoda razčleni v množico faz.
         */
        public static PhasesEnumSet valueOf(String arg) {
            var split = arg.split(",");
            var set = new PhasesEnumSet(EnumSet.noneOf(Phase.class));
            for (var s : split) {
                var phase = Phase.valueOf(s.trim());
                if (phase == null) {
                    throw new IllegalArgumentException("Could not parse <Phase>!");
                }
                set.add(phase);
            }
            return set;
        }

        @Override
        public String toString() {
            if (isEmpty()) {
                return "{}";
            }
            var sb = new StringBuilder();
            sb.append("{");
            for (var phase : Phase.values()) {
                if (contains(phase)) {
                    sb.append(phase.toString() + ",");
                }
            }
            sb.deleteCharAt(sb.length() - 1);
            sb.append("}");
            return sb.toString();
        }
    }
}
