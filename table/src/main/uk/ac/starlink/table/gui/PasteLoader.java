package uk.ac.starlink.table.gui;

import java.awt.Component;
import java.awt.Toolkit;
import java.io.IOException;
import uk.ac.starlink.table.StarTable;
import uk.ac.starlink.table.StarTableFactory;
import uk.ac.starlink.util.gui.StringPaster;

/**
 * MouseListener which will load a table when a string is pasted from
 * the system selection into a component its listening to.
 * To use this class, subclass it implementing the abstract 
 * {@link #tableLoaded} method and install it on a component using
 * {@link java.awt.Component#addMouseListener}.
 * Any time you paste a string into the component from the system 
 * selection (by default, using a single click of the middle mouse button)
 * it will be submitted to the table factory using 
 * {@link uk.ac.starlink.table.StarTableFactory#makeStarTable(java.lang.String)}
 * and loaded asynchronously.  If it loads successfully then
 * {@link #tableLoaded} will be called.
 * If the pasted text is very long, it's ignored.
 *
 * @author   Mark Taylor (Starlink)
 * @since    3 Dec 2004
 */
public abstract class PasteLoader extends StringPaster {

    private final Component parent_;
    private final StarTableFactory tableFactory_;

    /**
     * Constructor.
     *
     * @param  parent  parent component (may be used for placing windows)
     * @param  factory  factory to be used for loading tables
     */
    public PasteLoader( Component parent, StarTableFactory factory ) {
        parent_ = parent;
        tableFactory_ = factory;
    }

    protected void pasted( String loc ) {
        final String id = loc.trim();
        if ( id.length() < 240 ) {
            new LoadWorker( new PastedTableConsumer(), id ) {
                public StarTable attemptLoad() throws IOException {
                    return tableFactory_.makeStarTable( id );
                }
            }.invoke();
        }
    }

    protected Toolkit getToolkit() {
        return parent_ == null ? super.getToolkit()
                               : parent_.getToolkit();
    }

    /**
     * Invoked if a table specified by pasting a string into a component
     * watched by this listener is loaded successfully.
     *
     * @param  table  table
     * @param  location    the pasted string (trimmed of spaces)
     */
    protected abstract void tableLoaded( StarTable table, String location );

    /**
     * Table consumer implementation which calls PasteLoader's 
     * tableLoaded method on completion.
     */
    private class PastedTableConsumer extends BasicTableConsumer {

        private String id_;

        PastedTableConsumer() {
            super( parent_ );
        }

        public void loadStarted( String id ) {
            id_ = id;
            super.loadStarted( id );
        }

        protected void setLoading( boolean loading ) {
            super.setLoading( loading );
        }

        protected void tableLoaded( StarTable table ) {
            PasteLoader.this.tableLoaded( table, id_ );
        }
    }
}
