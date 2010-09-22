package uk.ac.starlink.table.gui;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import javax.swing.JProgressBar;
import javax.swing.Timer;
import uk.ac.starlink.table.StarTable;
import uk.ac.starlink.table.TableSink;

/**
 * TableSink implementation which messages a progress bar with row updates.
 *
 * @author   Mark Taylor
 * @since    26 Aug 2010
 */
public class ProgressBarTableSink implements TableSink {

    private final JProgressBar progBar_;
    private final Timer timer_;
    private int itab_;
    private long irow_;
    private long nrow_;

    /**
     * Constructs a default sink.
     *
     * @param  progBar  progress bar to message
     */
    public ProgressBarTableSink( JProgressBar progBar ) {
        this( progBar, 100, 1 );
    }

    /**
     * Constructs a sink with given parameters.
     *
     * @param  progBar  progress bar to message
     * @param  updateMillis  time in milliseconds between progress bar updates
     * @param  showTableIndex  whether to prefix row count with table index
     *         in progress bar text:
     *         -1 for never, 0 for always, 1 for only 2nd and subsequent tables
     */
    public ProgressBarTableSink( JProgressBar progBar, int updateMillis,
                                 final int showTableIndex ) {
        progBar_ = progBar;
        timer_ = new Timer( updateMillis, new ActionListener() {
            public void actionPerformed( ActionEvent evt ) {
                progBar_.setValue( (int) Math.min( irow_, Integer.MAX_VALUE ) );
                StringBuffer sbuf = new StringBuffer();
                if ( itab_ > showTableIndex && showTableIndex >= 0 ) {
                    sbuf.append( itab_ )
                        .append( ": " );
                }
                sbuf.append( irow_ );
                if ( nrow_ > 0 ) {
                    sbuf.append( " / " )
                        .append( nrow_ );
                }
                progBar_.setString( sbuf.toString() );
            }
        } );
    }

    public void acceptMetadata( StarTable meta ) {
        itab_++;
        irow_ = 0;
        nrow_ = meta.getRowCount();
        progBar_.setMinimum( 0 );
        progBar_.setValue( 0 );
        if ( nrow_ > 0 ) {
            progBar_.setMaximum( (int) Math.min( nrow_, Integer.MAX_VALUE ) );
        }
        progBar_.setIndeterminate( nrow_ <= 0 );
        timer_.start();
    }

    public void acceptRow( Object[] row ) {
        irow_++;
    }

    public void endRows() {
        timer_.stop();
        progBar_.setIndeterminate( false );
    }

    /**
     * Returns the progress bar used by this object.
     *
     * @return  progress bar
     */
    public JProgressBar getProgressBar() {
        return progBar_;
    }

    /**
     * Ensure all resources are released and no further changes will be
     * made to the progress bar.
     */
    public void dispose() {
        timer_.stop();
        progBar_.setString( "" );
        progBar_.setValue( 0 );
        progBar_.setIndeterminate( false );
    }
}
