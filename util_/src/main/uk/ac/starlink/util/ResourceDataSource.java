package uk.ac.starlink.util;

import java.net.URL;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;

/**
 * A DataSource which represents a resource available from the
 * class loader's {@link java.lang.ClassLoader#getResourceAsStream} method.
 *
 * @author   Mark Taylor (Starlink)
 */
public class ResourceDataSource extends DataSource {

    private String resource;
    private ClassLoader classLoader = getClass().getClassLoader();

    /**
     * Constructs a new ResourceDataSource from a resource name and
     * given size of intro buffer.
     * Note that like {@link java.lang.ClassLoader#getResource}
     * but unlike {@link java.lang.Class#getResource} the resource name 
     * is assumed absolute, and should '/'.
     *
     * @param resource  the path of the resource represented by this DataSource
     * @param introLimit  the intro buffer size
     */
    @SuppressWarnings("this-escape")
    public ResourceDataSource( String resource, int introLimit ) {
        super( introLimit );
        this.resource = resource;
        setName( resource.substring( resource.lastIndexOf( '/' ) + 1 ) );
    }

    /**
     * Constructs a new ResourceDataSource from a resource name with a
     * default size of intro buffer.
     * Note that like {@link java.lang.ClassLoader#getResource}
     * but unlike {@link java.lang.Class#getResource} the resource name 
     * is assumed absolute, and should '/'.
     *
     * @param resource  the path of the resource represented by this DataSource
     */
    public ResourceDataSource( String resource ) {
        this( resource, DEFAULT_INTRO_LIMIT );
    }

    public InputStream getRawInputStream() throws IOException {
        InputStream istrm = getClassLoader().getResourceAsStream( resource );
        if ( istrm == null ) {
            throw new FileNotFoundException( "No such resource " + resource );
        }
        return istrm;
    }

    public URL getURL() {
        return getClassLoader().getResource( resource );
    }

    /**
     * Indicates whether this resource can be located by the class loader
     * or not.
     *
     * @return  true iff the getRawInputStream method will return 
     *          an input stream
     */
    public boolean exists() {
        return getClassLoader().getResource( resource ) != null;
    }

    /**
     * Returns the ClassLoader which is used for resource resolution.
     *
     * @return  the class loader
     */
    public ClassLoader getClassLoader() {
        return classLoader;
    }

    /**
     * Sets the ClassLoader which is used for resource resolution.
     *
     * @param  classLoader  the class loader
     */
    public void setClassLoader( ClassLoader classLoader ) {
        this.classLoader = classLoader;
    }
}
