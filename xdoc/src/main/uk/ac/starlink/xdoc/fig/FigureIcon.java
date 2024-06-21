package uk.ac.starlink.xdoc.fig;

import com.lowagie.text.Document;
import com.lowagie.text.DocumentException;
import com.lowagie.text.pdf.PdfWriter;
import java.awt.AlphaComposite;
import java.awt.Color;
import java.awt.Component;
import java.awt.Composite;
import java.awt.Dimension;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.font.FontRenderContext;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.io.BufferedOutputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import javax.imageio.ImageIO;
import javax.swing.AbstractAction;
import javax.swing.Icon;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.KeyStroke;

/**
 * Base class for drawings.
 * As well as implementing {@link javax.swing.Icon} it provides some 
 * utility methods for output.
 *
 * @author   Mark Taylor
 * @since    18 Sep 2007
 */
public abstract class FigureIcon implements Icon {

    private Rectangle bounds_;

    /**
     * Constructor.
     *
     * @param   bounds of image
     */
    protected FigureIcon( Rectangle bounds ) {
        setBounds( bounds );
    }

    /**
     * Implement this method to draw the figure content.
     *
     * @param  g2  graphics context
     */
    protected abstract void doDrawing( Graphics2D g2 );

    /**
     * Sets the bounds for this figure.
     *
     * @param  bounds   new bounds
     */
    public void setBounds( Rectangle bounds ) {
        bounds_ = new Rectangle( bounds );
    }

    /**
     * Returns the bounds for this figure.
     *
     * @return  bounds
     */
    public Rectangle getBounds() {
        return bounds_;
    }

    public int getIconWidth() {
        return bounds_.width;
    }

    public int getIconHeight() {
        return bounds_.height;
    }

    public void paintIcon( Component c, Graphics g, int x, int y ) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.translate( x - bounds_.x, y - bounds_.y );
        doDrawing( g2 );
    }

    /**
     * Displays the figure in a Swing window.
     */
    public void display() {
        final JFrame frame = new JFrame();
        frame.setDefaultCloseOperation( JFrame.EXIT_ON_CLOSE );
        JComponent container = new JPanel();
        container.setBackground( Color.WHITE );
        container.setOpaque( true );

        Dimension screen = Toolkit.getDefaultToolkit().getScreenSize();
        int xfact = this.getIconWidth() / screen.width;
        int yfact = this.getIconHeight() / screen.height;
        final Icon icon;
        if ( xfact == 0 && yfact == 0 ) {
            icon = this;
        }
        else {
            final int fact = Math.max( xfact + 1, yfact + 1 );
            icon = new Icon() {
                public int getIconWidth() {
                    return (int) Math.ceil( FigureIcon.this.getIconWidth() 
                                            / fact );
                }
                public int getIconHeight() {
                    return (int) Math.ceil( FigureIcon.this.getIconHeight()
                                            / fact );
                }
                public void paintIcon( Component c, Graphics g, int x, int y ) {
                    Graphics2D g2 = (Graphics2D) g;
                    AffineTransform trans = g2.getTransform();
                    g2.scale( 1.0 / fact, 1.0 / fact );
                    FigureIcon.this.paintIcon( c, g2, Math.round( x * fact ),
                                                      Math.round( y * fact ) );
                    g2.setTransform( trans );
                }
            };
        }
        container.add( new JLabel( icon ) );

        Object quitKey = "quit";
        container.getInputMap().put( KeyStroke.getKeyStroke( 'q' ), quitKey );
        container.getActionMap().put( quitKey, new AbstractAction() {
            public void actionPerformed( ActionEvent evt ) {
                frame.dispose();
            }
        } );

        frame.getContentPane().add( container );
        frame.pack();
        frame.setVisible( true );
    }

    /**
     * Exports the currently displayed plot to encapsulated postscript.
     * Note this method closes the output stream when it's done.
     *
     * @param  out  destination stream for the EPS
     */
    public void exportEps( OutputStream out ) throws IOException {

        /* Scale to a pixel size which makes the bounding box sit sensibly
         * on an A4 or letter page.  EpsGraphics2D default scale is 72dpi. */
        double padfrac = 0.05;
        double xdpi = bounds_.width / 6.0;
        double ydpi = bounds_.height / 9.0;
        double scale;
        int pad;
        if ( xdpi > ydpi ) {
            scale = 72.0 / xdpi;
            pad = (int) Math.ceil( bounds_.width * padfrac * scale );
        }
        else {
            scale = 72.0 / ydpi;
            pad = (int) Math.ceil( bounds_.height * padfrac * scale );
        }
        int xlo = (int) Math.floor( scale * bounds_.x ) - pad;
        int ylo = (int) Math.floor( scale * bounds_.y ) - pad;
        int xhi = (int)
                  Math.ceil( scale * ( bounds_.x + bounds_.width ) ) + pad;
        int yhi = (int)
                  Math.ceil( scale * ( bounds_.y + bounds_.height ) ) + pad;

        /* Construct a graphics object which will write postscript
         * down this stream. */
        FixedEpsGraphics2D g2 =
            new FixedEpsGraphics2D( "Figure", out, xlo, ylo, xhi, yhi );
        g2.scale( scale, scale );

        /* Do the drawing. */
        doDrawing( g2 );

        /* Note this close call *must* be made, otherwise the
         * eps file is not flushed or correctly terminated.
         * This closes the output stream too. */
        g2.close();
    }

    /**
     * Exports the currently displayed plot to PDF.
     *
     * @param   out  destination stream for the PDF
     */
    public void exportPdf( OutputStream out ) throws IOException {
        Document doc =
            new Document( new com.lowagie.text.Rectangle( bounds_.width,
                                                          bounds_.height ) );
        try {
            PdfWriter pWriter = PdfWriter.getInstance( doc, out );
            doc.open();
            Graphics2D g2 = pWriter.getDirectContent()
                           .createGraphics( bounds_.width, bounds_.height );
            g2.translate( -bounds_.x, -bounds_.y );
            doDrawing( g2 );
            g2.dispose();
            doc.close();
        }
        catch ( DocumentException e ) {
            throw (IOException)
                  new IOException( e.getMessage() ).initCause( e );
        }
    }

    /**
     * Exports this figure to an output stream using the ImageIO framework.
     *
     * @param  formatName  ImageIO format name
     * @param  transparent  true iff image will have a transparent background
     * @param  out  destination output stream; will not be closed
     */
    public void exportImageIO( String formatName, boolean transparent,
                               OutputStream out )
            throws IOException {

        /* Set up image to draw into. */
        BufferedImage image =
            new BufferedImage( bounds_.width, bounds_.height,
                               transparent ? BufferedImage.TYPE_INT_ARGB
                                           : BufferedImage.TYPE_INT_RGB );

        /* Prepare graphics context and clear background. */
        Graphics2D g2 = image.createGraphics();
        Color color = g2.getColor();
        Composite compos = g2.getComposite();
        g2.setComposite( AlphaComposite.getInstance( AlphaComposite.SRC ) );
        g2.setColor( new Color( 1f, 1f, 1f, 0f ) );
        g2.fillRect( 0, 0, bounds_.width, bounds_.height );
        g2.setColor( color );
        g2.setComposite( compos );

        /* Do the drawing. */
        g2.translate( - bounds_.x, - bounds_.y );
        doDrawing( g2 );
        g2.translate( + bounds_.x, + bounds_.y );
        g2.dispose();

        /* Write the prepared image. */
        boolean done = ImageIO.write( image, formatName, out );
        out.flush();
        if ( ! done ) {
            throw new IOException( "No handler for format " + formatName );
        }
    }

    /**
     * Does the work for the {@link #main} method.
     *
     * @param   args  argument vector
     * @param   fig   optional FigureIcon instance to draw; if null the
     *          class name must be supplied on the command line
     * @return  status - zero means success
     */
    public static int runMain( String[] args, FigureIcon fig )
            throws IOException {

        /* Usage string. */
        StringBuffer ubuf = new StringBuffer( "Usage:" )
                           .append( " FigureIcon" )
                           .append( " [" );
        Mode[] modes = Mode.getModes();
        for ( int i = 0; i < modes.length; i++ ) {
            if ( i > 0 ) {
                ubuf.append( '|' );
            }
            ubuf.append( modes[ i ].getFlag() );
        }
        ubuf.append( ']' );
        if ( fig == null ) {
            ubuf.append( " <fig-class>" );
        }
        String usage = ubuf.toString();

        /* Process arguments. */
        List<String> argList = new ArrayList<String>( Arrays.asList( args ) );
        String destination = null;
        Mode mode = null;
        for ( Iterator<String> it = argList.iterator(); it.hasNext(); ) {
            String arg = it.next();
            if ( arg.equals( "-o" ) && it.hasNext() ) {
                it.remove();
                destination = it.next();
                it.remove();
            }
            else if ( arg.startsWith( "-h" ) ) {
                it.remove();
                System.out.println( usage );
                return 0;
            }
            else if ( arg.startsWith( "-" ) ) {
                Mode m = null;
                for ( int i = 0; m == null && i < modes.length; i++ ) {
                    if ( modes[ i ].getFlag().equals( arg ) ) {
                        it.remove();
                        m = modes[ i ];
                        mode = m;
                    }
                }
                if ( m == null ) {
                    System.err.println( usage );
                    return 1;
                }
            }
            else if ( fig == null ) {
                it.remove();
                Class<?> clazz;
                try {
                    clazz = Class.forName( arg );
                }
                catch ( Throwable e ) {
                    System.err.println( "No class " + arg + ": " + e );
                    return 1;
                }
                try {
                    fig = (FigureIcon) clazz.getDeclaredConstructor()
                                            .newInstance();
                }
                catch ( Throwable e ) {
                    System.err.println( "Error instantiating " + clazz.getName()
                                      + ": " + e );
                    return 1;
                }
            }
        }
        if ( ! argList.isEmpty() || fig == null || mode == null ) {
            System.err.println( usage );
            return 1;
        }

        /* Do work. */
        mode.process( fig, destination );
        return 0;
    }

    /**
     * Output mode - defines what will be done with a figure in the main method.
     */
    private static abstract class Mode {

        private final String name_;

        /**
         * Constructor.
         *
         * @param  name  mode name
         */
        public Mode( String name ) {
            name_ = name;
        }

        /**
         * Processes a figure.
         *
         * @param  fig   icon to process
         * @param  destination  output destination  
         */
        abstract void process( FigureIcon fig, String destination )
            throws IOException;

        /**
         * Returns the flag which identifies this output mode.
         *
         * @return  flag
         */
        public String getFlag() {
            return "-" + name_;
        }

        public String toString() {
            return name_;
        }

        /**
         * Returns an output stream defined by a given destination string.
         *
         * @param  dest  filename or "-" for standard output
         * @return   output stream
         */
        private static OutputStream getOutputStream( String dest )
                throws IOException {
            return ( dest == null || "-".equals( dest ) )
                 ? (OutputStream) System.out
                 : (OutputStream)
                   new BufferedOutputStream( new FileOutputStream( dest ) );
        }

        /**
         * Returns all known output modes.
         */
        public static Mode[] getModes() {
            return new Mode[] {
                SWING,
                EPS,
                PDF,
                new ImageIOMode( "png", "png", false ),
                new ImageIOMode( "jpeg", "jpeg", false ),
            };
        }

        /** Output mode for display in a Swing window.  */
        private static final Mode SWING = new Mode( "swing" ) {
            void process( FigureIcon fig, String dest ) {
                fig.display();
            }
        };

        /** Output mode for Encapsulated Postscript generation. */
        private static final Mode EPS = new Mode( "eps" ) {
            void process( FigureIcon fig, String dest ) throws IOException {
                fig.exportEps( getOutputStream( dest ) );
            }
        };

        /** Output mode for PDF generation. */
        private static final Mode PDF = new Mode( "pdf" ) {
            void process( FigureIcon fig, String dest ) throws IOException {
                fig.exportPdf( getOutputStream( dest ) );
            }
        };

        /**
         * Output mode implementation which uses the ImageIO framework.
         */
        private static class ImageIOMode extends Mode {

            private final String formatName_;
            private final boolean transparent_;

            /**
             * Constructor. 
             *
             * @param  name  mode name
             * @param  formatName   ImageIO format name
             * @param  transparent  true iff image will have a
             *                      transparent background
             */
            ImageIOMode( String name, String formatName, boolean transparent ) {
                super( name );
                formatName_ = formatName;
                transparent_ = transparent;
            }

            void process( FigureIcon fig, String dest ) throws IOException {
                fig.exportImageIO( formatName_, transparent_,
                                   getOutputStream( dest ) );
            }
        }
    }

    /**
     * Main method.  Run with <code>-help</code> flag for usage.
     *
     * @param   args  argument vector
     */
    public static void main( String[] args ) throws IOException {
        int status = runMain( args, null );
        if ( status != 0 ) {
            System.exit( status );
        }
    }
}
