package uk.ac.starlink.topcat.plot2;

import java.io.IOException;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.LongSupplier;
import javax.swing.BoundedRangeModel;
import javax.swing.SwingUtilities;
import javax.swing.Timer;
import uk.ac.starlink.table.ProgressRowSplittable;
import uk.ac.starlink.table.RowAccess;
import uk.ac.starlink.table.RowSequence;
import uk.ac.starlink.table.RowSplittable;
import uk.ac.starlink.table.StarTable;
import uk.ac.starlink.table.WrapperRowSequence;
import uk.ac.starlink.ttools.plot2.data.DataStoreFactory;
import uk.ac.starlink.ttools.plot2.data.WrapperDataStoreFactory;

/**
 * DataStoreFactory wrapper which messages a progress bar model as row
 * data is read from the tables to fill the store.
 * 
 * @author   Mark Taylor
 * @since    8 Nov 2013
 */
public class ProgressDataStoreFactory extends WrapperDataStoreFactory {

    private final BoundedRangeModel progModel_;
    private static final int INTERVAL = 250;

    /**
     * Constructor.
     *
     * @param   baseFact   factory to which most behaviour is delegated
     * @param   progModel  progress bar data model to be messaged
     */
    public ProgressDataStoreFactory( DataStoreFactory baseFact,
                                     BoundedRangeModel progModel ) {
        super( baseFact );
        progModel_ = progModel;
    }

    protected RowSequence createRowSequence( StarTable table )
            throws IOException {
        long nrow = table.getRowCount();
        setZero( nrow );
        return new WrapperRowSequence( table.getRowSequence() ) {
            long irow;
            Timer timer = startProgressTimer( () -> irow );
            public boolean next() throws IOException {
                irow++;
                return super.next();
            }
            public void close() throws IOException {
                timer.stop();
                setZero( nrow );
                super.close();
            }
        };
    }

    protected RowSplittable createRowSplittable( StarTable table )
            throws IOException {
        long nrow = table.getRowCount();
        setZero( nrow );
        ProgressRowSplittable.Target target =
                new ProgressRowSplittable.Target() {
            AtomicLong irow = new AtomicLong();
            Timer timer = startProgressTimer( () -> irow.get() );
            public void updateCount( long count ) throws IOException {
                irow.set( count );
            }
            public void done( long count ) {
                timer.stop();
                setZero( nrow );
            }
        };
        return new ProgressRowSplittable( table.getRowSplittable(), target );
    }

    protected RowAccess createRowAccess( StarTable table ) throws IOException {
        return table.getRowAccess();
    }

    /**
     * Returns a running timer that will update the progress bar periodically.
     *
     * @param  counter  yields the current row count
     * @return  started timer
     */
    private Timer startProgressTimer( final LongSupplier counter ) {
        Timer timer = new Timer( INTERVAL, evt -> {
            progModel_.setValue( (int) counter.getAsLong() );
        } );
        timer.setInitialDelay( 0 );
        timer.setCoalesce( true );
        timer.start();
        return timer;
    }

    /**
     * Resets the progress bar to its minimum.
     */
    private void setZero( long nrow ) {
        SwingUtilities.invokeLater( () -> {
            progModel_.setMinimum( 0 );
            progModel_.setValue( 0 );
            progModel_.setMaximum( (int) nrow );
        } );
    }
}
