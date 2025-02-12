package com.jkantrell.mc.underilla.spigot.io;

import java.util.Arrays;
import java.util.stream.Collectors;

public class Tools {
    public static String exceptionToString(Exception e) {
        return Arrays.stream(e.getStackTrace()).map(s -> s.toString()).collect(Collectors.joining("\n", e.getMessage() + ": ", ""));
    }
}
