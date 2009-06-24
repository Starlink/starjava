/*
 * Copyright (C) 2006 Particle Physics and Astronomy Research Council
 *
 *  History:
 *     25-AUG-2006 (Peter W. Draper):
 *        Original version.
 */
package uk.ac.starlink.splat.data;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

import uk.ac.starlink.datanode.factory.CreationState;
import uk.ac.starlink.datanode.factory.DataNodeFactory;
import uk.ac.starlink.datanode.nodes.DataNode;
import uk.ac.starlink.datanode.nodes.DataObjectException;
import uk.ac.starlink.datanode.nodes.DataType;
import uk.ac.starlink.datanode.nodes.FITSFileDataNode;
import uk.ac.starlink.datanode.nodes.FITSStreamDataNode;
import uk.ac.starlink.datanode.nodes.FileDataNode;
import uk.ac.starlink.datanode.nodes.NodeUtil;
import uk.ac.starlink.datanode.nodes.PlainDataNode;
import uk.ac.starlink.datanode.nodes.VOTableDataNode;
import uk.ac.starlink.datanode.nodes.VOTableTableDataNode;
import uk.ac.starlink.datanode.nodes.XMLDataNode;
import uk.ac.starlink.datanode.nodes.ZipFileDataNode;
import uk.ac.starlink.ndx.Ndx;
import uk.ac.starlink.splat.data.SpecData;
import uk.ac.starlink.splat.data.SpecDataFactory;
import uk.ac.starlink.splat.util.SplatException;
import uk.ac.starlink.table.StarTable;
import uk.ac.starlink.table.StoragePolicy;
import uk.ac.starlink.util.DataSource;
import uk.ac.starlink.util.FileDataSource;
import uk.ac.starlink.votable.VOTableBuilder;

/**
 * Class of static methods for determining the type of a spectrum contained in
 * a local file using the techniques of the {@link uk.ac.starlink.datanode}
 * package, and creating {@link SpecData} instances from the file.
 *
 * @author Peter W. Draper
 */
public class SplatDataNode
{
    private static List shunnedClassList;
    private static List deprecatedClassList;

    private static SpecDataFactory specFactory = SpecDataFactory.getInstance();

    private static final StoragePolicy storagePolicy =
        StoragePolicy.getDefaultPolicy();

    /**
     * Constructs a new instance, hidden as this is a class of static members.
     */
    private SplatDataNode()
    {
        //  Do nothing.
    }

    /**
     * Returns whether a DataNode can be opened by SPLAT.
     *
     * @param node the node to test
     * @return true if the node is suitable
     */
    public static boolean isChoosable( DataNode node )
    {

        // SPLAT TEXT files (including line identifiers, which are not
        // distinguished), are assumed to be PlainDataNodes. Real test is
        // success or failure to read.
        if ( node instanceof PlainDataNode ) {
            return true;
        }

        // Tables of all flavours.
        if ( node.hasDataObject( DataType.TABLE ) ) {
            return true;
        }
        if ( node instanceof VOTableDataNode ||
             node instanceof VOTableTableDataNode ) {
            return true;
        }

        // FITS files. These usually contain spectral images in their
        // primary HDUs, but may also open a table in an extension, if
        // the image is dummy.
        if ( node instanceof FITSFileDataNode ) {
            return true;
        }

        // FITS streams. As above bt usually from a compressed source,
        // so must be a table.
        if ( node instanceof FITSStreamDataNode ) {
            return true;
        }

        // Array-based data that can be wrapped as NDXs.
        if ( node.hasDataObject( DataType.NDX ) ) {
            return true;
        }

        // Zipped files. Just try these as tables.
        if ( node instanceof ZipFileDataNode ) {
            return true;
        }

        return false;
    }

    /**
     * Turns a DataNode object into a {@link SpecData} instance.
     *
     * @param  node  the data node
     * @return SpecData made from <tt>node</tt>
     * @throws IOException if there's trouble
     */
    public static SpecData makeSpecData( DataNode node )
        throws IOException, SplatException
    {
        if ( isChoosable( node ) ) {

            // StarTable.
            if ( node.hasDataObject( DataType.TABLE ) ) {
                return makeSpecDataFromTable( node );
            }

            // VOTable
            if ( node instanceof VOTableDataNode ||
                 node instanceof VOTableTableDataNode ) {
                return makeSpecDataFromVOTable( node );
            }

            // FITS file (local).
            if ( node instanceof FITSFileDataNode ) {
                // Look for object that created this node. We're after the
                // file name, as the node path may be symbolic or rooted
                // differently.
                CreationState creator = node.getCreator();
                Object obj = creator.getObject();
                File file = null;
                if ( obj instanceof FileDataSource ) {
                    file = ((FileDataSource) obj).getFile();
                }
                else if ( obj instanceof File ) {
                    file = (File) obj;
                }
                if ( file != null ) {
                    return specFactory.get( file.getPath(),
                                            SpecDataFactory.FITS );
                }
            }

            // FITS stream from a file.
            if ( node instanceof FITSStreamDataNode ) {
                // Look for object that created this node. We're after the
                // file name, as the node path may be symbolic or rooted
                // differently.
                CreationState creator = node.getCreator();
                Object obj = creator.getObject();
                File file = null;
                if ( obj instanceof FileDataSource ) {
                    file = ((FileDataSource) obj).getFile();
                }
                else if ( obj instanceof File ) {
                    file = (File) obj;
                }
                if ( file != null ) {
                    return specFactory.get( file.getPath(),
                                            SpecDataFactory.TABLE );
                }
            }

            // NDX.
            if ( node.hasDataObject( DataType.NDX ) ) {
                return makeSpecDataFromNdx( node );
            }

            // Text file? Suck and see. Note backing must be a file.
            if ( node instanceof PlainDataNode ) {
                // Look for object that created this node.
                Object datsrc = node.getCreator().getObject();
                File file = null;
                if ( datsrc instanceof FileDataSource ) {
                    file = ((FileDataSource) datsrc).getFile();
                }
                else if ( datsrc instanceof File ) {
                    file = (File) datsrc;
                }
                if ( file != null ) {
                    return specFactory.get( file.getPath(),
                                            SpecDataFactory.TEXT );
                }
            }

            // Zipped file. Only supported as tables, so try that.
            if ( node instanceof ZipFileDataNode ) {
                return makeSpecDataFromTable( node );
            }
        }
        else {
            throw new IllegalArgumentException
                ( node + " does not contain a known spectrum type" );
        }
        return null;
    }

    /**
     * Make a {@link SpecData} object from a {@link DataNode} that contains
     * a {@link StarTable}.
     *
     * @param node a {@link DataNode} that is guaranteed to contain a
     *             {@link StarTable}.
     */
    protected static SpecData makeSpecDataFromTable( DataNode node )
        throws IOException, SplatException
    {
        StarTable table = null;
        try {
            table = (StarTable) node.getDataObject( DataType.TABLE );
        }
        catch ( DataObjectException e ) {
            return null;
        }

        // Generate suitable short and full names for this table.
        String fullName = NodeUtil.getNodePath( node );

        // Use the table name, if available (they rarely are), otherwise the
        // last element of the full name.
        String shortName = table.getName();
        if ( shortName == null || shortName.equals( "" ) ) {
            File file = new File( fullName );
            shortName = file.getName();
        }
        return specFactory.get( table, shortName, fullName );
    }

    /**
     * Make a {@link SpecData} object from a {@link DataNode} that contains
     * a {@link VOTableDataNode} or {@link VOTableTableDataNode}.
     *
     * @param node a {@link DataNode}
     */
    protected static SpecData makeSpecDataFromVOTable( DataNode node )
        throws IOException, SplatException
    {
        SpecData specData = null;
        StarTable starTable = null;

        try {
            DataSource datsrc =
                (DataSource) node.getDataObject( DataType.DATA_SOURCE );
            starTable = new VOTableBuilder().makeStarTable( datsrc, true,
                                                            storagePolicy );

            // Generate suitable short and full names for this table.
            String fullName = NodeUtil.getNodePath( node );
            String shortName = fullName;
            if ( starTable != null ) {
                specData = specFactory.get( starTable, shortName, fullName );
            }
            else {
                throw new SplatException( "Failed to open VOTable" );
            }
        }
        catch (Exception e) {
            throw new SplatException( "Failed to open VOTable", e );
        }

        return specData;
    }

    /**
     * Make a {@link SpecData} object from an {@link DataNode}
     * that contains an {@link Ndx}.
     *
     * @param node a {@link DataNode} that is guaranteed to have an NDX.
     */
    protected static SpecData makeSpecDataFromNdx( DataNode node )
        throws IOException, SplatException
    {
        Ndx ndx = null;
        try {
            ndx = (Ndx) node.getDataObject( DataType.NDX );
        }
        catch ( DataObjectException e ) {
            return null;
        }

        // Generate suitable short and full names for this Ndx.
        String fullName = NodeUtil.getNodePath( node );

        // Use the title as short name, unless it's not available, in which
        // case we use the tail of the full name.
        String shortName = ndx.hasTitle() ? ndx.getTitle() : "";
        if ( shortName == null || shortName.equals( "" ) ) {
            File file = new File( fullName );
            shortName = file.getName();
        }
        return specFactory.get( ndx, shortName, fullName );
    }

    /**
     * Does some customisation of a DataNodeFactory. Its builder list is
     * modified so it doesn't investigate any unnecessary nodes.
     */
    public static void customiseFactory( DataNodeFactory fact )
    {
        /* Make sure we have the list of DataNode classes we do not wish
         * to see. */
        if ( shunnedClassList == null ) {
            String[] shunned = new String[] {
                "uk.ac.starlink.datanode.nodes.HistoryDataNode"
            };
            List classes = new ArrayList();
            for ( int i = 0; i < shunned.length; i++ ) {
                try {
                    Class clazz =
                        Class.forName( shunned[ i ], true,
                              Thread.currentThread().getContextClassLoader() );
                    classes.add( clazz );
                }
                catch ( ClassNotFoundException e ) {
                    // not known, so won't be used in any case
                }
            }
            shunnedClassList = classes;

            deprecatedClassList =
                Arrays.asList( new Class[] {
                                   XMLDataNode.class,
                                   FileDataNode.class
                               } );
        }

        /* Remove each of the shunned classes from the factory. */
        for ( Iterator it = shunnedClassList.iterator(); it.hasNext(); ) {
            fact.removeNodeClass( (Class) it.next() );
        }

        /* Do some additional reordering. */
        for ( Iterator it = deprecatedClassList.iterator(); it.hasNext(); ) {
            fact.setDeprecatedClass( (Class) it.next() );
        }
    }
}
