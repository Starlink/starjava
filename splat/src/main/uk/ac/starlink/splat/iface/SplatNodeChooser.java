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
import javax.swing.Action;
import javax.swing.Box;
import javax.swing.JButton;

import uk.ac.starlink.datanode.factory.DataNodeFactory;
import uk.ac.starlink.datanode.nodes.DataNode;
import uk.ac.starlink.datanode.tree.TreeNodeChooser;
import uk.ac.starlink.splat.data.SpecData;
import uk.ac.starlink.util.gui.ErrorDialog;
import uk.ac.starlink.splat.data.SplatDataNode;

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
        SplatDataNode.customiseFactory( root.getChildMaker() );
        super.setRoot( root );
    }

    public synchronized DataNodeFactory getNodeMaker()
    {
        if ( nodeFact == null ) {
            nodeFact = new DataNodeFactory();
            SplatDataNode.customiseFactory( nodeFact );
        }
        return nodeFact;
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
                specData = SplatDataNode.makeSpecData( node );
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
}
