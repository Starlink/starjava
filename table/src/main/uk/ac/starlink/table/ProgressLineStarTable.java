package uk.ac.starlink.table;

import java.io.IOException;

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
    private static final int INITIAL_WAIT = 1000;

    /**
     * Constructs a new ProgressLineStarTable.
     *
     * @param   baseTable  the base table
     */
    public ProgressLineStarTable( StarTable baseTable ) {
        super( baseTable );
    }

    public RowSequence getRowSequence() throws IOException {
        final long nrow = getRowCount();
        final ProgressShower ps = nrow > 0 
                  ? (ProgressShower) new DeterminateProgressShower( nrow )
                  : (ProgressShower) new IndeterminateProgressShower();
        return new WrapperRowSequence( baseTable.getRowSequence() ) {

            boolean started = false;
            long alarm = System.currentTimeMillis() + INITIAL_WAIT;
            long irow = 0L;

            public void next() throws IOException {
                super.next();
                irow++;
                if ( System.currentTimeMillis() > alarm ) {
                    alarm = System.currentTimeMillis() + INTERVAL;
                    System.out.print( ps.getProgressLine( irow ) );
                    started = true;
                }
            }

            public boolean hasNext() {
                if ( super.hasNext() ) {
                    return true;
                }
                else {
                    if ( started ) {
                        System.out.println( ps.getFinishedLine( irow ) );
                    }
                    return false;
                }
            }
        };
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
