package uk.ac.starlink.table.gui;

import java.io.IOException;
import javax.swing.JProgressBar;
import javax.swing.SwingUtilities;
import uk.ac.starlink.table.RowSequence;
import uk.ac.starlink.table.StarTable;
import uk.ac.starlink.table.WrapperRowSequence;
import uk.ac.starlink.table.WrapperStarTable;

/**
 * A WrapperStarTable which behaves the same as its base, except that 
 * UI intervention is permitted on any RowSequence which is taken out
 * on it.  For one thing, the RowSequence will update a supplied
 * <tt>JProgressBar</tt> component as it iterates, and for another thing
 * {@link java.lang.Thread#interrupt}ing the thread in which the RowSequence
 * is being iterated over will cause the <tt>next</tt> method 
 * to throw an <tt>IOException</tt>.
 * Said <tt>RowSequence</tt>s may be used from any thread, that is 
 * they are not restricted to use from the AWT event dispatcher thread.
 *
 * @author   Mark Taylor (Starlink)
 */
public class ProgressBarStarTable extends WrapperStarTable {

    private JProgressBar progBar;

    public ProgressBarStarTable( StarTable baseTable, JProgressBar progBar ) {
        super( baseTable );
        this.progBar = progBar;
        setZero();
    }

    public RowSequence getRowSequence() throws IOException {
        long nrow = getRowCount();
        final long every = nrow > 0 ? nrow / 200L : 256; 
        return new WrapperRowSequence( baseTable.getRowSequence() ) {
            long counter;
            Runnable updater = new Runnable() {
                public void run() {
                    progBar.setValue( (int) getRowIndex() );
                }
            };
            public void next() throws IOException {
                if ( --counter < 0 ) {
                    SwingUtilities.invokeLater( updater );
                    counter = every;
                }
                super.next();
                boolean stop = Thread.interrupted();
                if ( stop || ! hasNext() ) {
                    setZero();
                }
                if ( stop ) {
                    throw new IOException( "Operation interrupted" );
                }
            }
        };
    }

    private void setZero() {
        SwingUtilities.invokeLater( new Runnable() {
            public void run() {
                progBar.setMinimum( 0 );
                progBar.setValue( 0 );
                long nrow = getRowCount();
                boolean determinate = nrow > 0 && nrow < Integer.MAX_VALUE;
                progBar.setIndeterminate( ! determinate );
                if ( determinate ) {
                    progBar.setMaximum( (int) nrow );
                }
            }
        } );
    }
}
