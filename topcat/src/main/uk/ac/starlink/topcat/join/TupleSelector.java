package uk.ac.starlink.topcat.join;

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
import uk.ac.starlink.table.join.MatchEngine;
import uk.ac.starlink.topcat.ColumnSelector;
import uk.ac.starlink.topcat.TablesListComboBoxModel;
import uk.ac.starlink.topcat.TopcatModel;

/**
 * Component which allows the user to select table columns corresponding
 * to the tuple elements required for a given 
 * {@link uk.ac.starlink.table.join.MatchEngine}.
 *
 * @author   Mark Taylor (Starlink)
 * @since    17 Mar 2004
 */
public class TupleSelector extends JPanel {

    private final MatchEngine engine;
    private final ColumnSelector[] colSelectors;
    private final ValueInfo[] infos;
    private final int nCols;
    private TopcatModel tcModel;

    /**
     * Constructs a new selector panel to provide column selections of the
     * tuple elements for a given match engine.
     *
     * @param  engine  match engine
     */
    public TupleSelector( MatchEngine engine ) {
        this.engine = engine;
        infos = engine.getTupleInfos();
        nCols = infos.length;
        JComponent main = Box.createVerticalBox();
        add( main );

        /* Set up a table selection box. */
        final JComboBox tableSelector = 
            new JComboBox( new TablesListComboBoxModel() );
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

        /* Set up selectors for the engine parameters. */
        colSelectors = new ColumnSelector[ nCols ];
        for ( int i = 0; i < nCols; i++ ) {
            colSelectors[ i ] = new ColumnSelector( infos[ i ] );
            main.add( colSelectors[ i ] );
        }
        main.add( Box.createVerticalGlue() );
    }

    /**
     * Returns the effective table described by this panel.
     * This is based on the table selected in the table selection box,
     * but containing only those columns in the argument selection box(es).
     *
     * @return  effective table selected by the user in this panel
     * @throws  IllegalStateException with a sensible message if the 
     *          user hasn't properly specified a table
     */
    public StarTable getEffectiveTable() {
        if ( tcModel == null ) {
            throw new IllegalStateException( "No table selected" );
        }
        final StarTable baseTable = tcModel.getViewModel().getSnapshot();
        ColumnStarTable effTable = new ColumnStarTable( baseTable ) {
            public long getRowCount() {
                return baseTable.getRowCount();
            }
        };
        for ( int j = 0; j < nCols; j++ ) {
            ColumnData cdata = colSelectors[ j ].getColumnData();
            if ( cdata == null ) {
                throw new IllegalStateException( "No " + infos[ j ].getName() +
                                                 " column selected" );
            }
            effTable.addColumn( cdata );
        }
        return effTable;
    }

    /**
     * Returns the currently selected table.
     *
     * @param  topcat model of the currently selected table
     */
    public TopcatModel getTable() {
        return tcModel;
    }

    /**
     * Sets this selector to work from a table described by a given 
     * TopcatModel.
     *
     * @param  tcModel  table to work with
     */
    private void setTable( TopcatModel tcModel ) {
        this.tcModel = tcModel;
        for ( int i = 0; i < nCols; i++ ) {
            colSelectors[ i ].setTable( tcModel );
        }
    }
}
