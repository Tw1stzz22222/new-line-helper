package com.new_line_transcript;

public class ModState {
    public static boolean running = false;

    public static void start() {
        running = true;
    }

    public static void terminate() {
        running = false;
    }
}
