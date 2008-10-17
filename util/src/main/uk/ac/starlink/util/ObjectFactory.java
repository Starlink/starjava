package uk.ac.starlink.util;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

/**
 * Manages dynamic creation of objects from a known set of classes.
 * An ObjectFactory keeps a list of classes with associated nicknames;
 * the idea is that you can obtain an instance of a given class by
 * supplying the nickname in question.
 * Instead of a nickname you can use the fully qualified classname,
 * whether or not it has previously been registered.
 * Any class registered must be a subclass of the superclass specified
 * when this factory is constructed, and must have a no-arg constructor.
 *
 * @author   Mark Taylor
 * @since    10 Aug 2005
 */
public class ObjectFactory {

    private final Class superClass_;
    private Map nameMap_ = new HashMap();
    private List nameList_ = new ArrayList();

    private static final Logger logger_ =
        Logger.getLogger( "uk.ac.starlink.util" );
 
    /**
     * Constructor.  
     *
     * @param  clazz  type which must be a supertype of any class registered
     *         with this factory
     */
    public ObjectFactory( Class clazz ) {
        superClass_ = clazz;
    }

    /**
     * Returns the class of which any object created by this factory 
     * is guaranteed to be an instance.
     *
     * @return   clazz
     */
    public Class getFactoryClass() {
        return superClass_;
    }

    /**
     * Registers a class with its nickname. 
     *
     * @param nickName  nickname
     * @param className  fully-qualified class name
     */
    public void register( String nickName, String className ) {
        nameList_.add( nickName );
        nameMap_.put( nickName, className );
    }

    /**
     * Returns a list of the nicknames which have been registered.
     *
     * @return  nickname array
     */
    public String[] getNickNames() {
        return (String[]) nameList_.toArray( new String[ 0 ] );
    }

    /**
     * Indicates whether this factory knows about a given name.
     * This may either be a registered nickname or a fully qualified 
     * classname for a class which is a subclass of this factory's
     * produced class.
     *
     * @param  name  name
     * @return  true iff <code>name</code> can sensibly be passed to
     *          {@link #createObject}
     */
    public boolean isRegistered( String name ) {

        /* Is it a registered nickname? */
        if ( nameMap_.containsKey( name ) ) {
            return true;
        }

        /* Is it a suitable fully qualified classname? */
        else {
            try {
                Class clazz = Class.forName( name );
                if ( superClass_.isAssignableFrom( clazz ) ) {
                     nameMap_.put( name, clazz.getName() );
                     return true;
                }
            }
            catch ( Throwable th ) {
            }
        }

        /* If neither, return false. */
        return false;
    }

    /**
     * Constructs and returns an object from one of the classes registered
     * with this factory.  If construction fails because the required
     * class is not on the classpath or there is some error in 
     * class initialization, a LoadException is thrown.
     * If the class is of the wrong sort (has no no-arg constructor,
     * is not a subtype of this factory's supertype) a RuntimeException
     * will be thrown.
     *
     * @param  name  classname/nickname of class to instantiate
     * @throws LoadException  if the load fails for unsurprising reasons
     */
    public Object createObject( String name ) throws LoadException {
        if ( ! isRegistered( name ) ) {
            throw new LoadException( "Unknown classname/nickname " + name );
        }
        String className = (String) nameMap_.get( name );
        logger_.config( "Instantiating " + className + " for " + name );
        Class clazz; 
        try {
            clazz = Class.forName( className );
        }
        catch ( LinkageError e ) {
            throw new LoadException( name + ": can't load " + className,
                                     e );
        }
        catch ( ClassNotFoundException e ) {
            throw new LoadException( name + ": can't load " + className,
                                     e );
        }
        if ( ! superClass_.isAssignableFrom( clazz ) ) {
            throw new ClassCastException( clazz + " does not subclass "
                                        + superClass_ );
        }
        try {
            return clazz.getConstructor( new Class[ 0 ] )
                        .newInstance( new Object[ 0 ] );
        }
        catch ( IllegalAccessException e ) {
            throw new RuntimeException( e );
        }
        catch ( NoSuchMethodException e ) {
            throw new RuntimeException( "No no-arg constructor for " + clazz,
                                        e );
        }
        catch ( InstantiationException e ) {
            throw new RuntimeException( e );
        }
        catch ( InvocationTargetException e ) {
            Throwable e2 = e.getCause();
            if ( e2 instanceof RuntimeException ) {
                throw (RuntimeException) e2;
            }
            else if ( e2 instanceof Error ) {
                throw (Error) e2;
            }
            else {
                throw new RuntimeException( e2 );
            }
        }
    }
}
