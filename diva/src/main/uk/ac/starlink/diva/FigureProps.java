/*
 * Copyright (C) 2002-2004 Central Laboratory of the Research Councils
 *
 *  History:
 *     20-FEB-2002 (Peter W. Draper):
 *       Original version.
 *     27-JAN-2004 (Peter W. Draper):
 *       Moved to DIVA package from SPLAT.
 */
package uk.ac.starlink.diva;

import java.awt.Color;
import java.awt.Font;
import java.awt.Paint;
import java.awt.AlphaComposite;
import java.util.List;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;

import org.w3c.dom.Element;

import uk.ac.starlink.diva.interp.BasicInterpolatorFactory;
import uk.ac.starlink.diva.interp.Interpolator;
import uk.ac.starlink.diva.interp.InterpolatorFactory;
import uk.ac.starlink.util.PrimitiveXMLEncodeDecode;
import uk.ac.starlink.util.XMLEncodeDecode;
import uk.ac.starlink.util.gui.AWTXMLEncodeDecode;

/**
 * A container class for storing the configuration properties of Figures
 * created by {@link DrawFigureFactory}. Also provides facilities for
 * creating a serialized version of the properties (so that the Figure can be
 * re-created) in XML.
 *
 * @author Peter W. Draper
 * @version $Id$
 * @see Draw
 * @see DrawFigureFactory
 * @see DrawActions
 */
public class FigureProps
    implements XMLEncodeDecode
{
    /**
     * DrawFigureFactory constant for this figure type.
     */
    private int type;

    /**
     * First X coordinate
     */
    private double x1;

    /**
     * First Y coordinate
     */
    private double y1;

    /**
     * Second X coordinate
     */
    private double x2;

    /**
     * Second Y coordinate
     */
    private double y2;

    /**
     * Array of X coordinates.
     */
    private double[] xa;

    /**
     * Array of Y coordinates.
     */
    private double[] ya;

    /**
     * Width
     */
    private double width;

    /**
     * Height
     */
    private double height;

    /**
     * Outline colour.
     */
    private Paint outline;

    /**
     * Fill colour.
     */
    private Paint fill;

    /**
     * A thickness for lines
     */
    private double thickness;

    /**
     * An Interpolator (for curves).
     */
    private Interpolator interpolator;

    /**
     * The InterpolatorFactory reference.
     */
    protected InterpolatorFactory interpolatorFactory = 
        BasicInterpolatorFactory.getInstance();

    /**
     * A String for a text display.
     */
    private String text = null;

    /**
     * A Font for the text string.
     */
    private Font font = null;

    /**
     * AlphaComposite for combining the figure.
     */
    private AlphaComposite composite  = null;
    private static final AlphaComposite
        defaultComposite = AlphaComposite.SrcOver;

    /**
     *  Default constructor. All items keep their default values.
     */
    public FigureProps()
    {
        reset();
    }

    /**
     *  Copy constructor.
     */
    public FigureProps( FigureProps props )
    {
        reset();
        copy( props );
    }

    /**
     *  Constructor that provides enough information to describe a
     *  rectangle.
     */
    public FigureProps( double x1, double y1, double width,
                        double height )
    {
        reset();
        setX1( x1 );
        setY1( y1 );
        setWidth( width );
        setHeight( height );
    }

    /**
     *  Constructor that provides enough information to describe a
     *  rectangle.
     */
    public FigureProps( double x1, double y1, double width,
                        double height, Paint outline )
    {
        reset();
        setX1( x1 );
        setY1( y1 );
        setHeight( width );
        setWidth( height );
        setOutline( outline );
    }

    /**
     *  Constructor that provides enough information to describe a
     *  rectangle, with other colour.
     */
    public FigureProps( double x1, double y1,
                        double width, double height,
                        double x2, double y2,
                        Paint outline, Paint fill )
    {
        reset();
        setX1( x1 );
        setY1( y1 );
        setX2( x2 );
        setY2( y2 );
        setWidth( width );
        setHeight( height );
        setOutline( outline );
        setFill( fill );
    }

    /**
     *  Constructor that provides enough information to describe an
     *  interpolated curve.
     */
    public FigureProps( Interpolator interpolator,
                        double x1, double y1,
                        Paint outline, double thickness )
    {
        reset();
        setInterpolator( interpolator );
        setX1( x1 );
        setY1( y1 );
        setOutline( outline );
        setThickness( thickness );
    }

    /**
     *  Constructor that provides enough information to describe a
     *  text String.
     */
    public FigureProps( double x1, double y1, String text, Font font,
                        Paint outline )
    {
        reset();
        setX1( x1 );
        setY1( y1 );
        setOutline( outline );
        setText( text );
        setFont( font );
    }

    /**
     *  Reset all items to their defaults.
     */
    public void reset()
    {
        setType( -1 );
        setX1( 0.0 );
        setY1( 0.0 );
        setX2( 0.0 );
        setY2( 0.0 );
        setXArray( null );
        setYArray( null );
        setWidth( 1.0 );
        setHeight( 1.0 );
        setOutline( Color.black );
        setFill( null );
        setInterpolator( null );
        setThickness( 1.0 );
        setText( null );
        setFont( null );
        setComposite( null );
    }

    /**
     *  Copy all items from another instance.
     */
    public void copy( FigureProps props )
    {
        setType( props.getType() );
        setX1( props.getX1() );
        setY1( props.getY1() );
        setX2( props.getX2() );
        setY2( props.getY2() );
        setXArray( props.getXArray() );
        setYArray( props.getYArray() );
        setWidth( props.getWidth() );
        setHeight( props.getHeight() );
        setOutline( props.getOutline() );
        setFill( props.getFill() );
        setInterpolator( props.getInterpolator() );
        setThickness( props.getThickness() );
        setText( props.getText() );
        setFont( props.getFont() );
        setComposite( props.getComposite() );
    }

    /**
     * Get the type of figure.
     *
     * @return the type of figure, -1 if not set, otherwise a constant
     * from {@link DrawFigureFactory}.
     */
    public int getType()
    {
        return type;
    }

    /**
     * Set the type of figure.
     *
     * @param type the type of figure, one of the constants from
     *             {@link DrawFigureFactory}.
     */
    public void setType( int type )
    {
        this.type = type;
    }

    /**
     * Set the type of figure using a symbolic name.
     *
     * @param type the type of figure, one of the values from
     * shortName of {@link DrawFigureFactory}.

     */
    public void setType( String type )
    {
        this.type = -1;
        for ( int i = 0; i < DrawFigureFactory.NUM_FIGURES; i++ ) {
            if ( DrawFigureFactory.SHORTNAMES[i].equals( type ) ) {
                this.type = i;
                return;
            }
        }
    }

    /**
     * Get the value of x1
     *
     * @return value of x1.
     */
    public double getX1()
    {
        return x1;
    }

    /**
     * Set the value of x1.
     *
     * @param x1 Value to assign to x1.
     */
    public void setX1( double x1 )
    {
        this.x1 = x1;
    }

    /**
     * Get the value of y1
     *
     * @return value of y1
     */
    public double getY1()
    {
        return y1;
    }

    /**
     * Set the value of y1.
     *
     * @param y1 Value to assign to y1.
     */
    public void setY1( double y1 )
    {
        this.y1 = y1;
    }

    /**
     * Get the value of x2.
     *
     * @return value of x2.
     */
    public double getX2()
    {
        return x2;
    }

    /**
     * Set the value of x2.
     * @param x2  Value to assign to x2.
     */
    public void setX2( double x2 )
    {
        this.x2 = x2;
    }

    /**
     * Get the value of y2.
     * @return value of y2.
     */
    public double getY2()
    {
        return y2;
    }

    /**
     * Set the value of y2.
     * @param y2 Value to assign to y2.
     */
    public void setY2( double y2 )
    {
        this.y2 = y2;
    }

    /**
     * Get the array of X coordinates.
     *
     * @return array of X coordinates.
     */
    public double[] getXArray()
    {
        return xa;
    }

    /**
     * Set the array of X coordinates.
     *
     */
    public void setXArray( double[] xa )
    {
        this.xa = xa;
    }

    /**
     * Get the array of Y coordinates.
     *
     * @return array of Y coordinates.
     */
    public double[] getYArray()
    {
        return ya;
    }

    /**
     * Set the array of Y coordinates.
     *
     */
    public void setYArray( double[] ya )
    {
        this.ya = ya;
    }

    /**
     * Get the value of width
     * @return value of width.
     */
    public double getWidth()
    {
        return width;
    }

    /**
     * Set the value of width
     * @param width Value to assign to xLength.
     */
    public void setWidth( double width )
    {
        this.width = width;
    }

    /**
     * Get the value of height
     * @return value of height.
     */
    public double getHeight()
    {
        return height;
    }

    /**
     * Set the value of height
     * @param height Value to assign to height
     */
    public void setHeight( double height )
    {
        this.height = height;
    }

    /**
     * Get the value of outline
     * @return value of outline.
     */
    public Paint getOutline()
    {
        return outline;
    }

    /**
     * Set the value of outline
     * @param outline Value to assign to outline
     */
    public void setOutline( Paint outline )
    {
        this.outline = outline;
    }

    /**
     * Get the value of fill
     * @return value of fill
     */
    public Paint getFill()
    {
        return fill;
    }

    /**
     * Set the value of fill
     * @param fill Value to assign to fill
     */
    public void setFill( Paint  fill )
    {
        this.fill = fill;
    }

    /**
     * Get the value of interpolator.
     *
     * @return value of interpolator.
     */
    public Interpolator getInterpolator()
    {
        return interpolator;
    }

    /**
     * Set the value of interpolator.
     *
     * @param interpolator Value to assign to interpolator.
     */
    public void setInterpolator( Interpolator interpolator )
    {
        this.interpolator = interpolator;
    }

    /**
     * Get the value of thickness.
     *
     * @return value of thickness
     */
    public double getThickness()
    {
        return thickness;
    }

    /**
     * Set the value of thickness.
     *
     * @param thickness Value to assign to thickness.
     */
    public void setThickness( double thickness )
    {
        this.thickness = thickness;
    }

    /**
     * Get the Text.
     *
     * @return value of text
     */
    public String getText()
    {
        return text;
    }

    /**
     * Set the text Text.
     *
     * @param text Value to assign to text.
     */
    public void setText( String text )
    {
        this.text = text;
    }

    /**
     * Get the Font.
     *
     * @return value of font
     */
    public Font getFont()
    {
        return font;
    }

    /**
     * Set the Font use together with the text String.
     *
     * @param font Value to assign to font.
     */
    public void setFont( Font font )
    {
        this.font = font;
    }

    /**
     * Get the AlphaComposite.
     *
     * @return value of composite
     */
    public AlphaComposite getComposite()
    {
        return composite;
    }

    /**
     * Set the AlphaComposite to use when drawing the figure.
     *
     * @param composite Value to assign to composite.
     */
    public void setComposite( AlphaComposite composite )
    {
        if ( composite != null ) {
            this.composite = composite;
        }
        else {
            this.composite = defaultComposite;
        }
    }

    public String toString()
    {
        return "FigureProps: " +
            " type = " + type +
            " x1 = " + x1 +
            " y1 = " + y1 +
            " x2 = " + x2 +
            " y2 = " + y2 +
            " width = " + width +
            " height = " + height +
            " outline = " + outline +
            " fill = " + fill +
            " thickness = " + thickness +
            " interpolator = " + interpolator +
            " text = " + text +
            " font = " + font +
            " composite = " + composite;
    }

    public void encode( Element rootElement )
    {
        //  Create children nodes for each of the properties that
        //  have been set.

        //  Type as symbolic name.
        String shortName = "unknown";
        if ( type != -1 ) {
            shortName = DrawFigureFactory.SHORTNAMES[type];
        }
        PrimitiveXMLEncodeDecode.addChildElement( rootElement, "type",
                                                  shortName );

        //  Positions.
        PrimitiveXMLEncodeDecode.addChildElement( rootElement, "x1", x1 );
        PrimitiveXMLEncodeDecode.addChildElement( rootElement, "y1", y1 );
        PrimitiveXMLEncodeDecode.addChildElement( rootElement, "x2", x2 );
        PrimitiveXMLEncodeDecode.addChildElement( rootElement, "y2", y2 );
        if ( xa != null && ya != null ) {
            //  Encode these in base64.
            Element base64Element = PrimitiveXMLEncodeDecode
                .addChildElement( rootElement, "xarray",
                                  encodeBase64DoubleArray( xa ));
            base64Element.setAttribute( "size",
               PrimitiveXMLEncodeDecode.intToString( xa.length ) );
            base64Element.setAttribute( "encoding", "base64" );

            base64Element = PrimitiveXMLEncodeDecode
                .addChildElement( rootElement, "yarray",
                                  encodeBase64DoubleArray( ya ));
            base64Element.setAttribute( "size",
               PrimitiveXMLEncodeDecode.intToString( ya.length ) );
            base64Element.setAttribute( "encoding", "base64" );
        }

        //  Lengths.
        PrimitiveXMLEncodeDecode.addChildElement( rootElement,
                                                  "width", width );
        PrimitiveXMLEncodeDecode.addChildElement( rootElement,
                                                  "height", height );

        //  Colors.
        if ( outline != null ) {
            AWTXMLEncodeDecode.addChildElement( rootElement,
                                                "outline", (Color)outline );
        }
        if ( fill != null ) {
            AWTXMLEncodeDecode.addChildElement( rootElement, "fill",
                                                (Color) fill );
        }

        //  Line thickness.
        PrimitiveXMLEncodeDecode.addChildElement( rootElement,
                                                  "thickness", thickness );

        //  Curve interpolator.
        if ( interpolator != null ) {
            writeInterpolator( PrimitiveXMLEncodeDecode
                               .addChildElement(rootElement,"interpolator") );
        }

        //  String.
        if ( text != null ) {
            PrimitiveXMLEncodeDecode.addChildElement( rootElement, "text",
                                                      text );
        }
        if ( font != null ) {
            AWTXMLEncodeDecode.addChildElement( rootElement, "font", font );
        }

        //  Composite.
        if ( composite != null ) {
            AWTXMLEncodeDecode.addChildElement( rootElement, "composite",
                                                composite );
        }
    }

    public void decode( Element rootElement )
    {
        List children =
            PrimitiveXMLEncodeDecode.getChildElements( rootElement );
        int size = children.size();
        Element element = null;
        String name = null;
        String value = null;
        for ( int i = 0; i < size; i++ ) {
            element = (Element) children.get( i );
            name = PrimitiveXMLEncodeDecode.getElementName( element );
            value = PrimitiveXMLEncodeDecode.getElementValue( element );
            setFromString( name, value, element );
        }
    }

    public String getTagName()
    {
        //  All figures start <drawfigure>
        //                    <type>name</type>
        //                    ...
        //                    </drawfigure>
        return "drawfigure";
    }

    /**
     * Set the value of a member variable by matching its name to a
     * known local property string.
     */
    public void setFromString( String name, String value, Element element )
    {
        if ( name.equals( "type" ) ) {
            setType( value );
            return;
        }
        if ( name.equals( "x1" ) ) {
            setX1( PrimitiveXMLEncodeDecode.doubleFromString( value ) );
            return;
        }
        if ( name.equals( "y1" ) ) {
            setY1( PrimitiveXMLEncodeDecode.doubleFromString( value ) );
            return;
        }
        if ( name.equals( "x2" ) ) {
            setX2( PrimitiveXMLEncodeDecode.doubleFromString( value ) );
            return;
        }
        if ( name.equals( "y2" ) ) {
            setY2( PrimitiveXMLEncodeDecode.doubleFromString( value ) );
            return;
        }
        if ( name.equals( "xarray" ) ) {
            String ssize = element.getAttribute( "size" );
            int size = PrimitiveXMLEncodeDecode.intFromString( ssize );
            setXArray( decodeBase64DoubleArray( size, value ) );
            return;
        }
        if ( name.equals( "yarray" ) ) {
            String ssize = element.getAttribute( "size" );
            int size = PrimitiveXMLEncodeDecode.intFromString( ssize );
            setYArray( decodeBase64DoubleArray( size, value ) );
            return;
        }
        if ( name.equals( "width" ) ) {
            setWidth( PrimitiveXMLEncodeDecode.doubleFromString( value ) );
            return;
        }
        if ( name.equals( "height" ) ) {
            setHeight( PrimitiveXMLEncodeDecode.doubleFromString( value ) );
            return;
        }
        if ( name.equals( "outline" ) ) {
            setOutline( AWTXMLEncodeDecode.colorFromString( value ) );
            return;
        }
        if ( name.equals( "fill" ) ) {
            setFill( AWTXMLEncodeDecode.colorFromString( value ) );
            return;
        }
        if ( name.equals( "thickness" ) ) {
            setThickness(PrimitiveXMLEncodeDecode.doubleFromString(value));
            return;
        }
        if ( name.equals( "interpolator" ) ) {
            readInterpolator( element );
            return;
        }
        if ( name.equals( "text" ) ) {
            setText( value );
            return;
        }
        if ( name.equals( "font" ) ) {
            setFont( AWTXMLEncodeDecode.fontFromString( value ) );
            return;
        }
        if ( name.equals( "composite" ) ) {
            setComposite( AWTXMLEncodeDecode.compositeFromString( value ) );
            return;
        }
    }

    /**
     * Read a stored interpolator from the given Element.
     *
     * <interpolator class="type">
     *    <xarray size="n" encoding="base64">base64enc</xarray>
     *    <yarray size="n" encoding="bass64">base64enc</yarray>
     * </interpolator>
     */
    protected void readInterpolator( Element element )
    {
        // Make an interpolator suitable to the stored type.
        String name = element.getAttribute( "type" );
        int type = interpolatorFactory.getTypeFromName( name );
        interpolator = interpolatorFactory.makeInterpolator( type );

        //  X data.
        List children =
            PrimitiveXMLEncodeDecode.getChildElements( element );
        Element child = (Element) children.get( 0 );
        String value = child.getAttribute( "size" );
        int size = PrimitiveXMLEncodeDecode.intFromString( value );
        value = PrimitiveXMLEncodeDecode.getElementValue( child );
        double[] x = decodeBase64DoubleArray( size, value );

        //  Y data
        child = (Element) children.get( 1 );
        value = child.getAttribute( "size" );
        size = PrimitiveXMLEncodeDecode.intFromString( value );
        value = PrimitiveXMLEncodeDecode.getElementValue( child );
        double[] y = decodeBase64DoubleArray( size, value );

        //  Activate the Interpolator.
        interpolator.setCoords( x, y, true );
    }

    /**
     * Write description of the interpolator to the given Element.
     *
     * <interpolator type="type">
     *    <xarray size="n" encoding="base64">base64enc</xarray>
     *    <yarray size="n" encoding="base64">base64enc</yarray>
     * </interpolator>
     */
    protected void writeInterpolator( Element element )
    {
        // Add interpolator type as a string.
        int type = BasicInterpolatorFactory.getInstance()
            .getInterpolatorType( interpolator );
        String name = BasicInterpolatorFactory.getInstance()
            .getShortName( type );
        element.setAttribute( "type", name );

        //  Add X and Y vertices.
        double[] array = interpolator.getXCoords();
        String value = encodeBase64DoubleArray( array );
        Element child = PrimitiveXMLEncodeDecode.addChildElement( element,
                                                                  "xarray",
                                                                  value );
        child.setAttribute( "size",
              PrimitiveXMLEncodeDecode.intToString( array.length ) );
        element.setAttribute( "encoding", "base64" );

        array = interpolator.getYCoords();
        value = encodeBase64DoubleArray( array );
        child = PrimitiveXMLEncodeDecode.addChildElement( element,
                                                          "yarray",
                                                          value );
        child.setAttribute( "size",
              PrimitiveXMLEncodeDecode.intToString( array.length ) );
        element.setAttribute( "encoding", "base64" );
    }

    /**
     * Decode an array of double stored in a base64 string.
     */
    protected double[] decodeBase64DoubleArray( int size, String base64 )
    {
        try {
            ByteArrayInputStream bis =
                new ByteArrayInputStream( base64.getBytes() );
            Base64InputStream b64is = new Base64InputStream( bis );
            DataInputStream dis = new DataInputStream( b64is );
            double[] array = new double[size];
            for ( int i = 0; i < size; i++ ) {
                array[i] = dis.readDouble();
            }
            dis.close();
            b64is.close();
            bis.close();
            return array;
        }
        catch (Exception e) {
            // Do nothing...
            e.printStackTrace();
        }
        return null;

    }

    /**
     * Encode an array of doubles as a base64 string.
     */
    protected String encodeBase64DoubleArray( double[] array )
    {
        try {
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            Base64OutputStream b64os = new Base64OutputStream( bos );
            DataOutputStream dos = new DataOutputStream( b64os );
            int size = array.length;
            for ( int i = 0; i < size; i++ ) {
                dos.writeDouble( array[i] );
            }
            b64os.endBase64();
            String result = bos.toString();
            dos.close();
            b64os.close();
            bos.close();
            return result;
        }
        catch (Exception e) {
            // Do nothing...
            e.printStackTrace();
        }
        return null;
    }
}
