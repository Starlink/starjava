package uk.ac.starlink.treeview;

import java.util.*;
import java.io.*;
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
 * <li> {@link NdxDataNode}
 * <li> {@link VOTableDataNode}
 * <li> {@link XMLDataNode}
 * <li> {@link ZipFileDataNode}
 * <li> {@link TarFileDataNode}
 * <li> {@link NDArrayDataNode}
 * <li> {@link FileDataNode}
 * </ul>
 * The factory will churn out a <code>DataNode</code> object based on
 * the object it is given for construction, the constructors available
 * from the known implementing objects (the above list), and optionally
 * a list of preferences which may be examined and modified using 
 * supplied methods.
 * <p>
 * The factory has a list of DataNodeBuilder objects which it uses
 * to try to construct nodes from any given object, be it a filename,
 * string, XML source, or whatever.  the {@link #makeDataNode} method 
 * passes the object to each suitable builder to see if it can turn 
 * it into a DataNode, and returns the first successful result.
 * Thus the list of DataNodeBuilders and its order determines what kind
 * of DataNode you will get.
 * <p>
 * There are two types of builder in the list.  The first is generated
 * by reflection on a number of DataNode-implementing classes as listed
 * above.  These are made out of suitable (one-argument) constructors
 * supplied by those classes.  The second type is a special one of
 * type {@link FileDataNodeBuilder}.  This is smart and fast and 
 * can make clever decisions about what kind of data node a given file
 * should be turned into.
 * <p>
 * Initially a newly constructed DataNodeFactory has a 
 * <tt>FileDataNodeBuilder</tt> at the head of the list, followed by
 * ones got from constructors of the known DataNode implementations.
 * This means that a file or string will get tackled first by 
 * the clever class, but if that fails it will trawl through all the
 * other possibilities.  Modifying the list of preferred classes
 * using {@link #setNodeClassList} or {@link #setPreferredClass}
 * will normally demote the <tt>FileDataNodeBuilder</tt> so that
 * all node construction is done by brute force in strict order
 * of classes in the class list.
 *
 * @author   Mark Taylor (Starlink)
 * @version  $Id$
 */
public class DataNodeFactory implements Cloneable {

    private boolean verbose;
    private PrintStream verbStream;
    private List classList;
    private List tried;
    private List builders;

    private static List defaultClassList;

    /**
     * Initialises a new <code>DataNodeFactory</code> with default settings.
     */
    public DataNodeFactory() {
        setNodeClassList( getDefaultClassList() );
        builders.addAll( 0, getSpecialBuilders() );
    }

    /**
     * Sets the list of preferred classes which this factory should produce.
     *
     * @param  classList  a List of Class objects.  Each of these will 
     *         be turned into a list of DataNodeBuilder objects.
     *         A copy of the list is used.
     */
    public void setNodeClassList( List classList ) {

        /* Store our copy of this list. */
        this.classList = new ArrayList( classList );

        /* Get a corresponding list of builders. */
        this.builders = new ArrayList();
        for ( Iterator cit = classList.iterator(); cit.hasNext(); ) {
            Class clazz = (Class) cit.next();
            DataNodeBuilder[] bbatch = DataNodeBuilder.getBuilders( clazz );
            if ( bbatch.length == 0 ) {
                System.err.println( "No builders from class " + clazz + "?" );
            }
            builders.addAll( Arrays.asList( bbatch ) );
        }
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
    public List getNodeClassList() {
        return Collections.unmodifiableList( classList );
    }

    /**
     * Ensures that the factory will not generate the indicated node type.
     *
     * @param  clazz               a Class to remove from the class list
     */
    public void removeNodeClass( Class clazz ) {
        List clist = classList;
        for ( Iterator it = clist.iterator(); it.hasNext(); ) {
            if ( it.next().equals( clazz ) ) {
                it.remove();
            }
        }
        setNodeClassList( clist );
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
     */
    public void setPreferredClass( Class pref ) {
        removeNodeClass( pref );
        List clist = classList;
        clist.add( 0, pref );
        setNodeClassList( clist );
    }

    /**
     * Sets the builder object to be used in preference to all others
     * for generating DataNode objects.  This is inserted at the head of
     * the list of builders.
     *
     * @param  builder  the builder to put at the head of the list
     */
    public void setPreferredBuilder( DataNodeBuilder builder ) {
        builders.add( 0, builder );
    }

    /**
     * Returns the list of builder objects actually used in sequence to
     * try creating datanodes.
     *
     * @return  the list of builders in order
     */
    public List getBuilders() {
        return builders;
    }

    /** 
     * Generates a new DataNode from a given object.
     * This looks for constructors in the preferred classes list
     * which take a single argument and tries to construct a new 
     * <tt>DataNode</tt> using each one with a suitable type.
     * Suitable constructors from classes in the class preference list
     * will be tried 
     * in turn until one is successful.  If <code>setVerbose</code> has
     * been called then extensive logging of the constructions tried and
     * why each one failed can be performed.
     *
     * @param   parent               the DataNode whose child this is
     * @param   obj                  an object which can be used by one of
     *                               the underlying constructors to generate
     *                               a <code>DataNode</code> object.
     * @return                       a new <code>DataNode</code> object
     * @throws  NoSuchDataException  if no constructor could be found and 
     *                               successfully invoked
     */
    public DataNode makeDataNode( DataNode parent, Object obj ) 
            throws NoSuchDataException {
        if ( verbose ) {
            verbStream.println( "\nTrying to construct DataNode from "
                              + obj + ":  " );
        }
        Class objClass = obj.getClass();
        tried = new ArrayList();
        for ( Iterator buildIt = builders.iterator(); buildIt.hasNext(); ) {
            DataNodeBuilder builder = (DataNodeBuilder) buildIt.next();
            if ( builder.suitable( objClass ) ) {
                if ( verbose ) {
                    verbStream.println( "    " + builder );
                }
                DataNode newNode = builder.buildNode( obj );
                if ( newNode != null ) {
                    if ( verbose ) {
                        verbStream.println( "SUCCESS: " + newNode + "\n" );
                    }
                    newNode.setCreator( 
                        new CreationState( this, builder, parent, obj ) );
                    return newNode;
                }
                tried.add( builder );
            }
        }

        /* Dropped off the end of the loop - no success. */
        throw new NoSuchDataException( 
             "No suitable node could be constructed for " + obj );
    }

    /**
     * Makes a DataNode from a Throwable.  This behaves the same as
     * <tt>makeDataNode</tt> but for convenience it doesn't throw a
     * NoSuchDataException, since it can guarantee to make a DataNode
     * from the throwable.
     *
     * @param  th  the Throwable object from which to construct the node
     * @param  parent   the DataNode whose child this is
     * @param  a DataNode (probably an ErrorDataNode) representing <tt>th</tt> 
     */
    public DataNode makeErrorDataNode( DataNode parent, Throwable th ) {
        try {
            return makeDataNode( parent, th );
        }
        catch ( NoSuchDataException e ) {
            return new ErrorDataNode( th );
        }
    }

    /**
     * Returns a string representation of this factory.
     * The returned string is a comprehensive list of which constructors
     * will be tried.
     *
     * @return  an ordered list of the constructors that the
     *          <code>makeDataNode</code> will try to use.
     */
    public String toString() {
        StringBuffer buf = 
            new StringBuffer( "DataNodeFactory with builders:\n" );
        for ( Iterator bit = builders.iterator(); bit.hasNext(); ) {
            buf.append( "    " )
               .append( bit.next().toString() )
               .append( '\n' );
        }
        return buf.toString();
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
    public List getClassesTried() {
        return tried;
    }

    public Object clone() {
        try {
            DataNodeFactory twin = (DataNodeFactory) super.clone();
            twin.setNodeClassList( classList );
            twin.tried = new ArrayList( tried );
            return twin;
        }
        catch ( CloneNotSupportedException e ) {
            throw new AssertionError();
        }
    }

    /**
     * Returns a list of builders which, for a default DataNodeFactory,
     * are invoked before any of the constructor-based ones.  
     * These allow cleverer decisions to be made about what kind of
     * node is generated from what object: if a given object 
     * could make either a FooDataNode or a BarDataNode but would be
     * better off as a BarDataNode, this can be done even if FooDataNode
     * was higher in the class preference list for this factory.
     * For the default configuration, this list should come first in
     * the list of builders, if the user reconfigures the preferred
     * class list it may slip lower down or disappear from the hierarchy.
     */
    public static List getSpecialBuilders() {

        final DataNodeBuilder fileBuilder = FileDataNodeBuilder.getInstance();

        DataNodeBuilder stringBuilder = new DataNodeBuilder() {
            public boolean suitable( Class objClass ) {
                return objClass.equals( String.class );
            }
            public DataNode buildNode( Object obj ) {
                if ( ! ( obj instanceof String ) ) {
                    return null;
                }
                return fileBuilder.buildNode( new File( (String) obj ) );
            }
            public String toString() {
                return "special DataNodeBuilder (java.lang.String)";
            }
        };

        List specials = new ArrayList();
        specials.add( fileBuilder );
        specials.add( stringBuilder );
        return specials;
    }

    /**
     * Returns the default class list which is installed into a new
     * DataNodeFactory on initialisation. 
     */
    public static List getDefaultClassList() {
        if ( defaultClassList == null ) {
            Class[] classes = new Class[] {
                NDFDataNode.class,
                WCSDataNode.class,
                ARYDataNode.class,
                HistoryDataNode.class,
                HDSDataNode.class,
                FITSDataNode.class,
                NdxDataNode.class,
                VOTableDataNode.class,
                XMLDataNode.class,
                VOTableTableDataNode.class,
                VOComponentDataNode.class,
                ZipFileDataNode.class,
                TarFileDataNode.class,
                NDArrayDataNode.class,
                FileDataNode.class,
            };
            defaultClassList = new ArrayList( Arrays.asList( classes ) );

            /* Some of the classes are contingent on having working HDS and
             * AST subsystems available, which may not be the case if the 
             * corresponding native libraries are not present.  Remove classes
             * we know that we will not be able to deal with. */
            if ( ! Driver.hasHDS ) {
                defaultClassList.remove( NDFDataNode.class );
                defaultClassList.remove( ARYDataNode.class );
                defaultClassList.remove( HistoryDataNode.class );
                defaultClassList.remove( HDSDataNode.class );
            }
            if ( ! Driver.hasAST ) {
                defaultClassList.remove( WCSDataNode.class );
            }
        }
        return defaultClassList;
    }


}
