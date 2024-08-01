package uk.ac.starlink.topcat;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import javax.swing.Box;
import javax.swing.ComboBoxModel;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import uk.ac.starlink.table.ColumnData;
import uk.ac.starlink.table.ColumnInfo;
import uk.ac.starlink.table.ColumnStarTable;
import uk.ac.starlink.table.StarTable;
import uk.ac.starlink.table.ValueInfo;

/**
 * Component which allows the user to select table columns corresponding
 * to a given set of  the tuple elements required for a given 
 * array of column metadata descriptions.
 *
 * @author   Mark Taylor (Starlink)
 * @since    17 Mar 2004
 */
public class TupleSelector extends JPanel {

    private final ColumnSelector[] colSelectors_;
    private final ValueInfo[] infos_;
    private final int nCols_;
    private TopcatModel tcModel_;

    /**
     * Constructor.
     *
     * @param  infos   metadata descriptors for each value required
     */
    @SuppressWarnings("this-escape")
    public TupleSelector( ValueInfo[] infos ) {
        super( new BorderLayout() );
        infos_ = infos;
        nCols_ = infos.length;
        JComponent main = AlignedBox.createVerticalBox();
        add( main, BorderLayout.NORTH );

        /* Set up a table selection box. */
        final JComboBox<TopcatModel> tableSelector =
            new TablesListComboBox( 250 );
        tableSelector.addItemListener( new ItemListener() {
            public void itemStateChanged( ItemEvent evt ) {
                setTable( (TopcatModel) tableSelector.getSelectedItem() );
            }
        } );
        Box line = Box.createHorizontalBox();
        JLabel label = new JLabel( "Table: " );
        label.setToolTipText( "Table to perform the matching on" );
        line.add( label );
        line.add( tableSelector );
        main.add( line );
        main.add( Box.createVerticalStrut( 5 ) );

        /* Set up selectors for the infos. */
        colSelectors_ = new ColumnSelector[ nCols_ ];
        for ( int i = 0; i < nCols_; i++ ) {
            colSelectors_[ i ] = new ColumnSelector( infos_[ i ], true );
            main.add( colSelectors_[ i ] );
        }
        main.add( Box.createVerticalGlue() );

        /* Align the selectors. */
        Dimension labelSize = new Dimension( 0, 0 );
        for ( int i = 0; i < nCols_; i++ ) {
            Dimension s = colSelectors_[ i ].getLabel().getPreferredSize();
            labelSize.width = Math.max( labelSize.width, s.width );
            labelSize.height = Math.max( labelSize.height, s.height );
        }
        for ( int i = 0; i < nCols_; i++ ) {
            colSelectors_[ i ].getLabel().setPreferredSize( labelSize );
        }
    }

    /**
     * Returns the effective table described by this panel.
     * This is based on the table selected in the table selection box,
     * but containing only those columns in the argument selection box(es).
     * The returned table is an effective view of a snapshot of 
     * the Apparent Table, which is to say that its rows are permuted
     * according to the current sort order and selection.
     *
     * @return  effective table selected by the user in this panel
     * @throws  IllegalStateException with a sensible message if the 
     *          user hasn't properly specified a table
     */
    public StarTable getEffectiveTable() {
        if ( tcModel_ == null ) {
            throw new IllegalStateException( "No table selected" );
        }
        final StarTable baseTable = tcModel_.getDataModel();
        ColumnStarTable effTable = new ColumnStarTable( baseTable ) {
            public long getRowCount() {
                return baseTable.getRowCount();
            }
        };
        for ( int j = 0; j < nCols_; j++ ) {
            ColumnData cdata = colSelectors_[ j ].getColumnData();
            if ( cdata == null ) {
                throw new IllegalStateException( "No " + infos_[ j ].getName() +
                                                 " column selected" );
            }
            effTable.addColumn( cdata );
        }
        return tcModel_.getViewModel().getRowPermutedView( effTable );
    }

    /**
     * Returns the currently selected table.
     *
     * @return  topcat model of the currently selected table
     */
    public TopcatModel getTable() {
        return tcModel_;
    }

    /**
     * Returns the string values currently entered for the tuple elements.
     * These are column names or JEL expressions.
     *
     * @return   an array of string values entered by the user representing
     *           the tuple values
     */
    public String[] getTupleExpressions() {
        String[] names = new String[ nCols_ ];
        for ( int j = 0; j < nCols_; j++ ) {
            names[ j ] = colSelectors_[ j ].getStringValue();
        }
        return names;
    }

    /**
     * Sets this selector to work from a table described by a given 
     * TopcatModel.
     *
     * @param  tcModel  table to work with
     */
    private void setTable( TopcatModel tcModel ) {
        tcModel_ = tcModel;
        for ( int i = 0; i < nCols_; i++ ) {
            colSelectors_[ i ].setTable( tcModel_ );
        }
    }
}
