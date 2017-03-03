package com.peterlaurence.trekadvisor.util;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.InputStreamReader;

/**
 * Utility class.
 *
 * Created by pla on 02/01/16.
 */
public class FileUtils {
    /**
     * Utility method to get the content of a text file as a {@code String}.
     *
     * @param file An existing file
     * @return the content of the file as {@code String}
     * @throws Exception
     */
    public static String getStringFromFile (File file) throws Exception {
        FileInputStream fis = new FileInputStream(file);
        BufferedReader reader = new BufferedReader(new InputStreamReader(fis));
        StringBuilder sb = new StringBuilder();

        String line;
        while ((line = reader.readLine()) != null) {
            sb.append(line).append("\n");
        }

        reader.close();
        fis.close();

        return sb.toString();
    }

    /**
     * Utility method to get the content of a text file as a {@code String}.
     *
     * @param inputStream An existing {@code InputStream}
     * @return the content of the file as {@code String}
     * @throws Exception
     */
    public static String getStringFromInputStream (InputStream inputStream) throws Exception {
        BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
        StringBuilder sb = new StringBuilder();

        String line;
        while ((line = reader.readLine()) != null) {
            sb.append(line).append("\n");
        }

        reader.close();
        inputStream.close();

        return sb.toString();
    }

    /**
     * Recursively delete a directory. Or, if it's just a file, deletes it.
     *
     * @param fileOrDirectory the {@link File} to delete
     */
    public static void deleteRecursive(File fileOrDirectory) {
        if (fileOrDirectory.isDirectory())
            for (File child : fileOrDirectory.listFiles())
                deleteRecursive(child);

        fileOrDirectory.delete();
    }

    /**
     * Get a {@link File} extension.
     * @param file
     * @return the file extension, or an empty {@link String} if any.
     */
    public static String getFileExtension(File file) {
        String name = file.getName();
        try {
            return name.substring(name.lastIndexOf(".") + 1);
        } catch (Exception e) {
            return "";
        }
    }
}
