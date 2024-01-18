package uk.ac.starlink.topcat.plot2;

import javax.swing.ListModel;
import uk.ac.starlink.table.ValueInfo;
import uk.ac.starlink.topcat.ColumnDataComboBoxModel;
import uk.ac.starlink.topcat.TopcatModel;
import uk.ac.starlink.ttools.plot2.LegendEntry;
import uk.ac.starlink.ttools.plot2.config.Specifier;
import uk.ac.starlink.ttools.plot2.layer.SpectrogramPlotter;

/**
 * LayerControl for plotting spectrograms.
 *
 * @author   Mark Taylor
 * @since    15 Aug 2013
 */
public class SpectrogramLayerControl extends BasicCoordLayerControl {

    private final SpectrogramPlotter plotter_;

    /**
     * Constructor.
     *
     * @param  plotter  spectrogram plotter
     * @param  tablesModel  list of available tables
     * @param  zsel    zone id specifier, may be null for single-zone plots
     * @param   baseConfigger   provides global configuration info
     */
    public SpectrogramLayerControl( SpectrogramPlotter plotter,
                                    ListModel<TopcatModel> tablesModel,
                                    Specifier<ZoneId> zsel,
                                    Configger baseConfigger ) {
        super( plotter, zsel,
               new SimplePositionCoordPanel( plotter.getCoordGroup()
                                                    .getExtraCoords(),
                                             null ),
               tablesModel, baseConfigger, false );
        plotter_ = plotter;
        assert plotter.getCoordGroup().getBasicPositionCount() == 0;
    }

    /**
     * It's difficult to know how to represent a spectrogram in a legend,
     * and it's probably not necessary.  The current implementation
     * just returns an empty array.
     */
    public LegendEntry[] getLegendEntries() {
        return new LegendEntry[ 0 ];
    }

    @Override
    protected void tableChanged( TopcatModel tcModel ) {
        super.tableChanged( tcModel );
        SimplePositionCoordPanel coordPanel =
            (SimplePositionCoordPanel) getCoordPanel();

        /* Fix default xExtent value to null, since it gives reasonable
         * results. */
        coordPanel.getColumnSelector( plotter_.getExtentCoordIndex(), 0 )
                  .setSelectedItem( null );

        /* Replace the default column selector for spectrum.
         * The default picks any column [with content class descended
         * from Object], because that's how the ValueInfo is described.
         * But we really only want columns with numeric vector values. */
        if ( tcModel != null ) {
            coordPanel
           .setColumnSelector( plotter_.getSpectrumCoordIndex(), 0,
                               createSpectrumColumnSelector( tcModel ) );
        }
    }

    /**
     * Returns a column selector that will only allow selection of
     * columns suitable for use as spectra.  That means ones with
     * non-tiny numeric array values.
     *
     * @param   tcModel   topcat model
     * @return  spectrum column selector model for tcModel
     */
    private static ColumnDataComboBoxModel
            createSpectrumColumnSelector( TopcatModel tcModel ) {
        ColumnDataComboBoxModel.Filter vectorFilter =
                new ColumnDataComboBoxModel.Filter() {
            public boolean acceptColumn( ValueInfo info ) {
                Class<?> clazz = info.getContentClass();
                int[] shape = info.isArray() ? info.getShape() : null;
                boolean isNumericArray =
                       double[].class.isAssignableFrom( clazz )
                    || float[].class.isAssignableFrom( clazz )
                    || long[].class.isAssignableFrom( clazz )
                    || int[].class.isAssignableFrom( clazz )
                    || short[].class.isAssignableFrom( clazz );
                boolean is1d = shape != null && shape.length == 1;
                boolean isVerySmall = is1d && shape[ 0 ] > 0 && shape[ 0 ] < 4;
                return isNumericArray && is1d && ! isVerySmall;
            }
        };
        return new ColumnDataComboBoxModel( tcModel, vectorFilter,
                                            true, false );
    }
}
