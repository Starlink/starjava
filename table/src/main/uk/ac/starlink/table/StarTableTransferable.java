package uk.ac.starlink.table;

import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.util.List;
import java.util.ArrayList;

/**
 * Implements Transferable based on a StarTable.
 * It can serialise itself in one of the StarTable output formats.
 * Currently, VOTable with BINARY inline data is used
 * (the associated MIME type is "application/x-votable+xml").
 *
 * @author   Mark Taylor (Starlink)
 */
class StarTableTransferable implements Transferable {

    private final List flavorList = new ArrayList();
    private final StarTable table;
    private final StarTableOutput outputter;

    /**
     * Constructs a new Transferable which can transfer a given StarTable.
     *
     * @param  table  table to transfer
     */
    public StarTableTransferable( StarTableOutput outputter, StarTable table ) {
        this.outputter = outputter;
        this.table = table;
        DataFlavor flavor =
            new DataFlavor( outputter.getTransferWriter().getMimeType(),
                            "StarTable" );
        flavorList.add( flavor );
    }

    public DataFlavor[] getTransferDataFlavors() {
        return (DataFlavor[]) flavorList.toArray( new DataFlavor[ 0 ] );
    }

    public boolean isDataFlavorSupported( DataFlavor flavor ) {
        return flavorList.contains( flavor );
    }

    public Object getTransferData( DataFlavor flavor )
            throws UnsupportedFlavorException, IOException {
        if ( ! isDataFlavorSupported( flavor ) ) {
            throw new UnsupportedFlavorException( flavor );
        }

        /* We need to return an input stream from which the serialized
         * table can be read. */
        assert InputStream.class
              .isAssignableFrom( flavor.getRepresentationClass() );

        /* First arrange to serialize the table down an output stream 
         * in a separate thread. */
        final PipedOutputStream ostrm = new PipedOutputStream();
        new Thread() {
            public void run() {
                try {
                    outputter.getTransferWriter()
                             .writeStarTable( table, ostrm );
                }
                catch ( IOException e ) {
                    // may well catch an exception if the reader stops reading
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

        /* Now return an input stream which reads from this.
         * For reasons I don't understand the Java Drag And Drop Specification
         * appears to say the returned input stream has to be of a class which 
         * has a constructor which takes a single arg of type InputStream,
         * so we wrap it in a FilterInputStream here.  Seems to do the
         * trick, anyway. */
        InputStream istrm = new PipedInputStream( ostrm );
        return new FilterInputStream( istrm ) {};
    }

}
