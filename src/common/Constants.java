package common;

public class Constants {
    private Constants() {}

    public static final int WordSize;
    public static final int x86 = 4; // 4B
    public static final int x64 = 8; // 8B

    // 'Standardna knjižnica'
    public static final String printStringLabel = "print_str";
    public static final String printIntLabel    = "print_int";
    public static final String printLogLabel    = "print_log";
    public static final String randIntLabel     = "rand_int";
    public static final String seedLabel        = "seed";

    // 'Registri'
    public static final String framePointer     = "{FP}";
    public static final String stackPointer     = "{SP}";

    static {
        /**
         * Ciljna arhitektura je x86.
         */
        WordSize = x86;
    }
}
