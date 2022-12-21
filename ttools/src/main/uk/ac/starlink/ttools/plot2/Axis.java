package uk.ac.starlink.ttools.plot2;

import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.geom.AffineTransform;

/**
 * Does geometry and drawing for a straight line axis.
 * Linear and logarithmic scales are supported; obtain one using
 * the {@link #createAxis createAxis} factory method.
 *
 * @author   Mark Taylor
 * @since    12 Feb 2013
 */
@Equality
public abstract class Axis {

    private final int glo_;
    private final int ghi_;
    private final double dlo_;
    private final double dhi_;

    /**
     * Constructor.
     *
     * @param   glo  minimum graphics coordinate
     * @param   ghi  maximum graphics coordinate
     * @param   dlo  minimum data coordinate
     * @param   dhi  maximum data coordinate
     */
    protected Axis( int glo, int ghi, double dlo, double dhi ) {
        if ( ! ( glo < ghi ) ) {
            throw new IllegalArgumentException( "Bad graphics bounds" );
        }
        if ( ! ( dlo < dhi ) ) {
            throw new IllegalArgumentException( "Bad data bounds" );
        }
        glo_ = glo;
        ghi_ = ghi;
        dlo_ = dlo;
        dhi_ = dhi;
    }

    /**
     * Converts a data coordinate to the graphics position on this axis.
     *
     * @param   d  data coordinate
     * @return  graphics coordinate
     */
    public abstract double dataToGraphics( double d );

    /**
     * Converts a graphics position on this axis to a data coordinate.
     *
     * @param   g  graphics coordinate
     * @return   data coordinate
     */
    public abstract double graphicsToData( double g );

    /**
     * Returns the data bounds that result from performing an axis zoom
     * about a given data position.
     *
     * @param   d0   data reference position for zoom
     * @param  factor  amount to zoom
     * @return   2-element array giving new new data min/max coordinates
     */
    public abstract double[] dataZoom( double d0, double factor );

    /**
     * Returns the data bounds that result from performing an axis pan
     * between two given data positions.
     *
     * @param  d0  source data position
     * @param  d1  destination data position
     * @return   2-element array giving new new data min/max coordinates
     */
    public abstract double[] dataPan( double d0, double d1 );

    /**
     * Returns the axis graphics bounds.
     * The first element of the result (<code>glo</code>)
     * is always strictly less than the second (<code>ghi</code>).
     *
     * @return  2-element array giving the graphics min/max coordinates
     */
    public int[] getGraphicsLimits() {
        return new int[] { glo_, ghi_ };
    }

    /**
     * Returns the axis data bounds.
     * The first element of the result (<code>dlo</code>)
     * is always strictly less than the second (<code>dhi</code>).
     *
     * @return  2-element array giving the data min/max coordinates
     */
    public double[] getDataLimits() {
        return new double[] { dlo_, dhi_ };
    }

    /**
     * Indicates whether the scaling on this axis is linear.
     *
     * @return  true  iff this axis is linear
     */
    public abstract boolean isLinear();

    /**
     * Draws an axis title and supplied tickmarks.
     *
     * @param  ticks  tickmark array
     * @param  title  axis label text, may be null
     * @param  captioner  text positioning object
     * @param  tickLook  tick drawing style
     * @param  orient  axis orientation code
     * @param  invert  whether to reverse sense of axis 
     * @param  g   graphics context
     */
    public void drawLabels( Tick[] ticks, String title,
                            Captioner captioner, TickLook tickLook,
                            Orientation orient, boolean invert, Graphics g ) {
        calculateLabels( ticks, title, captioner, tickLook, orient, invert, g );
    }

    /**
     * Determines the bounds for axis and tickmark annotations.
     * The returned value is a bounding box for everything that would be
     * drawn by a corresponding call to {@link #drawLabels}.
     *
     * @param  ticks  tickmark array
     * @param  title  axis label text, may be null
     * @param  captioner  text positioning object
     * @param  orient  axis orientation code
     * @param  invert  whether to reverse sense of axis 
     * @return   bounding box for all annotations
     */
    public Rectangle getLabelBounds( Tick[] ticks, String title,
                                     Captioner captioner, Orientation orient,
                                     boolean invert ) {
        return calculateLabels( ticks, title, captioner, TickLook.NONE,
                                orient, invert, null );
    }

    /**
     * Does the work for painting and calculating bounds for axis annotations.
     *
     * @param  ticks  tickmark array
     * @param  title  axis label text, may be null
     * @param  captioner  text positioning object
     * @param  tickLook  tick drawing style
     * @param  orient  axis orientation code
     * @param  invert  whether to reverse sense of axis 
     * @param  graphics context, or null if no painting is required
     * @return   bounding box for all annotations
     */
    private Rectangle calculateLabels( Tick[] ticks, String title,
                                       Captioner captioner, TickLook tickLook,
                                       Orientation orient, boolean invert,
                                       Graphics g ) {
        boolean hasGraphics = g != null;
        Graphics2D g2 = hasGraphics ? (Graphics2D) g : null;
        AffineTransform trans0 = hasGraphics ? g2.getTransform() : null;
        double det0 = trans0 == null ? 0 : trans0.getDeterminant();

        /* Without this check, you can get JVM crashes! */
        if ( Double.isNaN( det0 ) || det0 == 0 ) {
            hasGraphics = false;
            trans0 = null;
        }
        int cpad = captioner.getPad();
        Rectangle textBounds = null;
        Rectangle tickBounds = new Rectangle();

        /* Place ticks. */
        AffineTransform upTrans =
            AffineTransform.getScaleInstance( 1, orient.isDown() ? -1 : +1 );
        for ( int it = 0; it < ticks.length; it++ ) {
            Tick tick = ticks[ it ];
            Caption label = tick.getLabel();
            boolean hasText =
                label != null && label.toText().trim().length() > 0;
            int gx = (int) dataToGraphics( tick.getValue() );
            double tx = invert ? ghi_ - gx : gx - glo_;
            AffineTransform tTrans =
                AffineTransform.getTranslateInstance( tx, 0 );
            Rectangle cbounds = hasText
                              ? captioner.getCaptionBounds( label )
                              : null;
            AffineTransform oTrans = hasText
                                   ? orient.captionTransform( cbounds, cpad )
                                   : null;

            /* Update bounding box for tick labels. */
            if ( hasText ) {
                Rectangle box = combineTrans( tTrans, oTrans )
                               .createTransformedShape( cbounds ).getBounds();
                tickBounds.add( box );
            }

            /* If we are drawing, draw now. */
            if ( hasGraphics ) {
                g2.setTransform( combineTrans( trans0, tTrans, upTrans ) );
                if ( label != null ) {
                    tickLook.drawMajor( g2 );
                    if ( hasText ) {
                        g2.setTransform( combineTrans( trans0, tTrans,
                                                       oTrans ) );
                        captioner.drawCaption( label, g2 );
                    }
                }
                else {
                    tickLook.drawMinor( g2 );
                }
                g2.setTransform( trans0 );
            }
        }
        if ( hasGraphics ) {
            g2.setTransform( trans0 );
        }
        textBounds = combineRect( textBounds, tickBounds );
 
        /* Place title. */
        if ( title != null && title.length() > 0 ) {
            Caption titleCap = Caption.createCaption( title );
            Rectangle cbounds = captioner.getCaptionBounds( titleCap );
            int tx = ( ghi_ - glo_ ) / 2 - cbounds.width / 2;
            int ty = orient.isDown()
                   ? + tickBounds.height + cpad - cbounds.y
                   : - tickBounds.height - cpad - cbounds.height - cbounds.y;
            AffineTransform tTrans =
                AffineTransform.getTranslateInstance( tx, ty );
            if ( hasGraphics ) {
                g2.setTransform( combineTrans( trans0, tTrans ) );
                captioner.drawCaption( titleCap, g2 );
                g2.setTransform( trans0 );
            }
            Rectangle titleBounds =
                combineTrans( tTrans )
               .createTransformedShape( cbounds ).getBounds();
            textBounds = combineRect( textBounds, titleBounds );
        }
        return textBounds;
    }

    /**
     * Factory method to create a linear or logarithmic axis.
     *
     * @param   glo   minimum graphics coordinate
     * @param   ghi   maximum graphics coordinate
     * @param   dlo   minimum data coordinate
     * @param   dhi   maximum data coordinate
     * @param   log   true for logarithmic scaling, false for linear
     * @param   flip  true if the data coordinates should run
     *                in the opposite sense to the graphics coordinates
     */
    public static Axis createAxis( int glo, int ghi, double dlo, double dhi,
                                   boolean log, boolean flip ) {
        return log ? new LogAxis( glo, ghi, dlo, dhi, flip )
                   : new LinearAxis( glo, ghi, dlo, dhi, flip );
    }

    /**
     * Convenience method to combine multiple AffineTransforms and produce
     * the result (product) of them all. 
     *
     * @param   transforms   list of one or more transforms
     * @return  concatenation of input transforms
     */
    private static AffineTransform
            combineTrans( AffineTransform... transforms ) {
        AffineTransform trans = new AffineTransform();
        for ( int i = 0; i < transforms.length; i++ ) {
            trans.concatenate( transforms[ i ] );
        }
        return trans;
    }

    /**
     * Combines two rectangles, coping with the case where one or both
     * are null.
     *
     * @param   r1  first rectangle
     * @param   r2  second rectangle
     * @return   union of r1 and r2
     */
    private static Rectangle combineRect( Rectangle r1, Rectangle r2 ) {
        if ( r1 == null ) {
            return r2;
        }
        else {
            Rectangle rect = new Rectangle( r1 );
            if ( r2 != null ) {
                rect.add( r2 );
            }
            return rect;
        }
    }

    /**
     * Axis implementation with linear scaling.
     */
    private static class LinearAxis extends Axis {
        private final double a_;
        private final double a1_;
        private final double b_;
        private final double dlo_;
        private final double dhi_;

        /**
         * Constructor.
         *
         * @param   glo   minimum graphics coordinate
         * @param   ghi   maximum graphics coordinate
         * @param   dlo   minimum data coordinate
         * @param   dhi   maximum data coordinate
         * @param   flip  true if the data coordinates should run
         *                in the opposite sense to the graphics coordinates
         */
        public LinearAxis( int glo, int ghi, double dlo, double dhi,
                           boolean flip ) {
            super( glo, ghi, dlo, dhi );
            dlo_ = dlo;
            dhi_ = dhi;
            a_ = ( flip ? -1.0 : +1.0 ) * ( ghi - glo ) / ( dhi - dlo );
            a1_ = 1.0 / a_;
            b_ = ( flip ? ghi : glo ) - a_ * dlo;
        }

        public boolean isLinear() {
            return true;
        }

        public double dataToGraphics( double d ) {
            return b_ + a_ * d;
        }

        public double graphicsToData( double g ) {
            return ( g - b_ ) * a1_;
        }

        public double[] dataZoom( double d0, double factor ) {
            return zoom( dlo_, dhi_, d0, factor, false );
        }

        public double[] dataPan( double d0, double d1 ) {
            return pan( dlo_, dhi_, d0, d1, false );
        }

        @Override
        public int hashCode() {
            int code = 2359;
            code = 23 * code + Float.floatToIntBits( (float) a_ );
            code = 23 * code + Float.floatToIntBits( (float) b_ );
            code = 23 * code + Float.floatToIntBits( (float) dlo_ );
            code = 23 * code + Float.floatToIntBits( (float) dhi_ );
            return code;
        }

        @Override
        public boolean equals( Object o ) {
            if ( o instanceof LinearAxis ) { 
                LinearAxis other = (LinearAxis) o;
                return this.a_ == other.a_
                    && this.b_ == other.b_
                    && this.dlo_ == other.dlo_
                    && this.dhi_ == other.dhi_;
            }
            else {
                return false;
            }
        }
    }

    /**
     * Axis implementation with logarithmic scaling.
     */
    private static class LogAxis extends Axis {
        private final double a_;
        private final double a1_;
        private final double b_;
        private final double dlo_;
        private final double dhi_;

        /**
         * Constructor.
         *
         * @param   glo   minimum graphics coordinate
         * @param   ghi   maximum graphics coordinate
         * @param   dlo   minimum data coordinate
         * @param   dhi   maximum data coordinate
         * @param   flip  true if the data coordinates should run
         *                in the opposite sense to the graphics coordinates
         */
        public LogAxis( int glo, int ghi, double dlo, double dhi,
                        boolean flip ) {
            super( glo, ghi, dlo, dhi );
            dlo_ = dlo;
            dhi_ = dhi;
            a_ = ( flip ? -1.0 : +1.0 )
               * ( ghi - glo ) / ( Math.log( dhi ) - Math.log( dlo ) );
            b_ = ( flip ? ghi : glo ) - a_ * Math.log( dlo );
            a1_ = 1.0 / a_;
        }

        public boolean isLinear() {
            return false;
        }

        public double dataToGraphics( double d ) {

            /* Check explicitly for zero values and return a NaN rather than
             * -Infinity.  This is a bit questionable, and there may be a
             * case for changing the behaviour, but it avoids having to
             * make a number of checks for infinite values downstream. */
            return d > 0 ? b_ + a_ * Math.log( d )
                         : Double.NaN;
        }

        public double graphicsToData( double g ) {
            return Math.exp( ( g - b_ ) * a1_ );
        }

        public double[] dataZoom( double d0, double factor ) {
            return zoom( dlo_, dhi_, d0, factor, true );
        }

        public double[] dataPan( double d0, double d1 ) {
            return pan( dlo_, dhi_, d0, d1, true );
        }

        @Override
        public int hashCode() {
            int code = -242442;
            code = 23 * code + Float.floatToIntBits( (float) a_ );
            code = 23 * code + Float.floatToIntBits( (float) b_ );
            code = 23 * code + Float.floatToIntBits( (float) dlo_ );
            code = 23 * code + Float.floatToIntBits( (float) dhi_ );
            return code;
        }

        @Override
        public boolean equals( Object o ) {
            if ( o instanceof LogAxis ) {
                LogAxis other = (LogAxis) o;
                return this.a_ == other.a_
                    && this.b_ == other.b_
                    && this.dlo_ == other.dlo_
                    && this.dhi_ == other.dhi_;
            }
            else {
                return false;
            }
        }
    }

    /**
     * Utility method for axis panning.
     *
     * @param   dlo  initial axis lower bound
     * @param   dhi  initial axis upper bound
     * @param   d0   pan gesture start position
     * @param   d1   pan gesture end position
     * @param   isLog  false for linear axis, true for logarithmic
     * @return  2-element array giving final (panned) axis {lower,upper} bounds
     */
    public static double[] pan( double dlo, double dhi,
                                double d0, double d1, boolean isLog ) {
        if ( isLog ) {
            double d10 = d0 / d1;
            double plo = dlo * d10;
            double phi = dhi * d10;
            return plo > Double.MIN_VALUE && phi < Double.MAX_VALUE
                 ? new double[] { plo, phi }
                 : new double[] { dlo, dhi };
        }
        else {
            double d10 = d1 - d0;
            double plo = dlo - d10;
            double phi = dhi - d10;
            return plo > -Double.MAX_VALUE && phi < +Double.MAX_VALUE
                 ? new double[] { plo, phi }
                 : new double[] { dlo, dhi };
        }
    }

    /**
     * Utility method for axis zooming.
     *
     * @param   dlo  initial axis lower bound
     * @param   dhi  initial axis upper bound
     * @param   d0   zoom gesture reference position
     * @param   factor  zoom factor
     * @param   isLog   false for linear axis, true for logarithmic
     * @return  2-element array giving final (zoomed) axis {lower,upper} bounds
     */
    public static double[] zoom( double dlo, double dhi,
                                 double d0, double factor, boolean isLog ) {
        if ( isLog ) {
            double f1 = 1. / factor;
            double zlo = d0 * Math.pow( dlo / d0, f1 );
            double zhi = d0 * Math.pow( dhi / d0, f1 );
            return zlo > Double.MIN_VALUE && zhi < Double.MAX_VALUE
                 ? new double[] { zlo, zhi }
                 : new double[] { dlo, dhi };
        }
        else {
            double f1 = 1. / factor;
            double zlo = d0 + ( dlo - d0 ) * f1;
            double zhi = d0 + ( dhi - d0 ) * f1;
            return zlo > -Double.MAX_VALUE && zhi < +Double.MAX_VALUE
                 ? new double[] { zlo, zhi }
                 : new double[] { dlo, dhi };
        }
    }
}
