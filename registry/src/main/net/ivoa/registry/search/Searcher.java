/*
 * Created by Ray Plante for the National Virtual Observatory
 * c. 2006
 */
package net.ivoa.registry.search;

import net.ivoa.registry.RegistryServiceException;

import java.lang.reflect.Method;
import java.lang.reflect.InvocationTargetException;

import javax.xml.soap.SOAPException;
import org.w3c.dom.Element;
import org.w3c.dom.DOMException;

/**
 * an encapsulation of a query that can be re-executed
 */
abstract class Searcher {

    int max = 50;
    int from = 1;
    Method method = null;
    Object[] args = null;
    Object service = null;

    Searcher(Object searchojb, Method searchmeth, int from, int max) {
        service = searchojb;
        method = searchmeth;
        this.from = from;
        this.max = max;
    }

    /**
     * get the position of the first record to fetch
     */
    public int getFrom() { return from; }

    /**
     * set the position of the first record to fetch
     */
    public void setFrom(int pos) { from = pos; }

    /**
     * get the maximum number of records to request
     */
    public int getMax() { return max; }

    /**
     * set the maximum number of records to request
     */
    public void setMax(int count) { max = count; }

    /**
     * execute the next configured search
     */
    public Element exec() 
         throws RegistryServiceException, SOAPException, DOMException
    {
        updateArgs();
        try {
            return ((Element) method.invoke(service, args));
        }
        catch (ClassCastException ex) {
            throw new InternalError("programmer error: search method has " +
                                    "wrong return type: " + method.getName());
        }
        catch (IllegalAccessException ex) {
            throw new InternalError("programmer error: inaccessible search " +
                                    "method: " + method.getName());
        }
        catch (IllegalArgumentException ex) {
            throw new InternalError("programmer error: incorrect arguments " +
                                    "to search method: " + method.getName());
        }
        catch (InvocationTargetException ex) {
            Throwable tex = ex.getTargetException();
            if (tex instanceof SOAPException) 
                throw ((SOAPException) tex);
            else if (tex instanceof RegistryServiceException) 
                throw ((RegistryServiceException) tex);
            else if (tex instanceof DOMException) 
                throw ((DOMException) tex);
            else {
                String name = tex.getClass().getName();
                int dot = name.lastIndexOf(".");
                if (dot >= 0) name = name.substring(dot+1);
                throw new RegistryServiceException(name + ": " +
                                                   tex.getMessage());
            }
        }
    }

    /**
     * update the args field for the last values of from and max.
     */
    public abstract void updateArgs();
}
