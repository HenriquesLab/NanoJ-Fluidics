package nanoj.pumpControl.java.sequentialProtocol;

import ij.IJ;
import org.micromanager.MenuPlugin;
import org.micromanager.Studio;
import org.micromanager.internal.utils.ReportingUtils;
import org.scijava.command.Command;
import org.scijava.plugin.Plugin;

import java.beans.IntrospectionException;
import java.io.File;
import java.io.FileNotFoundException;
import java.lang.reflect.Method;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;

@SuppressWarnings("unused")
@Plugin(type = Command.class, menuPath = "NanoJ-Fluidics")
public class MicroManagerPlugin implements MenuPlugin {

    static final ArrayList<File> NO_FILE_FOUND = new ArrayList<>();
    static final String DLL = ".dll";
    static final String JNILIB = ".jnilib";
    static final String JAR = ".jar";

    public static void LoadJars(URL url) throws IntrospectionException, MalformedURLException, FileNotFoundException {
        // Adapted from http://baptiste-wicht.com/posts/2010/05/tip-add-resources-dynamically-to-a-classloader.html
        URLClassLoader systemClassLoader = (URLClassLoader) ClassLoader.getSystemClassLoader();
        Class<URLClassLoader> classLoaderClass = URLClassLoader.class;

        ArrayList<File> listOfFiles = FindFilesOfType(url, JAR);

        if (listOfFiles.isEmpty())
            throw new FileNotFoundException("Did not find any jar files in " + url.getPath());

        for (File currentFile: listOfFiles) {
            URL currentURL = currentFile.toURI().toURL();

            try {
                Method method = classLoaderClass.getDeclaredMethod("addURL", URL.class);
                method.setAccessible(true);
                method.invoke(systemClassLoader, currentURL);
                System.out.println("Loaded the jar: " + currentFile.getName());
            } catch (Throwable t) {
                t.printStackTrace();
                throw new IntrospectionException("Error when adding url to system ClassLoader.");
            }
        }
    }


    public static ArrayList<File> FindFilesOfType(URL url, String termination) {
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


    @Override
    public void setContext(Studio studio) {}

    @Override
    public String getName() {
        return "NanoJ-Fluidics";
    }

    @Override
    public String getHelpText() {
        return "A plugin to design a protocol of steps to control a set of microfluidics devices in sequence.";
    }

    @Override
    public String getVersion() {
        return GUI.Version();
    }

    @Override
    public String getCopyright() {
        return "(C) 2016 MRC LMCB, UCL";
    }

    @Override
    public String getSubMenu() {
        return "";
    }

    @Override
    public void onPluginSelected() {
        try {
            String root = "file:" + System.getProperty("user.dir") + File.separator;
            LoadJars(new URL(root + "plugins"));
            LoadJars(new URL(root + "mmplugins"  + File.separator +"PumpPlugins"));
            GUI.INSTANCE.create();
        } catch (Exception e) {
            IJ.log("Error, problem when initiating GUI.");
            ReportingUtils.logError(e);
        }
    }
}