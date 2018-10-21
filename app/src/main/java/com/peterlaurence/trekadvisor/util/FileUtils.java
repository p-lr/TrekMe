package com.peterlaurence.trekadvisor.util;

import android.content.ContentResolver;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Environment;
import android.provider.OpenableColumns;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.InputStreamReader;

/**
 * Utility class.
 *
 * @author peterLaurence on 02/01/16.
 */
public class FileUtils {
    /**
     * Utility method to get the content of a text file as a {@code String}.
     *
     * @param file An existing file
     * @return the content of the file as {@code String}
     * @throws Exception
     */
    public static String getStringFromFile(File file) throws Exception {
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
    public static String getStringFromInputStream(InputStream inputStream) throws Exception {
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
     *
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

    /**
     * Get a {@link File} name without extension.
     *
     * @param file
     * @return the file name, or an empty {@link String} if any.
     */
    public static String getFileNameWithoutExtention(File file) {
        String name = file.getName();
        try {
            return name.substring(0, name.lastIndexOf("."));
        } catch (Exception e) {
            return "";
        }
    }

    /**
     * Retrieve a {@link Bitmap} given its relative path from the external storage directory.
     * Usually, this directory is "/storage/emulated/0".
     * For example : if {@code imagePath} is "trekavisor/maps/paris/paris.jpg", the full path of
     * the file that will be retrieved is "/storage/emulated/0/trekavisor/maps/paris/paris.jpg".
     * <p/>
     * NB : a nice way to generate thumbnails of maps is by using vipsthumbnail. Example :
     * vipsthumbnail big-image.jpg --interpolator bicubic --size 256x256 --crop
     *
     * @param imagePath the relative path of the image
     * @return the {@link Bitmap} object
     */
    public static Bitmap getBitmapFromPath(String imagePath) {
        File sd = Environment.getExternalStorageDirectory();
        File image = new File(sd, imagePath);
        BitmapFactory.Options bmOptions = new BitmapFactory.Options();
        return BitmapFactory.decodeFile(image.getAbsolutePath(), bmOptions);
    }

    /**
     * Extract the file name from URI returned from Intent.ACTION_GET_CONTENT
     */
    public static String getFileRealPathFromURI(ContentResolver contentResolver, Uri uri) {
        String result = null;
        String scheme = uri.getScheme();
        if (scheme != null && scheme.equals("content")) {
            try (Cursor cursor = contentResolver.query(uri, null, null, null, null)) {
                if (cursor != null && cursor.moveToFirst()) {
                    result = cursor.getString(cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME));
                }
            }
        }
        if (result == null) {
            String uriPath = uri.getPath();
            if (uriPath != null) {
                int cut = uriPath.lastIndexOf('/');
                if (cut != -1) {
                    result = uriPath.substring(cut + 1);
                }
            }
        }
        return result;
    }
}
