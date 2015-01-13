package uk.ac.starlink.ttools.plot2.layer;

import java.awt.Graphics;
import java.util.HashMap;
import java.util.Map;
import javax.swing.Icon;
import uk.ac.starlink.ttools.plot.Range;
import uk.ac.starlink.ttools.plot.Style;
import uk.ac.starlink.ttools.plot2.AuxScale;
import uk.ac.starlink.ttools.plot2.DataGeom;
import uk.ac.starlink.ttools.plot2.Decal;
import uk.ac.starlink.ttools.plot2.Drawing;
import uk.ac.starlink.ttools.plot2.LayerOpt;
import uk.ac.starlink.ttools.plot2.PlotLayer;
import uk.ac.starlink.ttools.plot2.Surface;
import uk.ac.starlink.ttools.plot2.data.CoordGroup;
import uk.ac.starlink.ttools.plot2.data.DataSpec;
import uk.ac.starlink.ttools.plot2.data.DataStore;
import uk.ac.starlink.ttools.plot2.paper.Paper;
import uk.ac.starlink.ttools.plot2.paper.PaperType;

/**
 * Abstract Plotter implementation that does all its painting using a Decal,
 * no Glyphs.
 *
 * @author   Mark Taylor
 * @since    15 Feb 2013
 */
public abstract class SimpleDecalPlotter<S extends Style>
                      extends AbstractPlotter<S> {

    /**
     * Constructor.
     *
     * @param   name   plotter name
     * @param   icon   plotter icon
     * @param   cgrp   coordinate group
     * @param   hasReports  whether plot reports are generated
     */
    protected SimpleDecalPlotter( String name, Icon icon, CoordGroup cgrp,
                                  boolean hasReports ) {
        super( name, icon, cgrp, hasReports );
    }

    public PlotLayer createLayer( final DataGeom geom, final DataSpec dataSpec,
                                  final S style ) {
        if ( dataSpec == null || style == null ) {
            return null;
        }
        else {
            final SimpleDecalPlotter<S> plotter = this;
            final boolean isOpaque = getLayerOpt( style ).isOpaque();
            return new AbstractPlotLayer( this, geom, dataSpec, style,
                                          getLayerOpt( style ) ) {
                public Drawing createDrawing( final Surface surface,
                                              Map<AuxScale,Range> auxRanges,
                                              final PaperType paperType ) {
                    return new UnplannedDrawing() {
                        protected void paintData( Paper paper,
                                                  final DataStore dataStore ) {
                            paperType.placeDecal( paper, new Decal() {
                                public void paintDecal( Graphics g ) {
                                    paintData2D( surface, dataStore,
                                                 geom, dataSpec, style, g,
                                                 paperType );
                                }
                                public boolean isOpaque() {
                                    return isOpaque;
                                }
                            } );
                        }
                    };
                }
            };
        }
    }

    /**
     * Indicates the layer optimisation options that apply to a given style.
     *
     * @param  style   plot style
     * @return  layer options
     */
    protected abstract LayerOpt getLayerOpt( S style );

    /**
     * Called during Decal painting to perform the actual plot.
     *
     * @param   surface  plot surface
     * @param   dataStore  data storage object
     * @param   geom  data geometry
     * @param   style  plot style
     * @param   g  graphics context
     * @param   paperType  paper type
     */
    protected abstract void paintData2D( Surface surface, DataStore dataStore,
                                         DataGeom geom, DataSpec dataSpec,
                                         S style, Graphics g,
                                         PaperType paperType );
}
