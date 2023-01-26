package uk.ac.starlink.ttools.gui;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.net.MalformedURLException;
import java.net.URL;
import uk.ac.starlink.ttools.build.Heading;

/**
 * Static methods to give the locations of run-time javadocs.
 * This class provides a central repository for knowledge about the
 * naming of the files/resources which contain the javadoc information
 * needed at runtime (classes accessible within JEL).
 *
 * @author   Mark Taylor (Starlink)
 * @since    2 Sep 2004
 */
public class DocNames {

    /**
     * Character used in resource names to indicate an array dimension.
     * Appended after the type name.
     */
    public static final char ARRAY_SUFFIX = ',';

    /**
     * Character used in resource names to separate tokens giving
     * classnames and array types.
     */
    public static final char TOKEN_SEPARATOR = '-';

    /**
     * Returns the URL which points to the documentation object for a 
     * given object, or null if none can be found.  Currently,
     * Classes, Fields, Methods and Headings are known about.
     *
     * @param   obj  object to be documented
     * @return   URL of doc file (probably HTML)
     */
    public static URL docURL( Object obj ) {
        if ( obj instanceof Class ) {
            return classDocURL( (Class<?>) obj );
        }
        else if ( obj instanceof Field ) {
            return fieldDocURL( (Field) obj );
        }
        else if ( obj instanceof Method ) {
            return methodDocURL( (Method) obj );
        }
        else if ( obj instanceof Heading ) {
            return headingDocURL( (Heading) obj );
        }
        else {
            return null;
        }
    }

    /**
     * Maps a type name to a word that will be used as a token in
     * a document resource name.
     *
     * @param  typeName  type name, for instance class name or primitive name
     * @return   word for use in resource name
     */
    public static String typeNameToWord( String typeName ) {
        return typeName == null
             ? null
             : typeName.replaceFirst( ".*[.$]", "" );
    }

    private static URL docURL( Class<?> clazz, String suffix ) {
        String fqname = clazz.getName();
        String rpath = "/" + fqname.replaceAll( "\\.", "/" ) + suffix;
        return clazz.getResource( rpath );
    }

    private static URL classDocURL( Class<?> clazz ) {
        return docURL( clazz, ".html" );
    }

    private static URL fieldDocURL( Field field ) {
        return docURL( field.getDeclaringClass(),
                       TOKEN_SEPARATOR + field.getName() + ".html" );
    }

    private static URL methodDocURL( Method method ) {
        StringBuffer mangle = new StringBuffer();
        Class<?>[] types = method.getParameterTypes();
        for ( int i = 0; i < types.length; i++ ) {
            mangle.append( TOKEN_SEPARATOR );
            int narray = 0;
            Class<?> clazz = types[ i ];
            while ( clazz.isArray() ) {
                narray++;
                clazz = clazz.getComponentType();
            }
            mangle.append( typeNameToWord( clazz.getName() ) );
            for ( int j = 0; j < narray; j++ ) {
                mangle.append( ARRAY_SUFFIX );
            }
        }
        return docURL( method.getDeclaringClass(),
                       TOKEN_SEPARATOR + method.getName()
                                       + mangle.toString() + ".html" );
    }

    private static URL headingDocURL( Heading heading ) {
        return docURL( Heading.class, heading.getDocSuffix() );
    }

}
