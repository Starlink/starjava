package uk.ac.starlink.topcat.vizier;

import java.awt.BorderLayout;
import java.awt.Component;
import java.util.Arrays;
import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.ListSelectionModel;
import javax.swing.SwingUtilities;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.TableModel;
import uk.ac.starlink.table.gui.StarJTable;
import uk.ac.starlink.util.gui.ArrayTableColumn;
import uk.ac.starlink.util.gui.ArrayTableModel;
import uk.ac.starlink.util.gui.ArrayTableSorter;

/**
 * Abstract Vizier mode which presents a pre-selected list of
 * catalogues to query.
 *
 * @author   Mark Taylor
 * @since    19 Oct 2009
 */
public abstract class BasicVizierMode implements VizierMode {

    private final JComponent panel_;
    private final String name_;
    private final JTable table_;
    private final ArrayTableModel tModel_;
    private VizierInfo vizinfo_;

    /**
     * Constructor.
     *
     * @param   name  mode name
     * @param   columns  array of Queryable-based columns for catalogue
     *          display
     */
    public BasicVizierMode( String name, ArrayTableColumn[] columns ) {
        name_ = name;
        panel_ = new JPanel( new BorderLayout() );
        tModel_ = new ArrayTableModel();
        tModel_.setColumns( columns );
        table_ = new JTable( tModel_ );
        table_.setSelectionMode( ListSelectionModel.SINGLE_SELECTION );
        ArrayTableSorter sorter = new ArrayTableSorter( tModel_ );
        sorter.install( table_.getTableHeader() );
        sorter.setSorting( 0, false );
        panel_.add( new JScrollPane( table_,
                                     JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,
                                     JScrollPane.HORIZONTAL_SCROLLBAR_NEVER ) );
    }

    public void setVizierInfo( VizierInfo vizinfo ) {
        vizinfo_ = vizinfo;
    }

    /**
     * Returns the vizier info object.
     *
     * @return  vizinfo
     */
    public VizierInfo getVizierInfo() {
        return vizinfo_;
    }

    public Component getComponent() {
        return panel_;
    }
    
    public String getName() {
        return name_;
    }

    public JTable getQueryableTable() {
        return table_;
    }

    public void readData() {
        populateTable();
    }

    /**
     * Provides the array of Queryable objects which represents the 
     * catalogues which can be searched by this mode.
     * The returned items must be compatible with the 
     * {@link uk.ac.starlink.util.gui.ArrayTableColumn}s
     * used by this mode.
     * This method is not called on the event dispatch thread.
     *
     * @return  array of queryable catalogues
     */
    protected abstract Queryable[] loadQueryables();

    /**
     * Called on the event dispatch thread to initiate load and later
     * display of the queryable catalogues used by this mode.
     */
    private void populateTable() {
        new Thread( "Vizier " + name_ ) {
            public void run() {
                final Queryable[] queryables = loadQueryables();
                SwingUtilities.invokeLater( new Runnable() {
                    public void run() {
                        tModel_.setItems( queryables );
                        StarJTable.configureColumnWidths( table_, 600, 1000 );
                        table_.setAutoResizeMode( JTable
                                                 .AUTO_RESIZE_ALL_COLUMNS );
                    }
                } );
            }
        }.start();
    }
}
