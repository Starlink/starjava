package uk.ac.starlink.table;

import java.io.IOException;
import java.io.PrintStream;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.atomic.AtomicLong;

/**
 * A WrapperStarTable which behaves the same as its base, except that
 * any RowSequence taken out on it will display an ASCII progress line 
 * on a terminal describing how far through the table it's got.
 * It might decide not to do this if the table is very short.
 *
 * @author   Mark Taylor (Starlink)
 */
public class ProgressLineStarTable extends WrapperStarTable {

    private static final char[] SPINNER = new char[] { '|', '/', '-', '\\', };
    private static final int INTERVAL = 500;
    private static final int INITIAL_WAIT = 500;

    private final PrintStream out_;

    /**
     * Constructs a new ProgressLineStarTable.
     *
     * @param   baseTable  the base table
     * @param   out  stream on which progress will be written - this should
     *          preferably be terminal-like, since it's going to have things 
     *          like carriage-returns ('\r') written to it
     */
    public ProgressLineStarTable( StarTable baseTable, PrintStream out ) {
        super( baseTable );
        out_ = out;
    }

    public RowSequence getRowSequence() throws IOException {
        final ProgressShower ps = createProgressShower();
        return new WrapperRowSequence( baseTable.getRowSequence() ) {

            final Timer timer_;
            boolean started_ = false;
            long irow_ = 0L;

            /* Constructor */ {
                timer_ = new Timer( "progress", true );
                timer_.schedule( new TimerTask() {
                    long ir = -1;
                    public void run() {
                        if ( irow_ > ir ) {
                            out_.print( ps.getProgressLine( ir = irow_ ) );
                            started_ = true;
                        }
                    }
                }, INITIAL_WAIT, INTERVAL );
            }

            public boolean next() throws IOException {
                if ( super.next() ) {
                    irow_++;
                    return true;
                }
                else {
                    return false;
                }
            }

            public void close() throws IOException {
                timer_.cancel();
                if ( started_ ) {
                    out_.println( ps.getFinishedLine( irow_ ) );
                }
                super.close();
            }
        };
    }

    public RowSplittable getRowSplittable() throws IOException {
        final ProgressShower ps = createProgressShower();
        ProgressRowSplittable.Target target =
                new ProgressRowSplittable.Target() {
            final Timer timer_;
            boolean started_;
            final AtomicLong count_ = new AtomicLong();

            /* Constructor. */ {
                timer_ = new Timer( "progress", true );
                timer_.schedule( new TimerTask() {
                    long ir = -1;
                    public void run() {
                        long c = count_.get();
                        if ( c > ir ) {
                            out_.print( ps.getProgressLine( ir = c ) );
                            started_ = true;
                        }
                    }
                }, INITIAL_WAIT, INTERVAL );
            }
  
            public void updateCount( long count ) {
                count_.set( count );
            }

            public void done( long count ) {
                timer_.cancel();
                if ( started_ ) {
                    out_.print( ps.getProgressLine( count ) );
                }
            }
        };
        return new ProgressRowSplittable( baseTable.getRowSplittable(),
                                          target );
    }

    /**
     * Returns a ProgressShower for use with this table.
     *
     * @return  new progress shower
     */
    private ProgressShower createProgressShower() {
        final long nrow = getRowCount();
        return nrow > 0 
             ? new DeterminateProgressShower( nrow )
             : new IndeterminateProgressShower();
    }

    /**
     * Abstract class subclassed to determine how the progress line is
     * represented.
     */
    private static abstract class ProgressShower {
        abstract String getProgressLine( long rowCount );
        abstract String getFinishedLine( long rowCount );
    }

    /**
     * ProgressShower that works when you don't know the total number of rows.
     */
    private static class IndeterminateProgressShower extends ProgressShower {
        int progCount;

        String getProgressLine( long irow ) {
            StringBuffer buf = new StringBuffer()
               .append( '\r' )
               .append( SPINNER[ progCount ] )
               .append( ' ' )
               .append( irow )
               .append( '\r' );
            progCount = ( progCount + 1 ) % SPINNER.length;
            return buf.toString();
        }

        String getFinishedLine( long irow ) {
            return new StringBuffer()
               .append( '\r' )
               .append( ' ' )
               .append( ' ' )
               .append( irow )
               .append( ' ' )
               .append( "(done)" )
               .append( '\r' )
               .toString();
        }
    }

    /**
     * ProgressShower that works when you know the total number of rows.
     */
    private static class DeterminateProgressShower extends ProgressShower {
        int progCount = 0;
        final long nRow;
        final String nRowString;
        final int nDigit;

        DeterminateProgressShower( long nRow ) {
            this.nRow = nRow;
            nRowString = Long.toString( nRow );
            nDigit = nRowString.length();
        }

        String getProgressLine( long irow ) {
            StringBuffer buf = new StringBuffer()
               .append( '\r' )
               .append( ' ' )
               .append( SPINNER[ progCount ] )
               .append( ' ' );
            progCount = ( progCount + 1 ) % SPINNER.length;
            String rowCountString = Long.toString( irow );
            int pad = nDigit - rowCountString.length();
            for ( int i = 0; i < pad; i++ ) {
                buf.append( ' ' );
            }
            buf.append( rowCountString )
               .append( '/' )
               .append( nRowString )
               .append( ' ' )
               .append( '|' );

            int nLeft = 78 - buf.length();
            int nDone = (int) ( ( irow * nLeft ) / nRow );
            for ( int i = 0; i < nLeft; i++ ) {
                buf.append( i < nDone ? '+' : ' ' );
            }
            buf.append( '|' )
               .append( '\r' );
            return buf.toString();
        }

        String getFinishedLine( long irow ) {
            StringBuffer buf = new StringBuffer( getProgressLine( irow ) );
            buf.setCharAt( 2, ' ' );
            return buf.toString();
        }

    }
}
