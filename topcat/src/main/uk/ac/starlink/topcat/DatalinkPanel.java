package uk.ac.starlink.topcat;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Rectangle;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTable;
import javax.swing.ListSelectionModel;
import javax.swing.border.Border;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.table.TableColumnModel;
import uk.ac.starlink.table.StarTable;
import uk.ac.starlink.table.gui.StarJTable;
import uk.ac.starlink.table.gui.StarTableModel;
import uk.ac.starlink.table.gui.TableRowHeader;
import uk.ac.starlink.util.IntList;
import uk.ac.starlink.vo.datalink.LinkColMap;
import uk.ac.starlink.vo.datalink.LinksDoc;

/**
 * Component that displays DataLink functionality of a table.
 * It will display a LinksDoc, providing UI options for invoking the
 * links that it defines.
 *
 * @author   Mark Taylor
 * @since    6 Feb 2017
 */
public class DatalinkPanel extends JPanel {

    private final JTable jtable_;
    private final JScrollPane tscroller_;
    private final TableRowHeader rowHeader_;
    private final LinkRowPanel linkPanel_;
    private LinksDoc linksDoc_;
    private Map<LinkColMap.ColDef<String>,String> selectionMap_;
    private String[] colnames_;

    private static final List<LinkColMap.ColDef<String>> CHAR_COLS =
        createCharCols();

    /**
     * Ordered list of DataLink table columns for display.
     * These will be displayed at the start (left) of the JTable,
     * and others will come later in their presented sequence.
     */
    private static final LinkColMap.ColDef<?>[] DISPLAY_COLS = {
        LinkColMap.COL_SEMANTICS,
        LinkColMap.COL_DESCRIPTION,
        LinkColMap.COL_CONTENTTYPE,
        LinkColMap.COL_CONTENTLENGTH,
    };

    /**
     * Constructor.
     *
     * @param   canReplaceContents  if true, a DataLink-type URL invocation
     *                              will replace the DataLink contents of
     *                              this panel with a new table;
     *                              if false, such an invocation will open
     *                              a new window
     * @param  hasAutoInvoke      true if URL panel should feature an
     *                            auto-invoke button
     */
    public DatalinkPanel( boolean canReplaceContents,
                          boolean hasAutoInvoke ) {
        super( new BorderLayout() );
        jtable_ = new StarJTable( false );
        jtable_.setColumnSelectionAllowed( false );
        jtable_.setRowSelectionAllowed( true );
        selectionMap_ = new HashMap<LinkColMap.ColDef<String>,String>();
        final ListSelectionModel rowSelModel = jtable_.getSelectionModel();
        rowSelModel.setSelectionMode( ListSelectionModel.SINGLE_SELECTION );
        rowSelModel.addListSelectionListener( new ListSelectionListener() {
            public void valueChanged( ListSelectionEvent evt ) {
                if ( ! rowSelModel.getValueIsAdjusting() ) {
                    configureSelectedRow( rowSelModel.getMinSelectionIndex() );
                }
            }
        } );
        
        JComponent tablePanel = new JPanel( new BorderLayout() );
        tscroller_ = new JScrollPane( jtable_ );
        rowHeader_ = new TableRowHeader( jtable_ );
        tscroller_.setRowHeaderView( rowHeader_ );
        tablePanel.add( tscroller_, BorderLayout.CENTER );
        tablePanel.setBorder( AuxWindow.makeTitledBorder( "DataLink Table" ) );

        UrlOptions urlopts =
            UrlOptions.createOptions( canReplaceContents ? this : null );
        linkPanel_ = new LinkRowPanel( urlopts, hasAutoInvoke );
        int width = hasAutoInvoke ? 750 : 650;

        tablePanel.setPreferredSize( new Dimension( width, 150 ) );
        linkPanel_.setPreferredSize( new Dimension( width, 300 ) );
        JSplitPane splitter =
            new JSplitPane( JSplitPane.VERTICAL_SPLIT, tablePanel, linkPanel_ );
        add( splitter, BorderLayout.CENTER );
    }

    /**
     * Sets the document to be displayed by this panel.
     * The table in the supplied LinksDoc must be random access.
     *
     * @param  linksDoc   document to display
     * @throws  IllegalArgumentException  if the supplied table is not
     *                                    random access
     */
    public void setLinksDoc( LinksDoc linksDoc ) {
        StarTable table = linksDoc.getResultTable();
        if ( ! table.isRandom() ) {
            throw new IllegalArgumentException( "Table is not random access" );
        }
        Rectangle visRect = tscroller_.getViewport().getViewRect();
        ListSelectionModel rowSelModel = jtable_.getSelectionModel();
        rowSelModel.clearSelection();
        linksDoc_ = linksDoc;
        configureJTable( jtable_, linksDoc );
        StarJTable.configureColumnWidths( jtable_, 200, 1000 );
        linkPanel_.setLinksDoc( linksDoc );
        if ( jtable_.getRowCount() > 0 ) {
            int isel = recoverSelectedRow();
            if ( isel < 0 ) {
                isel = 0;
            }
            rowSelModel.setSelectionInterval( isel, isel );
            Rectangle cellRect = jtable_.getCellRect( isel, 0, true );
            Rectangle scRect = new Rectangle( visRect.x, cellRect.y,
                                              visRect.width, cellRect.height );
            jtable_.scrollRectToVisible( scRect );
        }
        jtable_.repaint();
        jtable_.revalidate();
    }

    /**
     * Indicates whether this panel is currently set up for auto-invoke.
     * If true, then selecting a row in the displayed links document
     * will cause the link to be followed according to current settings
     * without further manual user intervention.
     *
     * @return   whether auto-invoke is in effect
     */
    public boolean isAutoInvoke() {
        return linkPanel_.isAutoInvoke();
    }

    /**
     * Returns the panel displaying the datalink rows.
     *
     * @return  link panel
     */
    public LinkRowPanel getLinkRowPanel() {
        return linkPanel_;
    }

    /**
     * Updates this panel's JTable using the contents of the supplied LinksDoc.
     * This method is invoked by {@link #setLinksDoc} and simply sets
     * JTable's model and column model appropriately,
     * but it may be overridden by subclasses to supply modified behaviour.
     *
     * @param  jtable  this panel's JTable to be updated
     * @param  linksDoc  links document about to be displayed by this panel
     */
    protected void configureJTable( JTable jtable, LinksDoc linksDoc ) {

        /* Prepare a list of column names to be displayed at the left of
         * the table.  If there is already a table displayed, use the
         * column sequence from that.  If not, use the default display
         * column sequence. */
        TableColumnModel tcm0 = jtable.getColumnModel();
        int nc0 = tcm0.getColumnCount();
        String[] cols0;
        if ( nc0 > 0 ) {
            cols0 = new String[ nc0 ];
            for ( int ic = 0; ic < nc0; ic++ ) {
                Object hdr = tcm0.getColumn( ic ).getHeaderValue();
                cols0[ ic ] = hdr == null ? null : hdr.toString();
            }
        }
        else {
            int ncd = DISPLAY_COLS.length;
            cols0 = new String[ ncd ];
            for ( int ic = 0; ic < ncd; ic++ ) {
                cols0[ ic ] = DISPLAY_COLS[ ic ].getName();
            }
        }

        /* Set the table model. */
        jtable.setModel( new StarTableModel( linksDoc.getResultTable() ) );
        rowHeader_.modelChanged();

        /* Arrange the columns so that those with column names matching
         * the prepared column sequence come first.  Any others appear
         * later in their supplied order.  Note this algorithm works
         * but may not be the most efficient, especially for very wide
         * tables.  I don't think that matters. */
        TableColumnModel tcm = jtable.getColumnModel();
        int nc = tcm.getColumnCount();
        for ( int ic0 = cols0.length - 1; ic0 >= 0; ic0-- ) {
            String cname0 = cols0[ ic0 ];
            for ( int ic = 0; ic < nc; ic++ ) {
                if ( cname0.equals( tcm.getColumn( ic ).getHeaderValue() ) ) {
                    tcm.moveColumn( ic, 0 );
                }
            }
        }
    }

    /**
     * Configures the display for a given selected row in the table.
     *
     * @param  irow  selected row index, or -1 for no selection
     */
    private void configureSelectedRow( int irow ) {

        /* Obtains the row data for the currently selected row. */
        Object[] row;
        if ( irow >= 0 ) {
            try {
                row = linksDoc_.getResultTable().getRow( irow );
            }
            catch ( IOException e ) {
                row = null;
            }
        }
        else {
            row = null;
        }

        /* Configures the link panel with the row data. */
        linkPanel_.setRow( row );

        /* Remembers the significant fields of the selected row, so that
         * a "corresponding" row can be selected as default if the links
         * table is later replaced. */
        if ( row != null ) {
            LinkColMap colMap = linksDoc_.getColumnMap();
            selectionMap_ = new HashMap<LinkColMap.ColDef<String>,String>();
            for ( LinkColMap.ColDef<String> col : CHAR_COLS ) {
                selectionMap_.put( col, colMap.getValue( col, row ) );
            }
        }
    }

    /**
     * Returns the index of a suitable row to select.  An attempt is made
     * to find a row that looks like the last selected row, which may have
     * been in a different table.
     *
     * <p>Since DataLink currently defines no obvious way to identify
     * "corresponding" rows in different DataLink tables, the implementation
     * of this method is necessarily ad hoc and hacky.
     *
     * @return   suitable row selection index, or -1 if none suggests itself
     */
    private int recoverSelectedRow() {
        StarTable table = linksDoc_.getResultTable();
        LinkColMap colMap = linksDoc_.getColumnMap();
        int nrow = (int) table.getRowCount();
        if ( nrow < 0 ) {
            assert false;
            return -1;
        }
        if ( nrow == 0 ) {
            return -1;
        }
        if ( nrow == 1 ) {
            return 0;
        }

        /* Generate an ad-hoc similarity score for the rows in the current
         * table in relation to the most recently selected row. */
        int[] scores = new int[ nrow ];
        LinkColMap.ColDef<String> descripCol = LinkColMap.COL_DESCRIPTION;
        assert CHAR_COLS.contains( descripCol );
        int nc = CHAR_COLS.size();
        for ( int irow = 0; irow < nrow; irow++ ) {
            Object[] row;
            try {
                row = table.getRow( irow );
            }
            catch ( IOException e ) {
                return -1;
            }

            /* Record identity of suitable columns. */
            int flagScore = 0;
            for ( int ic = 0; ic < nc; ic++ ) {
                LinkColMap.ColDef<String> col = CHAR_COLS.get( ic );
                String oldValue = selectionMap_.get( col );
                if ( oldValue != null &&
                     oldValue.equals( colMap.getValue( col, row ) ) ) {
                    flagScore = flagScore | ( 1 << ( nc - 1 - ic ) );
                }
            }

            /* As a tie-breaker use the length of common prefix of the
             * description entry.  This is not too unreasonable, but was
             * introduced because it works with Gaia DR3 DataLink tables. */
            int descripScore =
                getCommonPrefixLength( selectionMap_.get( descripCol ),
                                       colMap.getValue( descripCol, row ) );
            scores[ irow ] = flagScore << 8
                           | ( (0xff) & descripScore );
        }

        /* Identify the highest scoring row(s). */
        int hiScore = 0;
        int iHi = -1;
        IntList bests = new IntList();
        for ( int i = 0; i < nrow; i++ ) {
            int score = scores[ i ];
            if ( score > hiScore ) {
                hiScore = score;
                bests = new IntList();
            }
            if ( score == hiScore ) {
                bests.add( i );
            }
        }

        /* Return the best one where possible. */
        int nBest = bests.size();
        if ( nBest == 0 ) {
            return -1;
        }
        else if ( nBest == 1 ) {
            return bests.get( 0 );
        }
        else {
            return bests.get( 0 );
        }
    }

    /**
     * Returns an ordered list of DataLink table columns to examine to find out
     * whether a row in one DataLink table "corresponds to" a row in
     * another one.  The earlier-listed columns are more significant;
     * later ones are used as tie-breakers.
     *
     * @return  char col list
     */
    private static List<LinkColMap.ColDef<String>> createCharCols() {
        List<LinkColMap.ColDef<String>> list =
            new ArrayList<LinkColMap.ColDef<String>>();
        list.add( LinkColMap.COL_SEMANTICS );
        list.add( LinkColMap.COL_CONTENTTYPE );
        list.add( LinkColMap.COL_SERVICEDEF );
        list.add( LinkColMap.COL_DESCRIPTION );
        return list;
    }

    /**
     * Returns the length of string that two strings have in common at
     * their starts, giving a crude measure of similarity.
     * For two completely different strings this will be zero,
     * for two identical strings it will be their (common) length.
     *
     * @param   s1  first string
     * @return  s2  second string
     * @return  common prefix character count
     */
    private static int getCommonPrefixLength( String s1, String s2 ) {
        int leng = s1 == null || s2 == null
                 ? 0
                 : Math.min( s1.length(), s2.length() );
        for ( int ic = 0; ic < leng; ic++ ) {
            if ( s1.charAt( ic ) != s2.charAt( ic ) ) {
                return ic;
            }
        }
        return leng;
    }
}
