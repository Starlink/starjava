package uk.ac.starlink.ttools;

import java.awt.Graphics2D;
import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Encapsulates SVG Graphics2D implementation.
 * That is handled by the excellent JFreeSVG library, but different
 * versions of that library have to be used for Java &lt;=8
 * (JFreeSVG &lt;=3) and for Java &gt;=11 (JFreeSVG &gt;=4),
 * largely because dependency classes in the <code>java.xml.bind</code>
 * package moved/disappeared at Java 9.
 *
 * <p>If the starjava package moves to target Java 11+ rather than Java 8+,
 * this class can be retired.
 *
 * @author   Mark Taylor
 * @since    25 Jun 2026
 * @see  <a href="https://www.jfree.org/jfreesvg/">JFreeSVG</a>
 */
public abstract class SvgGraphicsProvider {

    private static final Logger logger_ =
        Logger.getLogger( "uk.ac.starlink.ttools" );
    private static SvgGraphicsProvider instance_;

    /**
     * Creates a Graphics2D object that can subsequently be asked for
     * an SVG string.
     *
     * @param  w  canvas width in pixels
     * @param  h  canvas height in pixels
     */
    public abstract Graphics2D createGraphics2D( int w, int h )
            throws IOException;

    /**
     * Returns an SVG string describing all the graphics operations that
     * have been carried out on a Graphics2D object supplied earlier by
     * the {@link #createGraphics2D} method of this object.
     * Output is to a single SVG element, undecorated by an XML or
     * DOCTYPE declaration. 
     *
     * <p>There are some subtleties in getting this right;
     * following advice and experimentation with JFreeSVG 3
     * (which I have assumed without testing apply equally to JFreeSVG 4),
     * setting both the viewBox attribute and dimensions (width/height)
     * attributes, with preserveAspectRatio left to defaults,
     * seems to provide the right behaviour for default and rescaled
     * image size in both IMG and OBJECT elements.
     *
     * @param   g2  graphics context supplied by this object
     * @return   SVG serialization of graphics operations
     */
    public abstract String getSVGElement( Graphics2D g2 )
            throws IOException;

    /**
     * Returns an instance of this class.
     * This is known to fail at Java versions 9 and 10, which are not
     * Long Term Support versions.
     *
     * @return   working instance
     * @throws  IOException  if no instance can be provided
     */
    public static SvgGraphicsProvider getInstance() throws IOException {
        if ( instance_ == null ) {
            instance_ = createInstance();
        }
        return instance_;
    }

    /**
     * Returns an instance of this class.
     * This is known to fail at Java versions 9 and 10, which are not
     * Long Term Support versions.
     *
     * @return   working instance
     * @throws  IOException  if no instance can be provided
     */
    private static SvgGraphicsProvider createInstance() throws IOException {
        try {
            Class.forName( "javax.xml.bind.DatatypeConverter" );
            logger_.info( "Using JFreeSVG version 3" );
            return new SvgGraphicsProvider3();
        }
        catch ( ClassNotFoundException e ) {
            logger_.info( "JFreeSVG version 3 not usable in current JVM, "
                        + "try v4" );
        }
        final SvgGraphicsProvider4 svgp4;
        try {
            svgp4 = new SvgGraphicsProvider4();
        }
        catch ( Throwable e ) {
            logger_.log( Level.WARNING,
                         "No JFreeSVG available in current JVM", e );
            throw new IOException( "No SVGGraphics provider", e );
        }
        logger_.info( "Using JFreeSVG version 4" );
        return svgp4;
    }

    /**
     * SvgGraphicsProvider implementation based on JFreeSVG version 3.
     */
    private static class SvgGraphicsProvider3 extends SvgGraphicsProvider {
        public Graphics2D createGraphics2D( int w, int h ) {
            org.jfree.graphics2d.svg.SVGUnits pixUnit =
                org.jfree.graphics2d.svg.SVGUnits.PX;
            return new org.jfree.graphics2d.svg.SVGGraphics2D( w, h, pixUnit );
        }
        public String getSVGElement( Graphics2D g2 ) {
            org.jfree.graphics2d.svg.SVGGraphics2D svgG2 = 
                (org.jfree.graphics2d.svg.SVGGraphics2D) g2;
            int w = svgG2.getWidth();
            int h = svgG2.getHeight();
            String id = null;
            boolean includeDimensions = true;
            org.jfree.graphics2d.svg.ViewBox viewBox =
                new org.jfree.graphics2d.svg.ViewBox( 0, 0, w, h );
            org.jfree.graphics2d.svg.PreserveAspectRatio aspect = null;
            org.jfree.graphics2d.svg.MeetOrSlice meet = null;
            return svgG2.getSVGElement( id, includeDimensions, viewBox,
                                        aspect, meet );
        }
    }

    /**
     * SvgGraphicsProvider implementation based on JFreeSVG version 4.
     * Since the build is done using a Java 8 JDK, reference to all
     * the JFreeSVG classes, which are Java 11-dependent and hence
     * use 0x37 for the classfile major version, the library classes
     * cannot be seen at build time, so all access has to be via
     * reflection.
     */
    private static class SvgGraphicsProvider4 extends SvgGraphicsProvider {
        private final Class<?> g2clazz_;
        private final Constructor<?> g2constructor_;
        private final Method getSvgMethod_;
        private final Method getWidthMethod_;
        private final Method getHeightMethod_;
        private final Object pxUnit_;
        private final Constructor<?> vbConstructor_;
        SvgGraphicsProvider4() throws ReflectiveOperationException {
            g2clazz_ = Class.forName( "org.jfree.svg.SVGGraphics2D" );
            Class<?> unitClazz = Class.forName( "org.jfree.svg.SVGUnits" );
            Class<?> viewboxClazz = Class.forName( "org.jfree.svg.ViewBox" );
            Class<?> preserveAspectRatioClazz =
                Class.forName( "org.jfree.svg.PreserveAspectRatio" );
            Class<?> meetOrSliceClazz =
                Class.forName( "org.jfree.svg.MeetOrSlice" );
            pxUnit_ = unitClazz.getMethod( "valueOf", String.class )
                               .invoke( null, "PX" );
            g2constructor_ =
                g2clazz_.getConstructor( int.class, int.class, unitClazz );
            getSvgMethod_ =
                g2clazz_.getMethod( "getSVGElement",
                                    String.class, boolean.class, viewboxClazz,
                                    preserveAspectRatioClazz,
                                    meetOrSliceClazz );
            getWidthMethod_ =
                g2clazz_.getMethod( "getWidth", new Class<?>[ 0 ] );
            getHeightMethod_ =
                g2clazz_.getMethod( "getHeight", new Class<?>[ 0 ] );
            vbConstructor_ =
                viewboxClazz.getConstructor( int.class, int.class,
                                             int.class, int.class );
        }
        public Graphics2D createGraphics2D( int w, int h ) throws IOException {
            try {
                return (Graphics2D)
                       g2constructor_.newInstance( Integer.valueOf( w ),
                                                   Integer.valueOf( h ),
                                                   pxUnit_ );
            }
            catch ( ReflectiveOperationException e ) {
                throw new IOException( e );
            }
        }
        public String getSVGElement( Graphics2D g2 ) throws IOException {
            try {
                String id = null;
                boolean includeDimensions = true;
                int w = ((Number) getWidthMethod_.invoke( g2 )).intValue();
                int h = ((Number) getHeightMethod_.invoke( g2 )).intValue();
                Object viewBox =
                    vbConstructor_.newInstance( Integer.valueOf( 0 ),
                                                Integer.valueOf( 0 ),
                                                Integer.valueOf( w ),
                                                Integer.valueOf( h ) );
                Object aspect = null;
                Object meet = null;
                return (String) getSvgMethod_.invoke( g2, id, includeDimensions,
                                                      viewBox, aspect, meet );
            }
            catch ( ReflectiveOperationException e ) {
                throw new IOException( e );
            }
        }
    }
}
