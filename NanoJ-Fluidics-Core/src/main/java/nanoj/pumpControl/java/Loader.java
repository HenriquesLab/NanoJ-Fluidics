package nanoj.pumpControl.java;

import org.apache.commons.lang3.SystemUtils;

import java.io.File;
import java.io.FileNotFoundException;
import java.net.URL;
import java.util.ArrayList;

/**
 * Utility to add classes to the classpath. It loads either all Jars or native libraries in a given directory.
 */
public class Loader {
    static final ArrayList<File> NO_FILE_FOUND = new ArrayList<>();
    static final String DLL = ".dll";
    static final String JNILIB = ".jnilib";

    public static void loadLibrary(URL url) throws FileNotFoundException {
        String lib;

        if (SystemUtils.IS_OS_WINDOWS) lib = DLL;
        else lib = JNILIB;

        ArrayList<File> listOfFiles = findFilesOfType(url, lib);
        if (listOfFiles.isEmpty())
            throw new FileNotFoundException("Did not find library files in " + url.getPath());

        for (File currentFile : listOfFiles) {
            try {
                System.load(currentFile.getPath());
                System.out.println("Loaded the library: " + currentFile.getName());
            } catch(UnsatisfiedLinkError e){
                System.err.println("Native code library failed to load.\n" + e);
                System.exit(1);
            }
        }
    }

    public static ArrayList<File> findFilesOfType(URL url, String termination) {
        File folder = new File(url.getPath());
        File[] listOfFiles = folder.listFiles();

        if (listOfFiles == null) return NO_FILE_FOUND;

        ArrayList<File> filteredListOfFiles = new ArrayList<>();

        for (File currentFile: listOfFiles) {
            String currentPath = currentFile.getAbsolutePath();

            if (currentPath.endsWith(termination)) {
                filteredListOfFiles.add(currentFile);
            }
        }

        return filteredListOfFiles;
    }
}