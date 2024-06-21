package uk.ac.starlink.datanode.factory;

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
import uk.ac.starlink.datanode.nodes.DataNode;
import uk.ac.starlink.datanode.nodes.ErrorDataNode;
import uk.ac.starlink.datanode.nodes.FileDataNode;
import uk.ac.starlink.datanode.nodes.NoSuchDataException;
import uk.ac.starlink.datanode.nodes.XMLDocument;
import uk.ac.starlink.util.Compression;
import uk.ac.starlink.util.DataSource;
import uk.ac.starlink.util.FileDataSource;

/**
 * Constructs a DataNode from an Object using a particular method or
 * constructor.  Instances of this class are the basic building blocks
 * used by DataNodeFactory to do its DataNode construction.
 */
public abstract class DataNodeBuilder {

    private Constructor constructor;
    private Class argClass;

    /**
     * Determine whether this builder can be used to work on an object
     * of a given class.
     *
     * @param   objClass  the class of an object which might be passed
     *          as the argument of <code>buildNode</code>
     * @return  whether it's OK to do that
     */
    public abstract boolean suitable( Class objClass );

    /**
     * Builds a DataNode from a given object.
     *
     * @param   obj   the object to build a datanode from
     * @return  a new DataNode made from <code>obj</code>
     * @throws  NoSuchDataException  if no new node can be created
     */
    public abstract DataNode buildNode( Object obj ) throws NoSuchDataException;

    /**
     * Returns the class which all nodes returned by the {@link #buildNode}
     * method will belong to.  DataNodeBuilder's implementation of this
     * returns <code>DataNode.class</code>, but implementations which can
     * be more specific should override this method.
     *
     * @return   superclass of all the classes of DataNode this builder
     *           can build
     */
    public Class getNodeClass() {
        return DataNode.class;
    }

    /**
     * Returns an array of DataNodeBuilder objects which are all the
     * ones that can be found by reflection in the supplied class.
     *
     * @param   clazz   a class to reflect on
     * @return  an array of builder objects found in <code>clazz</code>
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

        /* Use an XML Document builder on a DataSource. */
        if ( builderMap.containsKey( XMLDocument.class ) &&
             ! builderMap.containsKey( DataSource.class ) ) {
            final DataNodeBuilder docBuilder = 
                (DataNodeBuilder) builderMap.get( XMLDocument.class );
            DataNodeBuilder dataBuilder = 
                    new SimpleDataNodeBuilder( clazz, DataSource.class ) {

                public DataNode buildNode( Object obj )
                        throws NoSuchDataException {
                    DataSource datsrc = (DataSource) obj;
                    try {
                        XMLDocument xdoc = new XMLDocument( datsrc );
                        return docBuilder.buildNode( xdoc );
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
                            return fileBuilder.buildNode( file );
                        }

                        /* Treat it as a file if possible. */
                        else if ( datsrc instanceof FileDataSource &&
                                  fileBuilder != null ) {
                            File file = ((FileDataSource) datsrc).getFile();
                            Compression compress = datsrc.getCompression();
                            if ( datsrc.getCompression() == Compression.NONE ) {
                                datsrc.close();
                                return fileBuilder.buildNode( file );
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
                                return sourceBuilder.buildNode( datsrc );
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
                        return sourceBuilder.buildNode( datsrc );
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

        /* Use a File builder on a DataSource. */
        if ( builderMap.containsKey( File.class ) &&
             ! builderMap.containsKey( DataSource.class ) ) {
            final DataNodeBuilder fileBuilder =
                (DataNodeBuilder) builderMap.get( File.class );
            DataNodeBuilder sourceBuilder =
                    new SimpleDataNodeBuilder( clazz, DataSource.class ) {
                public DataNode buildNode( Object obj )
                        throws NoSuchDataException {
                    DataSource datsrc = (DataSource) obj;
                    if ( datsrc instanceof FileDataSource ) {
                        File file = ((FileDataSource) datsrc).getFile();
                        return fileBuilder.buildNode( file );
                    }
                    else {
                        throw new NoSuchDataException( 
                                      "Only file-type DataSources supported" );
                    }
                }
            };
            builders.add( sourceBuilder );
            builderMap.put( DataSource.class, sourceBuilder );
        }

        return (DataNodeBuilder[]) builders.toArray( new DataNodeBuilder[ 0 ] );
    }

}
