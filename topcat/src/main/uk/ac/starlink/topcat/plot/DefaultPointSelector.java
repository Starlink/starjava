package uk.ac.starlink.topcat.plot;

import java.util.Arrays;
import java.util.Date;
import javax.swing.Box;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import uk.ac.starlink.table.ColumnInfo;
import uk.ac.starlink.table.ColumnPermutedStarTable;
import uk.ac.starlink.table.StarTable;
import uk.ac.starlink.table.gui.StarTableColumn;
import uk.ac.starlink.topcat.ColumnCellRenderer;
import uk.ac.starlink.topcat.ColumnComboBoxModel;
import uk.ac.starlink.topcat.RestrictedColumnComboBoxModel;
import uk.ac.starlink.topcat.ToggleButtonModel;
import uk.ac.starlink.topcat.TopcatModel;
import uk.ac.starlink.util.gui.ShrinkWrapper;

/**
 * PointSelector concrete subclass which deals with the straightforward
 * case in which the returned data table consists of the columns selected
 * from the selector's table.
 *
 * @author   Mark Taylor
 * @since    23 Dec 2005
 */
public class DefaultPointSelector extends PointSelector {

    private final int ndim_;
    private final JComboBox[] colSelectors_;
    private final JComponent entryBox_;

    /**
     * Constructor.
     *
     * @param   axisNames  labels for the columns to choose
     * @param   toggleSets toggle sets to associate with each axes (may be null)
     */
    public DefaultPointSelector( String[] axisNames, ToggleSet[] toggleSets ) {
        super();
        ndim_ = axisNames.length;
        
        entryBox_ = Box.createVerticalBox();
        colSelectors_ = new JComboBox[ ndim_ ];
        for ( int i = 0; i < ndim_; i++ ) {
            String aName = axisNames[ i ];
            JComponent cPanel = Box.createHorizontalBox();
            cPanel.add( new JLabel( " " + aName + " Axis: " ) );
            entryBox_.add( Box.createVerticalStrut( 5 ) );
            entryBox_.add( cPanel );

            /* Add and configure the column selector. */
            colSelectors_[ i ] = new JComboBox();
            colSelectors_[ i ]
                .setRenderer( new ColumnCellRenderer( colSelectors_[ i ] ) );
            colSelectors_[ i ].addActionListener( actionForwarder_ );
            cPanel.add( new ShrinkWrapper( colSelectors_[ i ] ) );
            cPanel.add( Box.createHorizontalStrut( 5 ) );
            cPanel.add( new ComboBoxBumper( colSelectors_[ i ] ) );
            colSelectors_[ i ].setEnabled( false );
            cPanel.add( Box.createHorizontalStrut( 5 ) );

            /* Add any per-axis toggles requested. */
            if ( toggleSets != null ) {
                for ( int j = 0; j < toggleSets.length; j++ ) {
                    ToggleSet toggleSet = toggleSets[ j ];
                    JCheckBox checkBox = toggleSet.models_[ i ]
                                                  .createCheckBox();
                    checkBox.setText( toggleSet.name_ );
                    cPanel.add( Box.createHorizontalStrut( 5 ) );
                    cPanel.add( checkBox );
                }
            }

            /* Pad. */
            cPanel.add( Box.createHorizontalGlue() );
        }
        int pad = ndim_ == 1 ? colSelectors_[ 0 ].getPreferredSize().height
                             : 5;
        entryBox_.add( Box.createVerticalStrut( pad ) );
    }

    /**
     * Gets the panel which displays selectors for the columns.
     *
     * @return column selector panel
     */
    protected JComponent getColumnSelectorPanel() {
        return entryBox_;
    }

    public int getNdim() {
        return ndim_;
    }

    public boolean isValid() {
        if ( getTable() == null ) {
            return false;
        }
        StarTableColumn[] cols = getColumns();
        for ( int i = 0; i < cols.length; i++ ) {
            if ( cols[ i ] == null ||
                 cols[ i ] == ColumnComboBoxModel.NO_COLUMN ) {
                return false;
            }
        }
        return true;
    }

    public StarTable getData() {
        int[] colMap = new int[ ndim_ ];
        TopcatModel tcModel = getTable();
        StarTableColumn[] cols = getColumns();
        for ( int idim = 0; idim < ndim_; idim++ ) {
            colMap[ idim ] = cols[ idim ].getModelIndex();
        }
        return new SelectedColumnTable( tcModel, colMap );
    }

    protected void configureSelectors( TopcatModel tcModel ) {
        if ( tcModel == null ) {
            for ( int i = 0; i < ndim_; i++ ) {
                colSelectors_[ i ].setSelectedItem( null );
            }
        }
        else {
            for ( int i = 0; i < ndim_; i++ ) {
                colSelectors_[ i ].setModel(
                    new RestrictedColumnComboBoxModel( tcModel.getColumnModel(),
                                                       true ) {
                        public boolean acceptColumn( ColumnInfo cinfo ) {
                            return DefaultPointSelector.this
                                  .acceptColumn( cinfo );
                        }
                    }
                );
                colSelectors_[ i ].setEnabled( true );
            }
        }
    }

    protected void initialiseSelectors() {
        for ( int i = 0; i < ndim_; i++ ) {
            if ( i + 1 < colSelectors_[ i ].getItemCount() ) {
                colSelectors_[ i ].setSelectedIndex( i + 1 );
            }
        }
    }

    /**
     * Defines what columns will appear as possibles in the column selectors.
     *
     * @param  cinfo  column metadata
     * @return   true iff a column like <code>cinfo</code> should be
     *           choosable
     */
    private boolean acceptColumn( ColumnInfo cinfo ) {
        Class clazz = cinfo.getContentClass();
        return Number.class.isAssignableFrom( clazz )
            || Date.class.isAssignableFrom( clazz );
    }

    /**
     * Returns an array of the selected columns, one for each axis.
     * 
     * @return  columns array
     */
    private StarTableColumn[] getColumns() {
        StarTableColumn[] cols = new StarTableColumn[ ndim_ ];
        for ( int i = 0; i < ndim_; i++ ) {
            cols[ i ] = (StarTableColumn) colSelectors_[ i ].getSelectedItem();
        }
        return cols;
    }

    /**     
     * Encapsulates an array of toggle button models with an associated name.
     */         
    static class ToggleSet {
        final String name_;
        final ToggleButtonModel[] models_;
        ToggleSet( String name, ToggleButtonModel[] models ) {
            name_ = name;
            models_ = (ToggleButtonModel[]) models.clone(); 
        }       
    }               

    /**
     * Like a ColumnPermutedStarTable, but implements equals() properly.
     */
    private static class SelectedColumnTable extends ColumnPermutedStarTable {

        private final TopcatModel tcModel_;
        private final int[] colMap_;

        SelectedColumnTable( TopcatModel tcModel, int[] colMap ) {
            super( tcModel.getDataModel(), colMap, false );
            tcModel_ = tcModel;
            colMap_ = colMap;
        }

        public boolean equals( Object other ) {
            if ( other instanceof SelectedColumnTable ) {
                SelectedColumnTable o = (SelectedColumnTable) other;
                return this.tcModel_ == o.tcModel_ &&
                       Arrays.equals( this.colMap_, o.colMap_ );
            }
            else {
                return false;
            }
        }
        public int hashCode() {
            int code = tcModel_.hashCode();
            for ( int i = 0; i < colMap_.length; i++ ) {
                code = 23 * code + colMap_[ i ];
            }
            return code;
        }
    }
}
