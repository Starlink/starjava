package uk.ac.starlink.treeview;

import java.util.ArrayList;
import java.util.List;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;

/**
 * Constructs a DataNode from an Object using a particular method or
 * constructor.  Instances of this class are the basic building blocks
 * used by DataNodeFactory to do its DataNode construction.
 */
public abstract class DataNodeBuilder {

    private Constructor constructor;
    private Class argClass;
    private static boolean verbose = false;

    /**
     * Determine whether this builder can be used to work on an object
     * of a given class.
     *
     * @param   objClass  the class of an object which might be passed
     *          as the argument of <tt>buildNode</tt>
     * @return  whether it's OK to do that
     */
    public abstract boolean suitable( Class objClass );

    /**
     * Builds a DataNode from a given object.
     *
     * @param   obj   the object to build a datanode from
     * @return  a new DataNode made from <tt>obj</tt>, or null if it 
     * couldn't be done.
     */
    public abstract DataNode buildNode( Object obj );

    /**
     * Returns an array of DataNodeBuilder objects which are all the
     * ones that can be found by reflection in the supplied class.
     *
     * @param   clazz   a class to reflect on
     * @return  an array of builder objects found in <tt>clazz</tt>
     */
    public static DataNodeBuilder[] getBuilders( Class clazz ) {

        /* If clazz isn't a subclass of DataNode, none of the constructors
         * will be any good. */
        if ( ! DataNode.class.isAssignableFrom( clazz ) ) {
            return new DataNodeBuilder[ 0 ];
        }

        /* Otherwise, look at all the constructors and see which
         * might be OK, i.e. have a single argument. */
        List builders = new ArrayList();
        Constructor[] constructors = clazz.getDeclaredConstructors();
        for ( int i = 0; i < constructors.length; i++ ) {
            final Constructor constructor = constructors[ i ];
            Class[] argTypes = constructor.getParameterTypes();
            if ( argTypes.length == 1 ) {

                /* We can make a builder out of this constructor. */
                final Class argClass = argTypes[ 0 ];
                DataNodeBuilder builder = new DataNodeBuilder() {
                    public String toString() {
                        return constructor.getName() 
                             + "(" + argClass.getName() + ")";
                    }
                    public boolean suitable( Class objClass ) {
                        return argClass.isAssignableFrom( objClass );
                    }
                    public DataNode buildNode( Object obj ) {
                        Object[] args = new Object[] { obj };
                        try {
                            return (DataNode) constructor.newInstance( args );
                        }
                        catch ( InvocationTargetException e ) {
                            Throwable target = e.getTargetException();

                            /* If the constructor threw a NoSuchDataException, 
                             * it just means that obj wasn't suitable.
                             *  Return null. */
                            if ( target instanceof NoSuchDataException ) {
                                if ( verbose ) {
                                    target.printStackTrace();
                                }
                                return null;
                            }

                            /* Some other kind of exception was thrown by 
                             * the constructor.  Re-throw it. */
                           else {
                               return new DefaultDataNode( target );
                           }
                        }
                        catch ( InstantiationException e ) {
                            return new DefaultDataNode( e );
                        }
                        catch ( IllegalAccessException e ) {
                            return new DefaultDataNode( e );
                        }
                    }
                };
                builders.add( builder );
            }
        }
        return (DataNodeBuilder[]) builders.toArray( new DataNodeBuilder[ 0 ] );
    }

}
