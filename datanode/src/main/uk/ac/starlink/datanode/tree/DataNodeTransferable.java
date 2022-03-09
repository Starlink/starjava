package uk.ac.starlink.datanode.tree;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.net.URL;
import java.net.MalformedURLException;
import javax.xml.transform.Source;
import nom.tam.util.ArrayDataOutput;
import nom.tam.util.BufferedDataOutputStream;
import uk.ac.starlink.oldfits.FitsNdxHandler;
import uk.ac.starlink.table.StarTable;
import uk.ac.starlink.util.DataSource;
import uk.ac.starlink.votable.DataFormat;
import uk.ac.starlink.votable.VOTableWriter;
import uk.ac.starlink.datanode.nodes.DataNode;
import uk.ac.starlink.datanode.nodes.DataObjectException;
import uk.ac.starlink.datanode.nodes.DataType;
import uk.ac.starlink.datanode.nodes.NodeUtil;
import uk.ac.starlink.hdx.HdxException;
import uk.ac.starlink.ndx.Ndx;
import uk.ac.starlink.util.SourceReader;

/**
 * Transferable object used for transfer of {@link DataNode}s.
 */
public class DataNodeTransferable extends BasicTransferable {

    private DataNode node;

    public static final String VOTABLE_MIMETYPE = "application/xml";

    /**
     * Construct a new transferable from an existing data node.
     *
     * @param  node  the datanode
     */
    public DataNodeTransferable( DataNode node ) {
        this.node = node;

        /* If the node can supply a StarTable, add a suitable data source. */
        if ( node.hasDataObject( DataType.TABLE ) ) {
            try {
                StarTable table = 
                    (StarTable) node.getDataObject( DataType.TABLE );
                addDataSource( new VOTableDataSource( table ), 
                               VOTABLE_MIMETYPE );
            }
            catch ( DataObjectException e ) {
                e.printStackTrace();
            }
        }

        /* If the node can supply an NDX, add a suitable data source. */
        if ( node.hasDataObject( DataType.NDX ) ) {
            try {
                Ndx ndx = (Ndx) node.getDataObject( DataType.NDX );

                /* If it's persistent we can serialize it to XML. */
                if ( ndx.isPersistent() ) {
                    addDataSource( new XmlNdxDataSource( ndx ),
                                   "application/xml" );
                }

                /* In any case, we can squirt the whole lot as a FITS file
                 * with data inline. */
                if ( NodeUtil.hasTAMFITS() ) {
                    addDataSource( new FitsNdxDataSource( ndx ),
                                   "application/fits" );
                }
            }
            catch ( DataObjectException e ) {
                e.printStackTrace();
            }
        }

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

    /**
     * DataSource which provides a VOTable stream representing a table.
     */
    private static class VOTableDataSource extends DataSource {

        final StarTable table_;

        VOTableDataSource( StarTable table ) {
            table_ = table;
        }

        public URL getURL() {
            return table_.getURL();
        }

        public String getName() {
            String name = table_.getName();
            return name == null ? "Table" : name;
        }

        public InputStream getRawInputStream() throws IOException {
            final PipedOutputStream ostrm = new PipedOutputStream();
            PipedInputStream istrm = new PipedInputStream( ostrm );
            new Thread() {
                public void run() {
                    try {
                        VOTableWriter vosquirt = new VOTableWriter();
                        vosquirt.setDataFormat( DataFormat.BINARY );
                        vosquirt.setInline( true );
                        vosquirt.writeStarTable( table_, ostrm, null );
                    }
                    catch ( IOException e ) {
                        // May well catch an IOException if the reader
                        // stops reading
                    }
                    finally {
                        try {
                            ostrm.close();
                        }
                        catch ( IOException e ) {
                            // no action
                        }
                    }
                }
            }.start();
            return istrm;
        }
    }

    /**
     * DataSource which provides an XML stream representing an NDX.
     */
    private static class XmlNdxDataSource extends DataSource {

        final Ndx ndx_;

        XmlNdxDataSource( Ndx ndx ) {
            ndx_ = ndx;
        }

        public InputStream getRawInputStream() throws IOException {
            try {
                Source xsrc = ndx_.getHdxFacade().getSource( null );
                return new SourceReader().getXMLStream( xsrc );
            }
            catch ( HdxException e ) {
                throw (IOException) new IOException( e.getMessage() )
                                   .initCause( e );
            }
        }

        public String getName() {
            return ndx_.hasTitle() ? ndx_.getTitle() : "NDX";
        }

        public URL getURL() {
            return null;
        }
    }

    /**
     * DataSource which provides a FITS stream representing an NDX.
     */
    private static class FitsNdxDataSource extends DataSource {
  
        final Ndx ndx_;
  
        FitsNdxDataSource( Ndx ndx ) {
            ndx_ = ndx;
        }
  
        public InputStream getRawInputStream() throws IOException {
            final PipedOutputStream ostrm = new PipedOutputStream();
            InputStream istrm = new PipedInputStream( ostrm );
            new Thread() {
                public void run() {
                    try {
                        URL dummyUrl = new URL( "file://localhost/dummy" );
                        ArrayDataOutput strm =
                            new BufferedDataOutputStream( ostrm );
                        FitsNdxHandler.getInstance()
                                      .outputNdx( strm, dummyUrl, ndx_ );
                        strm.close();
                    }
                    catch ( MalformedURLException e ) {
                        throw new AssertionError( e );
                    }
                    catch ( IOException e ) {
                        // May well catch an exception here if not all
                        // the output is consumed
                    }
                    finally {
                        try {
                            ostrm.close();
                        }
                        catch ( IOException e ) {
                        }
                    }
                }
            }.start();
            return istrm;
        }
  
        public String getName() {
            return ndx_.hasTitle() ? ndx_.getTitle() : "NDX";
        }
  
        public URL getURL() {
            return null;
        }
    }
       
}
