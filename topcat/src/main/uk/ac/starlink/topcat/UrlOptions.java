package uk.ac.starlink.topcat;

import java.awt.Component;
import java.awt.Window;
import java.io.IOException;
import java.net.URL;
import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;
import javax.swing.JFrame;
import javax.swing.SwingUtilities;
import org.astrogrid.samp.Message;
import uk.ac.starlink.table.StarTable;
import uk.ac.starlink.table.StarTableFactory;
import uk.ac.starlink.topcat.activate.SampSender;
import uk.ac.starlink.topcat.activate.ViewDatalinkActivationType;
import uk.ac.starlink.topcat.func.BasicImageDisplay;
import uk.ac.starlink.topcat.func.Browsers;
import uk.ac.starlink.topcat.func.Sog;
import uk.ac.starlink.topcat.plot2.PlotWindowType;
import uk.ac.starlink.topcat.plot2.TablePlotDisplay;
import uk.ac.starlink.util.DataSource;

/**
 * Defines an action that consumes a URL.
 *
 * <p>Some useful implementations are provided by static factory methods.
 *
 * @author   Mark Taylor
 * @since    6 Feb 2018
 */
public class UrlOptions {

    private final UrlInvoker[] invokers_;
    private final Map<ResourceType,UrlInvoker> dfltMap_;
    private static final boolean allowSystem_ = false;

    public UrlOptions( UrlInvoker[] invokers,
                       Map<ResourceType,UrlInvoker> dfltMap ) {
        invokers_ = invokers;
        dfltMap_ = dfltMap;
    }

    public UrlInvoker[] getInvokers() {
        return invokers_;
    }

    public Map<ResourceType,UrlInvoker> getDefaultsMap() {
        return dfltMap_;
    }

    /**
     * Returns a UrlOptions instance.
     *
     * @param  dlPanel   existing datalink panel to use if one is required;
     *                   may be null
     * @param  contextTitle   string to use as base title for new windows etc;
     *                        may be null
     * @return  new instance
     */
    public static UrlOptions createOptions( DatalinkPanel dlPanel,
                                            String contextTitle ) {
        ControlWindow controlWin = ControlWindow.getInstance();
        UrlInvoker reportUrl = createReportInvoker();
        UrlInvoker viewImage = createViewImageInvoker();
        UrlInvoker loadTable = createLoadTableInvoker( controlWin );
        UrlInvoker plotTable = createPlotTableInvoker( controlWin );
        UrlInvoker browser = createBrowserInvoker();
        UrlInvoker download = createDownloadInvoker( dlPanel );
        UrlInvoker viewDatalink = createDatalinkInvoker( dlPanel, contextTitle);
        UrlInvoker viewFitsImage = createViewFitsImageInvoker();
        UrlInvoker sendFitsImage =
            createSampInvoker( "FITS Image", "image.load.fits", Safety.SAFE );
        UrlInvoker sendTable =
            createSampInvoker( "Table", "table.load.votable", Safety.SAFE );
        // Note additional spectrum metadata is not supplied here.
        UrlInvoker sendSpectrum =
            createSampInvoker( "Spectrum", "spectrum.load.ssa-generic",
                               Safety.SAFE );
        UrlInvoker[] invokers = new UrlInvoker[] {
            reportUrl,
            viewImage,
            loadTable,
            plotTable,
            viewDatalink,
            viewFitsImage,
            sendFitsImage,
            sendSpectrum,
            sendTable,
            download,
            browser,
        };
        Map<ResourceType,UrlInvoker> map =
            new LinkedHashMap<ResourceType,UrlInvoker>();
        map.put( ResourceType.TABLE, loadTable );
        map.put( ResourceType.DATALINK, viewDatalink );
        map.put( ResourceType.FITS_IMAGE, viewFitsImage );
        map.put( ResourceType.SPECTRUM, sendSpectrum );
        map.put( ResourceType.IMAGE, viewImage );
        map.put( ResourceType.WEB, browser );
        map.put( ResourceType.UNKNOWN, browser );
        assert map.keySet()
              .equals( new HashSet<ResourceType>
                                  ( Arrays.asList( ResourceType.values() ) ) );
        return new UrlOptions( invokers, map );
    }

    /**
     * Returns an invoker that simply reports the text of the URL
     * as its success outcome message.
     *
     * @return  new invoker
     */
    private static UrlInvoker createReportInvoker() {
        return new AbstractUrlInvoker( "Report URL", Safety.SAFE ) {
            public Outcome invokeUrl( URL url ) {
                return url == null ? Outcome.failure( "No URL" )
                                   : Outcome.success( url.toString() );
            }
        };
    }

    /**
     * Returns an invoker for loading a table into the application.
     *
     * @param  controlWin  application control window
     * @return  new invoker
     */
    private static UrlInvoker
            createLoadTableInvoker( final ControlWindow controlWin ) { 
        final boolean isSelect = true;
        final StarTableFactory tfact = controlWin.getTableFactory();
        return new AbstractUrlInvoker( "Load Table",
                                       allowSystem_ ? Safety.UNSAFE
                                                    : Safety.SAFE ) {
            public Outcome invokeUrl( URL url ) {
                final String loc = url.toString();
                final StarTable table;
                try {
                    DataSource datsrc =
                        DataSource.makeDataSource( loc, allowSystem_ );
                    table = tfact.makeStarTable( datsrc );
                }
                catch ( IOException e ) {
                    return Outcome.failure( e );
                }
                final AtomicReference<TopcatModel> tcmref =
                    new AtomicReference<TopcatModel>();
                try {
                    SwingUtilities.invokeAndWait( new Runnable() {
                        public void run() {
                            tcmref.set( controlWin
                                       .addTable( table, loc, isSelect ) );
                        }
                    } );
                }
                catch ( Exception e ) {
                    return Outcome.failure( e );
                }
                return Outcome.success( tcmref.get().toString() );
            }
        };
    }

    /**
     * Returns an invoker for plotting a table directly without loading it.
     *
     * @param  controlWin  application control window
     * @return  new invoker
     */
    private static UrlInvoker
            createPlotTableInvoker( final ControlWindow controlWin ) {
        final StarTableFactory tfact = controlWin.getTableFactory();
        final TablePlotDisplay plotDisplay =
            new TablePlotDisplay( controlWin, PlotWindowType.PLANE,
                                  "Downloaded", false );
        return new AbstractUrlInvoker( "Plot Table",
                                       allowSystem_ ? Safety.UNSAFE
                                                    : Safety.SAFE ) {
            public Outcome invokeUrl( URL url ) {
                String loc = url.toString();
                final StarTable table;
                try {
                    DataSource datsrc =
                        DataSource.makeDataSource( loc, allowSystem_ );
                    table = tfact.makeStarTable( datsrc );
                }
                catch ( IOException e ) {
                    return Outcome.failure( e );
                }
                SwingUtilities.invokeLater( new Runnable() {
                    public void run() {
                        plotDisplay.showPlotWindow( table );
                    }
                } );
                return Outcome.success( loc );
            }
        };
    }

    /**
     * Returns an invoker for displaying DataLink table in a given panel.
     *
     * @param   dlPanel  pre-existing datalink table display panel,
     *                   or null to create one on demand
     * @param  contextTitle  context for titling window
     * @return  new invoker
     */
    private static UrlInvoker
            createDatalinkInvoker( final DatalinkPanel dlPanel,
                                   final String contextTitle ) {
        return new AbstractUrlInvoker( "View DataLink Table", Safety.SAFE ) {
            private DatalinkPanel dlPanel_ = dlPanel;
            public Outcome invokeUrl( URL url ) {
                final boolean isNewPanel;
                synchronized ( this ) {
                    isNewPanel = dlPanel_ == null;
                    if ( isNewPanel ) {
                        dlPanel_ = new DatalinkPanel( true, true );
                    }
                }
                if ( isNewPanel ) {
                    String title =
                        createTitle( contextTitle, "View Datalink Table" );
                    SwingUtilities.invokeLater( new Runnable() {
                        public void run() {
                            JFrame frm = new JFrame( title );
                            frm.getContentPane().add( dlPanel_ );
                            frm.pack();
                            frm.setVisible( true );
                        }
                    } );
                }
                Window window = isNewPanel
                              ? null
                              : SwingUtilities.windowForComponent( dlPanel_ );
                return ViewDatalinkActivationType
                      .invokeLocation( url.toString(), dlPanel_, window );
            }
        };
    }

    /**
     * Returns an invoker for viewing a generic image in a suitable viewer.
     *
     * @return   new invoker
     */
    private static UrlInvoker createViewImageInvoker() {
        return new AbstractUrlInvoker( "View image internally",
                                       Safety.SAFE ) {
            public Outcome invokeUrl( URL url ) {
                String loc = url.toString();
                String label = "Image";
                String msg = BasicImageDisplay.displayBasicImage( label, loc );
                return Outcome.success( msg );
            }
        };
    }

    /**
     * Returns an invoker for viewing a FITS image in a suitable viewer.
     *
     * @return   new invoker
     */
    private static UrlInvoker createViewFitsImageInvoker() {
        return new AbstractUrlInvoker( "View FITS image internally",
                                       Safety.SAFE ) {
            public Outcome invokeUrl( URL url ) {
                String loc = url.toString();
                String label = "FITS Image";
                String msg = TopcatUtils.canSog()
                           ? Sog.sog( label, loc )
                           : BasicImageDisplay.displayBasicImage( label, loc );
                return Outcome.success( msg );
            }
        };
    }

    /**
     * Returns an invoker for loading the URL into the system browser.
     *
     * @return  new invoker
     */
    private static UrlInvoker createBrowserInvoker() {
        return new AbstractUrlInvoker( "Show web page", Safety.UNSAFE ) {
            public Outcome invokeUrl( URL url ) {
                Browsers.systemBrowser( url.toString() );
                return Outcome.success( url.toString() );
            }
        };
    }

    /**
     * Returns an invoker for downloading the URL to an interactively
     * acquired local file.
     *
     * @return  new invoker
     */
    private static UrlInvoker createDownloadInvoker( Component parent ) {
        final DownloadDialog dialog =
            DownloadDialog.createSystemDialog( parent );
        // Safe as long as the user sees the dialogue window before the
        // download happens.  If it was done without user intervention
        // it should be marked as UNSAFE.
        return new AbstractUrlInvoker( "Download URL", Safety.SAFE ) {
            public Outcome invokeUrl( URL url ) {
                return dialog.userDownload( url );
            }
        };
    }

    /**
     * Returns a UrlInvoker that sends a SAMP load-type message.
     * The MType is supplied, and the message sent includes the
     * url string as the value of the message's "url" parameter.
     *
     * @param   contentName  user-readable type for resource to be sent
     * @param   mtype   MType for message to send
     * @param   safety   safety of MType
     * @return   new invoker
     */
    private static UrlInvoker createSampInvoker( final String contentName,
                                                 final String mtype,
                                                 Safety safety ) {
        final SampSender sender = new SampSender( mtype );
        return new AbstractUrlInvoker( "Send " + contentName, safety ) {
            public Outcome invokeUrl( URL url ) {
                Message message = new Message( mtype );
                message.addParam( "url", url.toString() );
                return sender.activateMessage( message );
            }
        };
    }

    /**
     * Returns a title for use for instance in titling a window
     * based on optional context and optional sub parts.
     *
     * @param  contextTitle   higher-level title text, may be null
     * @param  subTitle   lower-level title text, may be null
     * @return  title string, may be null
     */
    private static String createTitle( String contextTitle, String subTitle ) {
        StringBuffer sbuf = new StringBuffer();
        if ( contextTitle != null && contextTitle.trim().length() > 0 ) {
            sbuf.append( contextTitle );
        }
        if ( sbuf.length() > 0 &&
             subTitle != null && subTitle.trim().length() > 0 ) {
            sbuf.append( " - " );
        }
        if ( subTitle != null && subTitle.trim().length() > 0 ) {
            sbuf.append( subTitle );
        }
        return sbuf.length() > 0 ? sbuf.toString() : null;
    }

    /**
     * Utility class providing a partial UrlInvoker implementation.
     */
    private static abstract class AbstractUrlInvoker implements UrlInvoker {
        private final String name_;
        private final Safety safety_;

        /**
         * Constructor.
         *
         * @param  name  invoker name
         */
        AbstractUrlInvoker( String name, Safety safety ) {
            name_ = name;
            safety_ = safety;
        }

        public String getTitle() {
            return name_;
        }

        public Safety getSafety() {
            return safety_;
        }

        @Override
        public String toString() {
            return name_;
        }
    }
}
