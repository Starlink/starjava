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
     * Returns the URL which points to the documentation object for a 
     * given object, or null if none can be found.  Currently,
     * Classes, Fields, Methods and Headings are known about.
     *
     * @param   obj  object to be documented
     * @return   URL of doc file (probably HTML)
     */
    public static URL docURL( Object obj ) {
        if ( obj instanceof Class ) {
            return classDocURL( (Class) obj );
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

    private static URL docURL( Class clazz, String suffix ) {
        String fqname = clazz.getName();
        String rpath = "/" + fqname.replaceAll( "\\.", "/" ) + suffix;
        return clazz.getResource( rpath );
    }

    private static URL classDocURL( Class clazz ) {
        return docURL( clazz, ".html" );
    }

    private static URL fieldDocURL( Field field ) {
        return docURL( field.getDeclaringClass(),
                       "-" + field.getName() + ".html" );
    }

    private static URL methodDocURL( Method method ) {
        StringBuffer mangle = new StringBuffer();
        Class[] types = method.getParameterTypes();
        for ( int i = 0; i < types.length; i++ ) {
            mangle.append( "-" );
            int narray = 0;
            Class clazz = types[ i ];
            while ( clazz.isArray() ) {
                narray++;
                clazz = clazz.getComponentType();
            }
            mangle.append( clazz.getName().replaceFirst( "^.*\\.", "" )
                                          .replaceAll( "\\$", "." ) );
            for ( int j = 0; j < narray; j++ ) {
                mangle.append( ',' );
            }
        }
        return docURL( method.getDeclaringClass(),
                       "-" + method.getName() + mangle.toString() + ".html" );
    }

    private static URL headingDocURL( Heading heading ) {
        return docURL( Heading.class, heading.getDocSuffix() );
    }

}
