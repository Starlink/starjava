package uk.ac.starlink.ttools.plot2.task;

import uk.ac.starlink.task.ChoiceParameter;
import uk.ac.starlink.task.Environment;
import uk.ac.starlink.task.Parameter;
import uk.ac.starlink.task.TaskException;
import uk.ac.starlink.ttools.plot2.DataGeom;
import uk.ac.starlink.ttools.plot2.Padding;
import uk.ac.starlink.ttools.plot2.PlotType;
import uk.ac.starlink.ttools.plot2.data.Coord;
import uk.ac.starlink.ttools.plot2.data.Input;
import uk.ac.starlink.ttools.plot2.data.InputMeta;

/**
 * Aggregates some miscellaneous information required for a plot task
 * that may not be available until execution time.
 *
 * @author   Mark Taylor
 * @since    22 Aug 2014
 */
public abstract class PlotContext<P,A> {

    private final PlotType<P,A> plotType_;
    private final DataGeom[] exampleGeoms_;

    /** Name of the standard geometry parameter. */
    public static final String GEOM_PARAM_NAME = "geom";

    /**
     * Constructor.
     * Information that is not dependent on other variables
     * (environment, layer suffix) is specified here.
     *
     * @param  plotType  plot type
     * @param  exampleGeoms   example data geoms
     */
    protected PlotContext( PlotType<P,A> plotType, DataGeom[] exampleGeoms ) {
        plotType_ = plotType;
        exampleGeoms_ = exampleGeoms;
    }

    /**
     * Returns the plot type.
     *
     * @return  plot type
     */
    public PlotType<P,A> getPlotType() {
        return plotType_;
    }

    /**
     * Returns a list of one or more DataGeom objects to be used for
     * example purposes.  These may be used to construct parameter
     * auto-documentation, which is needed in absence of an execution
     * environment.
     * The first item in the list is considered most important.
     *
     * @return  one or more example data geoms
     */
    public DataGeom[] getExampleGeoms() {
        return exampleGeoms_;
    }

    /**
     * Returns a parameter associated with a particular layer
     * required for determining DataGeom at runtime, if any.
     * The value class of the returned parameter may or may not be DataGeom.
     *
     * @param  layerSuffix  parameter suffix string identifying a plot layer
     * @return   parameter used for determining DataGeom, or null
     */
    public abstract Parameter<?> getGeomParameter( String layerSuffix );

    /**
     * Returns the DataGeom to use for a given layer in the context of a
     * given execution environment.
     *
     * @param  env  execution environment
     * @param  layerSuffix  parameter suffix string identifying a plot layer
     * @return  datageom
     */
    public abstract DataGeom getGeom( Environment env, String layerSuffix )
            throws TaskException;

    /**
     * Constructs a PlotContext which allows per-layer choice of DataGeom
     * between those known by a given plot type.
     * The choice is offered (a per-layer parameter is present) even if
     * only a single DataGeom is known by the PlotType.
     * This might conceivably be useful,
     * in that it allows pluggable DataGeoms specified by classname.
     *
     * @param  plotType  plot type
     * @return  standard plot context
     */
    public static <P,A> PlotContext<P,A>
            createStandardContext( final PlotType<P,A> plotType ) {
        final DataGeom[] geoms = plotType.getPointDataGeoms();
        return new PlotContext<P,A>( plotType, geoms ) {

            public DataGeom getGeom( Environment env, String suffix )
                    throws TaskException {
                return new ParameterFinder<Parameter<DataGeom>>() {
                    public Parameter<DataGeom> createParameter( String sfix ) {
                        return getGeomParameter( sfix );
                    }
                }.getParameter( env, suffix ).objectValue( env );
            }

            /**
             * Creates a DataGeom selection parameter named with a given suffix.
             *
             * @param  suffix  layer suffix
             * @return  parameter
             */
            public Parameter<DataGeom> getGeomParameter( String suffix ) {
                return new DataGeomParameter( GEOM_PARAM_NAME, suffix, geoms );
            }
        };
    }

    /**
     * Constructs a PlotContext which always uses a fixed given DataGeom.
     * No DataGeom-specific parameters are required or provided.
     *
     * @param  plotType  plot type
     * @param  geom   data geom used in all cases
     * @return  fixed-geom plot context
     */
    public static <P,A> PlotContext<P,A>
            createFixedContext( final PlotType<P,A> plotType,
                                final DataGeom geom ) {
        return new PlotContext<P,A>( plotType, new DataGeom[] { geom } ) {
            public Parameter<?> getGeomParameter( String suffix ) {
                return null;
            }
            public DataGeom getGeom( Environment env, String suffix ) {
                return geom;
            }
        };
    }

    /**
     * Parameter used for choosing between DataGeoms.
     */
    private static class DataGeomParameter extends ChoiceParameter<DataGeom> {

        /**
         * Constructor.
         *
         * @param  name  basic parameter name
         * @param  suffix  layer suffix
         * @param  geoms  list of known geom options;
         *                the first item is set as the parameter default
         */
        public DataGeomParameter( String name, String suffix,
                                  DataGeom[] geoms ) {
            super( name + suffix, geoms );
            setDefaultOption( geoms[ 0 ] );
            setPrompt( "Data geometry variant for layer " + suffix );
            StringBuffer sbuf = new StringBuffer();
            for ( DataGeom geom : geoms ) {
                sbuf.append( "<li>" )
                    .append( "<code>" )
                    .append( stringifyOption( geom ) )
                    .append( "</code>: " );
                int iin = 0;
                for ( Coord coord : geom.getPosCoords() ) {
                    for ( Input input : coord.getInputs() ) {
                        if ( iin++ > 0 ) {
                            sbuf.append( ", " );
                        }
                        InputMeta meta = input.getMeta();
                        sbuf.append( "<code>" )
                            .append( meta.getShortName() )
                            .append( suffix )
                            .append( "</code>" );
                        String desc = meta.getShortDescription();
                        if ( desc != null ) {
                            sbuf.append( " (" )
                                .append( desc )
                                .append( ")" );
                        }
                    }
                }
                sbuf.append( "</li>\n" );
            }
            String optsTxt = sbuf.toString();
            setDescription( new String[] {
                "<p>Selects the geometry for coordinates of data in layer",
                "<code>" + suffix + "</code>.",
                "This determines what parameters must be supplied",
                "to specify coordinate data for that layer.",
                "</p>",
                "<p>Options, with the (suffixed) coordinate parameters",
                "they require, are:",
                "<ul>",
                optsTxt,
                "</ul>",
                "</p>",
            } );
        }

        @Override
        public String stringifyOption( DataGeom geom ) {
            return geom.getVariantName().toLowerCase();
        }
    }
}
