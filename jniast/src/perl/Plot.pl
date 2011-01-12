#!/usr/bin/perl -w

use strict;

use JMaker;
use SrcReader;

my( $cName ) = "Plot";
my( $aName );
my( $fName );

print <<'__EOT__';
package uk.ac.starlink.ast;

import java.awt.Graphics;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.geom.Rectangle2D;
import javax.swing.JPanel;
import javax.swing.JComponent;
import javax.swing.JFrame;
import uk.ac.starlink.ast.grf.DefaultGrf;
import uk.ac.starlink.ast.grf.DefaultGrfMarker;

__EOT__

makeClassHeader(
   Name => $cName,
   purpose => ClassPurpose( $cName ),
   descrip => ClassDescrip( $cName ),
   version => '$Id$',
   author => "Mark Taylor (Starlink)",
   extra => q{
      <h4>Usage</h4>
      Normally a <code>Plot</code> will be an object owned by a JComponent
      (i.e. some object in a subclass of JComponent).  In this case, the Plot's 
      <code>paint</code> method should be called in the JComponent's
      <code>paintComponent</code> method.  A minimal Plot-containing
      JComponent might look like this:
      <pre>
         class PlotHolder extends JPanel {
             public Plot plot;
             protected void paintComponent( Graphics g ) {
                 super.paintComponent( g );
                 plot.paint( g );
             }
         }
      </pre>
   },
);

print "public class Plot extends FrameSet {\n";

my( $frameDescrip ) = jdocize( ArgDescrip( $cName, "frame" ) );
my( $baseboxDescrip ) = jdocize( q{
   an 4-element array giving the coordinates of two points in 
   the supplied Frame (or base Frame if a FrameSet was supplied)
   which correspond to the bottom left and top right corners 
   of the Rectangle <code>graphrect</code>.
   The first pair of values should give the (x,y) coordinates of the 
   bottom left corner and the second pair the top right corner.
   Note that the order in which these points are
   given is important because it defines up, down, left and right 
   for subsequent graphical operations. 
} );


print <<__EOT__;
    /* Private fields. */

    private Grf grfobj;

    /**
     * Perform initialization required for JNI code at class load time.
     */
    static {
        nativeInitializePlot();
    }
    private native static void nativeInitializePlot();

    /**
     * Creates a $cName which plots onto a given rectangle.
     *
     * \@param  frame    $frameDescrip
     * \@param  graphrect the rectangle which will form the area
     *                  onto which plotting is done (note this excludes the
     *                  area used for graph annotation).
     * \@param  basebox  $baseboxDescrip
     */
    public Plot( Frame frame, Rectangle2D graphrect, double[] basebox ) {
        this( frame, graphrect, basebox, 0, 0, 0, 0 );
    }

    /**
     * Creates a $cName which plots onto a given rectangle with 
     * specified proportional spaces round the edge.
     *
     * \@param  frame    $frameDescrip
     * \@param  graphrect  the rectangle within which both the plot and 
     *                    space for axis annotations etc will be drawn
     * \@param  basebox  $baseboxDescrip
     * \@param  lgap     gap in graphics coordinates (pixels) free for
     *                  annotation at the left of the Rectangle
     * \@param  rgap     gap in graphics coordinates (pixels) free for
     *                  annotation at the right of the Rectangle
     * \@param  bgap     gap in graphics coordinates (pixels) free for
     *                  annotation at the bottom of the Rectangle
     * \@param  tgap     gap in graphics coordinates (pixels) free for
     *                  annotation at the top of the Rectangle
     * \@throws AstException if any error occurs in the AST library, or if
     *                      the specified gaps don't leave enough room for
     *                      any plotting
     */
    public Plot( Frame frame, Rectangle2D graphrect, double[] basebox,
                 int lgap, int rgap, int bgap, int tgap ) {

        /* Check that the bounds are sensible. */
        if ( lgap + rgap >= graphrect.getWidth() || lgap < 0 || rgap < 0 ||
             tgap + bgap >= graphrect.getHeight() || tgap < 0 || bgap < 0 ) {
            throw new AstException( "Gaps leave no space for the plot" );
        }
        float[] graphbox = new float[ 4 ];
        graphbox[ 0 ] = (float) ( graphrect.getX() + lgap );
        graphbox[ 1 ] = (float) ( graphrect.getY() + 
                                  graphrect.getHeight() - bgap );
        graphbox[ 2 ] = (float) ( graphrect.getX() + 
                                  graphrect.getWidth() - rgap );
        graphbox[ 3 ] = (float) ( graphrect.getY() + tgap );
        construct( frame, graphbox, basebox );

        /* DefaultGrf needs a JComponent to get off the ground, because the
         * underlying uk.ac.starlink.ast.grf.DefaultGrf class does.
         * I believe that this is not fundamentally necessary, but we 
         * have to pander to its needs for now.  Construct a dummy 
         * component for this purpose. */
        JFrame toplevel = new JFrame();
        JComponent comp = new JPanel();
        comp.setPreferredSize( new Dimension( graphrect.getBounds().width,
                                              graphrect.getBounds().height ) );
        toplevel.getContentPane().add( comp );
        toplevel.pack();
        comp.getGraphics().setClip( graphrect );

        /* Create the default Grf object. */
        grfobj = new DefaultGrf( comp );
    }

__EOT__

print <<'__EOT__';

    /* Native method which constructs the underlying AST Plot object. */
    private native void construct( Frame frame, float[] graphbox,
                                   double[] basebox );

    /**
     * Paints the component.  This method calls the <code>paint</code>
     * method of this Plot's <code>Grf</code> object to (re)paint all the
     * graphics caused by any methods called on this Plot since its
     * creation or the last call of <code>clear</code>.
     * It should normally be invoked by the <code>paintComponent</code> 
     * method of the <code>JComponent</code> which holds this Plot.
     *
     * @param  g  the graphics context into which to paint the graphics
     */
    public void paint( Graphics g ) {
        grfobj.paint( g );
    }

    /**
     * Clears the component.  This method calls the <code>clear</code>
     * method of this Plot's <code>Grf</code> to reset the list of
     * items painted by the <code>paint</code> method.  This method does
     * not actually erase any graphics from the screen, but will cause
     * an immediately following call of <code>paint</code> to draw
     * nothing.
     */
    public void clear() {
       grfobj.clear();
    }

    /**
     * Gets the Grf object which implements the graphics.
     * By default this is a {@link uk.ac.starlink.ast.grf.DefaultGrf},
     * but custom implementations
     * of the Grf interface may be written and substituted if required.
     * 
     * @return  the Grf object which is used for plotting
     */
    public Grf getGrf() {
        return grfobj;
    }

    /**
     * Sets the Grf object which implements the graphics.
     * By default this is a {@link uk.ac.starlink.ast.grf.DefaultGrf},
     * but custom implementations
     * of the Grf interface may be written and substituted if required.
     *
     * @param   grf  the Grf object to use for plotting
     */
    public void setGrf( Grf grf ) {
        grfobj = grf;
    }

    /**
     * Returns a fairly deep copy of this object.
     * The <tt>Grf</tt> of the returned copy however
     * is a reference to the same object as the Grf of this object.
     *
     * @return  copy
     */
    public AstObject copy() {
        AstObject copy = super.copy();
        ((Plot) copy).grfobj = this.grfobj;
        return copy;
    }

    /*
     * It is necessary to override the getter and setter methods, since
     * at least some of these reference the grf object belonging to this
     * Plot.  The implementations from AstObject don't swap the grf
     * object back into place prior to the calls - the effect of this
     * is normally a coredump. 
     */
    public native String getC( String attrib );
    public native double getD( String attrib );
    public native float getF( String attrib );
    public native long getL( String attrib );
    public native int getI( String attrib );

    public native void setC( String attrib, String value );
    public native void setD( String attrib, double value );
    public native void setF( String attrib, float value );
    public native void setL( String attrib, long value );
    public native void setI( String attrib, int value );

    public native boolean test( String attrib );
    public native void set( String settings );

__EOT__


makeNativeMethod(
   name => ( $fName = "border" ),
   purpose => FuncPurpose( $fName ),
   descrip => FuncDescrip( $fName ),
   return => { type => 'void' },
   params => [],
);

makeNativeMethod(
   name => ( $fName = "boundingBox" ),
   purpose => FuncPurpose( $fName ),
   descrip => FuncDescrip( $fName ),
   return => {
      type => 'Rectangle2D',
      descrip => q{
          a rectangle giving the bounds of the area affected by the
          previous plot call
      },
   },
   params => [],
);

makeNativeMethod(
   name => ( $fName = "clip" ),
   purpose => FuncPurpose( $fName ),
   descrip => FuncDescrip( $fName ),
   return => { type => 'void' },
   params => [
      {
         name => ( $aName = "iframe" ),
         type => 'int',
         descrip => ArgDescrip( $fName, $aName ),
      },
      {
         name => ( $aName = "lbnd" ),
         type => 'double[]',
         descrip => ArgDescrip( $fName, $aName ),
      },
      {
         name => ( $aName = "ubnd" ),
         type => 'double[]',
         descrip => ArgDescrip( $fName, $aName ),
      },
   ],
);
   
makeNativeMethod(
   name => ( $fName = "curve" ),
   purpose => FuncPurpose( $fName ),
   descrip => FuncDescrip( $fName ),
   return => { type => 'void' },
   params => [
      {
         name => ( $aName = "start" ),
         type => 'double[]',
         descrip => ArgDescrip( $fName, $aName ),
      },
      {
         name => ( $aName = "finish" ),
         type => 'double[]',
         descrip => ArgDescrip( $fName, $aName ),
      },
   ],
);

makeNativeMethod(
   name => ( $fName = "genCurve" ),
   purpose => FuncPurpose( $fName ),
   descrip => FuncDescrip( $fName ),
   return => { type => 'void' },
   params => [
      {
         name => ( $aName = "map" ),
         type => 'Mapping',
         descrip => ArgDescrip( $fName, $aName ),
      },
   ],
);

makeNativeMethod(
   name => ( $fName = "grid" ),
   purpose => FuncPurpose( $fName ),
   descrip => FuncDescrip( $fName ),
   return => { type => 'void' },
   params => [],
);

makeNativeMethod(
   name => ( $fName = "gridLine" ),
   purpose => FuncPurpose( $fName ),
   descrip => FuncDescrip( $fName ),
   return => { type => 'void' },
   params => [
      {
         name => ( $aName = "axis" ),
         type => 'int',
         descrip => ArgDescrip( $fName, $aName ),
      },
      {
         name => ( $aName = "start" ),
         type => 'double[]',
         descrip => ArgDescrip( $fName, $aName ),
      },
      {
         name => ( $aName = "length" ),
         type => 'double',
         descrip => ArgDescrip( $fName, $aName ),
      },
   ],
);


makeNativeMethod(
   name => ( $fName = "mark" ),
   purpose => FuncPurpose( $fName ),
   descrip => FuncDescrip( $fName ),
   return => { type => 'void' },
   params => [
      {
         name => ( $aName = "nmark" ),
         type => 'int',
         descrip => ArgDescrip( $fName, $aName ),
      },
      {
         name => ( $aName = "ncoord" ),
         type => 'int',
         descrip => ArgDescrip( $fName, $aName ),
      },
      {
         name => ( $aName = "in" ),
         type => 'double[][]',
         descrip => ArgDescrip( $fName, $aName ),
      },
      {
         name => ( $aName = "type" ),
         type => 'int',
         descrip => q{
            a value specifying the type (e.g. shape) of the marker 
            to be drawn.  The available types are defined by the
            Grf object which implements the graphics (by default
            this is {@link uk.ac.starlink.ast.grf.DefaultGrf}, 
            which uses the types defined by 
            {@link uk.ac.starlink.ast.grf.DefaultGrfMarker}).
         }
      }
   ],
);

makeNativeMethod(
   name => ( $fName = "polyCurve" ),
   purpose => FuncPurpose( $fName ),
   descrip => FuncDescrip( $fName ),
   return => { type => 'void' },
   params => [
      {
         name => ( $aName = "npoint" ),
         type => 'int',
         descrip => ArgDescrip( $fName, $aName ),
      },
      {
         name => ( $aName = "ncoord" ),
         type => 'int',
         descrip => ArgDescrip( $fName, $aName ),
      },
      {
         name => ( $aName = "in" ),
         type => 'double[][]',
         descrip => ArgDescrip( $fName, $aName ),
      },
   ],
);

makeNativeMethod(
   name => ( $fName = "text" ),
   purpose => FuncPurpose( $fName ),
   descrip => FuncDescrip( $fName ),
   return => { type => 'void' },
   params => [
      {
         name => ( $aName = "text" ),
         type => 'String',
         descrip => ArgDescrip( $fName, $aName ),
      },
      {
         name => ( $aName = "pos" ),
         type => 'double[]',
         descrip => ArgDescrip( $fName, $aName ),
      },
      {
         name => ( $aName = "up" ),
         type => 'float[]',
         descrip => ArgDescrip( $fName, $aName ),
      },
      {
         name => ( $aName = "just" ),
         type => 'String',
         descrip => ArgDescrip( $fName, $aName ),
      },
   ],
);

my( @args );

@args = (
   name => ( $aName = "border" ),
   type => 'boolean',
   purpose => AttPurpose( $aName ),
   descrip => AttDescrip( $aName ),
);
makeGetAttrib( @args );
makeSetAttrib( @args );

@args = (
   name => ( $aName = "clip" ),
   type => 'int',
   purpose => AttPurpose( $aName ),
   descrip => AttDescrip( $aName ),
);
makeGetAttrib( @args );
makeSetAttrib( @args );

@args = (
   name => ( $aName = "clipOp" ),
   type => 'boolean',
   purpose => AttPurpose( $aName ),
   descrip => AttDescrip( $aName ),
);
makeGetAttrib( @args );
makeSetAttrib( @args );

@args = (
   name => ( $aName = "colour" ),
   type => 'int',
   purpose => AttPurpose( $aName ),
   descrip => AttDescrip( $aName ),
   extra => q{
      The mapping of an integer value to a colour is done by the
      Grf object which implements the graphics.  By default this is
      a {@link uk.ac.starlink.ast.grf.DefaultGrf} object,
      which interprets the value like the result of a Color.getRGB()
      call on the {@link java.awt.Color} object which represents the colour
      to use.  
      This means the top 8 bits of the integer represent alpha blending
      (<code>0xff</code> means opaque and <code>0x00</code> means transparent)
      and the bottom 24 bits represent RGB intensity.  For example 
      opaque (i.e. normal) light grey would be <code>0xffc0c0c0</code>
      and semi-transparent light grey would be <code>0x80c0c0c0</code>.
      <p>
      <b>Note that</b> this means using a simple 24-bit RGB representation
      will give you a transparent colour, which is probably not what you
      want.  Assuming you want to be able to see the results you should
      do <code>setColour(java.awt.Color.LIGHT_GRAY.getRGB())</code>
      or <code>setColour(0xffc0c0c0)</code>, 
      and <i>not</i> <code>setColour(0xc0c0c0)</code>.

      @see uk.ac.starlink.ast.grf.DefaultGrf#attr
   }
);
makeGetAttribByElement( @args );
makeSetAttribByElement( @args );
makeSetAttrib( @args );

@args = (
   name => ( $aName = "drawAxes" ),
   type => 'boolean',
   purpose => AttPurpose( $aName ),
   descrip => AttDescrip( $aName ),
);
makeGetAttribByAxis( @args );
makeSetAttribByAxis( @args );
makeGetAttrib( @args );
makeSetAttrib( @args );

@args = (
   name => ( $aName = "drawTitle" ),
   type => 'boolean',
   purpose => AttPurpose( $aName ),
   descrip => AttDescrip( $aName ),
);
makeGetAttrib( @args );
makeSetAttrib( @args );

@args = (
   name => ( $aName = "edge" ),
   type => 'String',
   purpose => AttPurpose( $aName ),
   descrip => AttDescrip( $aName ),
);
makeGetAttribByAxis( @args );
makeSetAttribByAxis( @args );

my $escDescrip = AttDescrip( "escape" );
$escDescrip =~ 
   s/astEscapes/{\@link uk.ac.starlink.ast.grf.GrfEscape#setEscapes}/g;
@args = (
   name => ( $aName = "escape" ),
   type => 'boolean',
   purpose => AttPurpose( $aName ),
   descrip => $escDescrip,
   extra => q{
       @see  uk.ac.starlink.ast.grf.GrfEscape
   },
);
makeGetAttrib( @args );
makeSetAttrib( @args );

@args = (
   name => ( $aName = "font" ),
   type => 'int',
   purpose => AttPurpose( $aName ),
   descrip => AttDescrip( $aName ),
   extra => q{
      
      In the default Grf implementation 
      ({@link uk.ac.starlink.ast.grf.DefaultGrf}
      the submitted integer is mapped to a font using the 
      {@link uk.ac.starlink.ast.grf.DefaultGrfFontManager} class.

      @see uk.ac.starlink.ast.grf.DefaultGrf#attr
   },
);
makeGetAttribByElement( @args );
makeSetAttribByElement( @args );
makeGetAttrib( @args );
makeSetAttrib( @args );

@args = (
   name => ( $aName = "gap" ),
   type => 'double',
   purpose => AttPurpose( $aName ),
   descrip => AttDescrip( $aName ),
);
makeGetAttribByAxis( @args );
makeSetAttribByAxis( @args );
makeSetAttrib( @args );

@args = (
   name => ( $aName = "grid" ),
   type => 'boolean',
   purpose => AttPurpose( $aName ),
   descrip => AttDescrip( $aName ),
);
makeGetAttrib( @args );
makeSetAttrib( @args );

@args = (
   name => ( $aName = "invisible" ),
   type => 'boolean',
   purpose => AttPurpose( $aName ),
   descrip => AttDescrip( $aName ),
);
makeGetAttrib( @args );
makeSetAttrib( @args );

@args = (
   name => ( $aName = "labelAt" ),
   type => 'double',
   purpose => AttPurpose( $aName ),
   descrip => AttDescrip( $aName ),
);
makeGetAttribByAxis( @args );
makeSetAttribByAxis( @args );

@args = (
   name => ( $aName = "labelUnits" ),
   type => 'boolean',
   purpose => AttPurpose( $aName ),
   descrip => AttDescrip( $aName ),
);
makeGetAttribByAxis( @args );
makeSetAttribByAxis( @args );
makeSetAttrib( @args );

@args = (
   name => ( $aName = "labelUp" ),
   type => 'boolean',
   purpose => AttPurpose( $aName ),
   descrip => AttDescrip( $aName ),
);
makeGetAttribByAxis( @args );
makeSetAttribByAxis( @args );
makeSetAttrib( @args );

@args = (
   name => ( $aName = "labelling" ),
   type => 'String',
   purpose => AttPurpose( $aName ),
   descrip => AttDescrip( $aName ),
);
makeGetAttrib( @args );
makeSetAttrib( @args );

@args = (
   name => ( $aName = "logGap" ),
   type => 'double',
   purpose => AttPurpose( $aName ),
   descrip => AttDescrip( $aName ),
);
makeGetAttribByAxis( @args );
makeSetAttribByAxis( @args );
makeSetAttrib( @args );

@args = (
   name => ( $aName = "logLabel" ),
   type => 'boolean',
   purpose => AttPurpose( $aName ),
   descrip => AttDescrip( $aName ),
);
makeGetAttribByAxis( @args );
makeSetAttribByAxis( @args );
makeSetAttrib( @args );

@args = (
   name => ( $aName = "logPlot" ),
   type => 'boolean',
   purpose => AttPurpose( $aName ),
   descrip => AttDescrip( $aName ),
);
makeGetAttribByAxis( @args );
makeSetAttribByAxis( @args );
makeSetAttrib( @args );

@args = (
   name => ( $aName = "logTicks" ),
   type => 'boolean',
   purpose => AttPurpose( $aName ),
   descrip => AttDescrip( $aName ),
);
makeGetAttribByAxis( @args );
makeSetAttribByAxis( @args );
makeSetAttrib( @args );

@args = (
   name => ( $aName = "majTickLen" ),
   type => 'double',
   purpose => AttPurpose( $aName ),
   descrip => AttDescrip( $aName ),
);
makeGetAttribByAxis( @args );
makeSetAttribByAxis( @args );
makeSetAttrib( @args );

@args = (
   name => ( $aName = "minTickLen" ),
   type => 'double',
   purpose => AttPurpose( $aName ),
   descrip => AttDescrip( $aName ),
);
makeGetAttribByAxis( @args );
makeSetAttribByAxis( @args );
makeSetAttrib( @args );

@args = (
   name => ( $aName = "minTick" ),
   type => 'int',
   purpose => AttPurpose( $aName ),
   descrip => AttDescrip( $aName ),
);
makeGetAttribByAxis( @args );
makeSetAttribByAxis( @args );
makeSetAttrib( @args );

@args = (
   name => ( $aName = "numLab" ),
   type => 'boolean',
   purpose => AttPurpose( $aName ),
   descrip => AttDescrip( $aName ),
);
makeGetAttribByAxis( @args );
makeSetAttribByAxis( @args );
makeSetAttrib( @args );

@args = (
   name => ( $aName = "numLabGap" ),
   type => 'double',
   purpose => AttPurpose( $aName ),
   descrip => AttDescrip( $aName ),
);
makeGetAttribByAxis( @args );
makeSetAttribByAxis( @args );
makeSetAttrib( @args );

@args = (
   name => ( $aName = "size" ),
   type => 'double',
   purpose => AttPurpose( $aName ),
   descrip => AttDescrip( $aName ),
   extra => q{
      The implementation of this method is determined by the current Grf
      object.

      @see uk.ac.starlink.ast.grf.DefaultGrf#attr
   }
);
makeGetAttribByElement( @args );
makeSetAttribByElement( @args );
makeGetAttrib( @args );
makeSetAttrib( @args );

@args = (
   name => ( $aName = "style" ),
   type => 'int',
   purpose => AttPurpose( $aName ),
   descrip => AttDescrip( $aName ),
   extra => q{

      The available line styles are defined by the Grf object being used to 
      implement the graphics 
      (by default {@link uk.ac.starlink.ast.grf.DefaultGrf}).

      @see uk.ac.starlink.ast.grf.DefaultGrf#attr
   },
);
makeGetAttribByElement( @args );
makeSetAttribByElement( @args );
makeSetAttrib( @args );

@args = (
   name => ( $aName = "textLab" ),
   type => 'boolean',
   purpose => AttPurpose( $aName ),
   descrip => AttDescrip( $aName ),
);
makeGetAttribByAxis( @args );
makeSetAttribByAxis( @args );
makeSetAttrib( @args );

@args = (
   name => ( $aName = "textLabGap" ),
   type => 'double',
   purpose => AttPurpose( $aName ),
   descrip => AttDescrip( $aName ),
);
makeGetAttribByAxis( @args );
makeSetAttribByAxis( @args );
makeSetAttrib( @args );

@args = (
   name => ( $aName = "tickAll" ),
   type => 'boolean',
   purpose => AttPurpose( $aName ),
   descrip => AttDescrip( $aName ),
);
makeGetAttrib( @args );
makeSetAttrib( @args );

@args = (
   name => ( $aName = "titleGap" ),
   type => 'double',
   purpose => AttPurpose( $aName ),
   descrip => AttDescrip( $aName ),
);
makeGetAttrib( @args );
makeSetAttrib( @args );

@args = (
   name => ( $aName = "tol" ),
   type => 'double',
   purpose => AttPurpose( $aName ),
   descrip => AttDescrip( $aName ),
);
makeGetAttrib( @args );
makeSetAttrib( @args );

@args = (
   name => ( $aName = "width" ),
   type => 'double',
   purpose => AttPurpose( $aName ),
   descrip => AttDescrip( $aName ),
   extra => q{
      The implementation of this method is determined by the current Grf
      object.

      @see uk.ac.starlink.ast.grf.DefaultGrf#attr
   }
);
makeGetAttribByElement( @args );
makeSetAttribByElement( @args );
makeSetAttrib( @args );

print "}\n";


