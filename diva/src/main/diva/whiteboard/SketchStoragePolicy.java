/*
 * $Id: SketchStoragePolicy.java,v 1.8 2001/07/22 22:02:26 johnr Exp $
 *
 * Copyright (c) 1998-2001 The Regents of the University of California.
 * All rights reserved. See the file COPYRIGHT for details.
 */

package diva.whiteboard;

import diva.gui.AppContext;
import diva.gui.Application;
import diva.gui.DefaultStoragePolicy;
import diva.gui.DesktopContext;
import diva.gui.Document;
import diva.gui.ExtensionFileFilter;
import diva.gui.GUIUtilities;
import diva.gui.toolbox.JStatusBar;

import java.io.File;
import javax.swing.JFileChooser;
import javax.swing.JOptionPane;
import javax.swing.filechooser.FileFilter;

/**
 * A SketchStoragePolicy implements methods of closing, opening, and
 * saving sketch documents.  Only the saveAs method is overwritten,
 * the rest of the methods are the same as DefaultStoragePolicy.
 *
 * @author Heloise Hse (hwawen@eecs.berkeley.edu)
 * @version $Revision: 1.8 $
 */
public class SketchStoragePolicy extends DefaultStoragePolicy {
    /**
     * The file extension for sketch documents.
     */
    public final static String SML = "sml";

    /**
     * The file extension for JPEG images.
     */
    public final static String JPEG = "jpg";
    
    /** Save the document to a user-specified location. Open a file
     * chooser and forward the request to the document. Don't change
     * the document's file object.  Do nothing if the document is
     * null. Return true if successul, otherwise false.
     *
     * File extension is checked and modified in the following way:
     * <ol>
     * <li>If the selected file filter is "*.*", then the file
     * will be saved in .sml format.  If the file name does not have a
     * ".sml" extension, it is appended with ".sml".</li>
     * <li>If the selected file filter is ".gif" or ".sml", the
     * corresponding file extension will be appended if the file name
     * lacks the extension.</li>
     * </ol>
     */
    public boolean saveAs (Document d) {
        if (d != null) {
            int result;
            Application app = d.getApplication();
            AppContext context = app.getAppContext();
	    
            // Open a chooser dialog
            JFileChooser fc = getSaveFileChooser();
            fc.setCurrentDirectory(
                    new File(System.getProperty("user.dir")));
            result = fc.showSaveDialog(context.makeComponent());
            if (result == JFileChooser.APPROVE_OPTION) {
                File chosenFile = fc.getSelectedFile();
                // Check to see if the file's extension matches the current
                // file filter
                FileFilter filter = fc.getFileFilter();
                File file = checkExtension(chosenFile, filter);
                System.out.println("Final chosen file name is " + file);
                if (file.exists()) {
                    // Query on overwrite
                    int opt = JOptionPane.showConfirmDialog(
			    context.makeComponent(),
                            "File " + file.getName() + "exists. Overwrite?",
                            "Overwrite file?",
                            JOptionPane.YES_NO_OPTION);
                    if (opt != JOptionPane.YES_OPTION) {
                        context.showStatus("File not saved");
                        return false;
                    }
                }
                try {
                    String ext =
                        GUIUtilities.getFileExtension(file).toLowerCase();
                    if(ext.equals(SML)){
                        d.saveAs(file);
                    }
                    /*                    else if(ext.equals(JPEG)){
                                          ((Whiteboard)app).saveAsJPEG(d, file);
                                          }*/
                    else {
                        String err = "Unknown file format: " + ext;
                        throw new RuntimeException(err);
                    }
                } catch (Exception e) {
                    GUIUtilities.showStackTrace(context.makeComponent(), e,
                            "DefaultStoragePolicy failed on \"save as\" operation");
                    return false;
                }
                d.setFile(file);
                context.showStatus("Saved " + d.getTitle());
                d.setDirty(false);                
                return true;
            } else {
                return false;
            }
        }
        return true;
    }

    /**
     * If the current filter is an accept all filter, then give the
     * file the ".sml" extension.  For all other filters, make sure
     * the file extension matches the filter, otherwise append the
     * correct extension to the file naem.
     */
    private File checkExtension(File chosenFile, FileFilter currentFilter){
        File finalFile = chosenFile;
        JFileChooser fc = getSaveFileChooser();
        String currentFileName = chosenFile.getName();
        if(currentFilter == fc.getAcceptAllFileFilter()){
            // Save as .sml
            System.out.println("*.* filter used ");
            String ext = "." + SML;
            if(!chosenFile.getName().endsWith(ext)){
                String path = chosenFile.getAbsolutePath();
                System.out.println("Absolute path = " + path);
                String name =
                    path.substring(0, path.lastIndexOf(File.separatorChar))
                    + File.separatorChar + chosenFile.getName() + ext;
                System.out.println("New file path = " + name);
                finalFile = new File(name);
            }
        }
        else if(currentFilter.accept(chosenFile)){
            // do nothing
            ExtensionFileFilter ff = (ExtensionFileFilter)currentFilter;
            System.out.println(ff + " filter used ");
        }
        else {
            //append ext
            System.out.println("Missing extension...");
            ExtensionFileFilter ff = (ExtensionFileFilter)currentFilter;
            String name = chosenFile.getName() +
                "." + ff.getDefaultExtension();
            String path = chosenFile.getAbsolutePath();
            System.out.println("Absolute path = " + path);
            name = path.substring(0, path.lastIndexOf(File.separatorChar))
                + File.separatorChar + name;
            System.out.println("New file path = " + name);
            finalFile = new File(name);
        }
        return finalFile;
    }
}


