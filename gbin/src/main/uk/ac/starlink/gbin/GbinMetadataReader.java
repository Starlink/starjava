package uk.ac.starlink.gbin;

import java.io.FileInputStream;
import java.io.InputStream;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;

/**
 * Provides a utility method for reading metadata from a GBIN file.
 *
 * @author   Mark Taylor
 * @since    13 Aug 2014
 */
public class GbinMetadataReader {

    /**
     * Attempts to read a metadata object from a GbinReader object.
     *
     * <p><strong>NOTE:</strong> this method is effectively destructive:
     * if you read metadata from a GbinReader you cannot then go on
     * to read data records from it.
     *
     * <p>A wide range of exceptions and errors may be thrown by this
     * method, since it involves reflection all sorts of things can
     * go wrong.
     *
     * @param  gbinReaderObj   object implementing
     *                         <code>gaia.cu1.tools.dal.gbin.GbinReader</code>
     * @return  metadata if possible
     */
    public static GbinMeta attemptReadMetadata( Object gbinReaderObj )
            throws Throwable {
        Object metaObj = gbinReaderObj.getClass()
                        .getMethod( "getGbinMetaData", new Class[ 0 ] )
                        .invoke( gbinReaderObj, new Object[ 0 ] );
        return (GbinMeta)
               Proxy
              .newProxyInstance( GbinObjectReader.class.getClassLoader(),
                                 new Class<?>[] { GbinMeta.class },
                                 new ReflectionInvocationHandler( metaObj ) );
    }

    /**
     * Generic proxy InvocationHandler implementation that invokes methods
     * on a target object assumed to have the same method signatures as
     * those of the proxy class.  The invoked methods do not throw any
     * throwables, if there's a problem they just return null.
     * This implementation is therefore suitable where the return values
     * are not critical to the operation of the application.
     */
    private static class ReflectionInvocationHandler
            implements InvocationHandler {
        private final Object target_;

        /**
         * Constructor.
         *
         * @param  target  target object
         */
        ReflectionInvocationHandler( Object target ) {
            target_ = target;
        }

        public Object invoke( Object proxy, Method method, Object[] args )
                throws Throwable {
            try {
                Method targetMethod =
                    target_.getClass().getMethod( method.getName(),
                                                  method.getParameterTypes() );
                return targetMethod.invoke( target_, args );
            }
            catch ( Throwable e ) {
                return null;
            }
        }
    }

    /**
     * Attempts to read metadata from the GBIN file named on the command line
     * and writes the description to stdout.
     */
    public static void main( String[] args ) throws Throwable {
        InputStream in = new FileInputStream( args[ 0 ] );
        GbinObjectReader.initGaiaTools();
        Object gbinRdrObj = GbinObjectReader.createGbinReaderObject( in );
        GbinMeta meta = attemptReadMetadata( gbinRdrObj );
        System.out.println( meta );
    }
}
