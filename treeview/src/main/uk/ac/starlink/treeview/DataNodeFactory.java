package uk.ac.starlink.treeview;

import java.util.*;
import java.io.*;
import java.lang.reflect.*;
import java.text.*;

/**
 * Factory class for constructing {@link DataNode} objects.
 * Instances of this class can be used to construct a <code>DataNode</code> 
 * from a generic input - for instance a <code>String</code> or a 
 * <code>File</code>.
 * It tries to find the most appropriate existing object of one of the
 * classes which implements <code>DataNode</code>.
 * The classes it knows about, in rough order of preference, are:
 * <ul>
 * <li> {@link NDFDataNode}
 * <li> {@link WCSDataNode}
 * <li> {@link ARYDataNode}
 * <li> {@link HistoryDataNode}
 * <li> {@link HDSDataNode}
 * <li> {@link FITSDataNode}
 * <li> {@link XMLDocumentDataNode}
 * <li> {@link XMLElementDataNode}
 * <li> {@link ZipFileDataNode}
 * <li> {@link FileDataNode}
 * <li> {@link NdxDataNode}
 * <li> {@link NDArrayDataNode}
 * </ul>
 * The factory will churn out a <code>DataNode</code> object based on
 * the object it is given for construction, the constructors available
 * from the known implementing objects (the above list), and optionally
 * a list of preferences which may be examined and modified using 
 * supplied methods.
 *
 * @author   Mark Taylor (Starlink)
 * @version  $Id$
 */
public class DataNodeFactory implements Cloneable {

    private boolean verbose;
    private PrintStream verbStream;
    private Class[] classList;
    private Class[] classesTried;

    private static final Class[] initialClassList = new Class[] {
        NDFDataNode.class,
        WCSDataNode.class,
        ARYDataNode.class,
        HistoryDataNode.class,
        HDSDataNode.class,
        FITSDataNode.class,
        // XMLDocumentDataNode.class,
        // XMLElementDataNode.class,
        ZipFileDataNode.class,
        NdxDataNode.class,
        NDArrayDataNode.class,
        FileDataNode.class
    };

    /**
     * Initialises a new <code>DataNodeFactory</code> with default settings.
     */
    public DataNodeFactory() {
    }

    /**
     * Sets the list of preferred classes which this factory should produce.
     *
     * @param  nodeClassList        an array of Classes, each of which must 
     *                              implement the <code>DataNode</code> 
     *                              interface.  These classes will be searched,
     *                              in order, for constructors which can 
     *                              generate a <code>DataNode</code> object 
     *                              when the <code>makeDataNode</code> method 
     *                              is called.
     * @throws  ClassCastException  if any of the elements of 
     *                              <code>nodeClassList</code> is not an 
     *                              instance of <code>DataNode</code>
     */
    public void setNodeClassList( Class[] nodeClassList ) {

        /* Check that all the supplied classes implement DataNode. */
        for ( int i = 0; i < nodeClassList.length; i++ ) {
            Class clazz = nodeClassList[ i ];
            if ( ! DataNode.class.isAssignableFrom( clazz ) ) {
                throw new ClassCastException( clazz.toString() 
                                            + " is not a DataNode" );
            }
        }

        /* OK we can use the supplied class list. */
        this.classList = nodeClassList;
    }

    /**
     * Gets the list of preferred classes which this factory will produce.
     *
     * @return  an array of Classes, each of which implements the 
     *          <code>DataNode</code> interface.  These are the classes which
     *          are searched, in order, for constructors which can generate
     *          a <code>DataNode</code> object when the 
     *          <code>makeDataNode</code> method is called.
     */
    public Class[] getNodeClassList() {

        /*
         * We have to check for an uninitialized classList.  The more obvious
         * method of setting classList using a static initializer is not 
         * possible because of circular dependencies between this and the
         * client DataNode implementors with regard to static initialization.
         */
        if ( classList == null ) {
            classList = initialClassList;
        }
        return classList;
    }

    /**
     * Gets an iterator over the known class list.
     *
     * @return  an <code>Iterator</code> over all the Classes in the known
     *          class list
     */
    public Iterator getNodeClassIterator() {
        return Arrays.asList( getNodeClassList() ).iterator();
    }

    /**
     * Ensures that the factory will not generate the indicated node type.
     *
     * @param  clazz               a Class to remove from the class list
     */
    public void removeNodeClass( Class clazz ) {
        Class[] oldlist = getNodeClassList();
        int nc = oldlist.length;
        Vector cvec = new Vector( nc );
        for ( int i = 0; i < nc; i++ ) {
            if ( ! oldlist[ i ].equals( clazz ) ) {
                cvec.add( oldlist[ i ] );
            }
        }
        setNodeClassList( (Class[]) cvec.toArray( new Class[ 0 ] ) );
    }
   

    /**
     * Sets the class you would most like to see generated by the factory.
     *
     * @param   pref                a Class, which must implement the 
     *                              <code>DataNode</code> interface, to be
     *                              used as the preferred object type that
     *                              the <code>makeDataNode</code> method will
     *                              generate.  What this in fact does is to
     *                              push it to the head of the preferred classes
     *                              list (removing it from its current position
     *                              in that list if necessary).
     *
     * @throws  ClassCastException  if <code>pref</code> is not an instance of
     *                              <code>DataNode</code>
     */
    public void setPreferredClass( Class pref ) {

        /* Check we have been supplied with a class which implements 
         * DataNode. */
        if ( ! DataNode.class.isAssignableFrom( pref ) ) {
            throw new ClassCastException( pref.toString() 
                                        + " is not a DataNode" );
        }

        /*
         * Construct a new list with the preferred one at the front (and if
         * necessary removed from the middle) and set this object's list to it.
         */
        Iterator classIt = getNodeClassIterator();
        Vector newList = new Vector();
        newList.add( pref );
        while ( classIt.hasNext() ) {
            Class cl = (Class) classIt.next();
            if ( cl != pref ) {
                newList.add( cl );
            }
        }
        setNodeClassList( (Class[]) newList.toArray( new Class[ 0 ] ) );
    }

    /** 
     * Generates a new DataNode object based on the runtime type of a supplied
     * object.  This looks for a constructor in the preferred classes list
     * which takes a single argument of the runtime type of <code>obj</code>,
     * and tries to construct a new <code>DataNode</code> object using it.
     * Constructors which take a single argument of the runtime type of
     * type <code>obj</code> in the class preference list will be tried 
     * in turn until one is successful.  If <code>setVerbose</code> has
     * been called then extensive logging of the constructions tried and
     * why each one failed can be performed.
     *
     * @param   obj                  an object which can be used by one of
     *                               the underlying constructors to generate
     *                               a <code>DataNode</code> object.
     *                               Constructors which take a single argument
     *                               of the runtime class of <code>obj</code>
     *                               of classes in the class preference list
     *                               will be tried in turn until one is 
     *                               successful.
     * @return                       a new <code>DataNode</code> object
     * @throws  NoSuchDataException  if no constructor could be found and 
     *                               successfully invoked
     */
    public DataNode makeDataNode( Object obj ) throws NoSuchDataException {
        return makeDataNode( obj.getClass(), obj );
    }

    /** 
     * Generates a new DataNode object based on a supplied object of an
     * indicated type.
     * This looks for a constructor in the preferred classes list
     * which takes a single argument of type <code>objClass</code>,
     * and tries to construct a new <code>DataNode</code> object using it.
     * Constructors which take a single argument of type <code>objClass</code>
     * in the class preference list will be tried in turn until one is 
     * successful.
     *
     * @param   obj                  an object which can be used by one of
     *                               the underlying constructors to generate
     *                               a <code>DataNode</code> object.
     * @param   objClass             the type of the single argument of 
     *                               constructors of the underlying classes 
     *                               to use.
     * @return                       a new <code>DataNode</code> object
     * @throws  NoSuchDataException  if no constructor could be found and 
     *                               successfully invoked
     */
    public DataNode makeDataNode( Class objClass, Object obj ) 
            throws NoSuchDataException {

        /* Set up arrays of types and values for the subclass constructors. */
        if ( verbose ) {
            verbStream.println( "\nTrying to construct DataNode from "
                              + objClass.getName() + " " + obj + ":  " );
        }
        ArrayList tried = new ArrayList();
        Class[] argClasses = new Class[] { objClass };
        Object[] argValues = new Object[] { obj };

        /* For each subclass we know about, try to find and execute a 
         * constructor of the correct type. */
        DataNode newDataNode = null;
        boolean success = false;
        Iterator classIt = getNodeClassIterator();
        while ( classIt.hasNext() ) {
            Class nodeClass = (Class) classIt.next();
            try {
                Constructor constructor = 
                    nodeClass.getConstructor( argClasses );
                if ( verbose ) {
                    verbStream.print( "   " + nodeClass.getName() + ":  " );
                }
                newDataNode = (DataNode) constructor.newInstance( argValues );
                success = true;
                if ( verbose ) {
                    verbStream.println( "SUCCESS" );
                }
                break;
            }

            /* This constructor threw an exception while attempting to 
             * construct the node.  This is probably a NoSuchDataException,
             * which is fine - note the failure if required, and move on
             * to the next class. */
            catch ( InvocationTargetException e ) {
                tried.add( nodeClass );
                Throwable target = e.getTargetException();
                if ( verbose ) {
                    target.printStackTrace( verbStream );
                }

                /* Nope, we need to throw this.  The following code just 
                 * throws an unchecked Throwable based on the target exception
                 * in the most straightforward way possible (i.e. it wraps
                 * it in an unchecked Exception if necessary). */
                if ( ! ( target instanceof NoSuchDataException ) ) {
                    if ( target instanceof RuntimeException ) {
                        throw (RuntimeException) target;
                    }
                    else if ( target instanceof Error ) {
                        throw (Error) target;
                    }
                    else {
                        throw (RuntimeException) 
                              new RuntimeException().initCause( target );
                    }
                }
            }

            /* Constructor for the arg class we are considering does
             * not exist - no problem. */
            catch ( NoSuchMethodException e ) {
            }

            /* An unexpected exception has occurred - rethrow it. */
            catch ( Exception e ) {
                e.printStackTrace();
                throw new RuntimeException( e.toString() );
            }
        }

        /* Keep a record of the classes we tried. */
        classesTried = (Class[]) tried.toArray( new Class[ 0 ] );

        /* If no node could be constructed, say so. */
        if ( ! success ) {
            throw new NoSuchDataException( 
                      "No suitable DataNode could be constructed from " +
                      obj + " of type " + objClass.getName() );
        }

        /* Store information about how this node was created. */
        newDataNode.setCreator( new CreationState( this, objClass, obj ) );

        /* Return the node. */
        return newDataNode;
    }

    /**
     * Returns a string representation of this factory.
     * The returned string is a comprehensive list of which constructors
     * will be tried, listed by the class of the arguments.
     *
     * @return  an ordered list of the constructors that the
     *          <code>makeDataNode</code> will try to use.
     */
    public String toString() {
        String result = "";

        /* 
         * Examine all the classes which this DataNodeFactory knows about.
         * For each one, get a list of all the single-argument constructors
         * we might use to create a DataNode.  Write all the constructors
         * into a Map; the keys are the type of the single argument of
         * the constructors, and the values are Vectors containing a 
         * Constructor which we might use.
         */
        Iterator classIt = getNodeClassIterator();
        TreeMap paramClassMap = new TreeMap( new StringComparator() );
        while ( classIt.hasNext() ) {
            Class nodeClass = (Class) classIt.next();
            Constructor[] constructors = nodeClass.getConstructors();
            for ( int i = 0; i < constructors.length; i++ ) {
                Constructor constructor = constructors[ i ];
                Class[] params = constructor.getParameterTypes();
                if ( params.length == 1 ) {
                    Class paramClass = params[ 0 ];
                    if ( ! paramClassMap.containsKey( paramClass ) ) {
                        paramClassMap.put( paramClass, new Vector( 1 ) );
                    }
                    ( (Vector) paramClassMap.get( paramClass ) )
                   .add( constructor );
                }
            }
        }

        /*
         * Now output the constructors we know how to use.  Because of the
         * way the Map of Vectors was generated, this will come out
         * in order of priority for each constructor argument type.
         */
        Iterator entryIt = paramClassMap.entrySet().iterator();
        while ( entryIt.hasNext() ) {
            Map.Entry entry = (Map.Entry) entryIt.next();
            if ( ! result.equals( "" ) ) {
                result += "\n";
            }
            result += entry.getKey().toString() + "\n";
            Iterator constIt = ((Vector) entry.getValue()).iterator();
            while ( constIt.hasNext() ) {
                Constructor constr = (Constructor) constIt.next();
                result += "    " + constr.getName() + "\n";
            }
        }
        return result;
    }

  
    /** 
     * Determines whether verbose information about attempted constructions
     * is written.  Output is to standard error.  
     * Intended for debugging purposes.
     *
     * @param  isverb  whether to log information
     */
    public void setVerbose( boolean isverb ) {
        verbose = isverb;
        if ( verbose && verbStream == null ) {
            verbStream = System.err;
        }
    }

    /**
     * Return a list of the classes whose constructors were used in 
     * attempting to construct a DataNode during the last call of
     * <code>makeDataNode</code>.
     *
     * @return  the list of attempted classes
     */
    public Class[] getClassesTried() {
        return classesTried;
    }

    public Object clone() {
        try {
            DataNodeFactory twin = (DataNodeFactory) super.clone();
            twin.classList = (Class[]) classList.clone();
            twin.classesTried = (Class[]) classesTried.clone();
            return twin;
        }
        catch ( CloneNotSupportedException e ) {
            throw new AssertionError();
        }
    }
}
