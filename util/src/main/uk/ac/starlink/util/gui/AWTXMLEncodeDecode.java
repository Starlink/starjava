/*
 * Copyright (C) 2004 Central Laboratory of the Research Councils
 *
 *  History:
 *    19-JAN-2004 (Peter W. Draper):
 *       Original version.
 */
package uk.ac.starlink.util.gui;

import java.awt.Color;
import java.awt.Font;
import java.awt.AlphaComposite;
import uk.ac.starlink.util.PrimitiveXMLEncodeDecode;
import org.w3c.dom.Element;

/**
 * A static utility class for encoding and decoding some AWT
 * primitives to and from XML.
 *
 * @author Peter W. Draper
 * @version $Id$
 */
public class AWTXMLEncodeDecode
{
    /**
     * Static class, so no public constructor.
     */
    private AWTXMLEncodeDecode()
    {
        // Do nothing.
    }

    /**
     * Add an element with a Font value as a child of another element.
     */
    public static void addChildElement( Element rootElement, String name,
                                        Font value )
    {
        PrimitiveXMLEncodeDecode.addChildElement( rootElement,
                                                  name,
                                                  fontToString( value ) );
    }

    /**
     * Convert a Font to a string.
     */
    public static String fontToString( Font font )
    {
        String style;
        switch ( font.getStyle() ) {
           case Font.BOLD | Font.ITALIC:
               style =  "bolditalic";
               break;
           case Font.BOLD:
               style = "bold";
               break;
           case Font.ITALIC:
               style = "italic";
               break;
           default:
               style = "plain";
        }
        return font.getFamily() + "-" + style + "-" + font.getSize();
    }

    /**
     * Convert a String back to a Font.
     */
    public static Font fontFromString( String value )
    {
        return Font.decode( value );
    }

    /**
     * Add an element with a Color value as a child of another element.
     */
    public static void addChildElement( Element rootElement, String name,
                                        Color value )
    {
        PrimitiveXMLEncodeDecode.addChildElement( rootElement,
                                                  name,
                                                  colorToString( value ) );
    }

    /**
     * Convert a Color object to a string.
     */
    public static String colorToString( Color value )
    {
        int ivalue = value.getRGB();
        if ( ivalue == -1 ) {
            ivalue = 0;
        }
        return Integer.toString( ivalue );
    }

    /**
     * Convert a String object back to a Color object.
     */
    public static Color colorFromString( String value )
    {
        return
            new Color( PrimitiveXMLEncodeDecode.intFromString( value ) );
    }

    /**
     * Add an element with an AlphaComposite value as a child of
     * another element.
     */
    public static void addChildElement( Element rootElement, String name,
                                        AlphaComposite value )
    {
        PrimitiveXMLEncodeDecode.addChildElement( rootElement,
                                                  name,
                                                  compositeToString(value));
    }

    /**
     * Convert an AlphaComposite object to a string.
     */
    public static String compositeToString( AlphaComposite value )
    {
        int ivalue = value.getRule();
        float fvalue = value.getAlpha();
        return Integer.toString( ivalue ) + ":" + Float.toString( fvalue );
    }

    /**
     * Convert a String object back to an AlphaComposite object.
     */
    public static AlphaComposite compositeFromString( String value )
    {
        String[] subValues = value.split( ":" );
        return
            AlphaComposite.getInstance( PrimitiveXMLEncodeDecode
                                           .intFromString( subValues[0] ),
                                        (float) PrimitiveXMLEncodeDecode
                                           .doubleFromString( subValues[1] ));
    }
}

