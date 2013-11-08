package uk.ac.starlink.topcat.plot2;

import java.io.IOException;
import javax.swing.BoundedRangeModel;
import javax.swing.SwingUtilities;
import uk.ac.starlink.table.RowSequence;
import uk.ac.starlink.table.WrapperRowSequence;
import uk.ac.starlink.ttools.plot2.data.DataStoreFactory;

/**
 * DataStoreFactory wrapper which messages a progress bar model as row
 * data is read from the tables to fill the store.
 * The assumption is that the <code>readDataStore</code> method will
 * only ever be running from one thread at a time; if not, the progress
 * bar updates will get messy.
 * 
 * @author   Mark Taylor
 * @since    8 Nov 2013
 */
public class ProgressDataStoreFactory extends WrapperDataStoreFactory {

    private final BoundedRangeModel progModel_;

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

    protected RowSequence createWrapperRowSequence( RowSequence baseSeq,
                                                    long nrow ) {
        return new ProgressRowSequence( baseSeq, nrow );
    }

    /**
     * Sets the current state of the progress bar.
     * This method may be called from any thread.
     *
     * @param  irow  row index (progress model value)
     */
    private void setProgress( final int irow ) {
        SwingUtilities.invokeLater( new Runnable() {
            public void run() {
                progModel_.setValue( irow );
            }
        } );
    }

    /**
     * RowSequence implementation that messages this object's progress bar
     * as rows are read.
     */
    private class ProgressRowSequence extends WrapperRowSequence {

        final int step;
        long irow = -1;

        /**
         * Constructor.
         *
         * @param  baseSeq  row sequence to which most behaviour is delegated
         * @param  lnrow   number of rows in sequence
         */
        ProgressRowSequence( RowSequence baseSeq, long lnrow ) {
            super( baseSeq );
            final int nrow = (int) Math.min( lnrow, Integer.MAX_VALUE );
            step = Math.max( nrow / 200, 1000 );
            SwingUtilities.invokeLater( new Runnable() {
                public void run() {
                    progModel_.setMinimum( 0 );
                    progModel_.setMaximum( nrow );
                }
            } );
        }

        @Override
        public boolean next() throws IOException {
            if ( ++irow % step == 0 ) {
                setProgress( (int) irow );
            }
            return super.next();
        }

        @Override
        public void close() throws IOException {
            setProgress( 0 );
            super.close();
        }
    }
}
