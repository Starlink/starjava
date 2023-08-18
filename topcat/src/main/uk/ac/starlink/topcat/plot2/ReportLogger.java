package uk.ac.starlink.topcat.plot2;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import uk.ac.starlink.ttools.plot2.Ganger;
import uk.ac.starlink.ttools.plot2.PlotLayer;
import uk.ac.starlink.ttools.plot2.ReportKey;
import uk.ac.starlink.ttools.plot2.ReportMap;

/**
 * Accepts plot reports on behalf of a layer control and reports them
 * through the logging system.  This is not intended to be the primary
 * way that this information is conveyed to the user, but it's a functional
 * fallback/placeholder where a report submission GUI is not available.
 *
 * @author   Mark Taylor
 * @since    10 Dec 2014
 */
public class ReportLogger {

    private final Level level_;
    private final LayerControl control_;
    private Set<String> reportStrings_;
    private static final Logger logger_ =
        Logger.getLogger( "uk.ac.starlink.topcat.plot2" );

    /**
     * Constructs a logger with an explicit logging level.
     *
     * @param   control   layer control on behalf of which reports will be
     *                    dealt with
     * @param   level   logging level at which reports will be logged
     */
    public ReportLogger( LayerControl control, Level level ) {
        control_ = control;
        level_ = level;
        reportStrings_ = new HashSet<String>();
    }

    /**
     * Constructs a logger with a default logging level.
     *
     * @param   control   layer control on behalf of which reports will be
     *                    dealt with
     */
    public ReportLogger( LayerControl control ) {
        this( control, Level.WARNING );
    }

    /**
     * Issues logging messages relating to the reports of plot layers
     * generated with this object's layer control.
     *
     * @param   reports   plot reports
     * @param   ganger   ganger in effect when these reports were generated
     */
    public void submitReports( Map<LayerId,ReportMap> reports,
                               Ganger<?,?> ganger ) {
        if ( logger_.isLoggable( level_ ) ) {

            /* Only attempt any work for those map entries that correspond
             * to plot layers this object's control has issued. */
            Set<String> rstrings = new HashSet<>();
            for ( TopcatLayer tcLayer : control_.getLayers( ganger ) ) {
                if ( tcLayer.getPlotter().hasReports() ) {
                    for ( PlotLayer plotLayer : tcLayer.getPlotLayers() ) {
                        if ( plotLayer != null ) {
                            ReportMap report =
                                reports.get( LayerId.createLayerId(plotLayer) );
                            if ( report != null ) {
                                StringBuffer sbuf = new StringBuffer();

                                /* Only report items listed by the layer's
                                 * plotter.  Others are special-purpose items
                                 * not intended for general reporting
                                 * to the user. */
                                for ( ReportKey<?> key : report.keySet() ) {
                                    if ( key.isGeneralInterest() ) {
                                        Object value = report.get( key );
                                        if ( value != null ) {
                                            if ( sbuf.length() > 0 ) {
                                                sbuf.append( "; " );
                                            }
                                            sbuf.append( key.getMeta()
                                                        .getShortName() )
                                            .append( ": " )
                                            .append( value.toString() );
                                        }
                                    }
                                }

                                /* Write a message through the logging system
                                 * if there  is anything to say, and if
                                 * it's different from the last time
                                 * this method was called. */
                                if ( sbuf.length() > 0 ) {
                                    String text = sbuf.toString();
                                    rstrings.add( text );
                                    if ( ! reportStrings_.contains( text ) ) {
                                        logger_.log( level_, text );
                                    }
                                }
                            }
                        }
                    }
                }
            }
            reportStrings_ = rstrings;
        }
    }
}
