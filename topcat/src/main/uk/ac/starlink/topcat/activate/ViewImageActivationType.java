package uk.ac.starlink.topcat.activate;

import java.awt.Image;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.swing.JComboBox;
import javax.swing.SwingUtilities;
import uk.ac.starlink.sog.SOG;
import uk.ac.starlink.sog.SOGNavigatorImageDisplay;
import uk.ac.starlink.sog.SOGNavigatorImageDisplayFrame;
import uk.ac.starlink.table.ColumnData;
import uk.ac.starlink.topcat.ImageWindow;
import uk.ac.starlink.topcat.LineBox;
import uk.ac.starlink.topcat.Outcome;
import uk.ac.starlink.topcat.Safety;
import uk.ac.starlink.topcat.TopcatModel;
import uk.ac.starlink.topcat.TopcatUtils;

/**
 * ActivationType for displaying an image in the internal viewer.
 *
 * @author   Mark Taylor
 * @since    27 Mar 2018
 */
public class ViewImageActivationType implements ActivationType {

    private final Viewer[] viewers_;

    public ViewImageActivationType() {
        viewers_ = createViewers();
    }

    public String getName() {
        return "Display image";
    }

    public String getDescription() {
        return "Displays the content of a file or URL column as an image"
             + " using an internal image display tool."
             + " At least FITS, JPEG and PNG are supported.";
    }

    public ActivatorConfigurator createConfigurator( TopcatModelInfo tinfo ) {
        return new ImageColumnConfigurator( tinfo, viewers_ );
    }

    public Suitability getSuitability( TopcatModelInfo tinfo ) {
        return tinfo.tableHasFlag( ColFlag.IMAGE )
             ? Suitability.SUGGESTED
             : tinfo.getUrlSuitability();
    }

    /**
     * Configurator implementation for URLs pointing to images.
     */
    private static class ImageColumnConfigurator extends UrlColumnConfigurator {
        final TopcatModel tcModel_;
        final JComboBox viewerSelector_;
        private static final String VIEWER_KEY = "viewer";

        /**
         * Constructor.
         *
         * @param   tinfo   table information
         * @param  viewers  available viewer windows
         */
        ImageColumnConfigurator( TopcatModelInfo tinfo, Viewer[] viewers ) {
            super( tinfo, "Image",
                   new ColFlag[] { ColFlag.IMAGE, ColFlag.URL, } );
            tcModel_ = tinfo.getTopcatModel();
            viewerSelector_ = new JComboBox( viewers );
            getQueryPanel().add( new LineBox( "Image Viewer",
                                              viewerSelector_ ) );
        }
        public Activator createActivator( ColumnData cdata ) {
            Viewer viewer = getViewer();
            String label = getWindowLabel( cdata );
            return viewer.createActivator( cdata, label );
        }
        public String getConfigMessage( ColumnData cdata ) {
            return null;
        }
        public ConfigState getState() {
            ConfigState state = getUrlState();
            state.saveSelection( VIEWER_KEY, viewerSelector_ );
            return state;
        }
        public void setState( ConfigState state ) {
            setUrlState( state );
            state.restoreSelection( VIEWER_KEY, viewerSelector_ );
        }
        public Safety getSafety() {
            return getViewer().getSafety();
        }
        private Viewer getViewer() {
            return (Viewer) viewerSelector_.getSelectedItem();
        }
    }

    /**
     * Returns available viewers.
     *
     * @return  viewers
     */
    private static Viewer[] createViewers() {
        List<Viewer> list = new ArrayList<Viewer>();
        list.add( new BasicViewer() );
        if ( TopcatUtils.canSog() ) {
            list.add( new SogViewer() );
        }
        return list.toArray( new Viewer[ 0 ] );
    }

    /**
     * Object capable of displaying an image on activation.
     */
    private static abstract class Viewer {
        private final String name_;

        /**
         * Constructor.
         *
         * @param  name  suitable for presentation in GUI
         */
        Viewer( String name ) {
            name_ = name;
        }

        /**
         * Creates an activator that does the work for this viewer.
         * The label identifies the window; if the same label is used,
         * the viewing window will be re-used.
         *
         * @param  cdata  column containing image location
         * @param  label   window label
         * @return   new activator
         */
        public abstract Activator createActivator( ColumnData cdata,
                                                   String label );

        /**
         * Returns safety status of this viewer.
         *
         * @return safety
         */
        public abstract Safety getSafety();

        @Override
        public String toString() {
            return name_;
        }
    }

    /**
     * Viewer implementation using ImageWindow.  No dependencies.
     */
    private static class BasicViewer extends Viewer {
        private final Map<String,ImageWindow> winMap_;
        private final boolean allowSystem_;
        BasicViewer() {
            super( "Basic" );
            allowSystem_ = false;
            winMap_ = new HashMap<String,ImageWindow>();
        }
        public Activator createActivator( final ColumnData cdata,
                                          final String label ) {
            return new UrlColumnConfigurator
                      .LocationColumnActivator( cdata, false ) {
                protected Outcome activateLocation( String loc, long lrow ) {
                    final ImageWindow imwin = getImageWindow( label );
                    final Image image;
                    try {
                        image = imwin.createImage( loc, allowSystem_ );
                    }
                    catch ( IOException e ) {
                        return Outcome.failure( e );
                    }
                    SwingUtilities.invokeLater( new Runnable() {
                        public void run() {
                            imwin.setImage( image );
                        }
                    } );
                    return Outcome.success( loc );
                }
            };
        }

        public Safety getSafety() {
            return allowSystem_ ? Safety.UNSAFE : Safety.SAFE;
        }

        /**
         * Returns the ImageWindow for use with a given label.
         *
         * @param  label  re-use label
         */
        ImageWindow getImageWindow( String label ) {
            if ( !winMap_.containsKey( label ) ) {
                ImageWindow imwin = new ImageWindow( null );
                imwin.setTitle( label );
                winMap_.put( label, imwin );
            }
            ImageWindow imwin = winMap_.get( label );
            if ( ! imwin.isShowing() ) {
                imwin.setVisible( true );
            }
            return imwin;
        }
    }

    /**
     * Viewer implementation for use with SoG.
     */
    private static class SogViewer extends Viewer {
        private SOG sog_;
        private final Map<String,SOGNavigatorImageDisplay> sogMap_;
        SogViewer() {
            super( "SoG" );
            sogMap_ = new HashMap<String,SOGNavigatorImageDisplay>();
        }
        public Activator createActivator( final ColumnData cdata,
                                          final String label ) {
            return new UrlColumnConfigurator
                      .LocationColumnActivator( cdata, true ) {
                protected Outcome activateLocation( String loc, long lrow ) {
                    try {
                        /* The setFilename method takes a fileOrUrl. */
                        getSogger( label ).setFilename( loc, false );
                        return Outcome.success( loc );
                    }
                    catch ( Exception e ) {
                        return Outcome.failure( "Trouble loading " + loc );
                    }
                }
            };
        }

        public Safety getSafety() {
            return Safety.SAFE;
        }

        /**
         * Returns the SOG display for use with a given label.
         *
         * @param  label  re-use label
         */
        SOGNavigatorImageDisplay getSogger( String label ) {
            assert TopcatUtils.canSog();
            if ( sog_ == null ) {
                synchronized ( SogViewer.class ) {
                    sog_ = new SOG();
                    sog_.setDoExit( false );
                }
            }
            if ( ! sogMap_.containsKey( label ) ) {
                SOGNavigatorImageDisplay rootDisplay =
                     (SOGNavigatorImageDisplay) sog_.getImageDisplay();
                SwingUtilities.windowForComponent( rootDisplay )
                              .setVisible( false );
                Object win = rootDisplay.newWindow();
                SOGNavigatorImageDisplay sogger =
                    (SOGNavigatorImageDisplay)
                    ((SOGNavigatorImageDisplayFrame) win)
                   .getImageDisplayControl().getImageDisplay();
                sogger.setDoExit( false );
                sogger.setTitle( label );
                sogMap_.put( label, sogger );
            }
            SOGNavigatorImageDisplay sogger = sogMap_.get( label );
            if ( ! sogger.isShowing() ) {
                SwingUtilities.windowForComponent( sogger ).setVisible( true );
            }
            return sogger;
        }
    }
}
