package uk.ac.starlink.topcat.plot;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Arrays;
import javax.swing.Box;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import uk.ac.starlink.table.ColumnInfo;
import uk.ac.starlink.table.ColumnData;
import uk.ac.starlink.table.ColumnStarTable;
import uk.ac.starlink.table.StarTable;
import uk.ac.starlink.topcat.ToggleButtonModel;
import uk.ac.starlink.topcat.TopcatModel;

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
    private final String[] axisNames_;
    private final ErrorModeSelectionModel[] errorModeModels_;
    private final AxisDataSelector[] dataSelectors_;
    private final JComponent entryBox_;

    /**
     * Constructs a point selector with no error bar capability.
     *
     * @param   styles   initial style set
     * @param   axisNames  labels for the columns to choose, one per axis
     * @param   toggleSets toggle sets to associate with each axis;
     *                     <code>toggleSets</code> itself or any of
     *                     its elements may be null
     */
    public DefaultPointSelector( MutableStyleSet styles, String[] axisNames,
                                 ToggleSet[] toggleSets ) {
        this( styles, axisNames, toggleSets, new ErrorModeSelectionModel[ 0 ] );
    }
    

    /**
     * Constructs a point selector optionally with error bar capability.
     *
     * @param   styles   initial style set
     * @param   axisNames  labels for the columns to choose, one per axis
     * @param   toggleSets toggle sets to associate with each axis;
     *                     <code>toggleSets</code> itself or any of
     *                     its elements may be null
     * @param   errorModeModels  selection models for error modes, one per axis
     */
    public DefaultPointSelector( MutableStyleSet styles, String[] axisNames,
                                 ToggleSet[] toggleSets,
                                 ErrorModeSelectionModel[] errorModeModels ) {
        super( styles );
        axisNames_ = axisNames;
        errorModeModels_ = errorModeModels;
        ndim_ = axisNames.length;

        /* Assemble names for toggle buttons. */
        int ntog = toggleSets == null ? 0 : toggleSets.length;
        String[] togNames = new String[ ntog ];
        for ( int itog = 0; itog < ntog; itog++ ) {
            togNames[ itog ] = toggleSets[ itog ].name_;
        }

        /* Prepare the visual components. */
        entryBox_ = Box.createVerticalBox();
        dataSelectors_ = new AxisDataSelector[ ndim_ ];
        for ( int idim = 0; idim < ndim_; idim++ ) {

            /* Prepare toggle buttons. */
            ToggleButtonModel[] togModels = new ToggleButtonModel[ ntog ];
            for ( int itog = 0; itog < ntog; itog++ ) {
                ToggleSet togSet = toggleSets[ itog ];
                togModels[ itog ] = togSet == null ? null
                                                   : togSet.models_[ idim ];
            }

            /* Create and add column selectors. */
            dataSelectors_[ idim ] =
                new AxisDataSelector( axisNames[ idim ], togNames, togModels );
            dataSelectors_[ idim ].addActionListener( actionForwarder_ ); 
            dataSelectors_[ idim ].setEnabled( false );
            entryBox_.add( Box.createVerticalStrut( 5 ) );
            entryBox_.add( dataSelectors_[ idim ] );
        }

        /* Place the visual components. */
        int pad = ndim_ == 1 ? dataSelectors_[ 0 ].getPreferredSize().height
                             : 5;
        entryBox_.add( Box.createVerticalStrut( pad ) );
        entryBox_.add( Box.createVerticalGlue() );

        /* Fix for changes to the error mode selections to modify the
         * state of the axis data selectors. */
        for ( int id = 0; id < ndim_; id++ ) {
            final int idim = id;
            errorModeModels_[ idim ].addActionListener( new ActionListener() {
                public void actionPerformed( ActionEvent evt ) {
                    updateAnnotator();
                    dataSelectors_[ idim ]
                        .setErrorMode( errorModeModels_[ idim ].getMode() );
                }
            } );
        }
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

    public boolean isReady() {
        if ( getTable() == null ) {
            return false;
        }
        ColumnData[] cols = getColumns();
        for ( int i = 0; i < cols.length; i++ ) {
            if ( cols[ i ] == null ) {
                return false;
            }
        }
        return true;
    }

    public StarTable getData() {
        return new ColumnDataTable( getTable(), getColumns() );
    }

    /**
     * Returns one of the the column selector boxes used by this selector.
     *
     * @param  icol  column index
     * @return column selector box
     */
    public JComboBox getColumnSelector( int icol ) {
        return dataSelectors_[ icol ].getMainSelector();
    }

    public AxisEditor[] createAxisEditors() {
        AxisEditor[] eds = new AxisEditor[ ndim_ ];
        for ( int i = 0; i < ndim_; i++ ) {
            final AxisEditor axed = new AxisEditor( axisNames_[ i ] );
            final JComboBox csel = dataSelectors_[ i ].getMainSelector();
            csel.addActionListener( new ActionListener() {
                public void actionPerformed( ActionEvent evt ) {
                    ColumnData cdata = (ColumnData) csel.getSelectedItem();
                    axed.setAxis( cdata == null ? null
                                                : cdata.getColumnInfo() );
                }
            } );
            eds[ i ] = axed;
        }
        return eds;
    }

    protected void configureSelectors( TopcatModel tcModel ) {
        for ( int i = 0; i < ndim_; i++ ) {
            dataSelectors_[ i ].setTable( tcModel );
        }
    }

    protected void initialiseSelectors() {
        for ( int i = 0; i < ndim_; i++ ) {
            JComboBox colSelector = dataSelectors_[ i ].getMainSelector();
            if ( i + 1 < colSelector.getItemCount() ) {
                colSelector.setSelectedIndex( i + 1 );
            }
        }
    }

    /**
     * Returns an array of the selected columns, one for each axis.
     * 
     * @return  columns array
     */
    private ColumnData[] getColumns() {
        ColumnData[] cols = new ColumnData[ ndim_ ];
        for ( int i = 0; i < ndim_; i++ ) {
            cols[ i ] = (ColumnData) dataSelectors_[ i ].getMainSelector()
                                                        .getSelectedItem();
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
     * Table class built up from ColumnData objects.  Implements equals().
     */
    private static class ColumnDataTable extends ColumnStarTable {

        private final TopcatModel tcModel_;
        private final ColumnData[] cols_;

        /**
         * Constructor.
         *
         * @param   tcModel  topcat model
         * @param   cols   array of columns
         */
        ColumnDataTable( TopcatModel tcModel, ColumnData[] cols ) {
            tcModel_ = tcModel;
            cols_ = cols;
            for ( int i = 0; i < cols.length; i++ ) {
                addColumn( cols[ i ] );
            }
        }

        public long getRowCount() {
            return tcModel_.getDataModel().getRowCount();
        }

        public boolean equals( Object o ) {
            if ( o instanceof ColumnDataTable ) {
                ColumnDataTable other = (ColumnDataTable) o;
                return this.tcModel_ == other.tcModel_
                    && Arrays.equals( this.cols_, other.cols_ );
            }
            else {
                return false;
            }
        }

        public int hashCode() {
            return tcModel_.hashCode() + Arrays.asList( cols_ ).hashCode();
        }
    }
}
