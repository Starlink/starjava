package uk.ac.starlink.treeview;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.net.MalformedURLException;
import uk.ac.starlink.util.DataSource;

/**
 * Transferable object used for transfer of {@link DataNode}s.
 */
public class DataNodeTransferable extends BasicTransferable {

    private DataNode node;

    /**
     * Construct a new transferable from an existing data node.
     *
     * @param  node  the datanode
     */
    public DataNodeTransferable( DataNode node ) {
        this.node = node;

        /* Allow the node to install its own custom flavours if it wants. */
        if ( node instanceof Draggable ) {
            try {
                ((Draggable) node).customiseTransferable( this );
            }
            catch ( IOException e ) {
                e.printStackTrace();
            }
        }

    //  /* Provide an XML view if there is one. */
    //  Source xsrc = getXMLSource( node );
    //  if ( xsrc != null ) {
    //      addXML( xsrc, "application/xml" );
    //  }

        /* Try to identify a URL. */
        URL url = getURL( node );
        if ( url != null ) {
            addURL( url );
        }
    }

    /**
     * Returns the node associated with this transferable.
     *
     * @return  the node
     */
    public DataNode getDataNode() {
        return node;
    }

    /**
     * Utility method to examine a DataNode to find out whether there is
     * a URL which can be applied to it.
     *
     * @param  node  the DataNode
     * @return  a URL which references <tt>node</tt> if there is one,
     *          otherwise <tt>null</tt>
     */
    private static URL getURL( DataNode node ) {
        Object creator = node.getCreator().getObject();
        if ( creator instanceof URL ) {
            return (URL) creator;
        }
        if ( creator instanceof DataSource ) {
            return ((DataSource) creator).getURL();
        }
        if ( creator instanceof File ) {
            try {
                return ((File) creator).toURL();
            }
            catch ( MalformedURLException e ) {
                // never mind
            }
        }
        if ( creator instanceof String ) {
            try {
                return new URL( (String) creator );
            }
            catch ( MalformedURLException e ) {
                // never mind
            }
        }
        return null;
    }
}
