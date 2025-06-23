package fr.formiko.mc.underilla.paper.io;

import java.util.Arrays;
import java.util.stream.Collectors;

public class Tools {
    private Tools() {}
    public static String exceptionToString(Exception e) {
        return Arrays.stream(e.getStackTrace()).map(s -> s.toString()).collect(Collectors.joining("\n", e.getMessage() + ": ", ""));
    }
}
