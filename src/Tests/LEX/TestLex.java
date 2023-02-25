package Tests.LEX;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Scanner;
import compiler.lexer.Lexer;

public class TestLex {
    public static void main(String[] args) {
        System.out.println("Tests by THICC :)");

        // get number of test files
        int numberOfTests = getNumberOfTests();

        // delete all .err files inside Tests/LEX
        deleteErrFiles();

        // run tests
        runTest(numberOfTests);
    }

    /**
     * Counts the number of all files inside test folder
     * @return number of test files
     */
    private static int getNumberOfTests() {
        int numberOfTests = 0;
        try {
            numberOfTests = new File("src/Tests/LEX").list().length / 2;
        } catch (NullPointerException e) {
            System.out.println("Folder not found!");
            System.exit(1);
        }
        return numberOfTests;
    }

    /**
     * Deletes all .err files inside Tests/LEX
     * Minimizes the confusion when comparing files
     */
    private static void deleteErrFiles() {
        // delete all .err files inside Tests/LEX
        File folder = new File("src/Tests/LEX");
        File[] listOfFiles = folder.listFiles();
        for (File file : listOfFiles) {
            if (file.isFile() && file.getName().endsWith(".err")) {
                file.delete();
            }
        }
    }

    /**
     * Runs all tests
     * @param numberOfTests number of tests inside current folder
     */
    private static void runTest(int numberOfTests) {
        String output = "";
        String expected = "";
        double countOK = 0;

        for (int i = 1; i <= numberOfTests; i++) {
            output = getOutput(i);
            expected = readFile("src/Tests/LEX/test" + String.format("%02d", i) + ".out");
            System.out.print("Test" + String.format("%02d", i) + ": ");
            if (output.equals(expected)) {
                countOK++;
                System.out.println("OK");
            } else {
                System.out.println("FAIL");
                System.out.println("Compare files: src/Tests/LEX/test" + String.format("%02d", i) + ".out and src/Tests/LEX/test" + String.format("%02d", i) + ".err");
                writeErrFile(output, i);
            }
        }

        // print ration
        System.out.println("Passed: " + (int)countOK + "/" + numberOfTests + "; " + String.format("%.2f", countOK / numberOfTests * 100) + "%");
    }

    /**
     * Gets program output for a given test
     * @param testNumber ID number of the test
     * @return output produced by the program
     */
    private static String getOutput(int testNumber) {
        String input = readFile("src/Tests/LEX/test" + String.format("%02d", testNumber) + ".pins");
        var symbols = new Lexer(input).scan();
        StringBuilder output = new StringBuilder();
        for (var symbol : symbols) {
            output.append(symbol.toString());
            output.append("\n");
        }
        return output.toString();
    }

    /**
     * Reads file and returns its content as a string
     * @param path path to the file
     * @return content of the file
     */
    private static String readFile(String path) {
        StringBuilder text = new StringBuilder();
        try {
            Scanner sc = new Scanner(new File(path));
            while (sc.hasNextLine()) {
                text.append(sc.nextLine());
                text.append("\n");
            }
            sc.close();
        } catch (Exception e) {
            System.out.println("Error while reading file!\nFilename: " + path);
            System.exit(1);
        }
        return text.toString();
    }

    /**
     * Writes output to a file.
     * Only happens for test that failed.
     * @param output output produced by the program
     * @param testNum ID number of the test
     */
    private static void writeErrFile(String output, int testNum) {
        try {
            File file = new File("src/Tests/LEX/test" + String.format("%02d", testNum) + ".err");
            file.createNewFile();
            FileWriter writer = new FileWriter(file);
            writer.write(output.strip());
            writer.close();
        } catch (IOException e) {
            System.out.println("Error while writing file!\nFilename: test" + String.format("%02d", testNum) + ".err");
            System.exit(1);
        }
    }
}
