package uk.ac.starlink.ttools.gui;

import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import javax.swing.Icon;
import javax.swing.ImageIcon;

/**
 * Stores icons used by the ttools package.
 *
 * @author   Mark Taylor
 * @since    9 Feb 2007
 */
public class ResourceIcon {

    /**
     * Sole class instance.
     * This doesn't do anything but may be useful for referencing members.
     */
    public static final ResourceIcon REF = new ResourceIcon();

    public static final Icon

        /* MethodBrowser icons. */
        FOLDER_NODE = readIcon( "folder_node.png" ),
        LIBRARY_NODE = readIcon( "book_leaf.png" ),
        FUNCTION_NODE = readIcon( "fx_leaf.png" ),
        CONSTANT_NODE = readIcon( "c_leaf.png" ),

        /* Plot2 icons. */
        PLOT_FUNCTION = readIcon( "sinx.png" ),
        PLOT_CONTOUR = readIcon( "plot-contour.png" ),
        PLOT_LINE = readIcon( "plot-line.png" ),
        PLOT_LABEL = readIcon( "plot-label.png" ),
        PLOT_AREALABEL = readIcon( "plot-arealabel.png" ),
        PLOT_SPECTRO = readIcon( "plot-spectro.png" ),
        PLOT_HEALPIX = readIcon( "plot-healpix.png" ),
        PLOT_SKYGRID = readIcon( "plot-skygrid.png" ),
        PLOT_STATLINE = readIcon( "plot-statline.png" ),
        PLOT_STATMARK = readIcon( "plot-statmark.png" ),
        FORM_HISTOGRAM = readIcon( "form-histogram.png" ),
        FORM_KDE = readIcon( "form-kde.png" ),
        FORM_KNN = readIcon( "form-knn.png" ),
        FORM_DENSOGRAM = readIcon( "form-densogram.png" ),
        PLOT_LINK2 = readIcon( "plot-link2.png" ),
        FORM_MARK = readIcon( "form-mark.png" ),
        FORM_SIZE = readIcon( "form-size.png" ),
        FORM_SIZEXY = readIcon( "form-sizexy.png" ),
        FORM_XYELLIPSE = readIcon( "form-xyellipse.png" ),
        FORM_SKYELLIPSE = readIcon( "form-skyellipse.png" ),
        FORM_ELLIPSE_CORR = readIcon( "form-ellipse-corr.png" ),
        FORM_ERROR = readIcon( "form-error.png" ),
        FORM_ERROR1 = readIcon( "form-error1.png" ),
        FORM_VECTOR = readIcon( "form-vector.png" ),
        FORM_FILL = readIcon( "form-fill.png" ),
        FORM_SKYDENSITY = readIcon( "form-skydensity.png" ),
        FORM_GRID = readIcon( "form-grid.png" ),
        FORM_QUANTILE = readIcon( "form-quantile.png" ),
        FORM_LINEARFIT = readIcon( "form-linearfit.png" ),
        FORM_GAUSSIAN = readIcon( "form-gaussian.png" ),
        FORM_LINK2 = readIcon( "form-link2.png" ),
        FORM_LINK3 = readIcon( "form-link3.png" ),
        FORM_MARKS2 = readIcon( "form-marks2.png" ),
        FORM_MARKS3 = readIcon( "form-marks3.png" ),
        FORM_MARKS4 = readIcon( "form-marks4.png" ),
        FORM_POLYLINE = readIcon( "form-quad-line.png" ),
        FORM_AREA = readIcon( "form-area.png" ),
        MODE_FLAT = readIcon( "mode-flat.png" ),
        MODE_AUTO = readIcon( "mode-auto.png" ),
        MODE_DENSITY = readIcon( "mode-density.png" ),
        MODE_ALPHA = readIcon( "mode-transparent.png" ),
        MODE_ALPHA_FIX = readIcon( "mode-transparent-lock.png" ),
        MODE_AUX = readIcon( "mode-aux.png" ),
        MODE_WEIGHT = readIcon( "mode-weight.png" ),
        MODE_RGB = readIcon( "mode-rgb.png" ),
        DRAG1 = readIcon( "drag1.png" ),
        DRAG2 = readIcon( "drag2.png" ),
        DRAG3 = readIcon( "drag3.png" ),
        CLICK1 = readIcon( "click1.png" ),
        CLICK2 = readIcon( "click2.png" ),
        CLICK3 = readIcon( "click3.png" ),
        MOUSE_WHEEL = readIcon( "mwheel.png" ),
        ZERO = readIcon( "ozero.png" ),
        COLOR_WHEEL = readIcon( "colorwheel18.png" ),

        /* Placeholder and terminator. */
        TTOOLS_DOWHAT = readIcon( "burst.png" );

    /**
     * Private constructor prevents instantiation.
     */
    ResourceIcon() {
    }

    /**
     * Reads an icon from a filename representing a resource in this 
     * class's package.
     *
     * @param  name  image name in this package
     */
    private static Icon readIcon( String name ) {
        try {
            URL url = ResourceIcon.class.getResource( name );
            if ( url == null ) {
                return null;
            }
            InputStream in = new BufferedInputStream( url.openStream() );
            byte[] buf = new byte[ 4096 ];
            ByteArrayOutputStream bout = new ByteArrayOutputStream();
            try {
                for ( int n; ( n = in.read( buf ) ) >= 0; ) {
                    bout.write( buf, 0, n );
                }
            }
            finally {
                in.close();
                bout.close();
            }
            return new ImageIcon( bout.toByteArray() );
        }
        catch ( IOException e ) {
            return null;
        }
    }
}
