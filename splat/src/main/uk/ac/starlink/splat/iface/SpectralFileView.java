/*
 * Copyright (C) 2003 Central Laboratory of the Research Councils
 *
 *  History:
 *     12-MAY-2003 (Peter W. Draper):
 *       Original version.
 */
package uk.ac.starlink.splat.iface;

import java.io.File;
import javax.swing.filechooser.FileView;
import javax.swing.Icon;

import uk.ac.starlink.splat.data.SpecDataFactory;
import uk.ac.starlink.splat.util.Utilities;
import uk.ac.starlink.datanode.nodes.IconFactory;

/**
 * Set the Icons for any spectral types that are known to SPLAT
 * (really Datanode and SPLAT).
 *
 * @author Peter W. Draper
 * @version $Id$
 */
public class SpectralFileView
    extends FileView
{
    // Note defaulted methods use the Look and Feel filechooser
    // implementation.

    public String getTypeDescription( File f )
    {
        String extension = Utilities.getExtension( f );
        String type = null;
        
        if ( extension != null ) {
            String[][] extensions = SpecDataFactory.extensions;
            String[] longNames = SpecDataFactory.longNames;
            for ( int i = 0; i < longNames.length; i++ ) {
                for ( int j = 0; j < extensions[i].length; j++ ) {
                    if ( extensions[i][j].equals( extension ) ) {
                        type = longNames[i];
                        break;
                    }
                }
                if ( type != null ) break;
            }
        }
        return type;
    }
    
    public Icon getIcon( File f )
    {
        String extension = Utilities.getExtension( f );
        Icon icon = null;
        
        if ( extension != null ) {
            String[][] extensions = SpecDataFactory.extensions;
            short[] datanodeIcons = SpecDataFactory.datanodeIcons;
            for ( int i = 0; i < extensions.length; i++ ) {
                for ( int j = 0; j < extensions[i].length; j++ ) {
                    if ( extensions[i][j].equals( extension ) ) {
                        icon = IconFactory.getIcon( datanodeIcons[i] );
                        break;
                    }
                }
                if ( icon != null ) break;
            }
        }
        return icon;
    }
    
    //  The following code would let Datanode decide which icons
    //  to use. Seemed a bit slow in practice.
    //         DataNodeFactory factory = new DataNodeFactory();
    //         try {
    //             node = factory.makeDataNode( null, f );
    //         }
    //         catch ( NoSuchDataException e ) {
    //             return null;
    //         }
    //         Icon icon = node.getIcon();
    //         return icon;
}
