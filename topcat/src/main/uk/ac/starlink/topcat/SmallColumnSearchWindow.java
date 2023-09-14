package uk.ac.starlink.topcat;

import java.awt.Rectangle;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.util.regex.Pattern;
import javax.swing.Action;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.ListSelectionModel;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JScrollBar;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.table.TableModel;
import uk.ac.starlink.util.IntList;

/**
 * SearchWindow subclass that searches for content in the columns
 * of a displayed JTable.
 * The table is assumed to be non-huge, so that searching can be done
 * on the Event Dispatch Thread.
 *
 * <p>Following a successful search, the matching rows of the JTable
 * are Selected and the window is scrolled so that the first is visible.
 *
 * @author   Mark Taylor
 * @since    14 Sep 2023
 */
public class SmallColumnSearchWindow extends ColumnSearchWindow {

    private final JTable jtab_;
    private final JScrollPane scroller_;
    private final Action searchAct_;

    /**
     * Constructor.
     *
     * @param  title  title of dialogue window
     * @param  owner  window that owns this dialogue
     * @param  jtab   table displaying cells to search
     * @param  scroller  scroll pane containing jtab, may be null
     * @param  columnSelectorLabel  label for column selector
     */
    public SmallColumnSearchWindow( String title, Window owner,
                                    JTable jtab, JScrollPane scroller,
                                    String columnSelectorLabel ) {
        super( title, owner, columnSelectorLabel,
               new ColumnComboBoxModel( jtab.getColumnModel(), false ) );
        jtab_ = jtab;
        scroller_ = scroller;
        ActionForwarder forwarder = getActionForwarder();
        searchAct_ = new BasicAction( "Search", null ) {
            public void actionPerformed( ActionEvent evt ) {
                final Search search = createSearch();
                if ( search != null ) {
                    performSearch( search );
                }
            }
        };
        forwarder.addActionListener( evt -> updateActions() );
        updateActions();

        /* Hitting return in text field starts search. */
        getTextField().addActionListener( searchAct_ );

        /* Place components. */
        JComponent controlLine = getControlBox();
        controlLine.add( Box.createHorizontalGlue() );
        controlLine.add( new JButton( searchAct_ ) );
        controlLine.add( Box.createHorizontalGlue() );
        controlLine.setBorder( BorderFactory.createEmptyBorder( 5, 5, 5, 5 ) );
        pack();
    }

    /**
     * Returns the action that initiates a search, in accordance with
     * the currently configured GUI.
     *
     * @return  action
     */
    public Action getSearchAction() {
        return searchAct_;
    }

    /**
     * Update enabledness status.
     */
    private void updateActions() {
        searchAct_.setEnabled( createSearch() != null );
    }

    /**
     * Performs a search and selects matching rows.
     *
     * @param  search  characterises search required
     */
    private void performSearch( Search search ) {
        int jcol = search.getColumnIndex();
        Pattern pattern = search.getPattern();
        SearchScope scope = search.getScope();
        TableModel tmodel = jtab_.getModel();
        int nrow = tmodel.getRowCount();
        IntList finds = new IntList();
        for ( int irow = 0; irow < nrow; irow++ ) {
            Object cell = tmodel.getValueAt( irow, jcol );
            String txt = ( cell instanceof String || cell instanceof Number )
                       ? cell.toString()
                       : null;
            if ( txt != null && txt.trim().length() > 0 ) {
                if ( scope.matches( pattern.matcher( txt ) ) ) {
                    finds.add( irow );
                }
            }
        }
        int nfind = finds.size();
        ListSelectionModel selModel = jtab_.getSelectionModel();
        selModel.clearSelection();
        if ( nfind > 0 ) {
            selModel.setValueIsAdjusting( true );
            for ( int i = 0; i < nfind; i++ ) {
                int ir = finds.get( i );
                selModel.addSelectionInterval( ir, ir );
            }
            selModel.setValueIsAdjusting( false );
            scrollToRow( finds.get( 0 ) );
        }
        searchCompleted( nfind > 0 );
    }

    /**
     * Scrolls the table vertically to the given row index.
     *
     * @param  irow  row index
     */
    private void scrollToRow( int irow ) {
        if ( scroller_ != null ) {
            Rectangle viewRect = jtab_.getCellRect( irow, 0, false );
            int yMid = viewRect.y + viewRect.height / 2;
            JScrollBar vbar = scroller_.getVerticalScrollBar();
            vbar.setValue( yMid - vbar.getVisibleAmount() / 2 );
        }
    }
}
