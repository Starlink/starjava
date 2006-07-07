/*
 * Copyright© 2003 Sun Microsystems, Inc. All Rights Reserved.
 * 
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met: 
 * 
 * Redistribution of source code must retain the above copyright
 * notice, this list of conditions and the following disclaimer. 
 *
 * Redistribution in binary form must reproduce the above copyright
 * notice, this list of conditions and the following disclaimer in the
 * documentation and/or other materials provided with the
 * distribution. 
 * 
 * Neither the name of Sun Microsystems, Inc. or the names of
 * contributors may be used to endorse or promote products derived
 * from this software without specific prior written permission. 
 * 
 * This software is provided "AS IS," without a warranty of any
 * kind. ALL EXPRESS OR IMPLIED CONDITIONS, REPRESENTATIONS AND
 * WARRANTIES, INCLUDING ANY IMPLIED WARRANTY OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE OR NON-INFRINGEMENT, ARE HEREBY
 * EXCLUDED. SUN MICROSYSTEMS, INC. ("SUN") AND ITS LICENSORS SHALL
 * NOT BE LIABLE FOR ANY DAMAGES SUFFERED BY LICENSEE AS A RESULT OF
 * USING, MODIFYING OR DISTRIBUTING THIS SOFTWARE OR ITS
 * DERIVATIVES. IN NO EVENT WILL SUN OR ITS LICENSORS BE LIABLE FOR
 * ANY LOST REVENUE, PROFIT OR DATA, OR FOR DIRECT, INDIRECT, SPECIAL,
 * CONSEQUENTIAL, INCIDENTAL OR PUNITIVE DAMAGES, HOWEVER CAUSED AND
 * REGARDLESS OF THE THEORY OF LIABILITY, ARISING OUT OF THE USE OF OR
 * INABILITY TO USE THIS SOFTWARE, EVEN IF SUN HAS BEEN ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGES. 
 * 
 * You acknowledge that this software is not designed, licensed or
 * intended for use in the design, construction, operation or
 * maintenance of any nuclear facility.  
 * 
 * Copyright (C) 2003 Central Laboratory of the Research Councils
 *
 *  History:
 *     20-AUG-2003 (Peter W. Draper):
 *       Original version.
 */
package uk.ac.starlink.util.gui;

import java.io.File;
import java.io.FileNotFoundException;
import javax.swing.JFileChooser;
import sun.awt.shell.ShellFolder;

/**
 * A JFileChooser that attempts to do something useful with windows
 * shortcuts. Currently (1.4.1) Java just deals with these as simple
 * files, rather than a pointer to another file or directory
 * (shortcuts are files that contain information about the file they
 * are pointing at). This attempts to spot these files, open them and
 * then set either the selected file or directory to the one the
 * shortcut contains.
 * <p>
 * Note this class depends on the sun.awt.shell.ShellFolder class
 * which not part of the standard API, so can be changed without
 * notification and is therefore likely to break. In this case fix it
 * up or switch it to go back to the standard JFileChooser behaviour.
 * Also note that it doesn't work when multiple files are selected
 * (although it does work for a single file when multiple selections
 * are enabled).
 * <p>
 * The original code was copied from Bug Id: 4356160 on the SUN web
 * site so is also copyright SUN.
 *
 * @author Peter W. Draper
 * @version $Id$
 */      
public class BasicFileChooser
    extends JFileChooser
{
    /**
     * Construct a default instance.
     */
    public BasicFileChooser()
    {
        this( System.getProperty( "user.home" ) );
    }

    /**
     * Construct a instance with a choice of default directory. If
     * home is true then the users home directory is selected,
     * otherwise the process default directory is selected.
     */
    public BasicFileChooser( boolean home )
    {
        this( home ? System.getProperty( "user.home" ) : 
                     System.getProperty( "user.dir" ) );
    }

    /**
     * Construct a instance with a choice of default directory.
     */
    public BasicFileChooser( String defaultDirectory )
    {
        super( defaultDirectory );

        // For JDK1.4.2 we need to disable directory listing speed ups
        // for shortcuts to be recognised by ShellFolder.
        System.setProperty( "swing.disableFileChooserSpeedFix", "true" );
    }

    public void approveSelection() 
    {
        //  If running under Windows and only selecting files.
        if ( File.separatorChar == '\\' && 
             getFileSelectionMode() == FILES_ONLY ) {

            // Only one file can be processed this way, as this may
            // result in a change of directory (since the file pointed
            // to is selected, not the shortcut, clearly if we had
            // shortcuts in different directories this would be
            // impossible).
            File selectedFile = null;
            if ( isMultiSelectionEnabled() ) {
                File[] selectedFiles = getSelectedFiles();
                if ( selectedFiles.length == 1 ) {
                    selectedFile = selectedFiles[0];
                }
                else {
                    // Cannot handle any shortcuts, do fall back to
                    // default methods.
                    super.approveSelection();
                    return;
                }
            }
            else {
                selectedFile = getSelectedFile();
            }
            
            //  Is this a windows shortcut file?
            if ( selectedFile.getPath().endsWith( ".lnk" ) ) {
                File linkedTo = null;
                try {
                    linkedTo = ShellFolder
                        .getShellFolder( selectedFile ).getLinkLocation();
                } 
                catch ( FileNotFoundException ignore ) {
                    //  Do nothing.
                }
                if ( linkedTo != null ) {
                    if ( linkedTo.isDirectory() ) {
                        setCurrentDirectory( linkedTo );
                        return;
                    } 
                    else if ( ! linkedTo.equals( selectedFile ) ) {
                        if ( isMultiSelectionEnabled() ) {
                            setSelectedFiles( new File[] { linkedTo } );
                        }
                        setSelectedFile( linkedTo );
                    }
                }
            }
        }
        super.approveSelection();
    }
}    
