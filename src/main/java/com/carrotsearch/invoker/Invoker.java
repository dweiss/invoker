package com.carrotsearch.invoker;

import java.io.File;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.net.*;
import java.util.*;

/**
 * A simple invoker that constructs a classpath dynamically 
 * from all *.jar files under a given folder and invokes a given main class.
 */
public final class Invoker
{
    private static ArrayList<URL> cpLocations = new ArrayList<URL>();
    private static String mainClass;

    public static void main(String [] argsArray) throws Exception
    {
        ArrayList<String> args = new ArrayList<String>(Arrays.asList(argsArray));
        parseArgs(args);

        ClassLoader delegate;
        if (cpLocations.isEmpty()) {
            delegate = Invoker.class.getClassLoader();
        } else {
            delegate = new URLClassLoader(cpLocations.toArray(new URL[cpLocations.size()]));
        }

        Thread.currentThread().setContextClassLoader(delegate);

        Class<?> clazz = delegate.loadClass(mainClass);
        Method method = clazz.getMethod("main", String[].class);
        if (!Modifier.isStatic(method.getModifiers())) {
            System.err.println("main() method must be static in " + clazz.getName());
            System.exit(-1);
        }
        if (!Modifier.isPublic(method.getModifiers())) {
            System.err.println("main() method must be public in " + clazz.getName());
            System.exit(-1);
        }
        method.invoke(null, (Object) args.toArray(new String[args.size()]));
    }

    private static void parseArgs(ArrayList<String> args) throws MalformedURLException
    {
        for (Iterator<String> i = args.iterator(); i.hasNext();) {
            if (i.next().equals("-cpdir")) {
                i.remove();
                addDirJars(i.next());
                i.remove();
            }
        }
        
        if (args.isEmpty()) {
            System.err.println("Provide main class.");
            System.exit(-1);
        }

        mainClass = args.remove(0);
    }

    private static void addDirJars(String dirLocation) throws MalformedURLException
    {
        for (File f : new File(dirLocation).listFiles()) {
            if (f.isFile() && f.getName().toLowerCase(Locale.ENGLISH).endsWith(".jar")) {
                cpLocations.add(f.toURI().toURL());
            }
        }
    }
}
