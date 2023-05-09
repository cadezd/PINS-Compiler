package common;

import java.util.HashMap;

public class Constants {
    private Constants() {
    }

    public static final int WordSize;
    public static final int x86 = 4; // 4B
    public static final int x64 = 8; // 8B

    // 'Standardna knjižnica'
    public static final String printStringLabel = "print_str";
    public static final String printIntLabel = "print_int";
    public static final String printLogLabel = "print_log";
    public static final String randIntLabel = "rand_int";
    public static final String seedLabel = "seed";

    public static HashMap<String, String> stdLibrary = new HashMap<>();

    static {
        stdLibrary.put(printStringLabel, printStringLabel);
        stdLibrary.put(printIntLabel, printIntLabel);
        stdLibrary.put(printLogLabel, printLogLabel);
        stdLibrary.put(randIntLabel, randIntLabel);
        stdLibrary.put(seedLabel, seedLabel);
    }

    // 'Registri'
    public static final String framePointer = "{FP}";
    public static final String stackPointer = "{SP}";

    static {
        /**
         * Ciljna arhitektura je x86.
         */
        WordSize = x86;
    }
}
