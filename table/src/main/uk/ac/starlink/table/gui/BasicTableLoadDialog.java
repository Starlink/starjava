package uk.ac.starlink.table.gui;

import java.io.IOException;
import javax.swing.JDialog;
import uk.ac.starlink.table.StarTable;
import uk.ac.starlink.table.StarTableFactory;

/**
 * Skeleton implementation of a {@link TableLoadDialog} which can
 * load a single table.
 * Concrete subclasses need to populate this panel with components forming
 * the specific part of the query dialogue (presumably text fields,
 * combo boxes and so on) and then implement the
 * {@link #getTableSupplier} method which returns an object capable of
 * trying to load a table based on the current state of the component.
 * All the issues about threading are taken care of by the implementation
 * of this class.
 *
 * @author   Mark Taylor (Starlink)
 * @since    23 Dec 2004
 */
public abstract class BasicTableLoadDialog extends AbstractTableLoadDialog {

    private LoadWorker worker_;

    /**
     * Constructor.
     *
     * @param  name  dialogue name (typically used as text of a button)
     * @param  description  dialogue description (typeically used as
     *         tooltip text)
     */
    public BasicTableLoadDialog( String name, String description ) {
        super( name, description );
    }

    /**
     * Concrete subclasses should implement this method to supply a
     * TableSupplier object which can attempt to load a table based on
     * the current state (as filled in by the user) of this component.
     * If the state is not suitable for an attempt at loading a table
     * (e.g. some components are filled in in an obviously wrong way)
     * then a runtime exception such as <tt>IllegalStateException</tt>
     * or <tt>IllegalArgumentException</tt>, with a human-readable 
     * message, should be thrown.
     *
     * @return  table supplier corresponding to current state of this component
     * @throws  RuntimeException  if validation fails
     */
    protected abstract TableSupplier getTableSupplier()
            throws RuntimeException;

    /**
     * Defines an object which can attempt to load a particular table.
     */
    public interface TableSupplier {

        /**
         * Attempts to load a table.
         * This synchronous method is not to be called on the event
         * dispatch thread.
         *
         * @param  factory  factory used for loading if necessary
         * @param  format   format string
         */
        StarTable getTable( StarTableFactory factory, String format )
                throws IOException;

        /**
         * Returns a string representation (location maybe) of the table
         * which this object will load.
         *
         * @return  table id
         */
        String getTableID();
    }

    protected void submitLoad( JDialog dialog, final StarTableFactory tfact,
                               final String format, TableConsumer consumer ) {
        final TableSupplier supplier = getTableSupplier();
        synchronized ( this ) {
            worker_ = new LoadWorker( new DialogConsumer( consumer, dialog ),
                                      supplier.getTableID() ) {
                protected StarTable[] attemptLoads() throws IOException {
                    StarTable table = supplier.getTable( tfact, format );
                    synchronized ( BasicTableLoadDialog.this ) {
                        if ( (LoadWorker) this == worker_ ) {
                            worker_ = null;
                        }
                    }
                    return new StarTable[] { table };
                }
            };
        }
        setBusy( true );
        worker_.invoke();
    }

    protected void cancelLoad() {
        synchronized ( this ) {
            if ( worker_ != null ) {
                worker_.interrupt();
                worker_ = null;
            }
        }
    }

    /**
     * TableConsumer implementation which handles aspects of the load
     * sequence which are specific to this GUI.
     * It passes on table consumption events to an underlying consumer
     * object, but also makes appropriate changes to the state of the GUI.
     */
    private class DialogConsumer implements TableConsumer {
        private final TableConsumer baseConsumer_;
        private final JDialog dia_;
        private String id_;
        
        /**
         * Constructor.
         *
         * @param   baseConsumer  table consumer wrapped by this one
         * @param   dia  dialogue on behalf of which this is operating
         */
        DialogConsumer( TableConsumer baseConsumer, JDialog dia ) {
            baseConsumer_ = baseConsumer;
            dia_ = dia;
        }

        public void loadStarted( String id ) {
            baseConsumer_.loadStarted( id );
        }

        public boolean loadSucceeded( StarTable table ) {
            setBusy( false );
            boolean success = baseConsumer_.loadSucceeded( table );
            assert isActive( dia_ );
            if ( success ) {
                dia_.dispose();
            }
            return success;
        }

        public void loadFailed( Throwable th ) {
            setBusy( false );
            baseConsumer_.loadFailed( th );
        }
    }
}
