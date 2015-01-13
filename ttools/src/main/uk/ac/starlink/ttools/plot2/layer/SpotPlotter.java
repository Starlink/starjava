package uk.ac.starlink.ttools.plot2.layer;

import java.awt.Color;
import java.awt.Component;
import java.awt.Graphics;
import java.awt.Rectangle;
import java.awt.geom.Point2D;
import javax.swing.Icon;
import java.util.Map;
import uk.ac.starlink.ttools.plot.Range;
import uk.ac.starlink.ttools.plot.Style;
import uk.ac.starlink.ttools.plot2.AuxScale;
import uk.ac.starlink.ttools.plot2.DataGeom;
import uk.ac.starlink.ttools.plot2.Drawing;
import uk.ac.starlink.ttools.plot2.Glyph;
import uk.ac.starlink.ttools.plot2.LayerOpt;
import uk.ac.starlink.ttools.plot2.Pixer;
import uk.ac.starlink.ttools.plot2.PlotLayer;
import uk.ac.starlink.ttools.plot2.PlotUtil;
import uk.ac.starlink.ttools.plot2.Surface;
import uk.ac.starlink.ttools.plot2.config.ConfigKey;
import uk.ac.starlink.ttools.plot2.config.ConfigMap;
import uk.ac.starlink.ttools.plot2.config.StyleKeys;
import uk.ac.starlink.ttools.plot2.data.Coord;
import uk.ac.starlink.ttools.plot2.data.DataSpec;
import uk.ac.starlink.ttools.plot2.data.DataStore;
import uk.ac.starlink.ttools.plot2.data.TupleSequence;
import uk.ac.starlink.ttools.plot2.geom.CubeSurface;
import uk.ac.starlink.ttools.plot2.paper.Paper;
import uk.ac.starlink.ttools.plot2.paper.PaperType;
import uk.ac.starlink.ttools.plot2.paper.PaperType2D;
import uk.ac.starlink.ttools.plot2.paper.PaperType3D;

/**
 * Fairly minimal example plotter implementation.
 * It paints a fixed-size marker, for which the only style configuration
 * option is colour.
 * Admittedly, it's not all that simple.
 *
 * <p>This implements most of the required interfaces more or less from
 * scratch to show what's going on.  
 * Reusing infrastructure from some other existing plotters
 * (e.g. subclassing {@link ShapeForm}) may be more sensible
 * where appropriate.
 *
 * @author   Mark Taylor
 * @since    17 Feb 2013
 */
public class SpotPlotter extends AbstractPlotter<SpotPlotter.SpotStyle> {

    /**
     * Constructor.
     */
    public SpotPlotter() {
        super( "Spot", createSpotIcon( Color.RED ), 1, new Coord[ 0 ] );
    }

    public String getPlotterDescription() {
        return PlotUtil.concatLines( new String[] {
            "<p>Plots a fixed sized marker at each data point.",
            "This is a minimal plotter implementation,",
            "intended as an implementation example.",
            "More capable plotters exist which do the same thing.",
            "</p>",
        } );
    }

    public ConfigKey[] getStyleKeys() {
        return new ConfigKey[] {
            StyleKeys.COLOR,
        };
    }

    public SpotStyle createStyle( ConfigMap config ) {
        return new SpotStyle( config.get( StyleKeys.COLOR ) );
    }

    public PlotLayer createLayer( final DataGeom geom,
                                  final DataSpec dataSpec,
                                  final SpotStyle style ) {
        LayerOpt opt = new LayerOpt( style.getColor(), true );
        return new AbstractPlotLayer( this, geom, dataSpec, style, opt ) {
            public Drawing createDrawing( final Surface surface,
                                          Map<AuxScale,Range> auxRanges,
                                          PaperType paperType ) {
                if ( paperType instanceof PaperType2D ) {
                    final PaperType2D ptype = (PaperType2D) paperType;
                    return new UnplannedDrawing() {
                        protected void paintData( Paper paper,
                                                  DataStore dataStore ) {
                            paintSpots2D( style, surface, geom, dataSpec,
                                          dataStore, ptype, paper );
                        }
                    };
                }
                else if ( paperType instanceof PaperType3D ) {
                    final PaperType3D ptype = (PaperType3D) paperType;
                    final CubeSurface surf = (CubeSurface) surface;
                    return new UnplannedDrawing() {
                        protected void paintData( Paper paper,
                                                  DataStore dataStore ) {
                            paintSpots3D( style, surf, geom, dataSpec,
                                          dataStore, ptype, paper );
                        }
                    };
                }
                else {
                    throw new IllegalArgumentException( "paper type" );
                }
            }
        };
    }

    /**
     * Does the work for plotting on a 2D surface.
     *
     * @param  style  plot style
     * @param  surface  plot surface
     * @param  geom  coordinage geometry
     * @param  spec  specifies data coordinates
     * @param  dataStore  stores data
     * @param  paperType  2D paper type
     * @param  paper   paper appropriate for paperType
     */
    private void paintSpots2D( SpotStyle style, Surface surface,
                               DataGeom geom, DataSpec dataSpec,
                               DataStore dataStore,
                               PaperType2D paperType, Paper paper ) {
        Glyph spotGlyph = createSpotGlyph();
        Color spotColor = style.color_;
        int icPos = getCoordGroup().getPosCoordIndex( 0, geom );
        double[] dpos = new double[ surface.getDataDimCount() ];
        Point2D.Double gp = new Point2D.Double();
        TupleSequence tseq = dataStore.getTupleSequence( dataSpec );
        while ( tseq.next() ) {
            if ( geom.readDataPos( tseq, icPos, dpos ) &&
                 surface.dataToGraphics( dpos, true, gp ) ) {
                paperType.placeGlyph( paper, gp.x, gp.y, spotGlyph, spotColor );
            }
        }
    }

    /**
     * Does the work for plotting on a 3D surface.
     *
     * @param  style  plot style
     * @param  surface  plot surface
     * @param  geom  coordinage geometry
     * @param  spec  specifies data coordinates
     * @param  dataStore  stores data
     * @param  paperType  3D paper type
     * @param  paper   paper appropriate for paperType
     */
    private void paintSpots3D( SpotStyle style, CubeSurface surface,
                               DataGeom geom, DataSpec dataSpec,
                               DataStore dataStore,
                               PaperType3D paperType, Paper paper ) {
        Glyph spotGlyph = createSpotGlyph();
        Color spotColor = style.getColor();
        int icPos = getCoordGroup().getPosCoordIndex( 0, geom );
        double[] dpos = new double[ surface.getDataDimCount() ];
        Point2D.Double gp = new Point2D.Double();
        double[] dz = new double[ 1 ];
        TupleSequence tseq = dataStore.getTupleSequence( dataSpec );
        while ( tseq.next() ) {
            if ( geom.readDataPos( tseq, icPos, dpos ) &&
                 surface.dataToGraphicZ( dpos, true, gp, dz ) ) {
                paperType.placeGlyph( paper, gp.x, gp.y, dz[ 0 ],
                                      spotGlyph, spotColor );
            }
        }
    }

    /**
     * Does the actual painting for the spots this plotter uses to
     * mark data points.  Paints in the current foreground colour.
     *
     * @param   g  graphics context
     * @param  x  X coordinate of spot centre
     * @param  y  Y coordinate of spot centre
     */
    private static void paintSpotShape( Graphics g, int x, int y ) {
        g.fillRect( x - 2, y - 2, 4, 4 );
    }

    /**
     * Adapts spot drawing into an Icon.
     *
     * @param   color   icon colour
     * @returnn  new style icon
     */
    private static Icon createSpotIcon( final Color color ) {
        return new Icon() {
            public int getIconWidth() {
                return 4;
            }
            public int getIconHeight() {
                return 4;
            }
            public void paintIcon( Component c, Graphics g, int x, int y ) {
                Color color0 = g.getColor();
                g.setColor( color );
                paintSpotShape( g, x, y );
            }
        };
    }

    /**
     * Adapts spot drawing into a Glyph.
     *
     * @return  new spot glyph
     */
    private static Glyph createSpotGlyph() {
        final int np = 9;
        final int[] xs = new int[ np ];
        final int[] ys = new int[ np ];
        int ip = 0;
        for ( int ix = -1; ix < 2; ix++ ) {
            for ( int iy = -1; iy < 2; iy++ ) {
                xs[ ip ] = ix;
                ys[ ip ] = iy;
                ip++;
            }
        }
        return new Glyph() {
            public void paintGlyph( Graphics g ) {
                g.fillRect( -1, -1, 3, 3 );
            }
            public Pixer createPixer( Rectangle clip ) {
                return Pixers.createArrayPixer( xs, ys, np );
            }
        };
    }

    /**
     * Style implementation for spots.
     * Only colour is configurable.
     */
    public static class SpotStyle implements Style {
        final Color color_;

        /**
         * Constructor.
         *
         * @param  color  spot colour
         */
        SpotStyle( Color color ) {
            color_ = color;
        }
        public Color getColor() {
            return color_;
        }
        public Icon getLegendIcon() {
            return createSpotIcon( color_ );
        }
        public boolean equals( Object o ) {
            return o instanceof SpotStyle
                && this.color_.equals( ((SpotStyle) o).color_ );
        }
        public int hashCode() {
            return color_.hashCode();
        }
    } 
}
