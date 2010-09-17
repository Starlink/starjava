package uk.ac.starlink.table.gui;

import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import javax.swing.JComponent;
import javax.swing.TransferHandler;
import uk.ac.starlink.table.StarTable;
import uk.ac.starlink.table.StarTableFactory;

/**
 * TransferHandler which will load a table if it is dragged onto its
 * owner component.
 *
 * <p>Concrete implementations of this abstract class must implement the
 * {@link #getLoadClient} method to determine how loaded tables will be
 * consumed.
 *
 * @author  Mark Taylor
 * @since   16 Sep 2010
 */
public abstract class TableLoadTransferHandler extends TransferHandler {

    private final StarTableFactory tfact_;

    /**
     * Constructor.
     *
     * @param  tfact  factory to handle transferred-in tables
     */
    public TableLoadTransferHandler( StarTableFactory tfact ) {
        tfact_ = tfact;
    }

    /**
     * Returns a GUI consumer for any tables loaded by this panel.
     * It will be called once for each load sequence; the returned object
     * may or may not be the same one each time.
     *
     * @return  load client ready to accept tables
     */
    protected abstract TableLoadClient getLoadClient();

    public boolean canImport( JComponent comp, DataFlavor[] flavors ) {
        return tfact_.canImport( flavors );
    }

    public boolean importData( JComponent comp, final Transferable trans ) {

        /* The table has to be loaded in line here, i.e. on the event
         * dispatch thread, since otherwise the weird IPC magic which 
         * provides the inputstream from the Transferable will go away.
         * This is unfortunate, since it might be slow, but
         * I don't *think* there's any alternative. */
        final TableLoadClient client = getLoadClient();
        client.startSequence();
        client.setLabel( "Dropped Table" );
        StarTable table;
        try {
            table = tfact_.makeStarTable( trans );
        }
        catch ( Throwable error ) {
            client.loadFailure( error );
            client.endSequence( false );
            return false;
        }
        assert table != null;
        client.loadSuccess( table );
        client.endSequence( true );
        return true;
    }

    public int getSourceActions( JComponent comp ) {
        return NONE;
    }
}
