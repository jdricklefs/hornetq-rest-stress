package com.cubiclegrizzly.hornetstress;

public class HivePrinter {

    private static final int DOTS_PER_LINE = 100;

    private static int printCtr = 0;

    public static synchronized void printM() {
        printCtr += 1;
        if (printCtr > DOTS_PER_LINE) {
            System.out.print("\n");
            printCtr = 0;
        }
        System.out.print("M");
    }

    private static String getPrefix(int id) {
        return "(" + id + "): ";
    }

    public static synchronized void printLn(String msg) {
        System.out.print("\n");
        System.out.println(msg);
        printCtr = 0;
    }

    public static synchronized void printLn(int id, String msg) {
        System.out.print("\n");
        System.out.println(getPrefix(id) + msg);
        printCtr = 0;
    }

    public static synchronized void printErr(int id, String msg) {
        System.out.print("\n");
        System.err.println(getPrefix(id) + msg);
        printCtr = 0;
    }

}
