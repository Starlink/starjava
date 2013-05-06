package uk.ac.starlink.ttools.plot2.layer;

import uk.ac.starlink.ttools.plot.Style;
import uk.ac.starlink.ttools.plot2.DataGeom;
import uk.ac.starlink.ttools.plot2.PlotLayer;
import uk.ac.starlink.ttools.plot2.PlotUtil;
import uk.ac.starlink.ttools.plot2.config.ConfigException;
import uk.ac.starlink.ttools.plot2.config.ConfigKey;
import uk.ac.starlink.ttools.plot2.config.ConfigMap;
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
public class ShapePlotter extends TuplePlotter<ShapeStyle>
                          implements ModePlotter<ShapeStyle> {
    private final ShapeForm form_;
    private final ShapeMode mode_;

    /**
     * Constructor.
     *
     * @param   form  shape determiner
     * @param   mode  colour determiner
     */
    public ShapePlotter( ShapeForm form, ShapeMode mode ) {
        super( form.getFormName() + "-" + mode.getModeName(),
               form.getFormIcon(),
               PlotUtil.arrayConcat( form.getExtraCoords(),
                                     mode.getExtraCoords() ) );
        form_ = form;
        mode_ = mode;
    }

    public Form getForm() {
        return form_;
    }

    public Mode getMode(){
        return mode_;
    }

    public ConfigKey[] getStyleKeys() {
        return PlotUtil.arrayConcat( form_.getConfigKeys(),
                                     mode_.getConfigKeys() );
    }

    public ShapeStyle createStyle( ConfigMap config ) throws ConfigException {
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
        return geom.getPosCoords().length
             + form_.getExtraCoords().length;
    }

    /**
     * Creates an array of ShapePlotters, using all combinations of the
     * specified list of ShapeForms and ShapeModes.
     *
     * @param  forms  array of shape forms
     * @param  modes  array of shape modes
     * @return <code>forms.length*modes.length</code>-element array of plotters
     */
    public static ShapePlotter[] createShapePlotters( ShapeForm[] forms,
                                                      ShapeMode[] modes ) {
        int nf = forms.length;
        int nm = modes.length;
        ShapePlotter[] plotters = new ShapePlotter[ nf * nm ];
        int ip = 0;
        for ( int im = 0; im < nm; im++ ) {
            for ( int jf = 0; jf < nf; jf++ ) {
                @SuppressWarnings("unchecked")
                ShapePlotter p = new ShapePlotter( forms[ jf ], modes[ im ] );
                plotters[ ip++ ] = p;
            }
        }
        assert ip == plotters.length;
        return plotters;
    }
}
