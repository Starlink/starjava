package uk.ac.starlink.treeview;

import java.awt.AlphaComposite;
import java.awt.Cursor;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Insets;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.awt.image.ColorModel;
import java.awt.image.DataBuffer;
import java.awt.image.Raster;
import java.awt.image.SampleModel;
import java.awt.image.WritableRaster;
import java.lang.ref.Reference;
import java.lang.ref.WeakReference;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import javax.media.jai.PlanarImage;
import javax.media.jai.TileComputationListener;
import javax.media.jai.TileRequest;
import javax.swing.event.EventListenerList;
import jsky.image.gui.ImageDisplay;
import jsky.image.gui.ImageGraphicsHandler;

/**
 * Subclasses the JSky ImageDisplay class to do tile calculations out of
 * the event dispatch thread.
 * The JSky one just calls the <tt>getTile</tt> method of its displayed
 * <tt>PlanarImage</tt>, and if that takes a long time to return the
 * GUI can lock up.  This class does it the less naive way by queuing
 * tile calculation requests using (implicitly) JAI's TileScheduler.
 *
 * @author   Mark Taylor (Starlink)
 */
public class AsynchronousImageDisplay extends ImageDisplay {

    private PlanarImage displayImage;
    private SampleModel sampleModel;
    private ColorModel colorModel;
    private TileStore tileStore;
    private int tileWidth;
    private int tileHeight;
    private int minTileX;
    private int maxTileX;
    private int minTileY;
    private int maxTileY;
    private boolean busy;
    private boolean verbose = false;

    private static Cursor busyCursor = new Cursor( Cursor.WAIT_CURSOR );

    public AsynchronousImageDisplay() {
        super();
    }

    public void updateImage() {

        /* Prepare for queued tile calculation. */
        displayImage = getImageProcessor().getDisplayImage();
        sampleModel = displayImage.getSampleModel();
        colorModel = displayImage.getColorModel();
        tileWidth = displayImage.getTileWidth();
        tileHeight = displayImage.getTileHeight();
        minTileX = displayImage.getMinTileX();
        maxTileX = displayImage.getMinTileX() + displayImage.getNumXTiles() - 1;
        minTileY = displayImage.getMinTileY();
        maxTileY = displayImage.getMinTileY() + displayImage.getNumYTiles() - 1;

        /* Create a new tile store for keeping track of calculated tiles. */
        tileStore = new TileStore();

        /* Arrange for queued tiles to be displayed when ready. */
        displayImage.addTileComputationListener( new TileComputationListener() {
            public void tileCancelled( Object eventSource, 
                                       TileRequest[] requests,
                                       PlanarImage image, 
                                       int tileX, int tileY ) {
                tileStore.put( new Point( tileX, tileY), null );
            }

            public void tileComputationFailure( Object eventSource,
                                                TileRequest[] requests,
                                                PlanarImage image,
                                                int tileX, int tileY,
                                                Throwable situation) {
                tileStore.put( new Point( tileX, tileY ), null );
                situation.printStackTrace();
            }

            public void tileComputed( Object eventSource,
                                      TileRequest[] requests,
                                      PlanarImage image, int tileX, int tileY,
                                      Raster tile) {
                tileStore.cancelUnneededRequests();
                tileStore.put( new Point( tileX, tileY ), tile );
                Insets insets = getInsets();
                int x = displayImage.tileXToX( tileX ) + insets.left;
                int y = displayImage.tileYToY( tileY ) + insets.top;
                repaint( x, y, tileWidth, tileHeight );
            }
        } );

        /* Call the superclass's updateImage method to do the rest. */
        super.updateImage();
    }

    /**
     * Do the component painting.  Much of this code is pinched
     * from the jsky ImageDisplay implementation.
     */
    public void paintComponent( Graphics g ) {
        Graphics2D g2D = (Graphics2D) g;
     
        int componentWidth = getWidth();
        int componentHeight = getHeight();

        g2D.setComposite(AlphaComposite.Src);
        g2D.setColor(getBackground());
        g2D.fillRect(0, 0, componentWidth, componentHeight);

        // if displayImage is null, it's just a component
        if ( displayImage == null || sampleModel == null ) {
            return;
        }

        // Get the clipping rectangle and translate it into image coordinates.
        Rectangle clipBounds = g2D.getClipBounds();
        if (clipBounds == null) {
            clipBounds = new Rectangle(0, 0, componentWidth, componentHeight);
        }

        // Determine the extent of the clipping region in tile coordinates.
        Rectangle tilePoints = getTileSpace( clipBounds );
        int txmin = tilePoints.x;
        int tymin = tilePoints.y;
        int txmax = tilePoints.x + tilePoints.width - 1;
        int tymax = tilePoints.y + tilePoints.height - 1;

        Insets insets = getInsets();
        boolean iap = colorModel.isAlphaPremultiplied();

        for ( int tj = tymin; tj <= tymax; tj++ ) {
            int ty = displayImage.tileYToY( tj );
            for ( int ti = txmin; ti <= txmax; ti++ ) {
                int tx = displayImage.tileXToX( ti );

                /* Either get the ready tile or queue it for calculation. */
                Point tpt = new Point( ti, tj );
                Raster tile = tileStore.get( tpt );
                if ( tile == null ) {
                    tileStore.queue( tpt );
                }

                /* Do we have a tile ready to draw? */
                else {
                    DataBuffer dataBuffer = tile.getDataBuffer();
                    if ( dataBuffer != null ) {

                        /* If so then draw it. */
                        WritableRaster wr = 
                            tile.createWritableRaster( sampleModel,
                                                       dataBuffer, null );
                        BufferedImage bi = 
                            new BufferedImage( colorModel, wr, iap, null );
                        
                        AffineTransform translator =
                            AffineTransform
                           .getTranslateInstance( tx + insets.left, 
                                                  ty + insets.top );
                        g2D.drawRenderedImage( bi, translator );
                    }
                }
            }
        }
    }

    /**
     * This class is used as a short-term cache for tiles which have
     * been queued and calculated.  For each call of put, exactly
     * one subsequent call of remove should be made.  It has to be
     * thread-safe.
     */
    private class TileStore {
        private class Record { int refCount = 1; Raster raster; }
        private Map tileMap = new HashMap();
        private Map tileRefs = new HashMap();
        private Map tileRequests = new HashMap();

        /**
         * Submit a request to make the tile at a given point available.
         * The tile will be queued and the displayImage's 
         * TileComputationListeners will be notified when it is ready.
         */
        public synchronized void queue(Point pt) {
            if ( ! tileRequests.containsKey( pt ) ) {
                TileRequest req = displayImage.queueTiles( new Point[] { pt } );
                tileRequests.put( pt, req );
                setBusy( true );
                log( "Order:\t" + pt );
            }
        }

        /**
         * Purges requests from the tile computation queue if they are
         * for tiles which are no longer in the visible bounds of the
         * image.  This can be called periodically when the position
         * of the viewport on the image might have changed.
         */
        public synchronized void cancelUnneededRequests() {
            boolean showing = isShowing();
            Rectangle visibleTiles = getTileSpace( getVisibleRect() );
            int txmin = visibleTiles.x;
            int tymin = visibleTiles.y;
            int txmax = visibleTiles.x + visibleTiles.width - 1;
            int tymax = visibleTiles.y + visibleTiles.height - 1;
            for ( Iterator it = tileRequests.entrySet().iterator(); 
                  it.hasNext(); ) {
                Map.Entry ent = (Map.Entry) it.next();
                Point pt = (Point) ent.getKey();
                if ( ( ! showing ) ||
                     pt.x < txmin || pt.x > txmax || 
                     pt.y < tymin || pt.y > tymax ) {
                    TileRequest req = (TileRequest) ent.getValue();
                    Point[] reqpts = req.getTileIndices();
                    assert reqpts.length == 1;
                    assert reqpts[ 0 ].equals( pt );
                    req.cancelTiles( req.getTileIndices() );
                    it.remove();
                    tileMap.remove( pt );
                    log( "Cancel:\t" + pt + "  " + 
                         (showing ? (txmin+".."+txmax+", "+tymin+".."+tymax) 
                                  : "hidden") );
                }
            }
            if ( tileRequests.isEmpty() ) {
                setBusy( false );
            }
        }

        /**
         * This method should be called when a tile has been calculated,
         * to enter it into the list of tiles ready for collection.  
         */
        public synchronized void put( Point pt, Raster raster ) {
            Record rec;
            if ( tileMap.containsKey( pt ) ) {
                rec = (Record) tileMap.get( pt );
                rec.refCount++;
            }
            else {
                rec = new Record();
                rec.raster = raster;
                tileMap.put( pt, rec );
                tileRefs.put( pt, new WeakReference( raster ) );
            }
            log( "Put:\t" + pt + " " + rec.refCount );
        }

        /**
         * Tries to retrieve a tile from the list of ready tiles.
         * If one is ready for immediate collection the raster will be
         * returned, but if some work would have to be done to get it,
         * <tt>null</tt> will be returned.
         * <p>
         * This method should be used to collect a tile which has 
         * previously been requested using the <tt>queue</tt> method,
         * but may also be used to attempt to get a tile which has not
         * recently been asked for.  In the former case, the tile will
         * be available for sure, but in the latter case, it may be
         * there if it has not been garbage collected (weak references
         * are used to keep tabs on such formerly calculated tiles).
         * If it's not there, <tt>null</tt> will be returned.
         */
        public synchronized Raster get( Point pt ) {
            Raster raster;
            Record rec = (Record) tileMap.get( pt );
            if ( rec != null ) {
                raster = rec.raster;
                log( "Buy:\t" + pt + " " + rec.refCount );
                if ( --rec.refCount == 0 ) {
                    tileMap.remove( pt );
                    tileRequests.remove( pt );
                    if ( tileRequests.isEmpty() ) {
                        setBusy( false );
                    }
                }
            }
            else if ( tileRefs.containsKey( pt ) ) {
                Reference ref = (Reference) tileRefs.get( pt );
                raster = (Raster) ref.get();
                if ( raster == null ) {
                log( "Gone:\t" + pt );
                    tileRefs.remove( pt );
                }
                else {
                    log( "Steal:\t" + pt );
                }
            }
            else {
                raster = null;
            }
            return raster;
        }
    }

    /**
     * Calculates the area in tile space (pixel 0,0 represents the first
     * tile etc) which corresponds to a given area in pixel space.
     * Any tile which is wholly or partially covered by the pixel region
     * is included in the result.
     */
    private Rectangle getTileSpace( Rectangle pixelSpace ) {
        int txmin;
        int txmax;
        int tymin;
        int tymax;

        txmin = displayImage.XToTileX( pixelSpace.x );
        txmin = Math.max( txmin, minTileX );
        txmin = Math.min( txmin, maxTileX );

        txmax = displayImage.XToTileX( pixelSpace.x + pixelSpace.width - 1 );
        txmax = Math.max( txmax, minTileX );
        txmax = Math.min( txmax, maxTileX );

        tymin = displayImage.YToTileY( pixelSpace.y );
        tymin = Math.max( tymin, minTileY );
        tymin = Math.min( tymin, maxTileY );

        tymax = displayImage.YToTileY( pixelSpace.y + pixelSpace.height - 1 );
        tymax = Math.max( tymax, minTileY );
        tymax = Math.min( tymax, maxTileY );

        return new Rectangle( txmin, tymin, 
                              txmax - txmin + 1, tymax - tymin + 1 );
    }

    /**
     * Sets the busy status of this component.  The cursor changes 
     * accordingly.
     */
    private void setBusy( boolean busy ) {
        this.busy = busy;
        setCursor( busy ? busyCursor : null );
        log( "busy: " + busy );
    }

    /**
     * Logging method used for debugging to see how the tile requests
     * are going.
     */
    private void log( String msg ) {
        if ( verbose ) {
            System.out.println( msg );
        }
    }


    /* Listener business lifted exactly from JSky ImageDisplay. */

    private EventListenerList _listenerList = new EventListenerList();
    public void addImageGraphicsHandler(ImageGraphicsHandler igh) {
        _listenerList.add(ImageGraphicsHandler.class, igh);
    }
    public void removeImageGraphicsHandler(ImageGraphicsHandler igh) {
        _listenerList.remove(ImageGraphicsHandler.class, igh);
    }
    private void _notifyGraphicsHandlers(Graphics2D g) {
        Object[] listeners = _listenerList.getListenerList();
        for (int i = listeners.length - 2; i >= 0; i -= 2) {
            if (listeners[i] == ImageGraphicsHandler.class) {
                ((ImageGraphicsHandler) listeners[i + 1]).drawImageGraphics(this, g);
            }
        }
    }
}
