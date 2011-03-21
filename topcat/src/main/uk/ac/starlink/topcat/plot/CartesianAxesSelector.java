package uk.ac.starlink.topcat.plot;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import javax.swing.Box;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import uk.ac.starlink.table.ColumnData;
import uk.ac.starlink.table.StarTable;
import uk.ac.starlink.topcat.ToggleButtonModel;
import uk.ac.starlink.topcat.TopcatModel;
import uk.ac.starlink.ttools.plot.ErrorMode;

/**
 * AxesSelector implementation which deals with the straightforward
 * case in which the returned data table consists of the columns selected
 * from the selector's table.
 *
 * @author   Mark Taylor
 * @since    31 May 2007
 */
public class CartesianAxesSelector implements AxesSelector {

    private final String[] axisNames_;
    private final int ndim_;
    private final ErrorModeSelectionModel[] errorModeModels_;
    private final AxisDataSelector[] dataSelectors_;
    private final JComponent entryBox_;
    private TopcatModel tcModel_;

    /**
     * Constructor.
     */
    public CartesianAxesSelector( String[] axisNames,
                                  ToggleButtonModel[] logModels,
                                  ToggleButtonModel[] flipModels,
                                  ErrorModeSelectionModel[] errorModeModels ) {
        axisNames_ = axisNames;
        errorModeModels_ = errorModeModels;
        ndim_ = axisNames.length;

        /* Prepare the visual components. */
        entryBox_ = Box.createVerticalBox();
        dataSelectors_ = new AxisDataSelector[ ndim_ ];
        for ( int idim = 0; idim < ndim_; idim++ ) {

            /* Prepare toggle buttons. */
            String[] togNames = new String[] { "Log", "Flip", };
            ToggleButtonModel[] togModels = new ToggleButtonModel[] {
                ( logModels != null && logModels.length > idim )
                      ? logModels[ idim ]
                      : null,
                ( flipModels != null && flipModels.length > idim )
                      ? flipModels[ idim ]
                      : null,
            };

            /* Create and add column selectors. */
            dataSelectors_[ idim ] =
                new AxisDataSelector( axisNames[ idim ], togNames, togModels );
            dataSelectors_[ idim ].setEnabled( false );
            entryBox_.add( Box.createVerticalStrut( 5 ) );
            entryBox_.add( dataSelectors_[ idim ] );
        }

        /* Place the visual components. */
        entryBox_.add( Box.createVerticalStrut( 5 ) );

        /* Fix for changes to the error mode selections to modify the
         * state of the axis data selectors. */
        if ( errorModeModels_.length > 0 ) {
            for ( int id = 0; id < ndim_; id++ ) {
                final int idim = id;
                ActionListener listener = new ActionListener() {
                    public void actionPerformed( ActionEvent evt ) {
                        dataSelectors_[ idim ]
                            .setErrorMode( errorModeModels_[ idim ]
                                          .getErrorMode() );
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
    public JComponent getColumnSelectorPanel() {
        return entryBox_;
    }

    public JComboBox[] getColumnSelectors() {
        List selectorList = new ArrayList();
        for ( int idim = 0; idim < ndim_; idim++ ) {
            selectorList.addAll( Arrays.asList( dataSelectors_[ idim ]
                                       .getSelectors() ) );
        }
        return (JComboBox[]) selectorList.toArray( new JComboBox[ 0 ] );
    }

    public int getNdim() {
        return ndim_;
    }

    public boolean isReady() {
        if ( tcModel_ == null ) {
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
        return new ColumnDataTable( tcModel_, getColumns() );
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
                Object dat = errorSelectors[ isel ].getSelectedItem();
                colList.add( dat instanceof ColumnData ? (ColumnData) dat
                                                       : null );
            }
        }
        ColumnData[] cols =
            (ColumnData[]) colList.toArray( new ColumnData[ 0 ] );
        for ( int icol = 0; icol < cols.length; icol++ ) {
            if ( cols[ icol ] == null ) {
                cols[ icol ] = ConstantColumnData.ZERO;
            }
        }
        return new ColumnDataTable( tcModel_, cols );
    }

    public ErrorMode[] getErrorModes() {
        int nerr = errorModeModels_.length;
        ErrorMode[] modes = new ErrorMode[ nerr ];
        for ( int ierr = 0; ierr < nerr; ierr++ ) {
            modes[ ierr ] = errorModeModels_[ ierr ].getErrorMode();
        }
        return modes;
    }

    public StarTable getLabelData() {
        return null;
    }

    /**
     * Returns one of the axis selector boxes used by this selector.
     *
     * @param  icol  column index
     * @return axis selector
     */
    public AxisDataSelector getDataSelector( int icol ) {
        return dataSelectors_[ icol ];
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
                    Object dat = csel.getSelectedItem();
                    axed.setAxis( dat instanceof ColumnData
                                      ? ((ColumnData) dat).getColumnInfo()
                                      : null );
                }
            } );
            eds[ i ] = axed;
        }
        return eds;
    }

    public PointStore createPointStore( int npoint ) {
        return new CartesianPointStore( getNdim(), getErrorModes(), npoint );
    }

    public void setTable( TopcatModel tcModel ) {
        for ( int i = 0; i < ndim_; i++ ) {
            dataSelectors_[ i ].setTable( tcModel );
        }
        tcModel_ = tcModel;
    }

    /**
     * Returns the TopcatModel for which this selector is currently selecting
     * axes.
     *
     * @return  table
     */
    public TopcatModel getTable() {
        return tcModel_;
    }

    public void initialiseSelectors() {
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

    public void addActionListener( ActionListener listener ) {
        for ( int i = 0; i < dataSelectors_.length; i++ ) {
            dataSelectors_[ i ].addActionListener( listener );
        }
    }

    public void removeActionListener( ActionListener listener ) {
        for ( int i = 0; i < dataSelectors_.length; i++ ) {
            dataSelectors_[ i ].removeActionListener( listener );
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
            Object dat = dataSelectors_[ i ].getMainSelector()
                                            .getSelectedItem();
            cols[ i ] = dat instanceof ColumnData ? (ColumnData) dat
                                                  : ConstantColumnData.NAN;
        }
        return cols;
    }
}
