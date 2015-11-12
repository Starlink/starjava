package uk.ac.starlink.ttools.plot2.layer;

import uk.ac.starlink.ttools.plot.Style;
import uk.ac.starlink.ttools.plot2.DataGeom;
import uk.ac.starlink.ttools.plot2.PlotLayer;
import uk.ac.starlink.ttools.plot2.PlotUtil;
import uk.ac.starlink.ttools.plot2.config.ConfigException;
import uk.ac.starlink.ttools.plot2.config.ConfigKey;
import uk.ac.starlink.ttools.plot2.config.ConfigMap;
import uk.ac.starlink.ttools.plot2.data.CoordGroup;
import uk.ac.starlink.ttools.plot2.data.DataSpec;

/**
 * Plotter that plots shapes at each data point.
 * This is pretty flexible, and forms the basis for most of the plot types
 * available.
 *
 * <p>The shape plotted at each point is determined by the {@link ShapeForm}
 * and may be fixed (by Style) or parameterised by some other data coordinates.
 * The colouring for each shape may be fixed (by Style), or influenced by 
 * additional data coordinates and/or by the number of points plotted in
 * the same place (though the latter may also be implemented by the
 * PaperType).
 *
 * <p>The clever stuff is all in the ShapeForm and ShapeMode implementations.
 * This class just combines the characteristics of the two.
 * 
 * @author   Mark Taylor
 * @since    18 Feb 2013
 */
public class ShapePlotter extends AbstractPlotter<ShapeStyle> {

    private final ShapeForm form_;
    private final ShapeMode mode_;

    /**
     * Constructor.
     *
     * @param   name  plotter name
     * @param   form  shape determiner
     * @param   mode  colour determiner
     */
    public ShapePlotter( String name, ShapeForm form, ShapeMode mode ) {
        super( name,
               form.getFormIcon(),
               CoordGroup
              .createCoordGroup( form.getPositionCount(),
                                 PlotUtil
                                .arrayConcat( form.getExtraCoords(),
                                              mode.getExtraCoords() ) ),
               mode.hasReports() );
        form_ = form;
        mode_ = mode;
    }

    public String getPlotterDescription() {
        return PlotUtil.concatLines( new String[] {
            "<p><dl>",
            "<dt>Shape</dt>",
            "<dd>",
            form_.getFormDescription(),
            "</dd>",
            "<dt>Shading</dt>",
            "<dd>",
            mode_.getModeDescription(),
            "</dd>",
            "</dl></p>",
        } );
    }

    public ConfigKey[] getStyleKeys() {
        return PlotUtil.arrayConcat( form_.getConfigKeys(),
                                     mode_.getConfigKeys() );
    }

    public ShapeStyle createStyle( ConfigMap config ) {
        ShapeStyle style = new ShapeStyle( form_.createOutliner( config ),
                                           mode_.createStamper( config ) );
        assert style.equals( new ShapeStyle( form_.createOutliner( config ),
                                             mode_.createStamper( config ) ) );
        return style;
    }

    public PlotLayer createLayer( DataGeom geom, DataSpec dataSpec,
                                  ShapeStyle style ) {
        Outliner outliner = style.getOutliner();
        Stamper stamper = style.getStamper();
        return mode_.createLayer( this, form_, geom, dataSpec,
                                  outliner, stamper );
    }

    /**
     * Returns the index into a dataspec used by this plotter at which the 
     * first of its ShapeMode's "extra" coordinates is found.
     *
     * @param  geom  data position coordinate description
     * @return  index of first mode-specific coordinate
     */
    public int getModeCoordsIndex( DataGeom geom ) {
        return geom.getPosCoords().length * form_.getPositionCount()
             + form_.getExtraCoords().length;
    }

    /**
     * Creates an array of ShapeModePlotters, using all combinations of the
     * specified list of ShapeForms and ShapeModes.
     * Since these implement the {@link ModePlotter} interface,
     * other parts of the UI may be able to group them.
     *
     * @param  forms  array of shape forms
     * @param  modes  array of shape modes
     * @return <code>forms.length*modes.length</code>-element array of plotters
     */
    public static ShapeModePlotter[] createShapePlotters( ShapeForm[] forms,
                                                          ShapeMode[] modes ) {
        int nf = forms.length;
        int nm = modes.length;
        ShapeModePlotter[] plotters = new ShapeModePlotter[ nf * nm ];
        int ip = 0;
        for ( int im = 0; im < nm; im++ ) {
            ShapeMode mode = modes[ im ];
            for ( int jf = 0; jf < nf; jf++ ) {
                ShapeForm form = forms[ jf ];
                String name = form.getFormName() + "-" + mode.getModeName();
                @SuppressWarnings("unchecked")
                ShapeModePlotter p = new ShapeModePlotter( name, form, mode );
                plotters[ ip++ ] = p;
            }
        }
        assert ip == plotters.length;
        return plotters;
    }

    /**
     * Creates a single ShapePlotter using mode flat.  Suitable for
     * 2d plot types only.
     * The returned object is not a {@link ModePlotter}, so will not be
     * ganged together with other ShapePlotters.
     *
     * @param   form   shape form
     * @return   new 2d plotter
     */
    public static ShapePlotter createFlat2dPlotter( ShapeForm form ) {
        return new ShapePlotter( form.getFormName(), form, ShapeMode.FLAT2D );
    }

    /**
     * ShapePlotter subclass which additionally implements the ModePlotter
     * interface.
     */
    public static class ShapeModePlotter extends ShapePlotter
                                         implements ModePlotter<ShapeStyle> {
        private final ShapeForm sform_;
        private final ShapeMode smode_;

        /**
         * Constructor.
         *
         * @param   name  plotter name
         * @param   form  shape of markers
         * @param   mode  compositing mode
         */
        public ShapeModePlotter( String name, ShapeForm form, ShapeMode mode ) {
            super( name, form, mode );
            sform_ = form;
            smode_ = mode;
        }

        public ShapeForm getForm() {
            return sform_;
        }

        public ShapeMode getMode() {
            return smode_;
        }
    }
}
