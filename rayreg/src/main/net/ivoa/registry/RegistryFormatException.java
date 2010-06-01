/*
 * Created by Ray Plante for the National Virtual Observatory
 * c. 2006
 */
package net.ivoa.registry;

/**
 * an exception indicating an error was encountered while parsing or 
 * interpreting a response from a registry due to an incorrect format in
 * the message.
 */
public class RegistryFormatException extends RegistryAccessException {

    public RegistryFormatException() {
        this("Unknown registry response format error detected");
    }

    public RegistryFormatException(String msg) {
        super(msg);
    }

}
