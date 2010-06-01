/*
 * Created by Ray Plante for the National Virtual Observatory
 * c. 2006
 */
package net.ivoa.registry.search;

import net.ivoa.registry.RegistryServiceException;

/**
 * an exception indicating that the requested identifier does not exist
 * in the registry.
 */
public class IDNotFoundException extends RegistryServiceException {

    public IDNotFoundException() {
        this("Requested identifier not found");
    }

    public IDNotFoundException(String msg) {
        super(msg);
    }

}
