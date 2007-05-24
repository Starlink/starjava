package uk.ac.starlink.topcat;

import java.awt.Component;
import uk.ac.starlink.table.StarTable;
import uk.ac.starlink.table.StarTableOutput;
import uk.ac.starlink.table.gui.TableSaveChooser;
import uk.ac.starlink.table.TableSource;

/**
 * QueryWindow which allows the user to save a normal table 
 * (not a TopcatModel) to disk.
 *
 * @author   Mark Taylor
 * @since    23 May 2007
 */
public class SaveTableQueryWindow extends QueryWindow {

    private final TableSaveChooser chooser_;

    /**
     * Constructor.
     *
     * @param  title  window title
     * @param  parent   parent window
     * @param  tsrc    supplier of the table to be saved (invoked when the
     *                 window save operation is activated)
     * @param  sto    table output handler
     * @param  progress  true iff you want a save progress bar
     */
    public SaveTableQueryWindow( String title, Component parent,
                                 final TableSource tsrc, StarTableOutput sto,
                                 boolean progress ) {
        super( title, parent );
        chooser_ = new TableSaveChooser( sto ) {
            public StarTable getTable() {
                return tsrc.getStarTable();
            }
            public void done() {
                super.done();
                SaveTableQueryWindow.this.dispose();
            }
        };
        if ( progress ) {
            chooser_.setProgressBar( placeProgressBar() );
        }
        getAuxControlPanel().add( chooser_ );
    }

    /**
     * Sets the default format string with which tables will be saved.
     *
     * @param  fmt  format string
     */
    public void setDefaultFormat( String fmt ) {
        chooser_.setSelectedFormat( fmt );
    }

    public boolean perform() {
        return false;
    }
}
