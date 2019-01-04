/*
 * Copyright 2003 Sun Microsystems, Inc. All Rights Reserved.
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

/**
 * A JFileChooser that attempts to do something useful with windows
 * shortcuts. Currently (1.4.1) Java just deals with these as simple
 * files, rather than a pointer to another file or directory
 * (shortcuts are files that contain information about the file they
 * are pointing at). This attempts to spot these files, open them and
 * then set either the selected file or directory to the one the
 * shortcut contains.
 * <p>
 * The original code was copied from Bug Id: 4356160 on the SUN web
 * site so is also copyright SUN.
 * <p>
 * This class originally made use of the non-standard class
 * sun.awt.shell.ShellFolder to do some cleverer things with
 * Windows .lnk files.  Since use of that non-standard class
 * caused build problems with some java versions, references
 * to that class, including the corresponding functionality,
 * have been removed.  That probably means there is no point
 * in using this class any more, and client code should just
 * use JFileChooser instead.  It's possible that the underlying
 * problem has been fixed in any case since Java 1.4.1.
 *
 * @author Peter W. Draper
 * @author Mark Taylor
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
        }
        super.approveSelection();
    }
}
