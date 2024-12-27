package uk.ac.starlink.util;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Used to generate a proxy instance which implements a given interface and
 * delegates its calls to each of a given list of target implementations.
 * The content of the list of targets may be changed during the lifetime
 * of this object, but it's not a good idea to do it while a method is
 * being invoked.
 *
 * @author   Mark Taylor
 * @since    6 Jul 2011
 */
public class MultiplexInvocationHandler<T> implements InvocationHandler {

    private T[] targets_;

    /**
     * Constructor.
     *
     * @param  targets  target instances
     */
    @SuppressWarnings("this-escape")
    public MultiplexInvocationHandler( T[] targets ) {
        setTargets( targets );
    }

    /**
     * Sets the list of delegate implementations.
     *
     * @param  targets  target instances
     */
    public void setTargets( T[] targets ) {
        targets_ = targets;
    }

    /**
     * Returns the list of delegate implementations.
     *
     * @return  target instances
     */
    public T[] getTargets() {
        return targets_;
    }

    /**
     * Invokes a method by invoking the same method on each of this
     * handler's target instances.  If any invocation throws an exception,
     * it is thrown from this method and the method is not invoked on
     * later targets.  If the method terminates normally, the return value
     * is the return value of the invocation from the first target.
     */
    public Object invoke( Object proxy, Method method, Object[] args )
            throws Throwable {
        int nt = targets_.length;
        Object[] results = new Object[ nt ];
        for ( int i = 0; i < nt; i++ ) {
            results[ i ] = method.invoke( targets_[ i ], args );
        }
        return nt > 0 ? results[ 0 ] : null;
    }

    /**
     * Returns a new proxy instance which implements the given interface type,
     * and which uses this handler to execute its methods.
     *
     * @param  clazz  interface the return value will implement
     * @return   multiplexing proxy instance
     */
    public T createMultiplexer( Class<T> clazz ) {
        // Generics rules mean that the interface type has to be exactly T;
        // type bounds can't be used in this context (<U super T> fails).

        // Proxy instance is guaranteed to be an instance of each 
        // class specified as a proxy interface.
        @SuppressWarnings( "unchecked" )
        T proxy = (T) Proxy.newProxyInstance( clazz.getClassLoader(),
                                              new Class<?>[] { clazz }, this );
        return proxy;
    }
}
