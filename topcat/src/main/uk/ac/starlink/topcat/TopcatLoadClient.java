package uk.ac.starlink.topcat;

import java.awt.Component;
import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;
import uk.ac.starlink.table.DescribedValue;
import uk.ac.starlink.table.StarTable;
import uk.ac.starlink.table.StarTableFactory;
import uk.ac.starlink.table.gui.TableLoadClient;
import uk.ac.starlink.table.gui.TableLoader;
import uk.ac.starlink.util.gui.ErrorDialog;

/**
 * Load client implementation which loads tables into TOPCAT.
 *
 * @author   Mark Taylor
 * @since    17 Sep 2010
 */
public class TopcatLoadClient implements TableLoadClient {

    private final Component parent_;
    private final ControlWindow controlWin_;
    private final LoadingToken token_;
    private String label_;
    private int nLoad_;

    /**
     * Constructor.
     *
     * @param   parent  parent component
     * @param   controlWin  control window
     */
    public TopcatLoadClient( Component parent, ControlWindow controlWin ) {
        parent_ = parent;
        controlWin_ = controlWin;
        token_ = new LoadingToken( "Table"  );
    }

    public StarTableFactory getTableFactory() {
        return controlWin_.createMonitorFactory( token_ );
    }

    public void startSequence() {
        controlWin_.addLoadingToken( token_ );
    }

    public void setLabel( String label ) {
        label_ = label;
        token_.setTarget( label );
    }

    public boolean loadSuccess( StarTable table ) {
        if ( table.getRowCount() == 0 ) {
            final String[] msg = { "Empty table " + label_ + ".",
                                   "Table contained no rows." };
            SwingUtilities.invokeLater( new Runnable() {
                public void run() {
                    JOptionPane
                        .showMessageDialog( parent_, msg,
                                            "Empty Table",
                                            JOptionPane.ERROR_MESSAGE );
                }
            } );
        }
        else {
            addTable( table );
        }
        return true;
    }

    /**
     * Takes a table and inserts it into the TOPCAT application, performing
     * some housekeeping tasks at the same time.
     *
     * @param   table  table to insert
     * @return   topcat model which holds the table
     */
    protected TopcatModel addTable( StarTable table ) {
        String location;
        DescribedValue srcParam =
            table.getParameterByName( TableLoader.SOURCE_INFO.getName() );
        if ( srcParam != null && srcParam.getValue() instanceof String ) {
            location = (String) srcParam.getValue();
        }
        else {
            location = label_;
            if ( nLoad_ > 0 ) {
                location += "-" + ( nLoad_ + 1 );
            }
        }
        nLoad_++;
        return controlWin_.addTable( table, location, true );
    }

    public boolean loadFailure( final Throwable error ) {
        SwingUtilities.invokeLater( new Runnable() {
            public void run() {
                if ( error instanceof OutOfMemoryError ) {
                    TopcatUtils.memoryError( (OutOfMemoryError) error );
                }
                else {
                    String[] msg = { "Error loading " + label_ + "." };
                    ErrorDialog.showError( parent_, "Load Error", error, msg );
                }
            }
        } );
        return false;
    }

    public void endSequence( boolean cancelled ) {
        controlWin_.removeLoadingToken( token_ );
    }

    /**
     * Returns the number of tables successfully loaded by this client.
     *
     * @return  load count
     */
    public int getLoadCount() {
        return nLoad_;
    }
}
