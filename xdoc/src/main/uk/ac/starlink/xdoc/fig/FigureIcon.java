package uk.ac.starlink.xdoc.fig;

import java.awt.Color;
import java.awt.Component;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.FontMetrics;
import java.awt.event.ActionEvent;
import java.awt.font.FontRenderContext;
import java.awt.geom.AffineTransform;
import java.io.BufferedOutputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import javax.swing.AbstractAction;
import javax.swing.Icon;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.KeyStroke;

/**
 * Base class for drawings.
 * As well as implemnting {@link javax.swing.Icon} it provides some 
 * utility methods for output.
 *
 * @author   Mark Taylor
 * @since    18 Sep 2007
 */
public abstract class FigureIcon implements Icon {

    private final int width_;
    private final int height_;

    /**
     * Constructor.
     *
     * @param   width   figure width in pixels
     * @param   height  figure height in pixels
     */
    protected FigureIcon( int width, int height ) {
        width_ = width;
        height_ = height;
    }

    /**
     * Implement this method to draw the figure content.
     *
     * @param  g2  graphics context
     */
    protected abstract void doDrawing( Graphics2D g2 );

    public int getIconWidth() {
        return width_;
    }

    public int getIconHeight() {
        return height_;
    }

    public void paintIcon( Component c, Graphics g, int x, int y ) {
        doDrawing( (Graphics2D) g.create() );
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
        container.add( new JLabel( this ) );

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
        Rectangle bounds = new Rectangle( 0, 0, width_, height_ );
        double padfrac = 0.05;
        double xdpi = bounds.width / 6.0;
        double ydpi = bounds.height / 9.0;
        double scale;
        int pad;
        if ( xdpi > ydpi ) {
            scale = 72.0 / xdpi;
            pad = (int) Math.ceil( bounds.width * padfrac * scale );
        }
        else {
            scale = 72.0 / ydpi;
            pad = (int) Math.ceil( bounds.height * padfrac * scale );
        }
        int xlo = (int) Math.floor( scale * bounds.x ) - pad;
        int ylo = (int) Math.floor( scale * bounds.y ) - pad;
        int xhi = (int) Math.ceil( scale * ( bounds.x + bounds.width ) ) + pad;
        int yhi = (int) Math.ceil( scale * ( bounds.y + bounds.height ) ) + pad;

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
     * Main method.  Run with <code>-help</code> flag for usage.
     *
     * @param   args  argument vector
     */
    public static void main( String[] args ) throws IOException {

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
        ubuf.append( " <fig-class>" );
        String usage = ubuf.toString();

        /* Process arguments. */
        List argList = new ArrayList( Arrays.asList( args ) );
        String destination = null;
        Mode mode = null;
        FigureIcon fig = null;
        for ( Iterator it = argList.iterator(); it.hasNext(); ) {
            String arg = (String) it.next();
            if ( arg.equals( "-o" ) && it.hasNext() ) {
                it.remove();
                destination = (String) it.next();
                it.remove();
            }
            else if ( arg.startsWith( "-h" ) ) {
                it.remove();
                System.out.println( usage );
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
                    System.exit( 1 );
                }
            }
            else if ( fig == null ) {
                it.remove();
                Class clazz;
                try {
                    clazz = Class.forName( arg );
                }
                catch ( Throwable e ) {
                    System.err.println( "No class " + arg + ": " + e );
                    System.exit( 1 );
                    return;
                }
                try {
                    fig = (FigureIcon) clazz.newInstance();
                }
                catch ( Throwable e ) {
                    System.err.println( "Error instantiating " + clazz.getName()
                                      + ": " + e );
                    System.exit( 1 );
                }
            }
        }
        if ( ! argList.isEmpty() || fig == null || mode == null ) {
            System.err.println( usage );
            System.exit( 1 );
        }

        /* Do work. */
        mode.process( fig, destination );
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
    }
}
