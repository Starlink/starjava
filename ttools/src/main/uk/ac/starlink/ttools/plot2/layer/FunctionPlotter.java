package uk.ac.starlink.ttools.plot2.layer;

import gnu.jel.CompilationException;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.Stroke;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;
import javax.swing.Icon;
import uk.ac.starlink.ttools.jel.JELFunction;
import uk.ac.starlink.ttools.plot.MarkShape;
import uk.ac.starlink.ttools.plot.MarkStyle;
import uk.ac.starlink.ttools.plot.Range;
import uk.ac.starlink.ttools.plot.Style;
import uk.ac.starlink.ttools.plot2.AuxScale;
import uk.ac.starlink.ttools.plot2.DataGeom;
import uk.ac.starlink.ttools.plot2.Decal;
import uk.ac.starlink.ttools.plot2.Drawing;
import uk.ac.starlink.ttools.plot2.LayerOpt;
import uk.ac.starlink.ttools.plot2.PlotLayer;
import uk.ac.starlink.ttools.plot2.PlotUtil;
import uk.ac.starlink.ttools.plot2.Plotter;
import uk.ac.starlink.ttools.plot2.Surface;
import uk.ac.starlink.ttools.plot2.config.BooleanConfigKey;
import uk.ac.starlink.ttools.plot2.config.ConfigException;
import uk.ac.starlink.ttools.plot2.config.ConfigKey;
import uk.ac.starlink.ttools.plot2.config.ConfigMap;
import uk.ac.starlink.ttools.plot2.config.ConfigMeta;
import uk.ac.starlink.ttools.plot2.config.OptionConfigKey;
import uk.ac.starlink.ttools.plot2.config.StringConfigKey;
import uk.ac.starlink.ttools.plot2.config.StyleKeys;
import uk.ac.starlink.ttools.plot2.data.Coord;
import uk.ac.starlink.ttools.plot2.data.DataSpec;
import uk.ac.starlink.ttools.plot2.data.DataStore;
import uk.ac.starlink.ttools.plot2.geom.PlaneSurface;
import uk.ac.starlink.ttools.plot2.paper.Paper;
import uk.ac.starlink.ttools.plot2.paper.PaperType;

/**
 * Dataless plotter that plots an analytic function.
 * JEL expressions are supplied to define the function.
 * The geometry is parameterised by the {@link FuncAxis} interface,
 * which in principle allows functions of any geometric variable
 * to be specified.
 *
 * @author   Mark Taylor
 * @since    15 Feb 2013
 */
public class FunctionPlotter implements Plotter<FunctionPlotter.FunctionStyle> {

    private final FuncAxis[] axes_;
    private static final Pattern TOKEN_REGEXP =
        Pattern.compile( "[A-Za-z_][A-Za-z0-9_]*" );
 
    /** FunctionPlotter instance for a 2-d plotting surface.  */
    public static final FunctionPlotter PLANE =
            new FunctionPlotter( PlaneAxis.values() );

    private static final ConfigKey<String> XNAME_KEY =
        new StringConfigKey( new ConfigMeta( "xname",
                                             "Independent Variable Name" ),
                             "x" );
    private static final ConfigKey<String> FEXPR_KEY =
        new StringConfigKey( new ConfigMeta( "fexpr", "Function Expression" ),
                             null );
    private static final ConfigKey<Boolean> ANTIALIAS_KEY =
        new BooleanConfigKey( new ConfigMeta( "antialias", "Antialiasing" ),
                              false );
    private final ConfigKey<FuncAxis> axisKey_;

    /**
     * Constructs a plotter with a given set of axis geometry options.
     *
     * @param  axes  options for function variable definitions
     */
    protected FunctionPlotter( FuncAxis[] axes ) {
        axes_ = axes;
        axisKey_ = new OptionConfigKey<FuncAxis>(
                       new ConfigMeta( "axis", "Independent Axis" ),
                                       FuncAxis.class, axes_, axes_[ 0 ] ) {
            public String valueToString( FuncAxis axis ) {
                return axis.getAxisName();
            }
        };
    }

    public String getPlotterName() {
        return "Function";
    }

    public Icon getPlotterIcon() {
        return PlotUtil.icon( "fx2.gif" );
    }

    public boolean hasPosition() {
        return false;
    }

    public Coord[] getExtraCoords() {
        return new Coord[ 0 ];
    }

    public ConfigKey[] getStyleKeys() {
        return new ConfigKey[] {
            axisKey_,
            XNAME_KEY,
            FEXPR_KEY,
            StyleKeys.COLOR,
            StyleKeys.THICKNESS,
            StyleKeys.DASH,
            ANTIALIAS_KEY,
        };
    }

    public FunctionStyle createStyle( ConfigMap config ) {
        String xname = config.get( XNAME_KEY );   
        String fexpr = config.get( FEXPR_KEY );
        if ( xname == null || xname.trim().length() == 0 ||
             fexpr == null || fexpr.trim().length() == 0 ) {
            return null;
        }
        if ( ! TOKEN_REGEXP.matcher( xname ).matches() ) {
            throw new ConfigException( XNAME_KEY,
                                       "Bad variable name \"" + xname + "\"" );
        }
        JELFunction jelfunc;
        try {
            jelfunc = new JELFunction( xname, fexpr );
        }
        catch ( CompilationException e ) {
            throw new ConfigException( FEXPR_KEY,
                                       "Bad expression \"" + fexpr + "\": "
                                     + e.getMessage(), e );
        }
        FuncAxis axis = config.get( axisKey_ );
        Color color = config.get( StyleKeys.COLOR );
        int thickness = config.get( StyleKeys.THICKNESS );
        float[] dash = config.get( StyleKeys.DASH );
        MarkStyle mstyle = MarkShape.POINT.getStyle( color, 0 );
        mstyle.setHidePoints( true );
        mstyle.setLine( MarkStyle.DOT_TO_DOT );
        mstyle.setLineWidth( thickness );
        mstyle.setDash( dash );
        boolean antialias = config.get( ANTIALIAS_KEY );
        return new FunctionStyle( jelfunc, axis, mstyle, antialias );
    }

    public PlotLayer createLayer( DataGeom geom, DataSpec dataSpec,
                                  final FunctionStyle style ) {
        if ( style == null ) {
            return null;
        }
        else {
            LayerOpt opt = new LayerOpt( style.markStyle_.getColor(), true );
            return new AbstractPlotLayer( this, null, null, style, opt ) {
                public Drawing createDrawing( Surface surface,
                                              Map<AuxScale,Range> auxRanges,
                                              PaperType paperType ) {
                    return new FunctionDrawing( style, surface, paperType );
                }
            };
        }
    }

    /**
     * Style class associated with this plotter.
     * The style includes the actual function definitions as well as
     * the usual things like colour, line thickness etc.
     */
    public static class FunctionStyle implements Style {
        private final JELFunction function_;
        private final FuncAxis axis_;
        private final MarkStyle markStyle_;
        private final boolean antialias_;
        private final Object functionId_;

        /**
         * Constructor.
         *
         * @param  function  analytic function definition
         * @param   axis  axis geometry
         * @param  markStyle  line style
         * @param   antialias  true to draw line antialiased
         */
        FunctionStyle( JELFunction function, FuncAxis axis,
                       MarkStyle markStyle, boolean antialias ) {
            function_ = function;
            axis_ = axis;
            markStyle_ = markStyle;
            antialias_ = antialias;
            functionId_ = Arrays.asList( new String[] {
                function_.getXVarName(),
                function_.getExpression(),
            } );
        }

        public Icon getLegendIcon() {
            return markStyle_.getLegendIcon();
        }

        @Override
        public String toString() {
            return function_.getExpression();
        }

        @Override
        public boolean equals( Object o ) {
            if ( o instanceof FunctionStyle ) {
                FunctionStyle other = (FunctionStyle) o;
                return this.functionId_.equals( other.functionId_ )
                    && this.axis_.equals( other.axis_ )
                    && this.markStyle_.equals( other.markStyle_ )
                    && this.antialias_ == other.antialias_;
            }
            else {
                return false;
            }
        }

        @Override
        public int hashCode() {
            int code = 23991;
            code = 23 * code + functionId_.hashCode();
            code = 23 * code + axis_.hashCode();
            code = 23 * code + markStyle_.hashCode();
            code = 23 * code + ( antialias_ ? 7 : 11 );
            return code;
        }
    }

    /**
     * Defines the geometric role of the dependent and independent variables
     * for a function.
     */
    public interface FuncAxis {

        /**
         * Returns the name of the geometry type for this object.
         * Typically this will be the name of the axis along which
         * the independent variable runs, if there is one.
         *
         * @return  function geometry name
         */
        String getAxisName();

        /**
         * Returns the values of the independent variable at which the
         * function must be evaluated for a given plot surface.
         * The drawn line will consist of a line drawn through the
         * function evaluations at these values of the independent variable.
         * The result should usually include some values a bit off the
         * edge of the visible part of the surface if applicable
         * so that the line goes right to the edge of the clipped region.
         *
         * @param  surface  plot surface on which function will be drawn
         * @return  array of independent variable values
         */
        double[] getXValues( Surface surface );

        /**
         * Converts the result of a function evaluation to a position
         * in the data space of the plot.
         *
         * @param  surface  plotting surface
         * @param  x  independent variable
         * @param  f  dependent variable
         * @param  dataPos  nDataDim-element array to receive data position
         *                  corresponding to <code>x</code>,<code>f(x)</code>
         * @return  true iff a valid data position resulted
         */
        boolean xfToData( Surface surface, double x, double f,
                          double[] dataPos );
    }

    /**
     * Drawing implementation for function plotter.
     *
     * <p>This could be implemented as a planned drawing, where the
     * plan consists of the Points to plot.
     * But since the calculation time (probably) only scales with the plot
     * image size not the size of a data set, it's probably not worth it.
     */
    private static class FunctionDrawing extends UnplannedDrawing {
        private final FunctionStyle style_;
        private final Surface surface_;
        private final PaperType paperType_;

        /**
         * Constructor.
         *
         * @param  style  style
         * @param  surface  plot surface
         * @param  paperType  paper type
         */
        FunctionDrawing( FunctionStyle style, Surface surface,
                         PaperType paperType ) {
            style_ = style;
            surface_ = surface;
            paperType_ = paperType;
        }

        protected void paintData( Paper paper, DataStore dataStore ) {
            paperType_.placeDecal( paper, new Decal() {
                public void paintDecal( Graphics g ) {
                    paintFunction( (Graphics2D) g );
                }
                public boolean isOpaque() {
                    return ! style_.antialias_;
                }
            } );
        }

        /**
         * Does the actual drawing of the function onto a given
         * graphics context.
         *
         * @param  g2  graphics context
         */
        private void paintFunction( Graphics2D g2 ) {
            Rectangle plotBounds = surface_.getPlotBounds();
            int gxlo = plotBounds.x;
            int gxhi = plotBounds.x + plotBounds.width;
            int gylo = plotBounds.y;
            int gyhi = plotBounds.y + plotBounds.height;
            Color color0 = g2.getColor();
            Stroke stroke0 = g2.getStroke();
            RenderingHints hints0 = g2.getRenderingHints();
            MarkStyle markStyle = style_.markStyle_;
            g2.setColor( markStyle.getColor() );
            g2.setStroke( markStyle.getStroke( BasicStroke.CAP_ROUND,
                                               BasicStroke.JOIN_ROUND ) );
            if ( style_.antialias_ ) {
                g2.setRenderingHint( RenderingHints.KEY_ANTIALIASING,
                                     RenderingHints.VALUE_ANTIALIAS_ON );
            }

            JELFunction function = style_.function_;
            FuncAxis axis = style_.axis_;
            double[] xs = axis.getXValues( surface_ );
            int np = xs.length;
            Point gpos = new Point();
            double[] dpos = new double[ surface_.getDataDimCount() ];
            List<Point> plist = new ArrayList<Point>();
            boolean lastInside = false;
            // I should probably use a PathIterator here to get floating
            // point coords.
            for ( int ip = 0; ip < np; ip++ ) {
                double x = xs[ ip ];
                double f = function.evaluate( x );
                if ( axis.xfToData( surface_, x, f, dpos ) &&
                     surface_.dataToGraphics( dpos, false, gpos ) ) {
                    boolean inside = plotBounds.contains( gpos );
                    if ( inside ) {
                        plist.add( new Point( gpos ) ); 
                    }
                    else {
                        int px = Math.min( gxhi, Math.max( gxlo, gpos.x ) );
                        int py = Math.min( gyhi, Math.max( gylo, gpos.y ) );
                        if ( lastInside ) {
                            plist.add( new Point( px, py ) );
                            plotPoints( g2, plist );
                            plist.clear();
                        }
                        else {
                            plist.clear();
                            plist.add( new Point( px, py ) );
                        }
                    }
                    lastInside = inside;
                }
                else {
                    plotPoints( g2, plist );
                    plist.clear();
                }
            }
            if ( lastInside ) {
                plotPoints( g2, plist );
            }
            g2.setColor( color0 );
            g2.setStroke( stroke0 );
            g2.setRenderingHints( hints0 );
        }

        /**
         * Plots a polyline between a list of graphics points.
         *
         * @param   g2  graphics context
         * @return  pointList  list of points
         */
        private void plotPoints( Graphics2D g2, List<Point> pointList ) {
            int np = pointList.size();
            if ( np > 0 ) {
                int[] gxs = new int[ np ];
                int[] gys = new int[ np ];
                for ( int ip = 0; ip < np; ip++ ) {
                    Point point = pointList.get( ip );
                    gxs[ ip ] = point.x;
                    gys[ ip ] = point.y;
                }
                g2.drawPolyline( gxs, gys, np );
            }
        }
    }

    /**
     * FuncAxis implementations for 2-d plots.
     */
    private enum PlaneAxis implements FuncAxis {

        /** Independent variable is X coordinate. */
        X( "Horizontal" ) {
            public double[] getXValues( Surface surface ) {
                PlaneSurface psurf = (PlaneSurface) surface;
                Rectangle plotBounds = psurf.getPlotBounds();
                int gxlo = plotBounds.x - 1;
                int gxhi = plotBounds.x + plotBounds.width + 1;
                Point gpos = new Point( 0, plotBounds.y );
                double[] xs = new double[ gxhi - gxlo ];
                for ( int i = 0; i < gxhi - gxlo; i++ ) {
                    gpos.x = gxlo + i;
                    xs[ i ] = psurf.graphicsToData( gpos, null )[ 0 ];
                }
                return xs;
            }

            public boolean xfToData( Surface surface, double x, double f,
                                     double[] dpos ) {
                dpos[ 0 ] = x;
                dpos[ 1 ] = f;
                return true;
            }
        },

        /** Independent variable is Y coordinate. */
        Y( "Vertical" ) {
            public double[] getXValues( Surface surface ) {
                PlaneSurface psurf = (PlaneSurface) surface;
                Rectangle plotBounds = psurf.getPlotBounds();
                int gylo = plotBounds.y - 1;
                int gyhi = plotBounds.y + plotBounds.height + 1;
                Point gpos = new Point( plotBounds.x, 0 );
                double[] ys = new double[ gyhi - gylo ];
                for ( int i = 0; i < gyhi - gylo; i++ ) {
                    gpos.y = gylo + i;
                    ys[ i ] = psurf.graphicsToData( gpos, null )[ 1 ];
                }
                return ys;
            }

            public boolean xfToData( Surface surface, double x, double f,
                                     double[] dpos ) {
                dpos[ 1 ] = x;
                dpos[ 0 ] = f;
                return true;
            }
        };

   //   THETA( "Rotation" ) {
   //       public double[] getXValues( Surface surface ) {
   //           PlaneSurface psurf = (PlaneSurface) surface;
   //           Rectangle plotBounds = psurf.getPlotBounds();
   //           Point origin = new double[ 2 ];
   //           psurf.dataToGraphics( new double[] { 0, 0 }, false, origin );
   //    fiddly bit here: try to work out sensible theta positions by
   //    looking at the corners of the bounds in relation to positio of
   //    the space origin.  Careful of log axes.
   //       }
   //       public boolean xfToData( Surface surface, double fx, double fy,
   //                                double[] dpos ) {
   //           dpos[ 0 ] = fy * Math.cos( fx );
   //           dpos[ 1 ] = fy * Math.sin( fx );
   //           return true;
   //       }
   //   },

        private final String name_;

        /**
         * Constructor.
         *
         * @param  name axis name
         */
        PlaneAxis( String name ) {
            name_ = name;
        }

        public String getAxisName() {
            return name_;
        }
    }
}
