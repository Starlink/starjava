package uk.ac.starlink.ttools.plot2.layer;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import javax.swing.Icon;
import uk.ac.starlink.ttools.plot2.PlotUtil;
import uk.ac.starlink.ttools.plot2.config.ConfigException;
import uk.ac.starlink.ttools.plot2.config.ConfigKey;
import uk.ac.starlink.ttools.plot2.config.ConfigMap;
import uk.ac.starlink.ttools.plot2.config.ConfigMeta;
import uk.ac.starlink.ttools.plot2.config.DoubleConfigKey;
import uk.ac.starlink.ttools.plot2.config.MultiPointConfigKey;
import uk.ac.starlink.ttools.plot2.config.OptionConfigKey;
import uk.ac.starlink.ttools.plot2.config.SliderSpecifier;
import uk.ac.starlink.ttools.plot2.config.Specifier;
import uk.ac.starlink.ttools.plot2.data.FloatingCoord;

/**
 * MultiPointForm with scaling options that make sense for plotting
 * markers on the sky with potentially absolute angular extents.
 *
 * @author   Mark Taylor
 * @since    2 Aug 2017
 */
public class SkyMultiPointForm extends MultiPointForm {

    private static final String UNIT_SHORTNAME = "unit";

    /** Config key for angular extent scaling. */
    public static final ConfigKey<Double> SCALE_KEY = createScaleKey();

    /** Config key for selecting angular extent units. */
    public static final ConfigKey<AngleUnit> UNIT_KEY =
        createUnitKey( "marker" );

    /**
     * Constructor.
     *
     * @param  name  shapeform name
     * @param  icon  shapeform icon
     * @param  description   XML description
     * @param  extraCoordSet   defines the extra positional coordinates
     *                         used to plot multipoint shapes
     * @param  rendererKey   config key for the renderer, defining the
     *                       plotted shape;
     *                       any renderer specified must be
     *                       expecting data corresponding
     *                       to the extraCoordSet parameter
     */
    public SkyMultiPointForm( String name, Icon icon, String description,
                              MultiPointCoordSet extraCoordSet,
                              MultiPointConfigKey rendererKey ) {
        super( name, icon, description, extraCoordSet, rendererKey,
               new ConfigKey[] { UNIT_KEY, SCALE_KEY } );
    }

    protected double getScaleFactor( ConfigMap config ) {
        AngleUnit angleUnit = config.get( UNIT_KEY );
        double unit = angleUnit == null ? Double.NaN
                                        : angleUnit.getValueInDegrees();
        double factor = config.get( SCALE_KEY ).doubleValue();
        return factor * ( Double.isNaN( unit ) ? 1.0 : unit );
    }

    protected boolean isAutoscale( ConfigMap config ) {
        return config.get( UNIT_KEY ) == null;
    }

    /**
     * Returns a sentence of XML text describing the units of a scaled
     * coordinate are specified.  This is suitable for inclusion in the
     * description of the relevant FloatingCoord.
     *
     * @return   description text
     */
    public static String getCoordUnitText() {
        return new StringBuffer()
            .append( "The units of this angular extent are determined by the " )
            .append( "<code>" )
            .append( UNIT_KEY.getMeta().getShortName() )
            .append( "</code>" )
            .append( " option." )
            .toString();
    }

    /**
     * Returns XML text suitable for inclusion in a MultiPointForm description
     * explaining how the scaling of marker sizes is controlled.
     *
     * @param   scaledCoords  coordinates that will be scaled by the
     *                        scale and unit config options
     * @param   shapename   human-readable name of the shape being plotted
     *                      by this form
     * @return  description text &lt;p&gt; element
     */
    public static String getScalingDescription( FloatingCoord[] scaledCoords,
                                                String shapename ) {
        StringBuffer cbuf = new StringBuffer();
        int nc = scaledCoords.length;
        for ( int ic = 0; ic < nc; ic++ ) {
            if ( ic == nc - 1 ) {
                cbuf.append( " and " );
            }
            else if ( ic > 0 ) {
                cbuf.append( ", " );
            }
            cbuf.append( "<code>" )
                .append( scaledCoords[ ic ]
                        .getInput().getMeta().getShortName() )
                .append( "</code>" );
        }
        String coordsTxt = cbuf.toString();
        return PlotUtil.concatLines( new String[] {
            "<p>The dimensions of the plotted " + shapename + "s",
            "are given by the",
            coordsTxt,
            "coordinates.",
            "The units of these values are specified using the",
            "<code>" + UNIT_KEY.getMeta().getShortName() + "</code> option.",
            "If only the relative rather than the absolute sizes",
            "are required on the plot,",
            "or if the units are not known,",
            "the special value",
            "<code>" + UNIT_KEY.getMeta().getShortName() + "="
                     + UNIT_KEY.valueToString( null ) + "</code>",
            "may be used;",
            "this applies a non-physical scaling factor",
            "to make the " + shapename + "s appear at some reasonable size",
            "in the plot.",
            "When <code>" + UNIT_KEY.getMeta().getShortName() + "="
                          + UNIT_KEY.valueToString( null ) + "</code>",
            ( UNIT_KEY.getDefaultValue() == null ? " (the default)" : "" ),
            shapename + "s will keep approximately the same screen size",
            "during zoom operations;",
            "when one of the angular units is chosen, they will keep",
            "the same size in data coordinates.",
            "</p>",
            "<p>Additionally, the",
            "<code>" + SCALE_KEY + "</code> option",
            "may be used to scale all the plotted " + shapename + "s",
            "by a given factor to make them all larger or smaller.",
            "</p>",
        } );
    }

    /**
     * Constructs the config key used for defining angular extent units.
     *
     * @param  shapename  user-readable term used in documentation to
     *                    describe what is plotted with angular extent
     * @return  new key
     */
    private static ConfigKey<AngleUnit> createUnitKey( String shapename ) {
        final String SCALED_OPT = "scaled";
        ConfigMeta meta = new ConfigMeta( UNIT_SHORTNAME, "Unit" );
        meta.setXmlDescription( new String[] {
            "<p>Defines the units in which the angular extents are specified.",
            "Options are degrees, arcseconds etc.",
            "If the special value <code>" + SCALED_OPT + "</code> is given",
            "then a non-physical scaling is applied to the",
            "input values to make the the largest " + shapename + "s",
            "appear at a reasonable size (a few tens of pixels)",
            "in the plot.",
            "</p>",
            "<p>Note that the actual plotted size of the " + shapename + "s",
            "can also be scaled using the",
            "<code>" + SCALE_KEY.getMeta().getShortName() + "</code> option;",
            "these two work together to determine the actual plotted sizes.",
            "</p>",
        } );
        List<AngleUnit> optList = new ArrayList<AngleUnit>();
        optList.add( null );
        optList.addAll( Arrays.asList( AngleUnit.values() ) );
        AngleUnit[] opts = optList.toArray( new AngleUnit[ 0 ] );
        OptionConfigKey<AngleUnit> key =
                new OptionConfigKey<AngleUnit>( meta, AngleUnit.class, opts,
                                                AngleUnit.DEGREE ) {
            public String getXmlDescription( AngleUnit unit ) {
                return unit == null
                     ? PlotUtil.concatLines( new String[] {
                           "a non-physical scaling is applied",
                           "based on the size of values present",
                       } )
                     : unit.getFullName();
            }
            @Override
            public String valueToString( AngleUnit unit ) {
                return unit == null ? SCALED_OPT : super.valueToString( unit );
            }
            @Override
            public AngleUnit stringToValue( String str )
                    throws ConfigException {
                AngleUnit unit = AngleUnit.getNamedUnit( str );
                if ( unit != null ) {
                    return unit;
                }
                else if ( SCALED_OPT.equalsIgnoreCase( str ) || str == null ) {
                    return null;
                }
                else {
                    throw new ConfigException( this, "No known unit: " + str );
                }
            }
        };
        key.setOptionUsage();
        key.addOptionsXml();
        return key;
    }

    /**
     * Constructs the config key used for unconditional scaling
     * of angualar extents.
     *
     * @return   new key
     */
    private static ConfigKey<Double> createScaleKey() {
        ConfigMeta meta = new ConfigMeta( "scale", "Scale" );
        meta.setStringUsage( "<number>" );
        meta.setShortDescription( "Size multiplier" );
        meta.setXmlDescription( new String[] {
            "<p>Scales the size of variable-sized markers",
            "like vectors and ellipses.",
            "The default value is 1, smaller or larger values",
            "multiply the visible sizes accordingly.",
            "</p>",
            "<p>The main purpose of this option is to tweak",
            "the visible sizes of the plotted markers for better visibility.",
            "The <code>" + UNIT_SHORTNAME + "</code> option",
            "is more convenient to account for the units in which the",
            "angular extent coordinates are supplied.",
            "If the markers are supposed to be plotted with their",
            "absolute angular extents visible, this option should be set",
            "to its default value of 1.",
            "</p>",
        } );
        return new DoubleConfigKey( meta, 1.0 ) {
            public Specifier<Double> createSpecifier() {
                return new SliderSpecifier( 1e-4, 1e+4, true, 1.0, false,
                                            SliderSpecifier.TextOption
                                                           .ENTER_ECHO );
            }
        };
    }
}
