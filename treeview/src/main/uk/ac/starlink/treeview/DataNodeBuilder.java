package uk.ac.starlink.treeview;

import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import javax.xml.transform.Source;
import javax.xml.transform.dom.DOMSource;
import org.w3c.dom.Node;
import uk.ac.starlink.util.Compression;
import uk.ac.starlink.util.DataSource;
import uk.ac.starlink.util.FileDataSource;
import uk.ac.starlink.util.URLDataSource;

/**
 * Constructs a DataNode from an Object using a particular method or
 * constructor.  Instances of this class are the basic building blocks
 * used by DataNodeFactory to do its DataNode construction.
 */
public abstract class DataNodeBuilder {

    private Constructor constructor;
    private Class argClass;
    public static boolean verbose = false;
    public static PrintStream verbStream = System.err;

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
     * @return  a new DataNode made from <tt>obj</tt>
     * @throws  NoSuchDataException  if no new node can be created
     */
    public abstract DataNode buildNode( Object obj ) throws NoSuchDataException;

    /**
     * Returns an array of DataNodeBuilder objects which are all the
     * ones that can be found by reflection in the supplied class.
     *
     * @param   clazz   a class to reflect on
     * @return  an array of builder objects found in <tt>clazz</tt>
     */
    public static DataNodeBuilder[] getBuilders( final Class clazz ) {

        /* If clazz isn't a subclass of DataNode, none of the constructors
         * will be any good. */
        if ( ! DataNode.class.isAssignableFrom( clazz ) ) {
            return new DataNodeBuilder[ 0 ];
        }

        /* Set up a list of the builders that we have. */
        final Map builderMap = new HashMap();

        /* Otherwise, look at all the constructors and see which
         * might be OK, i.e. have a single argument.  For each of these
         * construct a new builder. */
        List builders = new ArrayList();
        Constructor[] constructors = clazz.getDeclaredConstructors();
        for ( int i = 0; i < constructors.length; i++ ) {
            final Constructor constructor = constructors[ i ];
            Class[] argTypes = constructor.getParameterTypes();
            if ( argTypes.length == 1 ) {

                /* We can make a builder out of this constructor. */
                final Class argClass = argTypes[ 0 ];
                DataNodeBuilder builder = 
                        new SimpleDataNodeBuilder( clazz, argClass ) {
                    public DataNode buildNode( Object obj )
                            throws NoSuchDataException {
                        Object[] args = new Object[] { obj };
                        try {
                            return (DataNode) constructor.newInstance( args );
                        }
                        catch ( InvocationTargetException e ) {
                            Throwable target = e.getTargetException();

                            /* If the constructor threw a NoSuchDataException,
                             * it just means that obj wasn't suitable - 
                             * rethrow it. */
                            if ( target instanceof NoSuchDataException ) {
                                throw (NoSuchDataException) target;
                            }

                            /* Otherwise, something has gone wrong in the
                             * constructor.  Create an error node. */
                            return new ErrorDataNode( target );
                        }

                        /* The other kinds of exception probably represent
                         * programming errors.  Make an error node. */
                        catch ( InstantiationException e ) {
                            return new ErrorDataNode( e );
                        }
                        catch ( IllegalAccessException e ) {
                            return new ErrorDataNode( e );
                        }
                    }
                };
                builders.add( builder );
                builderMap.put( argClass, builder );
            }
        }

        /* We may be able to add some using standard techniques for getting
         * one kind of base object from another. */

        /* Use an XML Source builder on a DataSource. */
        if ( builderMap.containsKey( Source.class ) &&
             ! builderMap.containsKey( DataSource.class ) ) {
            final DataNodeBuilder xmlBuilder = 
                (DataNodeBuilder) builderMap.get( Source.class );
            DataNodeBuilder dataBuilder = 
                    new SimpleDataNodeBuilder( clazz, DataSource.class ) {

                public DataNode buildNode( Object obj )
                        throws NoSuchDataException {
                    DataSource datsrc = (DataSource) obj;
                    try {
                        Source xmlsrc = SourceDataNodeBuilder
                                       .makeDOMSource( datsrc );
                        return configureNode( xmlBuilder.buildNode( xmlsrc ),
                                              datsrc );
                    }
                    catch ( NoSuchDataException e ) {
                        datsrc.close();
                        throw e;
                    }
                }
            };
            builders.add( dataBuilder );
            builderMap.put( DataSource.class, dataBuilder );
        }

        /* Use a File or DataSource builder on a String. */
        if ( ( builderMap.containsKey( File.class ) ||
               builderMap.containsKey( DataSource.class ) ) && 
             ! builderMap.containsKey( String.class ) ) {
            final DataNodeBuilder fileBuilder = 
                (DataNodeBuilder) builderMap.get( File.class );
            final DataNodeBuilder sourceBuilder =
                (DataNodeBuilder) builderMap.get( DataSource.class );
            DataNodeBuilder stringBuilder = 
                    new SimpleDataNodeBuilder( clazz, String.class ) {

                public DataNode buildNode( Object obj )
                        throws NoSuchDataException {
                    String name = (String) obj;
                    DataSource datsrc = null;
                    try {
                        datsrc = DataSource.makeDataSource( name );

                        /* Special case for a FileDataNode itself (doesn't
                         * matter if this is compressed). */
                        if ( datsrc instanceof FileDataSource &&
                             clazz.equals( FileDataNode.class ) ) {
                            File file = ((FileDataSource) datsrc).getFile();
                            return configureNode( fileBuilder.buildNode( file ),
                                                  file );
                        }

                        /* Treat it as a file if possible. */
                        else if ( datsrc instanceof FileDataSource &&
                                  fileBuilder != null ) {
                            File file = ((FileDataSource) datsrc).getFile();
                            Compression compress = datsrc.getCompression();
                            if ( datsrc.getCompression() == Compression.NONE ) {
                                datsrc.close();
                                return configureNode( fileBuilder
                                                     .buildNode( file ), file );
                            }
                            else if ( sourceBuilder == null ) {
                                datsrc.close();
                                throw new NoSuchDataException(
                                    "File " + file + " is compressed (" +
                                    compress + ")" );
                            }
                        }

                        /* Not a file. */
                        if ( sourceBuilder != null ) {
                            try {
                                return configureNode( sourceBuilder
                                                     .buildNode( datsrc ),
                                                      datsrc );
                            }
                            catch ( NoSuchDataException e ) {
                                datsrc.close();
                                throw e;
                            }
                        }
                        else {
                            throw new NoSuchDataException( 
                                "No stream builder" );
                        }
                    }
                    catch ( IOException e ) {
                        if ( datsrc != null ) {
                            datsrc.close();
                        }
                        throw new NoSuchDataException( e );
                    }
                }
            };
            builders.add( stringBuilder );
            builderMap.put( String.class, stringBuilder );
        }

        /* Use a DataSource builder on a File. */
        if ( builderMap.containsKey( DataSource.class ) &&
             ! builderMap.containsKey( File.class ) ) {
            final DataNodeBuilder sourceBuilder = 
                (DataNodeBuilder) builderMap.get( DataSource.class );
            DataNodeBuilder fileBuilder = 
                    new SimpleDataNodeBuilder( clazz, File.class ) {

                public DataNode buildNode( Object obj ) 
                        throws NoSuchDataException {
                    File file = (File) obj;
                    DataSource datsrc = null;
                    try {
                        datsrc = new FileDataSource( file );
                        datsrc.setName( file.getName() );
                        return configureNode( sourceBuilder.buildNode( datsrc ),
                                              datsrc );
                    }
                    catch ( IOException e ) {
                        if ( datsrc != null ) {
                            datsrc.close();
                        }
                        throw new NoSuchDataException( e );
                    }
                }
            };
            builders.add( fileBuilder );
            builderMap.put( File.class, fileBuilder );
        }

        return (DataNodeBuilder[]) builders.toArray( new DataNodeBuilder[ 0 ] );
    }

    /**
     * Configures a datanode with some additional information if its
     * source is known.  This step is not essential, but can provide
     * the opportunity for more functionality in the viewer.
     *
     * @param  node  the DataNode to configure
     * @param  obj  the object on which <tt>node</tt> is based
     * @return  the original object <tt>obj</tt>, with the additional
     *          configuration, is returned for convenience
     */
    public static DataNode configureNode( DataNode node, Object obj ) {

        Object parent = null;
        String label = null;
        String path = null;

        if ( obj instanceof FileDataSource ) {
            File file = ((FileDataSource) obj).getFile();
            return configureNode( node, file );
        }

        else if ( obj instanceof File ) {
            File file = (File) obj;
            path = file.getAbsolutePath();
            label = file.getName();
            parent = file.getAbsoluteFile().getParent();
        }
 
        else if ( obj instanceof URLDataSource ) {
            path = ((URLDataSource) obj).getURL().toString();
        }

        else if ( obj instanceof PathedDataSource ) {
            path = ((PathedDataSource) obj).getPath();
        }

        else if ( obj instanceof DOMSource ) {
            DOMSource dsrc = (DOMSource) obj;
            String sysid = dsrc.getSystemId();
            Node pnode = dsrc.getNode().getParentNode();
            if ( pnode != null ) {
                parent = new DOMSource( pnode, sysid ); 
            }
            else if ( sysid != null && sysid.trim().length() > 0 ) {
                parent = sysid;
            }
        }

        /* Get a suitable label from a source name if we have one.  The format
         * of a DataSource name is not defined, but it may be some sort of
         * path - try to pick the last element of it. */
        if ( label == null && obj instanceof DataSource ) {
            String name = ((DataSource) obj).getName();
            Matcher match = Pattern.compile( "([^ /\\\\:]+)$" ).matcher( name );
            if ( match.matches() ) {
                label = match.group( 1 );
            }
        }

        /* Do the actual configuration. */
        if ( parent != null && node instanceof DefaultDataNode ) {
            ((DefaultDataNode) node).setParentObject( parent );
        }
        if ( path != null ) {
            node.setPath( path );
        }
        if ( label != null ) {
            node.setLabel( label );
        }

        /* Return the original object. */
        return node;
    }

}
