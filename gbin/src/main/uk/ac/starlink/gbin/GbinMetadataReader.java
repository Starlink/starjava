package uk.ac.starlink.gbin;

import java.io.FileInputStream;
import java.io.InputStream;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;

public class GbinMetadataReader {

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

    private static class ReflectionInvocationHandler
            implements InvocationHandler {
        private final Object target_;
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

    public static void main( String[] args ) throws Throwable {
        InputStream in = new FileInputStream( args[ 0 ] );
        GbinObjectReader.initGaiaTools();
        Object gbinRdrObj = GbinObjectReader.createGbinReaderObject( in );
        GbinMeta meta = attemptReadMetadata( gbinRdrObj );
        System.out.println( meta );
    }
}
