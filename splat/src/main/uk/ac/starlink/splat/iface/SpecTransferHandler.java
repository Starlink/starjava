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
import java.util.ArrayList;

import javax.swing.TransferHandler;
import javax.swing.JComponent;
import javax.swing.JList;

import uk.ac.starlink.splat.data.SpecData;
import uk.ac.starlink.splat.plot.PlotControl;
import uk.ac.starlink.splat.util.SplatException;

/**
 * A TransferHandler for dragging and dropping SpecData instances
 * between a JList using a SpecListModel and a PlotControl. This works
 * between different JVMs too.
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
    protected GlobalSpecPlotList
        globalList = GlobalSpecPlotList.getReference();

    /**
     * Define a flavor for transfering spectra.
     */
    public static final DataFlavor flavors[] = {
        new DataFlavor( DataFlavor.javaJVMLocalObjectMimeType +
                      ";class=uk.ac.starlink.splat.iface.SpecTransferHandler",
                      "Local SpecData" )
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
        return false;
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

    //  Drop event, if the target is suitable (i.e a PlotControl
    //  object), then get it to display any of the spectra that it is
    //  not already displaying.
    public boolean importData( JComponent comp, Transferable t )
    {
        if ( checkImportComponent( comp ) ) {
            if ( t.isDataFlavorSupported( flavors[0] ) ) {
                try {
                    ArrayList spectra = 
                        (ArrayList) t.getTransferData( flavors[0] );
                    int added = 0;

                    //  Add any unknowns to the global list (needed in
                    //  both cases). These will be imports from other
                    //  instances of SPLAT!
                    for ( int i = 0; i < spectra.size(); i++ ) {
                        SpecData spec = (SpecData) spectra.get( i );
                        if ( globalList.getSpectrumIndex( spec ) == -1 ) {
                            globalList.add( spec );
                            added++;
                        }
                    }

                    //  If importing to a PlotControl also arrange to
                    //  display any currently undisplayed spectra.
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
                                    // Not a good time to do anything.
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
            }
        }
        return false;
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
            return flavor.equals( flavors[0] );
        }
    }
}
