package org.ton.ton4j.utils;

/**
 * Utility class for ANSI color codes
 */
public final class AnsiColors {
    
    // Text colors
    public static final String ANSI_RESET = "\u001B[0m";
    public static final String ANSI_BLACK = "\u001B[30m";
    public static final String ANSI_RED = "\u001B[31m";
    public static final String ANSI_GREEN = "\u001B[32m";
    public static final String ANSI_YELLOW = "\u001B[33m";
    public static final String ANSI_BLUE = "\u001B[34m";
    public static final String ANSI_PURPLE = "\u001B[35m";
    public static final String ANSI_CYAN = "\u001B[36m";
    public static final String ANSI_WHITE = "\u001B[37m";
    
    // Background colors
    public static final String ANSI_BLACK_BACKGROUND = "\u001B[40m";
    public static final String ANSI_RED_BACKGROUND = "\u001B[41m";
    public static final String ANSI_GREEN_BACKGROUND = "\u001B[42m";
    public static final String ANSI_YELLOW_BACKGROUND = "\u001B[43m";
    public static final String ANSI_BLUE_BACKGROUND = "\u001B[44m";
    public static final String ANSI_PURPLE_BACKGROUND = "\u001B[45m";
    public static final String ANSI_CYAN_BACKGROUND = "\u001B[46m";
    public static final String ANSI_WHITE_BACKGROUND = "\u001B[47m";
    
    // Text styles
    public static final String ANSI_BOLD = "\u001B[1m";
    public static final String ANSI_UNDERLINE = "\u001B[4m";
    public static final String ANSI_BLINK = "\u001B[5m";
    public static final String ANSI_REVERSE = "\u001B[7m";
    public static final String ANSI_HIDDEN = "\u001B[8m";
    
    private AnsiColors() {
        // Private constructor to prevent instantiation
    }
    
    /**
     * Colorize text with the specified color
     * @param text The text to colorize
     * @param color The color to use
     * @return The colorized text
     */
    public static String colorize(String text, String color) {
        return color + text + ANSI_RESET;
    }
    
    /**
     * Colorize text with the specified color and background
     * @param text The text to colorize
     * @param color The color to use
     * @param background The background color to use
     * @return The colorized text
     */
    public static String colorize(String text, String color, String background) {
        return color + background + text + ANSI_RESET;
    }
    
    /**
     * Colorize text with red
     * @param text The text to colorize
     * @return The colorized text
     */
    public static String red(String text) {
        return colorize(text, ANSI_RED);
    }
    
    /**
     * Colorize text with green
     * @param text The text to colorize
     * @return The colorized text
     */
    public static String green(String text) {
        return colorize(text, ANSI_GREEN);
    }
    
    /**
     * Colorize text with yellow
     * @param text The text to colorize
     * @return The colorized text
     */
    public static String yellow(String text) {
        return colorize(text, ANSI_YELLOW);
    }
    
    /**
     * Colorize text with blue
     * @param text The text to colorize
     * @return The colorized text
     */
    public static String blue(String text) {
        return colorize(text, ANSI_BLUE);
    }
    
    /**
     * Colorize text with purple
     * @param text The text to colorize
     * @return The colorized text
     */
    public static String purple(String text) {
        return colorize(text, ANSI_PURPLE);
    }
    
    /**
     * Colorize text with cyan
     * @param text The text to colorize
     * @return The colorized text
     */
    public static String cyan(String text) {
        return colorize(text, ANSI_CYAN);
    }
    
    /**
     * Make text bold
     * @param text The text to make bold
     * @return The bold text
     */
    public static String bold(String text) {
        return ANSI_BOLD + text + ANSI_RESET;
    }
    
    /**
     * Underline text
     * @param text The text to underline
     * @return The underlined text
     */
    public static String underline(String text) {
        return ANSI_UNDERLINE + text + ANSI_RESET;
    }
}
