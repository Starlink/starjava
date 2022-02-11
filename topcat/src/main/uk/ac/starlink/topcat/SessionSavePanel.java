package uk.ac.starlink.topcat;

import java.awt.BorderLayout;
import java.util.ArrayList;
import java.util.List;
import javax.swing.Action;
import javax.swing.Box;
import javax.swing.DefaultComboBoxModel;
import javax.swing.JButton;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;
import javax.swing.table.TableColumnModel;
import javax.swing.table.TableModel;
import uk.ac.starlink.table.StarTable;
import uk.ac.starlink.table.StarTableWriter;
import uk.ac.starlink.table.gui.TableSaveChooser;
import uk.ac.starlink.votable.ColFitsPlusTableWriter;
import uk.ac.starlink.votable.FitsPlusTableWriter;
import uk.ac.starlink.votable.VOTableWriter;

/**
 * SavePanel for saving a TOPCAT session.  This saves state about loaded
 * tables (visible columns, defined subsets etc) rather than just the
 * apparent table.
 *
 * @author   Mark Taylor
 * @since    4 Aug 2010
 */
public class SessionSavePanel extends SavePanel {

    private final TopcatModelSelectionTable tSelector_;
    private final TableModelListener tableListener_;
    private TableSaveChooser saveChooser_;

    /**
     * Constructor.
     */
    public SessionSavePanel() {
        super( "Session",
               new DefaultComboBoxModel<String>( createFormatList() ) );
        tSelector_ = new TopcatModelSelectionTable( "Save", true );

        /* Listener to ensure that chooser enabledness is set right. */
        tableListener_ = new TableModelListener() {
            public void tableChanged( TableModelEvent evt ) {
                if ( saveChooser_ != null ) {
                    saveChooser_.setEnabled( tSelector_.getSelectedTables()
                                                       .length > 0 );
                }
            }
        };

        /* Buttons for select/deselect all. */
        Action allAct =
            MultiSavePanel.createSelectAllAction( tSelector_, true );
        Action noneAct =
            MultiSavePanel.createSelectAllAction( tSelector_, false );

        /* Place components. */
        setLayout( new BorderLayout() );
        JTable jtable = new JTable( tSelector_.getTableModel() );
        jtable.setRowSelectionAllowed( false );
        jtable.setColumnSelectionAllowed( false );
        jtable.setCellSelectionEnabled( false );
        TableColumnModel colModel = jtable.getColumnModel();
        colModel.getColumn( 0 ).setPreferredWidth( 32 );
        colModel.getColumn( 0 ).setMinWidth( 32 );
        colModel.getColumn( 0 ).setMaxWidth( 32 );
        colModel.getColumn( 1 ).setPreferredWidth( 300 );
        JScrollPane scroller =
            new JScrollPane( jtable, JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,
                                     JScrollPane.HORIZONTAL_SCROLLBAR_NEVER );
        add( scroller, BorderLayout.CENTER );
        Box buttBox = Box.createHorizontalBox();
        buttBox.add( new JButton( allAct ) );
        buttBox.add( Box.createHorizontalStrut( 5 ) );
        buttBox.add( new JButton( noneAct ) );
        buttBox.add( Box.createHorizontalGlue() );
        add( buttBox, BorderLayout.SOUTH );
    }

    public StarTable[] getTables() {
        TopcatModel[] tcModels = tSelector_.getSelectedTables();
        StarTable[] tables = new StarTable[ tcModels.length ];
        for ( int i = 0; i < tcModels.length; i++ ) {
            tables[ i ] = TopcatUtils.encodeSession( tcModels[ i ] );
        }
        return tables;
    }

    public void setActiveChooser( TableSaveChooser chooser ) {
        TableModel tModel = tSelector_.getTableModel();
        saveChooser_ = chooser;
        if ( saveChooser_ == null ) {
            tModel.removeTableModelListener( tableListener_ );
        }
        else {
            tModel.addTableModelListener( tableListener_ );
            saveChooser_.setEnabled( tSelector_.getSelectedTables()
                                               .length > 0 );
        }
    }

    /**
     * Returns the list of format names suitable for session saves.
     *
     * @return   list of format names
     */
    private static String[] createFormatList() {
        List<StarTableWriter> writerList = new ArrayList<StarTableWriter>();
        writerList.add( new FitsPlusTableWriter() );
        writerList.add( new ColFitsPlusTableWriter() );
        writerList.add( new VOTableWriter() );
        String[] names = new String[ writerList.size() ];
        for ( int i = 0; i < names.length; i++ ) {
            names[ i ] = writerList.get( i ).getFormatName();
        }
        return names;
    }
}
