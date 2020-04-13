package com.paultech;

public class StringUtils {
    private StringUtils() {}

    public static String trim(String value) {
        return value.trim();
    }
    public static String suffix(String value, String suffix) {
        return value + suffix;
    }

    public static String prefix(String value, String prefix) {
        return prefix + value;
    }
}
