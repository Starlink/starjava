package uk.ac.starlink.table.gui;

import java.io.IOException;
import javax.swing.JDialog;
import javax.swing.SwingUtilities;
import uk.ac.starlink.table.StarTable;
import uk.ac.starlink.table.StarTableFactory;

/**
 * Skeleton implementation of a {@link TableLoadDialog} which can load
 * multiple tables.
 * Concrete subclasses need to populate this panel with components forming
 * the specific part of the query dialogue (presumably text fields,
 * combo boxes and so on) and then implement the
 * {@link #getTablesSupplier} method which returns an object capable of
 * trying to load some tables based on the current state of the component.
 * All the issues about threading are taken care of by the implementation
 * of this class.
 *
 * @author   Mark Taylor (Starlink)
 * @since    22 Oct 2009
 */
public abstract class MultiTableLoadDialog extends AbstractTableLoadDialog {

    private MultiLoader loader_;

    /**
     * Constructor.
     *
     * @param  name  dialogue name
     * @param  description  dialogue description
     */
    public MultiTableLoadDialog( String name, String description ) {
        super( name, description );
    }

    /**
     * Concrete subclasses should implement this method to supply a
     * TablesSupplier object which can attempt to load some tables based on
     * the current state (as filled in by the user) of this component.
     * If the state is not suitable for an attempt at loading a table
     * (e.g. some components are filled in in an obviously wrong way)
     * then a runtime exception such as <tt>IllegalStateException</tt>
     * or <tt>IllegalArgumentException</tt>, with a human-readable
     * message, should be thrown.
     *
     * @return  table array supplier corresponding to current state 
     *          of this component
     * @throws  RuntimeException  if validation fails
     */
    protected abstract TablesSupplier getTablesSupplier()
            throws RuntimeException;

    protected void submitLoad( JDialog dialog, StarTableFactory tfact,
                               String format, TableConsumer consumer ) {
        MultiLoader loader =
            new MultiLoader( dialog, getTablesSupplier(), tfact, format,
                             consumer );
        setLoader( loader );
        loader.start();
    }

    protected void cancelLoad() {
        setLoader( null );
    }

    /**
     * Sets the loading thread currently working on behalf of this dialogue.
     * Null may be set to indicate that no loading is currently in progress.
     *
     * @param  loader  loader thread currently loading tables for this
     *         dialogue
     */
    private synchronized void setLoader( MultiLoader loader ) {
        loader_ = loader;
        setBusy( loader_ != null );
    }

    /**
     * Defines an object which can attempt to load a set of tables.
     */
    public interface TablesSupplier {

        /**
         * Returns a string representation (location maybe) of the tables
         * which this object will load.
         *
         * @return   common ID of tables
         */
        String getTablesID();

        /**
         * Attempts to load some tables.
         * This synchronous method is not to be called o nthe event
         * dispatch thread.
         *
         * @param  tfact   factory used for loading if necessary
         * @param  format   format string
         */
        StarTable[] getTables( StarTableFactory tfact, String format )
               throws IOException;
    }

    /**
     * Thread class which performs table loading and then messages the
     * result to the GUI.
     */
    private class MultiLoader extends Thread {
        final JDialog dialog_;
        final TablesSupplier supplier_;
        final StarTableFactory tfact_;
        final String format_;
        final TableConsumer consumer_;

        /**
         * Constructor.
         *
         * @param  dialog  dialogue window
         * @param  supplier   object which can load tables
         * @param  tfact   table factory
         * @param  format  format string; may be ignored
         */
        MultiLoader( JDialog dialog, TablesSupplier supplier,
                     StarTableFactory tfact, String format,
                     TableConsumer consumer ) {
            dialog_ = dialog;
            supplier_ = supplier;
            tfact_ = tfact;
            format_ = format;
            consumer_ = consumer;
        }

        /**
         * Indicates whether this thread is currently working on behalf
         * of the dialogue.  If not, it must discard any results and not
         * affect the GUI.
         *
         * @return  true  iff this thread is doing work for the dialogue
         */
        private boolean isActive() {
            return MultiLoader.this == loader_;
        }

        public void run() {
            final String id = supplier_.getTablesID();
            final boolean[] started = new boolean[ 1 ];
            SwingUtilities.invokeLater( new Runnable() {
                public void run() {
                    if ( isActive() ) {
                        consumer_.loadStarted( id );
                        started[ 0 ] = true;
                    }
                }
            } );
            Throwable error = null;
            StarTable[] tables = new StarTable[ 0 ];
            if ( isActive() ) {
                try {
                    tables = supplier_.getTables( tfact_, format_ );
                }
                catch ( Throwable e ) {
                    error = e;
                }
            }
            final StarTable[] tables1 = tables;
            final Throwable error1 = error;
            SwingUtilities.invokeLater( new Runnable() {
                public void run() {
                    if ( isActive() ) {
                        setBusy( false );
                    }
                    if ( started[ 0 ] ) {
                        if ( isActive() && tables1.length > 0 ) {
                            consumer_.loadSucceeded( tables1[ 0 ] );
                            for ( int i = 1; i < tables1.length; i++ ) {
                                consumer_.loadStarted( id + "-" + ( i + 1 ) );
                                consumer_.loadSucceeded( tables1[ i ] );
                            }
                        }
                        else {
                            consumer_.loadFailed( error1 );
                        }
                    }
                    if ( isActive() ) {
                        setLoader( null );
                        if ( tables1.length > 0 ) {
                            dialog_.dispose();
                        }
                    }
                }
            } );
        }
    }
}
