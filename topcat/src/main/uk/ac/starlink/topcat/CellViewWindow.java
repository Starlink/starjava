package uk.ac.starlink.topcat;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Window;
import java.io.IOException;
import java.util.Objects;
import javax.swing.Box;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JScrollPane;
import javax.swing.JSpinner;
import javax.swing.JTextArea;
import javax.swing.SpinnerNumberModel;
import javax.swing.event.TableModelListener;
import javax.swing.table.TableColumn;
import javax.swing.table.TableModel;
import uk.ac.starlink.table.ColumnData;
import uk.ac.starlink.table.ColumnInfo;
import uk.ac.starlink.table.StarTable;
import uk.ac.starlink.table.Tables;
import uk.ac.starlink.table.gui.LabelledComponentStack;
import uk.ac.starlink.table.gui.StarTableColumn;
import uk.ac.starlink.util.gui.ComboBoxBumper;
import uk.ac.starlink.util.gui.ShrinkWrapper;

/**
 * Window that displays the content of a table cell.
 *
 * @author   Fergus Baker
 * @author   Mark Taylor
 * @since    06 Mar 2026
 */
public class CellViewWindow extends AuxWindow {

    private final TopcatModel tcModel_;
    private final TableModel tableModel_;
    private final JTextArea textArea_;
    private final TopcatListener tcListener_;
    private final TableModelListener tableListener_;
    private final ColumnDataComboBox colSelector_;
    private final ColumnDataComboBoxModel colSelectorModel_;
    private final ToggleButtonModel rowfixModel_;
    private final JSpinner irowSpinner_;
    private ColumnData cdata_;
    private long irow_;
    private static final int MAXLEN = 16 * 1024 * 1024;

    /**
     * Constructor.
     *
     * Initialises the Cell View window without any display text.
     *
     */
    @SuppressWarnings("this-escape")
    public CellViewWindow( TopcatModel tcModel, Component parent ) {
        super( tcModel, "Cell View", parent );
        tcModel_ = tcModel;
        tableModel_ = tcModel.getViewModel();

        /* Set up GUI. */
        textArea_ = new JTextArea();
        textArea_.setEditable( false );
        textArea_.setLineWrap( true );
        textArea_.setWrapStyleWord( true );
        JScrollPane textScroller = new JScrollPane( textArea_ );
        colSelectorModel_ =
            new ColumnDataComboBoxModel( tcModel, Object.class, true );
        colSelector_ = new ColumnDataComboBox();
        colSelector_.setModel( colSelectorModel_ );
        colSelector_.addItemListener( evt -> {
            Object selected = colSelectorModel_.getSelectedItem();
            cdata_ = selected instanceof ColumnData
                   ? (ColumnData) selected
                   : null;
            updateText();
        } );
        Box colBox = Box.createHorizontalBox();
        colBox.add( new ShrinkWrapper( colSelector_ ) );
        colBox.add( Box.createHorizontalStrut( 5 ) );
        colBox.add( new ComboBoxBumper( colSelector_ ) );
        int nrow =
            Tables.checkedLongToInt( tcModel_.getDataModel().getRowCount() );
        SpinnerNumberModel spinnerModel =
            new SpinnerNumberModel( 1, 1, nrow + 1, 1 );
        irowSpinner_ = new JSpinner( spinnerModel );
        irowSpinner_.addChangeListener( evt -> {
            long irow1 = spinnerModel.getNumber().longValue();
            if ( irow1 > 0 ) {
                setRowIndex( irow1 - 1 );
            }
        } );
        rowfixModel_ =
            new ToggleButtonModel( "Fix row index", null, 
                                   "Set the view row explicitly rather than "
                                 + "using the latest activated row" );
        JRadioButton[] rowButts =
            rowfixModel_.createRadioButtons( "Activated", "Fixed" );
        rowfixModel_.setSelected( false );
        rowfixModel_.addChangeListener( evt -> {
            irowSpinner_.setEnabled( rowfixModel_.isSelected() );
        } );
        irowSpinner_.setEnabled( rowfixModel_.isSelected() );
        Box rowBox = Box.createHorizontalBox();
        rowBox.add( rowButts[ 0 ] );
        rowBox.add( rowButts[ 1 ] );
        rowBox.add( new ShrinkWrapper( irowSpinner_ ) );
        
        /* Update displayed contents on row activation. */
        tcListener_ = evt -> {
            int code = evt.getCode();
            Object datum = evt.getDatum();
            if ( code == TopcatEvent.ROW ) {
                if ( ! rowfixModel_.isSelected() ) {
                    setRowIndex( datum instanceof Number
                               ? ((Number) datum).longValue()
                               : -1 );
                }
            }
            else if ( code == TopcatEvent.COLUMN_DEF ) {
                updateText();
            }
        };
        tcModel_.addTopcatListener( tcListener_ );

        /* Update displayed contents if table contents changes. */
        tableListener_ = evt -> {
            if ( irow_ >= evt.getFirstRow() && irow_ <= evt.getLastRow() ) {
                updateText();
            }
        };
        tableModel_.addTableModelListener( tableListener_ );

        /* Place components. */
        LabelledComponentStack stack = new LabelledComponentStack();
        stack.addLine( "Column", colBox );
        stack.addLine( "Row", null, rowBox, true );
        JPanel panel = new JPanel( new BorderLayout() );
        panel.add( textScroller, BorderLayout.CENTER );
        panel.add( stack, BorderLayout.NORTH );

        /* These cause the this-escape warning, but are perfectly safe in this
         * context. */
        setPreferredSize( new Dimension( 500, 240 ) );
        getBodyPanel().setLayout( new BorderLayout() );
        getBodyPanel().add( panel );
        pack();
        addHelp( "CellViewWindow" );
    }

    /**
     * Sets the index of the row for which a table cell is to be displayed.
     *
     * @param  irow  row index
     */
    public void setRowIndex( long irow ) {
        irow_ = irow;
        int irow1 = Tables.checkedLongToInt( irow + 1 );
        if ( ((Number) irowSpinner_.getValue()).intValue() != irow1 ) {
            irowSpinner_.setValue( Integer.valueOf( irow1 ) );
        }
        updateText();
    }

    /**
     * Sets the index of the column for which a table cell is to be displayed.
     *
     * @param  icol  column index
     */
    public void setColumnIndex( int icol ) {
        TableColumn tcol = tcModel_.getColumnModel().getColumn( icol );
        if ( tcol instanceof StarTableColumn ) {
            StarTableColumn stcol = (StarTableColumn) tcol;
            ColumnData cdata = ColumnDataComboBoxModel
                              .createSimpleColumnData( tcModel_, stcol );
            colSelector_.setSelectedItem( cdata );
        }
        updateText();
    }

    @Override
    public void dispose() {
        super.dispose();
        tcModel_.removeTopcatListener( tcListener_ );
        tableModel_.removeTableModelListener( tableListener_ );
    }

    /**
     * Used to set the text to display in the Cell View window.
     *
     * @param text Text to display in this component.
     */
    private void updateText() {
        String text;
        if ( cdata_ != null && irow_ >= 0 ) {
            Object cell;
            try {
                cell = cdata_.readValue( irow_ );
            }
            catch ( IOException e ) {
                cell = null;
            }
            ColumnInfo cinfo = cdata_.getColumnInfo();
            text = cell == null
                 ? null
                 : cdata_.getColumnInfo().formatValue( cell, MAXLEN );
        }
        else {
            text = null;
        }
        if ( ! Objects.equals( textArea_.getText(), text ) ) {
            textArea_.selectAll();
            textArea_.replaceSelection( text );
            textArea_.setCaretPosition( 0 );
        }
    }
}
