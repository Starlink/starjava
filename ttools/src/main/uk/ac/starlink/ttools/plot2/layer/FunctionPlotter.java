package uk.ac.starlink.ttools.plot2.layer;

import gnu.jel.CompilationException;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.Stroke;
import java.awt.geom.Point2D;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import javax.swing.Icon;
import uk.ac.starlink.ttools.gui.ResourceIcon;
import uk.ac.starlink.ttools.jel.Constant;
import uk.ac.starlink.ttools.jel.JELFunction;
import uk.ac.starlink.ttools.plot.Style;
import uk.ac.starlink.ttools.plot2.AuxScale;
import uk.ac.starlink.ttools.plot2.DataGeom;
import uk.ac.starlink.ttools.plot2.Decal;
import uk.ac.starlink.ttools.plot2.Drawing;
import uk.ac.starlink.ttools.plot2.LayerOpt;
import uk.ac.starlink.ttools.plot2.PlotLayer;
import uk.ac.starlink.ttools.plot2.PlotUtil;
import uk.ac.starlink.ttools.plot2.Plotter;
import uk.ac.starlink.ttools.plot2.Span;
import uk.ac.starlink.ttools.plot2.Surface;
import uk.ac.starlink.ttools.plot2.config.BooleanConfigKey;
import uk.ac.starlink.ttools.plot2.config.ConfigException;
import uk.ac.starlink.ttools.plot2.config.ConfigKey;
import uk.ac.starlink.ttools.plot2.config.ConfigMap;
import uk.ac.starlink.ttools.plot2.config.ConfigMeta;
import uk.ac.starlink.ttools.plot2.config.OptionConfigKey;
import uk.ac.starlink.ttools.plot2.config.StringConfigKey;
import uk.ac.starlink.ttools.plot2.config.StyleKeys;
import uk.ac.starlink.ttools.plot2.data.DataSpec;
import uk.ac.starlink.ttools.plot2.data.DataStore;
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
public class FunctionPlotter extends
        AbstractPlotter<FunctionPlotter.FunctionStyle> {

    private final FuncAxis[] axes_;
    private Map<String,? extends Constant<?>> constMap_;
    private static final Pattern TOKEN_REGEXP =
        Pattern.compile( "[A-Za-z_][A-Za-z0-9_]*" );

    /**
     * Interval in pixels between samples on X/Y axis.
     * Possibly this should be a style parameter?
     */
    private static final double PIXEL_SPACING = 0.25;
 
    /** FunctionPlotter instance for a 2-d plotting surface.  */
    public static final FunctionPlotter PLANE =
            new FunctionPlotter( PlaneAxis.values() );

    /** Config key for the independent variable name. */
    public static final ConfigKey<String> XNAME_KEY =
        new StringConfigKey(
            new ConfigMeta( "xname", "Independent Variable Name" )
           .setStringUsage( "<name>" )
           .setShortDescription( "Independent variable name" )
           .setXmlDescription( new String[] {
                "<p>Name of the independent variable for use in the",
                "function expression.",
                "This is typically",
                "<code>x</code> for a horizontal independent variable and",
                "<code>y</code> for a vertical independent variable,",
                "but any string that is a legal expression language identifier",
                "(starts with a letter, continues with letters, numbers,",
                "underscores) can be used.",
                "</p>",
            } )
        , "x" );

    /** Config key for the function expression. */
    public static final ConfigKey<String> FEXPR_KEY =
        new StringConfigKey(
            new ConfigMeta( "fexpr", "Function Expression" )
           .setStringUsage( "<expr>" )
           .setShortDescription( "Expression for function" )
           .setXmlDescription( new String[] {
                "<p>An expression using TOPCAT's",
                "<ref id='jel'>expression language</ref>",
                "in terms of the independent variable",
                "to define the function.",
                "This expression must be standalone -",
                "it cannot reference any tables.",
                "</p>",
            } )
        , null );

    private final ConfigKey<FuncAxis> axisKey_;

    /**
     * Constructs a plotter with a given set of axis geometry options.
     *
     * @param  axes  options for function variable definitions
     */
    public FunctionPlotter( FuncAxis[] axes ) {
        super( "Function", ResourceIcon.PLOT_FUNCTION );
        axes_ = axes;
        axisKey_ = new OptionConfigKey<FuncAxis>(
                new ConfigMeta( "axis", "Independent Axis" )
               .setShortDescription( "Axis of independent variable" )
               .setXmlDescription( new String[] {
                    "<p>Which axis the independent variable varies along.",
                    "Options are currently",
                    "<code>" + PlaneAxis.X.getAxisName() + "</code> and",
                    "<code>" + PlaneAxis.Y.getAxisName() + "</code>.",
                    "</p>",
                } )
                , FuncAxis.class, axes_, axes_[ 0 ] ) {
            public String valueToString( FuncAxis axis ) {
                return axis == null ? null : axis.getAxisName();
            }
            public String getXmlDescription( FuncAxis axis ) {
                return null;
            }
        }.setOptionUsage();
    }

    /**
     * Sets a map from constant name to constant objects
     * for values that may be referenced by expressions plotted here.
     * The identity and/or content of this map may change over the
     * lifetime of this plotter.
     *
     * @param  constMap  map of JEL constants by name, may be null
     */
    public void setConstantMap( Map<String,? extends Constant<?>> constMap ) {
        constMap_ = constMap;
    }

    /**
     * Returns the map of constant objects 
     * for values that may be referenced by expressions plotted here.
     * The identity and/or content of this map may change over the
     * lifetime of this plotter.
     *
     * @return  map of JEL constants by name, may be null
     */
    public Map<String,? extends Constant<?>> getConstantMap() {
        return constMap_;
    }

    public String getPlotterDescription() {
        return PlotUtil.concatLines( new String[] {
            "<p>Plots an analytic function.",
            "This layer is currently only available for the Plane plots",
            "(including histogram).",
            "</p>",
        } );
    }

    public ConfigKey<?>[] getStyleKeys() {
        List<ConfigKey<?>> list = new ArrayList<ConfigKey<?>>();
        list.addAll( Arrays.asList( getFunctionStyleKeys() ) );
        list.add( StyleKeys.COLOR );
        list.addAll( Arrays.asList( StyleKeys.getStrokeKeys() ) );
        list.add( StyleKeys.ANTIALIAS );
        return list.toArray( new ConfigKey<?>[ 0 ] );
    }

    /**
     * Returns the subset of the style keys which defines the function
     * itself.
     *
     * @return   style keys for function definition
     */
    public ConfigKey<?>[] getFunctionStyleKeys() {
        return new ConfigKey<?>[] {
            axisKey_,
            XNAME_KEY,
            FEXPR_KEY,
        };
    }

    public FunctionStyle createStyle( ConfigMap config )
            throws ConfigException {
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
            jelfunc = new JELFunction( xname, fexpr, constMap_ );
        }
        catch ( CompilationException e ) {
            throw new ConfigException( FEXPR_KEY,
                                       "Bad expression \"" + fexpr + "\": "
                                     + e.getMessage(), e );
        }
        FuncAxis axis = config.get( axisKey_ );
        Color color = config.get( StyleKeys.COLOR );
        Stroke stroke = StyleKeys.createStroke( config, BasicStroke.CAP_ROUND,
                                                BasicStroke.JOIN_ROUND );
        boolean antialias = config.get( StyleKeys.ANTIALIAS );
        return new FunctionStyle( color, stroke, antialias, jelfunc, axis );
    }

    public PlotLayer createLayer( DataGeom geom, DataSpec dataSpec,
                                  final FunctionStyle style ) {
        if ( style == null ) {
            return null;
        }
        else {
            LayerOpt opt = new LayerOpt( style.getColor(), true );
            return new AbstractPlotLayer( this, null, null, style, opt ) {
                public Drawing createDrawing( Surface surface,
                                              Map<AuxScale,Span> auxSpans,
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
    public static class FunctionStyle extends LineStyle {
        private final JELFunction function_;
        private final Object functionId_;
        private final FuncAxis axis_;

        /**
         * Constructor.
         *
         * @param  color   line colour
         * @param  stroke  line stroke
         * @param   antialias  true to draw line antialiased
         * @param  function  analytic function definition
         * @param   axis  axis geometry
         */
        public FunctionStyle( Color color, Stroke stroke, boolean antialias,
                              JELFunction function, FuncAxis axis ) {
            super( color, stroke, antialias );
            function_ = function;
            functionId_ = Arrays.asList( new String[] {
                function_.getXVarName(),
                function_.getExpression(),
                Arrays.stream( function_.getReferencedConstants() )
                      .map( Constant::getValue )
                      .map( String::valueOf )
                      .collect( Collectors.joining( "," ) ),
            } );
            axis_ = axis;
        }

        @Override
        public String toString() {
            return function_.getExpression();
        }

        @Override
        public boolean equals( Object o ) {
            if ( o instanceof FunctionStyle ) {
                FunctionStyle other = (FunctionStyle) o;
                return super.equals( o )
                    && this.functionId_.equals( other.functionId_ )
                    && this.axis_.equals( other.axis_ );
            }
            else {
                return false;
            }
        }

        @Override
        public int hashCode() {
            int code = super.hashCode();
            code = 23 * code + functionId_.hashCode();
            code = 23 * code + axis_.hashCode();
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
                    return ! style_.getAntialias();
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
            JELFunction function = style_.function_;
            FuncAxis axis = style_.axis_;
            double[] xs = axis.getXValues( surface_ );
            int np = xs.length;
            LineTracer tracer =
                style_.createLineTracer( g2, surface_.getPlotBounds(), np,
                                         paperType_.isBitmap() );
            Color color = style_.getColor();
            Point2D.Double gpos = new Point2D.Double();
            double[] dpos = new double[ surface_.getDataDimCount() ];
            for ( int ip = 0; ip < np; ip++ ) {
                double x = xs[ ip ];
                double f = function.evaluate( x );
                if ( Double.isNaN( f ) ) {
                    tracer.flush();
                }
                else if ( axis.xfToData( surface_, x, f, dpos ) &&
                     surface_.dataToGraphics( dpos, false, gpos ) &&
                     PlotUtil.isPointReal( gpos ) ) {
                    tracer.addVertex( gpos.x, gpos.y, color );
                }
            }
            tracer.flush();
        }
    }

    /**
     * FuncAxis implementations for 2-d plots.
     */
    private enum PlaneAxis implements FuncAxis {

        /** Independent variable is X coordinate. */
        X( "Horizontal" ) {
            public double[] getXValues( Surface surface ) {
                Rectangle plotBounds = surface.getPlotBounds();
                int gxlo = plotBounds.x - 1;
                int gxhi = plotBounds.x + plotBounds.width + 1;
                int np = (int) ( ( gxhi - gxlo ) / PIXEL_SPACING );
                double[] xs = new double[ np ];
                Point2D.Double gpos = new Point2D.Double( gxlo, plotBounds.y );
                for ( int ip = 0; ip < np; ip++ ) {
                    xs[ ip ] = surface.graphicsToData( gpos, null )[ 0 ];
                    gpos.x += PIXEL_SPACING;
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
                Rectangle plotBounds = surface.getPlotBounds();
                int gylo = plotBounds.y - 1;
                int gyhi = plotBounds.y + plotBounds.height + 1;
                int np = (int) ( ( gyhi - gylo ) / PIXEL_SPACING );
                double[] ys = new double[ np ];
                Point2D.Double gpos = new Point2D.Double( plotBounds.x, gylo );
                for ( int ip = 0; ip < np; ip++ ) {
                    ys[ ip ] = surface.graphicsToData( gpos, null )[ 1 ];
                    gpos.y += PIXEL_SPACING;
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
   //           Rectangle plotBounds = psurf.getPlotBounds();
   //           Point origin = new double[ 2 ];
   //           surface.dataToGraphics( new double[] { 0, 0 }, false, origin );
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
