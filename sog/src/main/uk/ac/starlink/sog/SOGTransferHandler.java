/*
 * Copyright (C) 2003 Central Laboratory of the Research Councils
 *
 *  History:
 *     31-JUL-2003 (Peter W. Draper):
 *       Original version.
 */
package uk.ac.starlink.sog;

import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.net.URL;

import javax.xml.transform.stream.StreamSource;

import javax.swing.TransferHandler;
import javax.swing.JComponent;
import javax.swing.JList;

import uk.ac.starlink.jaiutil.HDXImage;

/**
 * A TransferHandler for accepting drop events that may contain
 * serialised images that we can display.
 *
 * @author Peter W. Draper
 * @version $Id$
 */
public class SOGTransferHandler
    extends TransferHandler
{
    /**
     * The flavors of objects that can be dropped.
     */
    public static final DataFlavor flavors[] = {

        // Flavors we can accept from Treeview.
        new DataFlavor( "application/xml;class=java.io.InputStream",
                        "NDX stream" ),
        new DataFlavor( DataFlavor.javaSerializedObjectMimeType +
                        ";class=java.net.URL", "URL" )
        /*NOT YET new DataFlavor( "application/fits;class=java.io.InputStream",
                                  "FITS stream" ),*/
    };

    /**
     * The component to display into.
     */
    protected SOGNavigatorImageDisplay imageDisplay = null;

    /**
     * Create an instance, remembering the component we're targetting.
     */
    public SOGTransferHandler( SOGNavigatorImageDisplay imageDisplay )
    {
        this.imageDisplay = imageDisplay;
    }

    public boolean canImport( JComponent comp, DataFlavor flavor[] )
    {
        for ( int i = 0, n = flavor.length; i < n; i++ ) {
            for ( int j = 0, m = flavors.length; j < m; j++ ) {
                if ( flavor[i].equals( flavors[j] ) ) {
                    return true;
                }
            }
        }
        return false;
    }

    public int getSourceActions( JComponent c )
    {
        return TransferHandler.COPY;
    }

    //  Drop event, extract the image from the Transferable, if we can.
    public boolean importData( JComponent comp, Transferable t )
    {
        DataFlavor[] importFlavors = t.getTransferDataFlavors();
        for ( int j = 0; j < importFlavors.length; j++ ) {
            if ( flavors[0].match( importFlavors[j] ) ) {
                return importNDXStream( comp, t );
            }
            if ( flavors[1].match( importFlavors[j] ) ) {
                return importURL( comp, t );
            }
            /*if ( flavors[2].match( importFlavors[j] ) ) {
              return importFITSStream( comp, t );
              }*/
        }
        return false;
    }

    protected boolean importNDXStream( JComponent comp, Transferable t )
    {
        boolean added = false;
        InputStream inputStream = null;
        try {
            inputStream = (InputStream) t.getTransferData( flavors[0] );
            StreamSource streamSource = new StreamSource( inputStream );
            HDXImage hdxImage = new HDXImage( streamSource );
            imageDisplay.setHDXImage( hdxImage );
            added = true;
        }
        catch (Exception e) {
            e.printStackTrace();
        }
        try { 
            inputStream.close();
        }
        catch (Exception e) {
            // Do nothing.
        }
        return added;
    }

    protected boolean importFITSStream( JComponent comp, Transferable t )
    {
        // NDX as FITS stream?
        System.out.println( "No Support for FITS streams" );
        return false;
    }

    protected boolean importURL( JComponent comp, Transferable t )
    {
        boolean added = false;
        try {
            URL url = (URL) t.getTransferData( flavors[1] );
            imageDisplay.setFilename( url.toString() );
            added = true;
        }
        catch (Exception e) {
            e.printStackTrace();
        }
        return added;
    }
}
