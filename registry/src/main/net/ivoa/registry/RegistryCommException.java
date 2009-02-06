/*
 * Created by Ray Plante for the National Virtual Observatory
 * c. 2006
 */
package net.ivoa.registry;

/**
 * an exception indicating an unexpected protocol error was detected while 
 * querying a registry.
 */
public class RegistryCommException extends RegistryAccessException {

    Exception target = null;

    public RegistryCommException() {
        this("Unknown registry communication error detected");
    }

    public RegistryCommException(String msg) {
        super(msg);
    }

    public RegistryCommException(Exception ex) {
        super(getExName(ex) + ": " + ex.getMessage());
        target = ex;
    }

    public Exception getTargetException() { return target; }

    static String getExName(Exception ex) {
        String name = ex.getClass().getName();
        int dot = name.lastIndexOf(".");
        if (dot >= 0) name = name.substring(dot+1);
        return name;
    }

}
