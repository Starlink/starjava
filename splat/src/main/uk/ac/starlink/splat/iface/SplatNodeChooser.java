/*
 * Copyright (C) 2004 Central Laboratory of the Research Councils
 *
 *  History:
 *     25-FEB-2004 (Peter W. Draper):
 *        Original version, based on Mark Taylor's NdxNodeChooser &
 *        TableNodeChooser.
 *     31-JAN-2005 (Peter W. Draper):
 *        Refactored out of Treeview and into SPLAT. Now uses the DataNode
 *        package rather than Treeview.
 */
package uk.ac.starlink.splat.iface;

import java.awt.Component;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import javax.swing.Action;
import javax.swing.Box;
import javax.swing.JButton;

import uk.ac.starlink.datanode.factory.CreationState;
import uk.ac.starlink.datanode.factory.DataNodeFactory;
import uk.ac.starlink.datanode.nodes.DataNode;
import uk.ac.starlink.datanode.nodes.DataObjectException;
import uk.ac.starlink.datanode.nodes.DataType;
import uk.ac.starlink.datanode.nodes.FITSFileDataNode;
import uk.ac.starlink.datanode.nodes.FileDataNode;
import uk.ac.starlink.datanode.nodes.NodeUtil;
import uk.ac.starlink.datanode.nodes.PlainDataNode;
import uk.ac.starlink.datanode.nodes.XMLDataNode;
import uk.ac.starlink.datanode.tree.NdxNodeChooser;
import uk.ac.starlink.datanode.tree.TableNodeChooser;
import uk.ac.starlink.datanode.tree.TreeNodeChooser;
import uk.ac.starlink.ndx.Ndx;
import uk.ac.starlink.splat.data.SpecData;
import uk.ac.starlink.splat.data.SpecDataFactory;
import uk.ac.starlink.splat.util.SplatException;
import uk.ac.starlink.table.StarTable;
import uk.ac.starlink.util.gui.ErrorDialog;
import uk.ac.starlink.util.FileDataSource;

/**
 * TreeNodeChooser subclass designed to return objects that can be constructed
 * into spectra for display by SPLAT. This currently covers Ndx, FITS,
 * TEXT files (these are just assumed to be any type not known) and
 * all Table types that are supported by STIL.
 *
 * @author Mark Taylor (Starlink)
 * @author Peter W. Draper (Starlink)
 */
public class SplatNodeChooser
    extends TreeNodeChooser
{
    private DataNodeFactory nodeFact;

    private static List shunnedClassList;
    private static List deprecatedClassList;

    private static SpecDataFactory specFactory = SpecDataFactory.getInstance();

    /**
     * Constructs a new chooser.  If <tt>root</tt> is <tt>null</tt>,
     * a default root is used (the user's current directory).
     *
     * @param   root  initial root of the tree to browse, or <tt>null</tt>
     */
    public SplatNodeChooser( DataNode root )
    {
        if ( root == null ) {
            File dir = new File( System.getProperty( "user.dir" ) );
            setRoot( getNodeMaker().makeChildNode( null, dir ) );
        }
        else {
            setRoot( root );
        }

        /* Add some buttons for recursive searches. */
        Action findSelectedAction = getSearchSelectedAction();
        Action findAllAction = getSearchAllAction();
        findSelectedAction.putValue( Action.SHORT_DESCRIPTION,
                                     "Locate all spectra under selected node");
        findAllAction.putValue( Action.SHORT_DESCRIPTION,
                                "Locate all spectra in the tree" );
        getButtonPanel().add( new JButton( findSelectedAction ) );
        getButtonPanel().add( Box.createHorizontalStrut( 10 ) );
        getButtonPanel().add( new JButton( findAllAction ) );
    }

    /**
     * Constructs a new chooser with a default root.
     */
    public SplatNodeChooser()
    {
        this( null );
    }

    public void setRoot( DataNode root )
    {
        customiseFactory( root.getChildMaker() );
        super.setRoot( root );
    }

    public synchronized DataNodeFactory getNodeMaker()
    {
        if ( nodeFact == null ) {
            nodeFact = new DataNodeFactory();
            customiseFactory( nodeFact );
        }
        return nodeFact;
    }

    /**
     * Allows selection of any node we can potentially display.
     *
     * @param node the node to test
     * @return true if the node is suitable
     */
    protected boolean isChoosable( DataNode node )
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

        // FITS files. These usually contain spectral images in their
        // primary HDUs, but may also open a table in an extension, if
        // the image is dummy.
        if ( node instanceof FITSFileDataNode ) {
            return true;
        }

        // Array-based data that can be wrapped as NDXs.
        if ( node.hasDataObject( DataType.NDX ) ) {
            return true;
        }
        return false;
    }

    /**
     * Pops up a modal dialog.
     * If an error occurs during access, the user will be informed,
     * and <tt>null</tt> will be returned.
     *
     * @param  parent  the parent component for the dialog
     * @param  buttonText  the text to appear on the 'choose' button
     * @param  title  the title of the dialog window
     * @return  a {@link SpecData} corresponding to the selected DataNode,
     *          or <tt>null</tt> if none was selected or
     */
    public SpecData choose( Component parent, String buttonText, String title )
    {
        SpecData specData = null;
        DataNode node = chooseDataNode( parent, buttonText, title );
        if ( node != null ) {
            try {
                specData = makeSpecData( node );
            }
            catch ( Exception e ) {
                ErrorDialog.showError( parent, "Bad Spectrum", e,
                                       "No spectrum loaded from: " + node );
                specData = null;
            }
        }
        return specData;
    }

    /**
     * Pops up a modal chooser dialog, with default characteristics.
     * If an error occurs the user will be informed, and <tt>null</tt> will be
     * returned.
     *
     * @param  parent  the parent component for the dialog
     * @return  an SpecData corresponding to the selected DataNode,
     *          or <tt>null</tt> if none was selected or there was
     *          an error.
     */
    public SpecData choose( Component parent )
    {
        return choose( parent, "Open", "Data browser" );
    }

    /**
     * Turns a DataNode object into a {@link SpecData} one.
     *
     * @param  node  the data node
     * @return SpecData made from <tt>node</tt>
     * @throws IOException if there's trouble
     */
    public SpecData makeSpecData( DataNode node )
        throws IOException, SplatException
    {
        if ( isChoosable( node ) ) {

            // StarTable.
            if ( node.hasDataObject( DataType.TABLE ) ) {
                return makeSpecDataFromTable( node );
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
        }
        else {
            throw new IllegalArgumentException
                ( node + " does not contain a known spectrum type" );
        }
        return null;
    }

    /**
     * Make a {@link SpecData} object from a {@link TableNodeChooser.Choosable}
     * that contains a {@link StarTable}.
     *
     * @param node a {@link DataNode} that is guaranteed to contain a 
     *             {@link StarTable}.
     */
    protected SpecData makeSpecDataFromTable( DataNode node )
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
     * Make a {@link SpecData} object from an {@link DataNode}
     * that contains an {@link Ndx}.
     *
     * @param node a {@link DataNode} that is guaranteed to have an NDX.
     */
    protected SpecData makeSpecDataFromNdx( DataNode node )
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
    private static void customiseFactory( DataNodeFactory fact )
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
