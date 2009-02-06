/*
 * Created by Ray Plante for the National Virtual Observatory
 * c. 2006
 */
package net.ivoa.registry;

/**
 * an exception indicating an error was detected by the server while 
 * querying a registry.
 */
public class RegistryAccessException extends Exception {

    public RegistryAccessException() {
        this("Unknown error detected while accessing a registry");
    }

    public RegistryAccessException(String msg) {
        super(msg);
    }

}
