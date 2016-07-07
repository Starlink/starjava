package uk.ac.starlink.gbin;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;

/**
 * Utility class for invoking methods on dynamically available classes
 * by reflection.
 *
 * @author   Mark Taylor
 * @since    7 Jul 2016
 */
class Proxies {

    private static final ClassLoader loader_ = Proxies.class.getClassLoader();
    private static final InvocationHandler NULLS_INVOKER =
        new InvocationHandler() {
            public Object invoke( Object proxy, Method method, Object[] args ) {
                return null;
            }
        };

    /**
     * Attempts to create a proxy that can invoke methods from a given
     * interface on a target object.  The target object is assumed to
     * implement all the methods from the interface.
     * Where invocation fails for whatever reason (including reflection
     * failure), such methods return null and do not throw an exception.
     * This implementation is therefore suitable where the return values
     * are not critical to the operation of the application.
     *
     * <p>If proxy creation fails, some throwable will result
     * 
     * @param   clazz  interface defining desired API
     * @param   target   object on which methods will be called,
     *                   should implement methods defined by <code>clazz</code>
     * @return  proxy object, not null
     */
    public static <T> T createReflectionProxy( Class<T> clazz, Object target )
            throws Exception {
        InvocationHandler invoker = new ReflectionInvocationHandler( target );
        Class<?>[] ifaces = new Class<?>[] { clazz };
        return clazz.cast( Proxy.newProxyInstance( loader_, ifaces, invoker ) );
    }

    /**
     * Creates a proxy for which all method invocations simply return null.
     *
     * @param   clazz  interface defining desired API
     * @return   proxy
     */
    public static <T> T createNullsProxy( Class<T> clazz ) {
        Class<?>[] ifaces = new Class<?>[] { clazz };
        InvocationHandler invoker = NULLS_INVOKER;
        return clazz.cast( Proxy.newProxyInstance( loader_, ifaces, invoker ) );
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
}
