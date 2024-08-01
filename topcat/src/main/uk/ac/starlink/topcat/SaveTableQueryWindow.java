package uk.ac.starlink.topcat;

import java.awt.BorderLayout;
import java.awt.Component;
import javax.swing.Action;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import uk.ac.starlink.table.StarTable;
import uk.ac.starlink.table.StarTableOutput;
import uk.ac.starlink.table.gui.FilestoreTableSaveDialog;
import uk.ac.starlink.table.gui.SystemTableSaveDialog;
import uk.ac.starlink.table.gui.TableSaveDialog;
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
    private TableSource tsrc_;

    /**
     * Constructor.
     *
     * @param  title  window title
     * @param  parent   parent window
     * @param  sto    table output handler
     * @param  progress  true iff you want a save progress bar
     */
    @SuppressWarnings("this-escape")
    public SaveTableQueryWindow( String title, Component parent,
                                 StarTableOutput sto, boolean progress ) {
        super( title, parent, false, true );
        chooser_ = new TableSaveChooser( sto,
                                         new TableSaveDialog[] {
                                             new FilestoreTableSaveDialog(),
                                             new SystemTableSaveDialog(),
                                         } ) {
            public StarTable[] getTables() {
                TableSource tsrc = getTableSource();
                return tsrc == null ? new StarTable[ 0 ]
                                    : new StarTable[] { tsrc.getStarTable() };
            }
            public void done() {
                super.done();
                SaveTableQueryWindow.this.dispose();
            }
        };
        if ( progress ) {
            chooser_.setProgressBar( placeProgressBar() );
        }
        JComponent mainBox = new JPanel( new BorderLayout() );
        mainBox.add( new JLabel( title + "." ), BorderLayout.NORTH );
        mainBox.add( chooser_, BorderLayout.CENTER );
        getAuxControlPanel().add( mainBox );

        /* Toolbar buttons. */
        Action[] saverActs = chooser_.getSaveDialogActions();
        for ( int ia = 0; ia < saverActs.length; ia++ ) {
            getToolBar().add( saverActs[ ia ] );
        }
        getToolBar().addSeparator();

        /* Help button.  This is not exactly the right help for this
         * window, but it's quite close. */
        addHelp( "TableSaveChooser" );
    }

    /**
     * Sets the source of tables to be written.
     * Should be called with a non-null value before the user is
     * invited to save.
     *
     * @param   tsrc supplier of the table to be saved (invoked when the
     *               window save operation is activated)
     */
    public void setTableSource( TableSource tsrc ) {
        tsrc_ = tsrc;
    }

    /**
     * Returns the currently configured table source.
     *
     * @return   supplier of the table to be saved (invoked when the
     *           window save operation is activated)
     */
    public TableSource getTableSource() {
        return tsrc_;
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
