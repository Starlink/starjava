package uk.ac.starlink.topcat.plot2;

import java.io.IOException;
import javax.swing.BoundedRangeModel;
import uk.ac.starlink.table.RowAccess;
import uk.ac.starlink.table.RowSequence;
import uk.ac.starlink.table.StarTable;
import uk.ac.starlink.table.WrapperRowSequence;
import uk.ac.starlink.ttools.plot2.data.DataStoreFactory;
import uk.ac.starlink.ttools.plot2.data.WrapperDataStoreFactory;

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

    protected RowSequence createRowSequence( StarTable table )
            throws IOException {
        final Progresser progresser =
            new Progresser( progModel_, table.getRowCount() );
        progresser.init();
        final RowSequence baseSeq = table.getRowSequence();
        return new WrapperRowSequence( baseSeq ) {
            @Override
            public boolean next() throws IOException {
                if ( baseSeq.next() ) {
                    progresser.increment();
                    return true;
                }
                else {
                    return false;
                }
            }
            @Override
            public void close() throws IOException {
                progresser.reset();
                baseSeq.close();
            }
        };
    }

    protected RowAccess createRowAccess( StarTable table ) throws IOException {
        return table.getRowAccess();
    }
}
