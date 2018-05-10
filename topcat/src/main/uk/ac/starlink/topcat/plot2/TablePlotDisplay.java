package uk.ac.starlink.topcat.plot2;

import java.awt.Component;
import javax.swing.DefaultListModel;
import uk.ac.starlink.table.StarTable;
import uk.ac.starlink.topcat.ControlWindow;
import uk.ac.starlink.topcat.ListModel2;
import uk.ac.starlink.topcat.TablesListComboBox;
import uk.ac.starlink.topcat.TopcatModel;
import uk.ac.starlink.topcat.TypedListModel;

/**
 * Opens a plot window with access to a table that can be supplied
 * without loading it into the TOPCAT application.
 * This table can be replaced, and the plot will update accordingly.
 * It works best if the replacement tables are all similar in structure.
 *
 * @author   Mark Taylor
 * @since    10 May 2018
 */
public class TablePlotDisplay {

    private final Component parent_;
    private final PlotWindowType plotType_;
    private final String tableLabel_;
    private final boolean isVisibleOnPlot_;
    private final ExtraTablesListModel tablesModel_;
    private StackPlotWindow plotWindow_;

    /**
     * Constructor.
     *
     * @param  parent  parent component
     * @param  plotType   type of plot window to open
     * @param  tableLabel   label for unloaded table as presented to user
     * @param  isVisibleOnPlot   if true, the plot window is forcibly
     *                           set visible every time the plot is displayed
     */
    public TablePlotDisplay( Component parent, PlotWindowType plotType,
                             String tableLabel, boolean isVisibleOnPlot ) {
        parent_ = parent;
        plotType_ = plotType;
        tableLabel_ = tableLabel;
        isVisibleOnPlot_ = isVisibleOnPlot;
        tablesModel_ = new ExtraTablesListModel( ControlWindow.getInstance()
                                                .getTablesListModel() );
    }

    /**
     * Displays a plot window with access to a given unloaded table.
     * If a plot window already exists, it is reused and the table is
     * inserted into it as a replacement for the one used last time.
     *
     * @param  table  table to display
     */
    public void showPlotWindow( StarTable table ) {
        TopcatModel tcModel =
            TopcatModel.createUnloadedTopcatModel( table, tableLabel_ );

        /* If this display hasn't been activated before,
         * create a suitable plot window, and initialise it to plot
         * data from the activated table. */
        if ( plotWindow_ == null ) {
            tablesModel_.purgeExtrasExcept( null ); // shouldn't be necessary
            tablesModel_.addExtra( tcModel );
            plotWindow_ = plotType_.createWindow( parent_, tablesModel_ );
            if ( tableLabel_ != null ) {
                plotWindow_.setTitle( plotWindow_.getTitle()
                                    + " (" + tableLabel_ + ")" );
            }
            Control dfltControl = plotWindow_.getControlManager()
                                 .createDefaultControl( tcModel );
            if ( dfltControl != null ) {
                plotWindow_.getControlStack().addControl( dfltControl );
            }
            plotWindow_.setVisible( true );
        }

        /* Otherwise, take the existing plot window and make sure that
         * whichever layer controls are currently configured to use
         * the most recently plotted table are reset to use the
         * newly plotted table. */
        else {
            TopcatModel tcModel0 = tablesModel_.getExtra0();
            tablesModel_.addExtra( tcModel );
            if ( tcModel0 != null ) {
                for ( LayerControl control :
                     plotWindow_.getControlStack().getStackModel()
                                .getLayerControls( false ) ) {
                    TablesListComboBox tsel = control.getTableSelector();
                    if ( tsel != null &&
                         tcModel0.equals( tsel.getSelectedItem() ) ) {
                        tsel.setSelectedItem( tcModel );
                    }
                }
            }
            tablesModel_.purgeExtrasExcept( tcModel );
            if ( isVisibleOnPlot_ || ! plotWindow_.isVisible() ) {
                plotWindow_.setVisible( true );
            }
        }
    }

    /**
     * Returns the plot window type for this display.
     *
     * @return  plot window type
     */
    public PlotWindowType getPlotWindowType() {
        return plotType_;
    }

    /**
     * Returns the plot window currently in use by this display.
     * If no display has so far been made, this may be null.
     *
     * @return   plot window, may be null
     */
    public StackPlotWindow getWindow() {
        return plotWindow_;
    }

    /**
     * ListModel<TopcatModel> implementation that combines a base model
     * and a new model that can contain extra TableModels.
     */
    private static class ExtraTablesListModel
                         extends ListModel2
                         implements TypedListModel<TopcatModel> {
        private final DefaultListModel extraModel_;

        /**
         * Constructor.
         *
         * @param  baseModel  main list model for tables
         */
        ExtraTablesListModel( TypedListModel<TopcatModel> baseModel ) {
            super( new DefaultListModel(), baseModel );
            extraModel_ = (DefaultListModel) getModel1();
        }

        @Override
        public TopcatModel getElementAt( int ix ) {
            return (TopcatModel) super.getElementAt( ix );
        }

        /**
         * Returns the first entry in the extras list.
         *
         * @param  first extra table, or null if none present
         */
        TopcatModel getExtra0() {
            return extraModel_.getSize() > 0
                 ? (TopcatModel) extraModel_.getElementAt( 0 )
                 : null;
        }

        /**
         * Adds a table to the extras list.
         *
         * @param  tcModel  table to add
         */
        void addExtra( TopcatModel tcModel ) {
            extraModel_.addElement( tcModel );
        }

        /**
         * Removes all elements from the extras list except the supplied one.
         * Following this call, the list will contain a maximum of one element.
         *
         * @param  tcModel  table to keep
         */
        void purgeExtrasExcept( TopcatModel tcModel ) {
            for ( int i = extraModel_.getSize() - 1; i >= 0; i-- ) {
                Object item = extraModel_.getElementAt( i );
                if ( item != tcModel ) {
                    extraModel_.removeElementAt( i );
                }
            }
        }
    }
}
