package com.peterlaurence.trekadvisor.util;

import java.io.PrintWriter;
import java.io.StringWriter;

/**
 * Utility class for general purposes.
 *
 * @author peterLaurence on 12/08/17.
 */
public class Tools {
    /**
     * Convert a {@link Throwable}'s stacktrace to a String.
     */
    public static String stackTraceToString(Throwable t) {
        StringWriter errors = new StringWriter();
        t.printStackTrace(new PrintWriter(errors));
        return errors.toString();
    }
}
