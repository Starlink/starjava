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
public class ShapePlotter extends AbstractPlotter<ShapeStyle>
                          implements ShapeModePlotter {

    private final ShapeForm form_;
    private final ShapeMode mode_;

    /**
     * Constructs a ShapePlotter with a coord group determined from
     * its supplied form and mode.
     *
     * @param   name  plotter name
     * @param   form  shape determiner
     * @param   mode  colour determiner
     */
    public ShapePlotter( String name, ShapeForm form, ShapeMode mode ) {
        this( name, form, mode,
              CoordGroup
             .createCoordGroup( form.getBasicPositionCount(),
                                PlotUtil
                               .arrayConcat( form.getExtraCoords(),
                                             mode.getExtraCoords() ) ) );
    }

    /**
     * Constructs a ShapePlotter with a given coord group.
     *
     * @param   name  plotter name
     * @param   form  shape determiner
     * @param   mode  colour determiner
     * @param   cgrp  coordinate group
     */
    protected ShapePlotter( String name, ShapeForm form, ShapeMode mode,
                            CoordGroup cgrp ) {
        super( name, form.getFormIcon(), cgrp, mode.hasReports() );
        form_ = form;
        mode_ = mode;
    }

    public ShapeForm getForm() {
        return form_;
    }

    public ShapeMode getMode() {
        return mode_;
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

    public ConfigKey<?>[] getStyleKeys() {
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
        geom = form_.adjustGeom( geom );
        return outliner.canPaint( dataSpec )
             ? mode_.createLayer( this, form_, geom, dataSpec,
                                  outliner, stamper )
             : null;
    }

    /**
     * Returns the index into a dataspec used by this plotter at which the 
     * first of its ShapeMode's "extra" coordinates is found.
     *
     * @param  geom  data position coordinate description
     * @return  index of first mode-specific coordinate
     */
    public int getModeCoordsIndex( DataGeom geom ) {
        return ( geom == null ? 0 : geom.getPosCoords().length )
               * form_.getBasicPositionCount()
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
    public static ShapePlotter[] createShapePlotters( ShapeForm[] forms,
                                                      ShapeMode[] modes ) {
        int nf = forms.length;
        int nm = modes.length;
        ShapePlotter[] plotters = new ShapePlotter[ nf * nm ];
        int ip = 0;
        for ( int im = 0; im < nm; im++ ) {
            ShapeMode mode = modes[ im ];
            for ( int jf = 0; jf < nf; jf++ ) {
                ShapeForm form = forms[ jf ];
                String name = form.getFormName() + "-" + mode.getModeName();
                plotters[ ip++ ] = new ShapePlotter( name, form, mode );
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
}
