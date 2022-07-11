package uk.ac.starlink.ttools.plot2.config;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.util.Hashtable;
import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.Box;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSlider;
import javax.swing.SwingConstants;
import uk.ac.starlink.ttools.gui.ResourceIcon;
import uk.ac.starlink.ttools.plot2.PlotUtil;
import uk.ac.starlink.ttools.plot2.ReportMap;
import uk.ac.starlink.ttools.plot2.Subrange;

/**
 * Config key that specifies a Subrange.
 *
 * @author   Mark Taylor
 * @since    23 Feb 2013
 */
public class SubrangeConfigKey extends ConfigKey<Subrange> {

    private final double vmin_;
    private final double vmax_;

    /**
     * Constructs a key with a given default.
     * The <code>vmin</code> and <code>vmax</code> parameters
     * do not impose any hard limits on the value associated with this key, 
     * but they influence the values offered by the Specifier component.
     *
     * @param   meta  metadata
     * @param   dflt  default subrange
     * @param   vmin  minimum value suggested by GUI
     * @param   vmax  maximum value suggested by GUI
     */
    public SubrangeConfigKey( ConfigMeta meta, Subrange dflt,
                              double vmin, double vmax ) {
        super( meta, Subrange.class, dflt );
        vmin_ = vmin;
        vmax_ = vmax;
    }

    /**
     * Constructs a key with the usual default.
     * The default subrange covers the whole range 0..1.
     * 
     * @param  meta  metadata
     */
    public SubrangeConfigKey( ConfigMeta meta ) {
        this( meta, new Subrange(), 0, 1 );
    }

    public String valueToString( Subrange value ) {
        return format( value.getLow(), 3 ) + "," + format( value.getHigh(), 3 );
    }

    public Subrange stringToValue( String txt ) throws ConfigException {
        String[] limits = txt.split( ",", -1 );
        if ( limits.length == 2 ) {
            String slo = limits[ 0 ].trim();
            String shi = limits[ 1 ].trim();
            try {
                double lo = slo.length() > 0 ? Double.parseDouble( slo ) : 0;
                double hi = shi.length() > 0 ? Double.parseDouble( shi ) : 1;
                if ( lo <= hi ) {
                    return new Subrange( lo, hi );
                }
                else {
                    throw new ConfigException( this, "lo <= hi violated" );
                }
            }
            catch ( NumberFormatException e ) {
                throw new ConfigException( this,
                                           "Bad number(s): \"" + slo + "\","
                                                       + " \"" + shi + "\"" );
            }
        }
        else {
            throw new ConfigException( this,
                                       "Should be two numbers "
                                     + "separated by a comma" );
        }
    }

    public Specifier<Subrange> createSpecifier() {
        return new SubrangeSpecifier( vmin_, vmax_ );
    }

    /**
     * Formats a subrange limit number for display.
     *
     * @param  dval  value
     * @param  nf  number of significant figures
     * @return  formatted value
     */
    private static String format( double dval, int nf ) {
        int m10 = (int) Math.round( Math.pow( 10, nf ) );
        int mf = (int) Math.round( dval * m10 );
        if ( mf == 0 ) {
            return "0";
        }
        else if ( mf == m10 ) {
            return "1";
        }
        else {
            return PlotUtil.formatNumber( dval, "0.0", nf );
        }
    }

    /**
     * Specifier that uses a double slider component.
     */
    private static class SubrangeSpecifier extends SpecifierPanel<Subrange> {
        private final double rmin_;
        private final double rmax_;
        private final JSlider slider_;
        private final JButton resetButton_;
        private static final int MIN = 0;
        private static final int MAX = 10000;

        /**
         * Constructor.
         */
        SubrangeSpecifier( double rmin, double rmax ) {
            super( true );
            rmin_ = rmin;
            rmax_ = rmax;
            slider_ = RangeSliderUtil.createRangeSlider( MIN, MAX );
            slider_.addChangeListener( getChangeForwarder() );
            boolean showLabels = false;
            if ( ! ( rmin == 0 && rmax == 1 ) ) {
                if ( showLabels ) {
                    Hashtable<Integer,JComponent> labels = new Hashtable<>();
                    labels.put( unscale( 0.0 ), new JLabel( "0" ) );
                    labels.put( unscale( 1.0 ), new JLabel( "1" ) );
                    slider_.setLabelTable( labels );
                    slider_.setPaintLabels( true );
                }
                Action resetAct = new AbstractAction( null, ResourceIcon.ZERO) {
                    public void actionPerformed( ActionEvent evt ) {
                        setSpecifiedValue( new Subrange() );
                    }
                };
                resetButton_ = new JButton( resetAct );
                resetButton_.setMargin( new Insets( 0, 0, 0, 0 ) );
            }
            else {
                resetButton_ = null;
            }
        }

        protected JComponent createComponent() {
            JComponent line = Box.createHorizontalBox();
            line.add( slider_ );
            if ( resetButton_ != null ) {
                line.add( resetButton_ );
            }
            line.addPropertyChangeListener( "enabled", evt -> {
                boolean isEnabled = line.isEnabled();
                slider_.setEnabled( isEnabled );
                if ( resetButton_ != null ) {
                    resetButton_.setEnabled( isEnabled );
                }
            } );
            return line;
        }

        public Subrange getSpecifiedValue() {
            int[] range = RangeSliderUtil.getSliderRange( slider_ );
            int ilo = range[ 0 ];
            int ihi = range[ 1 ];

            /* Don't return a zero range. */
            if ( ilo == ihi ) {
                int quantum = getQuantum();
                if ( ihi == MAX ) {
                    ilo = Math.max( MIN, ilo - quantum );
                }
                else {
                    ihi = Math.min( MAX, ihi + quantum );
                }
            }
            return new Subrange( scale( ilo ), scale( ihi ) );
        }

        public void setSpecifiedValue( Subrange subrange ) {
            RangeSliderUtil.setSliderRange( slider_,
                                            unscale( subrange.getLow() ),
                                            unscale( subrange.getHigh() ) );
        }

        public void submitReport( ReportMap report ) {
        }

        /**
         * Returns a small subrange value that can be used instead of zero
         * if the two slider handles are on top of each other.
         *
         * @return   slider range interval roughly equivalent to one pixel
         */
        private int getQuantum() {
            int npix = slider_.getOrientation() == SwingConstants.VERTICAL
                     ? slider_.getHeight()
                     : slider_.getWidth();
            npix = Math.max( 10, Math.min( 10000, npix ) );
            return Math.max( 1, ( MAX - MIN ) / npix );
        }

        private double scale( int ival ) {
            double p01 = ( ival - MIN ) / (double) ( MAX - MIN );
            return rmin_ + p01 * ( rmax_ - rmin_ );
        }

        private int unscale( double dval ) {
            double p01 = ( dval - rmin_ ) / ( rmax_ - rmin_ );
            return (int) Math.round( p01 * ( MAX - MIN ) ) + MIN;
        }
    }

    /**
     * Returns a metadata object to describe a SubrangeConfigKey for use
     * with a coordinate axis.
     *
     * @param  axname  abbreviated axis name (for CLI)
     * @param  axisName  full axis name (for GUI)
     * @return   metadata object describing a subrange config key for an axis
     */
    public static ConfigMeta createAxisSubMeta( String axname,
                                                String axisName ) {
        ConfigMeta meta =
            new ConfigMeta( axname + "sub",
                            ConfigMeta.capitalise( axisName ) + " Subrange" );
        meta.setStringUsage( "<lo>,<hi>" );
        meta.setXmlDescription( new String[] {
            "<p>Defines a normalised adjustment to the data range of the",
            axisName + " axis.",
            "The value may be specified as a comma-separated pair",
            "of two numbers,",
            "giving the lower and upper bounds of the range of",
            "of interest respectively.",
            "This sub-range is applied to the data range that would",
            "otherwise be used, either automatically calculated",
            "or explicitly supplied;",
            "zero corresponds to the lower bound and one to the upper.",
            "</p>",
            "<p>The default value \"<code>0,1</code>\" therefore has",
            "no effect.",
            "The range could be restricted to its lower half",
            "with the value <code>0,0.5</code>.",
            "</p>",
        } );
        return meta;
    }

    /**
     * Returns a metadaa object to describe a SubrangeConfigKey for use
     * as a restriction on a colour ramp (Shader).
     *
     * @param  axname  abbreviated axis name (for CLI)
     * @param  axisName  full axis name (for GUI)
     * @return   metadata object describing a subrange config key for a shader
     */
    public static ConfigMeta createShaderClipMeta( String axname,
                                                   String axisName ) {
        ConfigMeta meta = new ConfigMeta( axname + "clip", "Shader Clip" );
        meta.setStringUsage( "<lo>,<hi>" );
        meta.setShortDescription( axisName + " shader clip range" );
        meta.setXmlDescription( new String[] {
            "<p>Defines a subrange of the colour ramp to be used for",
            axisName + " shading.",
            "The value is specified as a (low,high) comma-separated pair",
            "of two numbers between 0 and 1.",
            "</p>",
            "<p>If the full range <code>0,1</code> is used,",
            "the whole range of colours specified by the selected",
            "shader will be used.",
            "But if for instance a value of <code>0,0.5</code> is given,",
            "only those colours at the left hand end of the ramp",
            "will be seen.",
            "</p>",
        } );
        return meta;
    }
}
