/*
 * Copyright (C) 2003 Central Laboratory of the Research Councils
 *
 *  History:
 *     19-MAR-2003 (Peter W. Draper):
 *       Original version.
 */
package uk.ac.starlink.splat.iface;

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

import uk.ac.starlink.splat.data.NDXSpecDataImpl;
import uk.ac.starlink.splat.data.SpecData;
import uk.ac.starlink.splat.data.SpecDataFactory;
import uk.ac.starlink.splat.plot.PlotControl;
import uk.ac.starlink.splat.util.SplatException;
import uk.ac.starlink.table.StarTable;
import uk.ac.starlink.table.StarTableFactory;

/**
 * A TransferHandler for dragging and dropping SpecData instances.
 * In SPLAT these are between any JLists showing the global list (i.e.
 * using a SpecListModel) and a PlotControl. Drop events outside of
 * SPLAT may be encodings of various types that need to be converted
 * into SpecData instances (Treeview is the primary source of these
 * and should be checked for the various types).
 * <p>
 * Notes that this all works between different JVMs as Transferables
 * containing serialized objects.
 *
 * @author Peter W. Draper
 * @version $Id$
 */
public class SpecTransferHandler
    extends TransferHandler
{
    /**
     *  The global list of spectra and plots.
     */
    protected GlobalSpecPlotList globalList = GlobalSpecPlotList.getInstance();

    /**
     * Spectra factory
     */
    protected SpecDataFactory specFactory = SpecDataFactory.getInstance();

    /**
     * Table factory.
     */
    protected StarTableFactory tableFactory = new StarTableFactory();

    /**
     * Various flavors we can import, doesn't include the
     * tables. These are provided by the table factory.
     */
    public static final DataFlavor flavors[] = {

        // Define a flavor for transfering spectra.
        new DataFlavor(DataFlavor.javaJVMLocalObjectMimeType +
                       ";class=uk.ac.starlink.splat.iface.SpecTransferHandler",
                       "Local SpecData" ),

        // Flavors we can accept from Treeview.
        new DataFlavor( "application/xml;class=java.io.InputStream",
                        "NDX stream" ),
        new DataFlavor( DataFlavor.javaSerializedObjectMimeType +
                        ";class=java.net.URL", "URL" )
        /*XXX Don't want one of these yet.
          new DataFlavor( "application/fits;class=java.io.InputStream",
          "FITS stream" ),*/
    };


    public boolean canImport( JComponent comp, DataFlavor flavor[] )
    {
        if ( checkImportComponent( comp ) ) {
            for ( int i = 0, n = flavor.length; i < n; i++ ) {
                for ( int j = 0, m = flavors.length; j < m; j++ ) {
                    if ( flavor[i].equals( flavors[j] ) ) {
                        return true;
                    }
                }
            }
        }

        //  Check tables.
        return tableFactory.canImport( flavor );
    }

    protected boolean checkImportComponent( JComponent comp )
    {
        if ( comp instanceof PlotControl ) {
            return true;
        }

        // The List can import too.
        return checkExportComponent( comp );
    }

    protected boolean checkExportComponent( JComponent comp )
    {
        //  Valid signature is a JList backed by a SpecListModel.
        if ( comp instanceof JList ) {
            JList list = (JList) comp;
            if ( list.getModel() instanceof SpecListModel )  {
                return true;
            }
        }
        return false;
    }

    public int getSourceActions( JComponent c )
    {
        return TransferHandler.COPY;
    }

    //  At the start of a drag event take a copy of the SpecData
    //  objects that are selected in the JList. These are then stored
    //  in a SpecTransferable object which will be presented to any
    //  targets.
    public Transferable createTransferable( JComponent comp )
    {
        if ( checkExportComponent( comp ) ) {
            JList list = (JList) comp;
            final int[] indices = list.getSelectedIndices();
            if ( indices.length > 0 ) {
                SpecListModel model = (SpecListModel) list.getModel();
                ArrayList spectra = new ArrayList( indices.length );
                for ( int i = 0; i < indices.length; i++ ) {
                    spectra.add( model.getSpectrum( indices[i] ) );
                }
                return new SpecTransferable( spectra, flavors );
            }
        }
        return null;
    }

    //  Drop event, if the target is a PlotControl object, then get it
    //  to display any of the spectra that it is not already
    //  displaying. If the Transferable is from a external application
    //  with known mimetype then create a spectrum and display and add
    //  it to the global list.
    public boolean importData( JComponent comp, Transferable t )
    {
        if ( checkImportComponent( comp ) ) {
            DataFlavor[] importFlavors = t.getTransferDataFlavors();

            //  Tables first, there are more of these that look just
            //  like URLS/FITS streams etc. XXXX Need to able to
            //  distinguish these as some table classes are greedy and
            //  will open any FITS file (with a table somewhere).
            if ( tableFactory.canImport( importFlavors ) ) {
                try {
                    StarTable table = tableFactory.makeStarTable( t );
                    if ( table != null ) {
                        return importTable( comp, table );
                    }
                }
                catch (Exception e) {
                    //  Do nothing.
                }
            }

            for ( int j = 0; j < importFlavors.length; j++ ) {
                
                if ( flavors[0].match( importFlavors[j] ) ) {
                    return importSpecData( comp, t );
                }
                if ( flavors[1].match( importFlavors[j] ) ) {
                    return importNDXStream( comp, t );
                }
                if ( flavors[2].match( importFlavors[j] ) ) {
                    return importURL( comp, t );
                }
                //if ( flavors[3].match( importFlavors[j] ) ) {
                //    return importFITSStream( comp, t );
                //}
            }

        }
        return false;
    }

    protected boolean importSpecData( JComponent comp, Transferable t )
    {
        try {
            ArrayList spectra = (ArrayList) t.getTransferData( flavors[0] );
            int added = 0;

            //  Add any unknowns to the global list (needed in both
            //  cases). These will be imports from other instances of
            //  SPLAT!
            for ( int i = 0; i < spectra.size(); i++ ) {
                SpecData spec = (SpecData) spectra.get( i );
                if ( globalList.getSpectrumIndex( spec ) == -1 ) {
                    globalList.add( spec );
                    added++;
                }
            }

            //  If importing to a PlotControl also arrange to display
            //  any currently undisplayed spectra.
            if ( comp instanceof PlotControl ) {
                added = 0;
                PlotControl plot = (PlotControl) comp;
                for ( int i = 0; i < spectra.size(); i++ ) {
                    SpecData spec = (SpecData) spectra.get( i );
                    if ( ! plot.isDisplayed( spec ) ) {
                        try {
                            globalList.addSpectrum( plot, spec );
                            added++;
                        }
                        catch (SplatException e) {
                            // Not a good time to do anything. Which is bad...
                            e.printStackTrace();
                        }
                    }
                }
            }
            return ( added > 0 );
        }
        catch ( UnsupportedFlavorException ignored ) {
            ignored.printStackTrace();
        }
        catch ( IOException ignored ) {
            ignored.printStackTrace();
        }
        return false;
    }

    protected boolean importNDXStream( JComponent comp, Transferable t )
    {
        boolean added = false;
        InputStream inputStream = null;
        try {
            inputStream = (InputStream) t.getTransferData( flavors[1] );
            StreamSource streamSource = new StreamSource( inputStream );
            NDXSpecDataImpl impl = new NDXSpecDataImpl( streamSource );
            SpecData spectrum = new SpecData( impl );
            displaySpectrum( comp, spectrum );
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
            URL url = (URL) t.getTransferData( flavors[2] );
            NDXSpecDataImpl impl = new NDXSpecDataImpl( url );
            SpecData spectrum = new SpecData( impl );
            displaySpectrum( comp, spectrum );
            added = true;
        }
        catch (Exception e) {
            e.printStackTrace();
        }
        return added;
    }

    protected boolean importTable( JComponent comp, StarTable table )
    {
        boolean added = false;
        try {
            SpecData spectrum = specFactory.get( table );
            displaySpectrum( comp, spectrum );
            added = true;
        }
        catch (Exception e) {
            e.printStackTrace();
        }
        return added;
    }

    protected void displaySpectrum( JComponent comp, SpecData spectrum )
    {
        globalList.add( spectrum );
        if ( comp instanceof PlotControl ) {
            PlotControl plot = (PlotControl) comp;
            if ( ! plot.isDisplayed( spectrum ) ) {
                try {
                    globalList.addSpectrum( plot, spectrum );
                }
                catch (Exception e) {
                    //  Ignore.
                }
            }
        }
    }

    // Inner class that implements Transferable. This is the object
    // that stores the information generated when the drag event
    // happens (this stores the list of spectra to be transferred and
    // our flavor signature for SpecData).
    protected class SpecTransferable
        implements Transferable
    {
        protected ArrayList spectra;
        protected DataFlavor[] flavors;

        public SpecTransferable( ArrayList spectra, DataFlavor[] flavors )
        {
            this.spectra = spectra;
            this.flavors = flavors;
        }

        public Object getTransferData( DataFlavor flavor)
        {
            if ( isDataFlavorSupported( flavor ) ) {
                return spectra;
            }
            return null;
        }

        public DataFlavor[] getTransferDataFlavors()
        {
            return flavors;
        }

        public boolean isDataFlavorSupported( DataFlavor flavor )
        {
            //  Note we only support SpecData internally.
            return flavor.equals( flavors[0] );
        }
    }
}
