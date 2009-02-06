/*
 * Created by Ray Plante for the National Virtual Observatory
 * c. 2006
 */
package net.ivoa.registry;

/**
 * an exception indicating an error was detected by the server while 
 * querying a registry.
 */
public class RegistryServiceException extends RegistryAccessException {

    public RegistryServiceException() {
        this("Unknown registry server error detected");
    }

    public RegistryServiceException(String msg) {
        super(msg);
    }

}
