package uk.ac.starlink.vo;

import java.net.URL;
import java.util.Map;
import org.w3c.dom.Element;

/**
 * Contains information about an example invocation for a DALI service.
 *
 * @see  <a href="http://www.ivoa.net/documents/DALI/index.html"
 *          >DALI v1.0 sec 2.3</a>
 */
public interface DaliExample {

    /**
     * Returns a URL pointing to the browser-renderable text
     * (probably XHTML) for this example.  This is likely to include
     * a trailing fragment part (#identifier).
     *
     * @return   example URL
     */
    URL getUrl();

    /**
     * Returns the DOM element corresponding to this example.
     * It should be renderable in a browser, and is probably XHTML.
     *
     * @return  example DOM element
     */
    Element getElement();

    /**
     * Returns the identifier for this example within its host document.
     *
     * @return  id
     */
    String getId();

    /**
     * Returns the capability to which this example applies,
     * if explicitly supplied.  This may be null if the capability
     * is implicit (the only one applicable to the service in question).
     *
     * @return  capability URI, or null
     */
    String getCapability();

    /**
     * Returns the user-readable name for this example.
     *
     * @return   name, should be short and plain text
     */
    String getName();

    /**
     * Returns a map of name-&gt;value pairs giving DALI 1.0-style
     * generic-parameters for this example.
     *
     * @return  map of generic-parameter values
     */
    Map<String,String> getGenericParameters();

    /**
     * Returns a map of name-&gt;value pairs giving RDFa properties for
     * this example, <em>excluding</em> those that form part of a
     * <code>generic-parameter</code> key/value pair.
     *
     * @return  map of non-generic-parameter property values
     */
    Map<String,String> getProperties();
}
