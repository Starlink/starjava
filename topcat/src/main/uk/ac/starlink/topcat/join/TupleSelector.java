package uk.ac.starlink.topcat.join;

import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.util.HashMap;
import java.util.Map;
import javax.swing.Box;
import javax.swing.ComboBoxModel;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.table.TableColumn;
import javax.swing.table.TableColumnModel;
import uk.ac.starlink.table.ColumnInfo;
import uk.ac.starlink.table.ColumnPermutedStarTable;
import uk.ac.starlink.table.StarTable;
import uk.ac.starlink.table.Tables;
import uk.ac.starlink.table.ValueInfo;
import uk.ac.starlink.table.gui.StarTableColumn;
import uk.ac.starlink.table.join.MatchEngine;
import uk.ac.starlink.topcat.ColumnCellRenderer;
import uk.ac.starlink.topcat.RestrictedColumnComboBoxModel;
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
    private final JComboBox[] colSelectors;
    private final ValueInfo[] infos;
    private final int nCols;
    private TopcatModel tcModel;
    private final Map modelsMap = new HashMap();

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
        tableSelector.setToolTipText( "Table to perform the matching on" );
        tableSelector.addItemListener( new ItemListener() {
            public void itemStateChanged( ItemEvent evt ) {
                setTable( (TopcatModel) tableSelector.getSelectedItem() );
            }
        } );
        Box line = Box.createHorizontalBox();
        line.add( new JLabel( "Table: " ) );
        line.add( tableSelector );
        main.add( line );

        /* Set up selectors for the engine parameters. */
        colSelectors = new JComboBox[ nCols ];
        for ( int i = 0; i < nCols; i++ ) {
            ValueInfo info = infos[ i ];
            JComboBox selector = new JComboBox();
            line = Box.createHorizontalBox();
            line.add( new JLabel( info.getName() + " column: " ) );
            line.add( selector );
            String units = info.getUnitString();
            if ( units != null && units.trim().length() > 0 ) {
                line.add( new JLabel( " (" + units + ")" ) );
            }
            selector.setToolTipText( info.getDescription() );
            selector.setRenderer( new ColumnCellRenderer() );
            colSelectors[ i ] = selector;
            main.add( line );
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
        StarTable baseTable = tcModel.getViewModel().getSnapshot();
        int[] colMap = new int[ nCols ];
        for ( int j = 0; j < nCols; j++ ) {
            TableColumn tcol = (TableColumn) 
                               colSelectors[ j ].getSelectedItem();
            if ( tcol == null ) {
                throw new IllegalStateException( "No " + infos[ j ].getName() +
                                                 " column selected" );
            }
            colMap[ j ] = tcol.getModelIndex();
        }
        return new ColumnPermutedStarTable( baseTable, colMap );
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
        ComboBoxModel[] selectorModels = getSelectorModels();
        for ( int i = 0; i < nCols; i++ ) {
            colSelectors[ i ].setModel( selectorModels[ i ] );
        }
    }

    /**
     * Returns a set of ComboBoxModels suitable for use as column selectors
     * for the current state of this TupleSelector.
     * If some models are cached from a previous invocation, these will
     * be returned (so that earlier selections on the models will be
     * remembered), but if it's the first time it's been called on this
     * object for a given TopcatModel, it will make some new default ones.
     *
     * @param  array of combo box models for the currently selected TopcatModel
     */
    private ComboBoxModel[] getSelectorModels() {
        if ( ! modelsMap.containsKey( tcModel ) ) {
            ComboBoxModel[] models = new ComboBoxModel[ nCols ];
            for ( int i = 0; i < nCols; i++ ) {
                models[ i ] = makeColumnSelectionModel( tcModel, infos[ i ] );
            }
            modelsMap.put( tcModel, models );
        }
        return (ComboBoxModel[]) modelsMap.get( tcModel );
    }

    /**
     * Returns a combobox model which allows selection of columns
     * from a table model suitable for a given argument.
     */
    private static ComboBoxModel makeColumnSelectionModel( TopcatModel tcModel,
                                                           ValueInfo argInfo ) {

        /* Make the model. */
        final Class clazz = argInfo.getContentClass();
        TableColumnModel columnModel = tcModel.getColumnModel();
        RestrictedColumnComboBoxModel model =
            new RestrictedColumnComboBoxModel( columnModel,
                                               argInfo.isNullable() ) {
                public boolean acceptColumn( ColumnInfo cinfo ) {
                    return clazz.isAssignableFrom( cinfo.getContentClass() );
                }
            };

        /* Have a guess what will be a good value for the initial
         * selection.  There is scope for doing this better. */
        int selection = -1;
        ColumnInfo[] cinfos =
            Tables.getColumnInfos( tcModel.getApparentStarTable() );
        int ncol = cinfos.length;
        String ucd = argInfo.getUCD();
        if ( ucd != null ) {
            for ( int i = 0; i < ncol && selection < 0; i++ ) {
                if ( model.acceptColumn( cinfos[ i ] ) &&
                     cinfos[ i ].getUCD() != null &&
                     cinfos[ i ].getUCD().indexOf( ucd ) >= 0 ) {
                    selection = i;
                }
            }
        }
        String name = argInfo.getName().toLowerCase();
        if ( name != null && selection < 0 ) {
            for ( int i = 0; i < ncol && selection < 0; i++ ) {
                if ( model.acceptColumn( cinfos[ i ] ) ) {
                    String cname = cinfos[ i ].getName();
                    if ( cname != null &&
                         cname.toLowerCase().startsWith( name ) ) {
                        selection = i;
                    }
                }
            }
        }
        if ( selection >= 0 ) {
            model.setSelectedItem( columnModel.getColumn( selection ) );
        }
        return model;
    }
}
