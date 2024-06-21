package uk.ac.starlink.datanode.tree;

import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.io.File;
import java.io.InputStream;
import java.io.IOException;
import java.net.URL;
import javax.swing.Icon;
import javax.swing.JComponent;
import javax.swing.TransferHandler;
import javax.swing.tree.TreePath;
import uk.ac.starlink.datanode.factory.DataNodeFactory;
import uk.ac.starlink.datanode.nodes.DataNode;
import uk.ac.starlink.datanode.nodes.NoSuchDataException;
import uk.ac.starlink.util.DataSource;
import uk.ac.starlink.util.TemporaryFileDataSource;

/**
 * Handles transferable exports to and from from the DataNodeJTree.
 */
public class DataNodeTransferHandler extends TransferHandler {

    private DataNodeFactory nodeMaker;

    public int getSourceActions( JComponent comp ) {
        return ( comp instanceof DataNodeJTree ) ? COPY : NONE;
    }

    public Icon getVisualRepresentation( Transferable trans ) {
        Icon icon = ( trans instanceof DataNodeTransferable )
            ? ((DataNodeTransferable) trans).getDataNode().getIcon()
            : null;
        return icon;
    }

    protected Transferable createTransferable( JComponent comp ) {
        if ( comp instanceof DataNodeJTree ) {
            DataNodeJTree tree = (DataNodeJTree) comp;
            DataNode node = tree.getDraggedNode();
            if ( node != null ) {
                Transferable trans = new DataNodeTransferable( node );
                return trans.getTransferDataFlavors().length > 0 ? trans
                                                                 : null;
            }
        }
        return null;
    }

    /**
     * Sets the DataNodeFactory which will be used by this handler to
     * perform import of Transferables (convert them into DataNodes).
     * By default this is set to <code>null</code>; unless it is set to
     * a non-null value, no node import will be permitted.
     */
    public void setNodeMaker( DataNodeFactory nodeMaker ) {
        this.nodeMaker = nodeMaker;
    }

    /**
     * If we are configured to import data, return true, on the grounds
     * that the DataNodeFactory should be able to have a go at 
     * turning almost anything into a DataNode.
     * However, only do it if the drop position would not be over a
     * node in the tree, since that might convey a false impression of
     * what's going to happen (the node will get appended to the root).
     */
    public boolean canImport( JComponent comp, DataFlavor[] flavors ) {
        if ( comp instanceof DataNodeJTree && nodeMaker != null ) {
            return true;
        } 
        return false;
    }

    /**
     * Attempts to import a transferable into the tree.
     */
    public boolean importData( JComponent comp, Transferable trans ) {

        /* If we're not configured for import, return directly. */
        if ( ! ( comp instanceof DataNodeJTree ) || nodeMaker == null ) {
            return false;
        }
        DataNodeJTree jtree = (DataNodeJTree) comp;
        DataNode root = (DataNode) jtree.getModel().getRoot();

        /* For each flavour in turn, get the corresponding object from
         * the transferable and feed it to our DataNodeFactory. 
         * Continue until we have made a DataNode. */
        int success = 0;
        DataFlavor[] flavors = trans.getTransferDataFlavors();
        for ( int i = 0; i < flavors.length; i++ ) {

            /* Try to append a new node based on this one to the tree. */
            try {
                DataNode node = makeDataNode( trans, flavors[ i ], root );
                importNode( jtree, node );
                success++;

                /* If we manage to add one node, return success status. */
                return true;
            }
            catch ( NoSuchDataException e ) {
                // DataNode node = nodeMaker.makeErrorDataNode( root, e );
                // importNode( jtree, node );
                // never mind - try the next one.
            }
            catch ( IOException e ) {
                e.printStackTrace();
            }
            catch ( UnsupportedFlavorException e ) {
                e.printStackTrace();  // shouldn't happen
            }
        }
 
        /* No node could be created.  Construct and append a helpful
         * error message. */
        if ( success == 0 ) {
            StringBuffer msg = new StringBuffer();
            msg.append( "Couldn't import transferable " + trans 
                      + "; tried flavours " + flavors.length + ":" );
            for ( int i = 0; i < flavors.length; i++ ) {
                msg.append( "\n   " )
                   .append( flavors[ i ] );
            }
            NoSuchDataException e = new NoSuchDataException( msg.toString() );
            importNode( jtree,
                        nodeMaker.makeErrorDataNode( root, e ) );
            return false;
        }

        /* Return success status. */
        return true;
    }

    /**
     * Pastes the system selection into the tree window.
     * This behaviour is seen in JTextComponent and subclasses
     * (see javax.swing.text.DefaultCaret for the implementation), but not
     * elsewhere in the JFC; however I think it's quite useful, it
     * means you can grab a text string and middle-click it on top
     * of a window to load that URL (or whatever) in.
     * <p>
     * This implementation only works with string-like contents of
     * the selection.  Short strings it feeds to the data node factory
     * as Strings, and long ones it ignores.
     *
     * @param  comp  the component into which to paste
     */
    public void pasteSystemSelection( JComponent comp ) {

        /* Get the system-wide selection. */
        Clipboard selection = comp.getToolkit().getSystemSelection();
        if ( comp instanceof DataNodeJTree && 
             selection != null ) {
            DataNodeJTree jtree = (DataNodeJTree) comp;
            DataNode root = (DataNode) jtree.getModel().getRoot();
            Transferable selTrans = selection.getContents( null );

            /* Only proceed if the selection is a string. */
            DataFlavor stringFlavor = DataFlavor.stringFlavor;
            if ( selTrans != null &&
                 selTrans.isDataFlavorSupported( stringFlavor ) ) {
               try {
                   String stringData = 
                       (String) selTrans.getTransferData( stringFlavor );
                   stringData = stringData.trim();
                   if ( stringData.length() < 160 ) {
                       DataNode node;
                       try {
                           node = nodeMaker.makeDataNode( root, stringData );
                       }
                       catch ( NoSuchDataException e ) {
                           node = nodeMaker.makeErrorDataNode( root, e );
                           node.setLabel( stringData );
                       }
                       importNode( jtree, node );
                   }
                   else {
                       // string is too long - unlikely to be useful to 
                       // the node factory
                   }
               }
               catch ( IOException e ) {  // unlikely
                   e.printStackTrace();
               }
               catch ( UnsupportedFlavorException e ) { // shouldn't happen 
                   e.printStackTrace();
               }
           }
       }
    }

    /**
     * Attempts to make a DataNode from a given Transferable using a
     * particular flavor.
     *
     * @param  trans  the transferable to try to turn into a DataNode
     * @param  flavor  a DataFlavor
     * @param  root  the root DataNode of the tree into which the new
     *               node will be imported
     * @return  a DataNode based on <code>trans</code> using <code>flavor</code>
     * @throws  NoSuchDataException  if it can't be done
     * @throws  UnsupportedFlavorException  if <code>trans</code> does not
     *          have any data of type <code>flavor</code>
     * @throws  IOException  if there is an error reading from the
     *          transferable's input stream
     */
    private DataNode makeDataNode( final Transferable trans, 
                                   final DataFlavor flavor, DataNode root )
            throws NoSuchDataException, IOException, 
                   UnsupportedFlavorException {
        Object obj;

        /* If it's an input stream (other than one which can get
         * deserialised into a java object), turn it into a 
         * DataSource for feeding to the factory. */
        Class clazz = flavor.getRepresentationClass();
        if ( InputStream.class.isAssignableFrom( clazz ) &&
             ! flavor.isFlavorSerializedObjectType() ) {
            InputStream istrm = (InputStream) trans.getTransferData( flavor );
            obj = new TemporaryFileDataSource( istrm, "Transferred" );
        }

        /* If it's a URL, turn it into a DataSource. */
        else if ( clazz.equals( URL.class ) ) {
            URL url = (URL) trans.getTransferData( flavor );
            if ( url.getProtocol().equals( "file" ) ) {
                File file = new File( url.getPath() );
                if ( file.exists() ) {
                    obj = file;
                }
                else {
                    throw new NoSuchDataException( "File URL " + url 
                                                 + " not present" );
                }
            }
            else {
                obj = DataSource.makeDataSource( url );
            }
        }

        /* Otherwise, use the object direct. */
        else {
            obj = trans.getTransferData( flavor );
        }

        /* Feed the object to the factory. */
        return nodeMaker.makeDataNode( root, obj );
    }

    /**
     * Appends a new DataNode to the root of the tree.
     *
     * @param  jtree  the tree component
     * @param  node  the new node to add
     */
    private void importNode( DataNodeJTree jtree, DataNode node ) {
        DataNodeTreeModel treeModel = (DataNodeTreeModel) jtree.getModel();
        DataNode root = (DataNode) treeModel.getRoot();
        treeModel.appendNode( node, root );
        TreePath tpath = new TreePath( treeModel.getPathToRoot( node ) );
        jtree.scrollPathToVisible( tpath );
        jtree.setSelectionPath( tpath );
    }
}
