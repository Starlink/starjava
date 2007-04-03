package uk.ac.starlink.topcat.plot;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import javax.swing.Box;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import uk.ac.starlink.table.ColumnInfo;
import uk.ac.starlink.table.ColumnData;
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
public class CartesianPointSelector extends PointSelector {

    private final int ndim_;
    private final String[] axisNames_;
    private final ErrorModeSelectionModel[] errorModeModels_;
    private final AxisDataSelector[] dataSelectors_;
    private final JComponent entryBox_;

    /** A column data object which contains zeroes. */
    private static final ColumnData ZERO_COLUMN_DATA = createZeroColumnData();

    /**
     * Constructs a point selector with no error bar capability.
     *
     * @param   styles   initial style set
     * @param   axisNames  labels for the columns to choose, one per axis
     * @param   toggleSets toggle sets to associate with each axis;
     *                     <code>toggleSets</code> itself or any of
     *                     its elements may be null
     */
    public CartesianPointSelector( MutableStyleSet styles, String[] axisNames,
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
    public CartesianPointSelector( MutableStyleSet styles, String[] axisNames,
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
        if ( errorModeModels_.length > 0 ) {
            for ( int id = 0; id < ndim_; id++ ) {
                final int idim = id;
                ActionListener listener = new ActionListener() {
                    public void actionPerformed( ActionEvent evt ) {
                        updateAnnotator();
                        dataSelectors_[ idim ]
                            .setErrorMode( errorModeModels_[ idim ].getMode() );
                    }
                };
                errorModeModels_[ idim ].addActionListener( listener );
                listener.actionPerformed( null );
            }
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
        return createColumnDataTable( getTable(), getColumns() );
    }

    /**
     * Returns a StarTable containing error information as selected in
     * this component.
     * The columns from the active selectors (if any) of each AxisDataSelector 
     * are packed one after another.  It is necessary to know the
     * <code>ErrorMode</code>s currently in force to make sense of this
     * information.
     *
     * @return   error data table
     */
    public StarTable getErrorData() {
        List colList = new ArrayList();
        for ( int idim = 0; idim < ndim_; idim++ ) {
            JComboBox[] errorSelectors =
                dataSelectors_[ idim ].getErrorSelectors();
            for ( int isel = 0; isel < errorSelectors.length; isel++ ) {
                colList.add( (ColumnData)
                             errorSelectors[ isel ].getSelectedItem() );
            }
        }
        ColumnData[] cols =
            (ColumnData[]) colList.toArray( new ColumnData[ 0 ] );
        for ( int icol = 0; icol < cols.length; icol++ ) {
            if ( cols[ icol ] == null ) {
                cols[ icol ] = ZERO_COLUMN_DATA;
            }
        }
        return createColumnDataTable( getTable(), cols );
    }

    public ErrorMode[] getErrorModes() {
        int nerr = errorModeModels_.length;
        ErrorMode[] modes = new ErrorMode[ nerr ];
        for ( int ierr = 0; ierr < nerr; ierr++ ) {
            modes[ ierr ] = errorModeModels_[ ierr ].getMode();
        }
        return modes;
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

    public PointStore createPointStore( int npoint ) {
        return new CartesianPointStore( ndim_, getErrorModes(), npoint );
    }

    protected void configureSelectors( TopcatModel tcModel ) {
        for ( int i = 0; i < ndim_; i++ ) {
            dataSelectors_[ i ].setTable( tcModel );
        }
    }

    /**
     * Initialises the selectors to the first ndim suitable columns available.
     * This is not particularly likely to be what the user is after, but
     * it means that a plot rather than a blank screen will be seen as soon
     * as the window is visible.
     */
    protected void initialiseSelectors() {
        Set usedCols = new HashSet();
        usedCols.add( null );

        /* Iterate over each dimension. */
        for ( int id = 0; id < ndim_; id++ ) {
            AxisDataSelector dataSelector = dataSelectors_[ id ];
            JComboBox colSelector = dataSelector.getMainSelector();

            /* Locate the next unused column in the list available from the
             * dimension's selection. */
            boolean done = false;
            for ( int ic = 0; ic < colSelector.getItemCount() && !done; ic++ ) {
                ColumnData col = (ColumnData) colSelector.getItemAt( ic );
                if ( ! usedCols.contains( col ) ) {

                    /* Set the column selector accordingly. */
                    colSelector.setSelectedItem( col );

                    /* Add the selected column, as well as any associated
                     * error columns, to the list of used ones. */
                    JComboBox[] errSels = dataSelector.getSelectors();
                    for ( int isel = 0; isel < errSels.length; isel++ ) {
                        usedCols.add( errSels[ isel ].getSelectedItem() );
                    }
                    done = true;
                }
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
}
