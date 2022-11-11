package uk.ac.starlink.ttools.plot2.geom;

import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Insets;
import java.awt.Rectangle;
import java.util.ArrayList;
import java.util.List;
import uk.ac.starlink.ttools.plot2.Caption;
import uk.ac.starlink.ttools.plot2.Captioner;

/**
 * Implementation class containing SkyAxisLabeller implementations.
 *
 * @author   Mark Taylor
 * @since    21 Feb 2013
 */
public class SkyAxisLabellers {

    /**
     * Private constructor prevents instantiation.
     */
    private SkyAxisLabellers() {
    }

    /**
     * Turns a sky grid line label into a Caption.
     * Some manipulation of the text for better LaTeX rendering is done.
     *
     * @param  label  grid line label
     * @return  caption object
     */
    public static Caption labelCaption( String label ) {
        return Caption.createCaption(
            label,
            txt -> txt.replaceAll( "([hms])", "^{\\\\text{$1}}" )
        );
    }

    /** Labeller implementation that does no drawing. */
    public static final SkyAxisLabeller NONE = new SkyAxisLabeller() {
        public String getLabellerName() {
            return "None";
        }
        public String getLabellerDescription() {
            return "Axes are not labelled";
        }
        public AxisAnnotation createAxisAnnotation( GridLiner gridLiner,
                                                    Captioner captioner,
                                                    SkySys skySys ) {
            return new AxisAnnotation() {
                public Insets getPadding( boolean withScroll ) {
                    return new Insets( 0, 0, 0, 0 );
                }
                public void drawLabels( Graphics g ) {
                }
            };
        }
    };

    /**
     * Basic labeller implementation.  Grid lines are drawn OK,
     * but not much effort is made to position axis labels sensibly.
     */
    public static final SkyAxisLabeller LAME = new SkyAxisLabeller() {
        public String getLabellerName() {
            return "Basic";
        }
        public String getLabellerDescription() {
            return "Labels are drawn somewhere near the grid line";
        }
        public AxisAnnotation createAxisAnnotation( final GridLiner gridLiner,
                                                    final Captioner captioner,
                                                    final SkySys skySys ) {
            return new AxisAnnotation() {
                public Insets getPadding( boolean withScroll ) {
                    return new Insets( 0, 0, 0, 0 );
                }
                public void drawLabels( Graphics g ) {
                    Graphics2D g2 = (Graphics2D) g;
                    double[][][] lines = gridLiner.getLines();
                    String[] labels = gridLiner.getLabels();
                    int nl = labels.length;
                    for ( int il = 0; il < nl; il++ ) {
                        double[][] line = lines[ il ];
                        String label = labels[ il ];
                        double[] seg0 = line[ 0 ];
                        double[] segN = line[ line.length - 1 ];
                        double px = seg0[ 0 ];
                        double py = seg0[ 1 ];
                        g2.translate( px, py );
                        captioner.drawCaption( labelCaption( label ), g2 );
                        g2.translate( -px, -py );
                    }
                }
            };
        }
    };

    /** Labeller implementation that draws labels outside the plot bounds. */
    public static final SkyAxisLabeller EXTERNAL =
        createExternalLabeller( "External",
                                "Labels are drawn outside the plot bounds",
                                false );

    /** Labeller implementation that draws labels inside the plot bounds. */
    public static final SkyAxisLabeller INTERNAL =
        createInternalLabeller( "Internal",
                                "Labels are drawn inside the plot bounds",
                                false );

    /** Labeller implementation like EXTERNAL but with sky system labels. */
    public static final SkyAxisLabeller EXTSYS =
        createExternalLabeller( "ExternalSys",
                                "Like <code>" + EXTERNAL.getLabellerName()
                              + "</code>, but the axes are also labelled "
                              + "with coordinate system axis names",
                                true );

    /** Labeller implementation like INTERNAL but with sky system labels. */
    public static final SkyAxisLabeller INTSYS =
        createInternalLabeller( "InternalSys",
                                "Like <code>" + INTERNAL.getLabellerName()
                              + "</code>, but the axes are also labelled "
                              + "with coordinate system axis names",
                                true );

    /**
     * Labeller implementation that draws labels outside the plot bounds
     * unless they don't appear, in which case it draws them inside.
     * Doesn't necessarily end up looking as sensible as it sounds.
     */ 
    public static final SkyAxisLabeller HYBRID =
            new TickSkyAxisLabeller( "Hybrid",
                                     "Grid lines are labelled outside the "
                                   + "plot bounds where possible, "
                                   + "but inside if they would otherwise "
                                   + "be invisible", false ) {
 
        protected SkyTick[] calculateTicks( double[][][] lines,
                                            Caption[] labels,
                                            Rectangle plotBounds ) {
            List<SkyTick> tickList = new ArrayList<SkyTick>();
            int nl = labels.length;
            for ( int il = 0; il < nl; il++ ) {
                Caption label = labels[ il ];
                double[][] line = lines[ il ];
                SkyTick tick = createExternalTick( label, line, plotBounds );
                if ( tick == null ) {
                    tick = createInternalTick( label, line );
                }
                if ( tick != null ) {
                    tickList.add( tick );
                }
            }
            return tickList.toArray( new SkyTick[ 0 ] );
        }
    };
 
    /**
     * Returns a list of the known SkyAxisLabeller instances.
     * The first element is null, which is interpreted as auto mode.
     *
     * @return  list of sky axis labellers
     */
    public static SkyAxisLabeller[] getKnownLabellers() {
        return new SkyAxisLabeller[] {
            null, EXTERNAL, INTERNAL, LAME, HYBRID, NONE, EXTSYS, INTSYS,
        };
    }

    /**
     * Returns the axis mode to use if choosing one automatically based
     * on plot characteristics.
     *
     * @param   skyFillsBounds  true if the sky region of the plane
     *                          fills all or most of the plotting region;
     *                          false if there are significant non-sky parts
     * @return   suitable axis labeller
     */
    public static SkyAxisLabeller getAutoLabeller( boolean skyFillsBounds ) {
        return skyFillsBounds ? EXTERNAL : INTERNAL;
    }

    /**
     * Creates a labeller that draws labels outside the plot bounds.
     *
     * @param  name  labeller name
     * @param  description  labeller description
     * @param  labelSys  true to add lon/lat coordinate label names
     * @return  new labeller
     */
    private static SkyAxisLabeller createExternalLabeller( String name,
                                                           String description,
                                                           boolean labelSys ) {
        return new TickSkyAxisLabeller( name, description, labelSys ) {
            protected SkyTick[] calculateTicks( double[][][] lines,
                                                Caption[] labels,
                                                Rectangle plotBounds ) {
                List<SkyTick> tickList = new ArrayList<SkyTick>();
                int nl = labels.length;
                for ( int il = 0; il < nl; il++ ) {
                    SkyTick tick =
                        createExternalTick( labels[ il ], lines[ il ],
                                            plotBounds );
                    if ( tick != null ) {
                        tickList.add( tick );
                    }
                }
                return tickList.toArray( new SkyTick[ 0 ] );
            }
        };
    }

    /**
     * Creates a labeller that draws labels inside the plot bounds.
     *
     * @param  name  labeller name
     * @param  description  labeller description
     * @param  labelSys  true to add lon/lat coordinate label names
     * @return  new labeller
     */
    private static SkyAxisLabeller createInternalLabeller( String name,
                                                           String description,
                                                           boolean labelSys ) {
        return new TickSkyAxisLabeller( name, description, labelSys ) {
            protected SkyTick[] calculateTicks( double[][][] lines,
                                                Caption[] labels,
                                                Rectangle plotBounds ) {
                List<SkyTick> tickList = new ArrayList<SkyTick>();
                int nl = labels.length;
                for ( int il = 0; il < nl; il++ ) {
                    SkyTick tick =
                        createInternalTick( labels[ il ], lines[ il ] );
                    if ( tick != null ) {
                        tickList.add( tick );
                    }
                }
                return tickList.toArray( new SkyTick[ 0 ] );
            }
        };
    }
}
