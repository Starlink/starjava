package uk.ac.starlink.topcat.activate;

import java.awt.Point;
import java.awt.Shape;
import java.awt.event.ActionListener;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.swing.Box;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.SwingUtilities;
import uk.ac.starlink.table.ColumnData;
import uk.ac.starlink.topcat.ColumnDataComboBox;
import uk.ac.starlink.topcat.ColumnDataComboBoxModel;
import uk.ac.starlink.topcat.ImageWindow;
import uk.ac.starlink.topcat.LineBox;
import uk.ac.starlink.topcat.Outcome;
import uk.ac.starlink.topcat.Safety;
import uk.ac.starlink.topcat.TopcatModel;
import uk.ac.starlink.util.gui.ShrinkWrapper;

/**
 * ActivationType for displaying an image in the internal viewer.
 *
 * @author   Mark Taylor
 * @since    27 Mar 2018
 */
public class GenericViewImageActivationType implements ActivationType {

    private final boolean isRegion_;
    private final Viewer[] viewers_;

    /**
     * Constructs an instance with configurable position highlighting.
     *
     * @param  isRegion   true iff position highlighting is allowed
     */
    public GenericViewImageActivationType( boolean isRegion ) {
        isRegion_ = isRegion;
        viewers_ = isRegion ? createRegionViewers() : createViewers();
    }

    public String getName() {
        return isRegion_ ? "Display image region"
                         : "Display image";
    }

    public String getDescription() {
        return ( isRegion_
                 ? "Displays the content of a file or URL column as an image"
                 : "Displays a region of an image referenced by a file or URL" )
             + " using an internal image display tool."
             + " At least FITS, JPEG and PNG are supported.";
    }

    public ActivatorConfigurator createConfigurator( TopcatModelInfo tinfo ) {
        return new ImageColumnConfigurator( tinfo, isRegion_, viewers_ );
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
        final boolean isRegion_;
        final boolean hasViewerChoice_;
        final JComboBox<Viewer> viewerSelector_;
        final NumberSelector xoffSelector_;
        final NumberSelector yoffSelector_;

        private static final String VIEWER_KEY = "viewer";
        private static final String XOFF_KEY = "xoff";
        private static final String YOFF_KEY = "yoff";

        /**
         * Constructor.
         *
         * @param   tinfo   table information
         * @param  isRegion  true iff position highlighting is included
         * @param  viewers  available viewer windows
         */
        ImageColumnConfigurator( TopcatModelInfo tinfo, boolean isRegion,
                                 Viewer[] viewers ) {
            super( tinfo, "Image",
                   new ColFlag[] { ColFlag.IMAGE, ColFlag.URL, } );
            tcModel_ = tinfo.getTopcatModel();
            isRegion_ = isRegion;
            hasViewerChoice_ = viewers.length > 1;
            JComponent queryPanel = getQueryPanel();
            ActionListener forwarder = getActionForwarder();

            viewerSelector_ = new JComboBox<Viewer>( viewers );
            viewerSelector_.addActionListener( forwarder );
            xoffSelector_ = new NumberSelector( tcModel_, "X Offset" );
            xoffSelector_.comboBox_.addActionListener( forwarder );
            yoffSelector_ = new NumberSelector( tcModel_, "Y Offset" );
            yoffSelector_.comboBox_.addActionListener( forwarder );
            if ( isRegion ) {
                queryPanel.add( xoffSelector_.createLine() );
                queryPanel.add( Box.createVerticalStrut( 5 ) );
                queryPanel.add( yoffSelector_.createLine() );
                queryPanel.add( Box.createVerticalStrut( 5 ) );
            }
            if ( hasViewerChoice_ ) {
                getQueryPanel()
               .add( new LineBox( "Image Viewer",
                                  new ShrinkWrapper( viewerSelector_ ) ) );
            }
        }
        public Activator createActivator( ColumnData locCdata ) {
            Viewer viewer = getViewer();
            String label = getWindowLabel( locCdata );
            if ( isRegion_ ) {
                assert viewer instanceof RegionViewer;
                ColumnData xoffCdata = xoffSelector_.getColumnData();
                ColumnData yoffCdata = yoffSelector_.getColumnData();
                return ((RegionViewer) viewer)
                      .createActivator( label, locCdata, xoffCdata, yoffCdata );
            }
            else {
                return viewer.createActivator( label, locCdata );
            }
        }
        public String getConfigMessage( ColumnData cdata ) {
            return null;
        }
        public ConfigState getState() {
            ConfigState state = getUrlState();
            if ( hasViewerChoice_ ) {
                state.saveSelection( VIEWER_KEY, viewerSelector_ );
            }
            if ( isRegion_ ) {
                state.saveSelection( XOFF_KEY, xoffSelector_.comboBox_ );
                state.saveSelection( YOFF_KEY, yoffSelector_.comboBox_ );
            }
            return state;
        }
        public void setState( ConfigState state ) {
            setUrlState( state );
            if ( hasViewerChoice_ ) {
                state.restoreSelection( VIEWER_KEY, viewerSelector_ );
            }
            if ( isRegion_ ) {
                state.restoreSelection( XOFF_KEY, xoffSelector_.comboBox_ );
                state.restoreSelection( YOFF_KEY, yoffSelector_.comboBox_ );
            }
        }
        public Safety getSafety() {
            return getViewer().getSafety();
        }
        private Viewer getViewer() {
            return viewerSelector_
                  .getItemAt( viewerSelector_.getSelectedIndex() );
        }
    }

    /**
     * Returns available viewers.
     *
     * @return  viewers
     */
    private static Viewer[] createViewers() {
        List<Viewer> list = new ArrayList<Viewer>();
        list.add( new BasicViewer( false ) );
        list.add( new BasicViewer( true ) );
        return list.toArray( new Viewer[ 0 ] );
    }

    /**
     * Returns available region viewers.
     *
     * @return  region viewers
     */
    private static RegionViewer[] createRegionViewers() {
        List<RegionViewer> list = new ArrayList<RegionViewer>();
        list.add( new BasicViewer( false ) );
        list.add( new BasicViewer( true ) );
        return list.toArray( new RegionViewer[ 0 ] );
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
         * @param  label   window label
         * @param  locCdata  column containing image location
         * @return   new activator
         */
        public abstract Activator createActivator( String label,
                                                   ColumnData locCdata );

        /**
         * Returns safety status of this viewer.
         *
         * @return  safety
         */
        public abstract Safety getSafety();

        @Override
        public String toString() {
            return name_;
        }
    }

    /**
     * Extends Viewer class to include position highlighting.
     */
    private static abstract class RegionViewer extends Viewer {

        /**
         * Constructor.
         *
         * @param  name  suitable for presentation in GUI
         */
        RegionViewer( String name ) {
            super( name );
        }

        /**
         * Creates an activator that does the work for this viewer.
         * The label identifies the window; if the same label is used,
         * the viewing window will be re-used.
         *
         * @param  label   window label
         * @param  locCdata  column containing image location
         * @param  xoffCdata   X offset column
         * @param  yoffCdata   Y offset column
         * @return   new activator
         */
        public abstract Activator createActivator( String label,
                                                   ColumnData locCdata,
                                                   ColumnData xoffCdata,
                                                   ColumnData yoffCdata );
    }

    /**
     * RegionViewer implementation using ImageWindow.  No dependencies.
     */
    private static class BasicViewer extends RegionViewer {
        private final Map<String,ImageWindow> winMap_;
        private final boolean allowSystem_;

        /**
         * Constructor.
         *
         * @param  allowSystem  whether to allow (potentially insecure)
         *                      system preprocessing commands in image location
         */
        BasicViewer( boolean allowSystem ) {
            super( "Basic" + ( allowSystem ? " (allow preprocessing)" : "" ) );
            allowSystem_ = allowSystem;
            winMap_ = new HashMap<String,ImageWindow>();
        }
        public Activator createActivator( String label, ColumnData locCdata ) {
            return createActivator( label, locCdata, null, null );
        }
        public Activator createActivator( final String label,
                                          final ColumnData locCdata,
                                          final ColumnData xoffCdata,
                                          final ColumnData yoffCdata ) {
            return new UrlColumnConfigurator
                      .LocationColumnActivator( locCdata, false ) {
                protected Outcome activateLocation( String loc, long lrow ) {
                    final ImageWindow imwin = getImageWindow( label );
                    final BufferedImage image;
                    try {
                        image = ImageWindow.createImage( loc, allowSystem_ );
                    }
                    catch ( IOException e ) {
                        return Outcome.failure( e );
                    }
                    if ( image == null ) {
                        return Outcome.failure( "No image " + loc );
                    }
                    double xoff = readNumber( xoffCdata, lrow );
                    double yoff = readNumber( yoffCdata, lrow );
                    int iw = image.getWidth();
                    int ih = image.getHeight();
                    final Point point =
                          ( xoff >= 0 && xoff <= iw && yoff >= 0 && yoff <= ih )
                        ? new Point( (int) xoff, (int) yoff )
                        : null;
                    SwingUtilities.invokeLater( new Runnable() {
                        public void run() {
                            imwin.setImagePoint( image, point );
                        }
                    } );
                    StringBuffer sbuf = new StringBuffer();
                    if ( point != null ) {
                        sbuf.append( "(" )
                            .append( point.x )
                            .append( ", " )
                            .append( point.y )
                            .append( ")" )
                            .append( ": " );
                    }
                    sbuf.append( loc );
                    return Outcome.success( sbuf.toString() );
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
     * Reads a numeric value from a data column.
     *
     * @param  cdata  column data
     * @param  lrow   row index
     * @return   numeric value, or NaN
     */
    private static double readNumber( ColumnData cdata, long lrow ) {
        if ( cdata != null ) {
            Object obj;
            try {
                obj = cdata.readValue( lrow );
            }
            catch ( IOException e ) {
                return Double.NaN;
            }
            if ( obj instanceof Number ) {
                return ((Number) obj).doubleValue();
            }
        }
        return Double.NaN;
    }

    /**
     * Component for choosing a numeric table column.
     */
    private static class NumberSelector {
        final String label_;
        final ColumnDataComboBoxModel model_;
        final JComboBox<ColumnData> comboBox_;

        /**
         * Constructor.
         *
         * @param  tcModel   topcat model
         * @param  label    GUI name for component
         */
        NumberSelector( TopcatModel tcModel, String label ) {
            label_ = label;
            model_ =
                new ColumnDataComboBoxModel( tcModel, Number.class, false );
            comboBox_ = new ColumnDataComboBox();
            comboBox_.setModel( model_ );
        }

        /**
         * Returns the column data selected for this item.
         *
         * @return  numeric column data
         */
        ColumnData getColumnData() {
            Object selValue = model_.getSelectedItem();
            return selValue instanceof ColumnData ? (ColumnData) selValue
                                                  : null;
        }

        /**
         * Returns a line component containing this selector and its
         * text label.
         *
         * @return  line component
         */
        JComponent createLine() {
            JComponent line = Box.createHorizontalBox();
            line.add( new JLabel( label_ + ": " ) );
            line.add( comboBox_ );
            return line;
        }
    }
}
