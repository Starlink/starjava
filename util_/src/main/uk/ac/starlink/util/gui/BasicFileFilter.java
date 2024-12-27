/*
 * Copyright (C) 2003 Central Laboratory of the Research Councils
 *
 *  History:
 *     20-AUG-2003 (Peter W. Draper):
 *       Original version.
 */
package uk.ac.starlink.util.gui;

import java.io.File;
import java.io.FilenameFilter;
import java.util.Enumeration;
import java.util.Hashtable;

import javax.swing.filechooser.FileFilter;

/**
 * A FileFilter for configuring a {@link javax.swing.JFileChooser} to only show
 * files that have one of a set of file extensions. Also implements
 * FilenameFilter to provide similar services for filtering directory
 * contents using the {@link java.io.File} class
 * (see {@link java.io.File#listFiles}).
 * <p>
 * Example - create filters for HDS and FITS files and use with a
 * BasicFileChooser.
 * <pre>
 *     BasicFileChooser chooser = new BasicFileChooser();
 *     BasicFileFilter fitsFilter = new BasicFileFilter(
 *                   new String{ "fit", "fits" }, "FITS files" )
 *     chooser.addChoosableFileFilter( fitsFilter );
 *     BasicFileFilter hdsFilter =
 *                     new BasicFileFilter( "hds", "HDS container files" );
 *     chooser.addChoosableFileFilter( hdsFilter );
 *     chooser.showOpenDialog( this );
 * </pre>
 * Example - filter the files in a directory.
 * <pre>
 *   BasicFileFilter idsFilter = new BasicFileFilter( "ids" );
 *   File dir = new File( "." );
 *   File[] files = dir.listFiles( idsFilter );
 * </pre>
 *
 * @version $Id$
 * @author Jeff Dinkins
 * @author Peter W. Draper
 */
public class BasicFileFilter
    extends FileFilter
    implements FilenameFilter
{
    private Hashtable<String,Object> filters = null;
    private String description = null;
    private String fullDescription = null;
    private boolean useExtensionsInDescription = true;

    /**
     * Creates a file filter. If no filters are added, then all
     * files are accepted.
     *
     * @see #addExtension
     */
    public BasicFileFilter()
    {
        this.filters = new Hashtable<String,Object>();
    }

    /**
     * Creates a file filter that accepts files with the given extension.
     * Example: new BasicFileFilter("jpg");
     *
     * @see #addExtension
     */
    public BasicFileFilter( String extension )
    {
        this( extension,null );
    }

    /**
     * Creates a file filter that accepts the given file type.
     * Example: new BasicFileFilter("jpg", "JPEG Image Images");
     *
     * Note that the "." before the extension is not needed. If
     * provided, it will be ignored.
     *
     * @see #addExtension
     */
    @SuppressWarnings("this-escape")
    public BasicFileFilter( String extension, String description )
    {
        this();
        if ( extension != null ) addExtension( extension );
        if ( description != null ) setDescription( description );
    }

    /**
     * Creates a file filter from the given string array.
     * Example: new BasicFileFilter(String {"gif", "jpg"});
     *
     * Note that the "." before the extension is not needed and
     * will be ignored.
     *
     * @see #addExtension
     */
    public BasicFileFilter( String[] filters )
    {
        this( filters, null );
    }

    /**
     * Creates a file filter from the given string array and description.
     * Example: new BasicFileFilter(String {"gif", "jpg"}, "Gif and JPG Images");
     *
     * Note that the "." before the extension is not needed and will be ignored.
     *
     * @see #addExtension
     */
    @SuppressWarnings("this-escape")
    public BasicFileFilter( String[] filters, String description )
    {
        this();
        for ( int i = 0; i < filters.length; i++ ) {
            // add filters one by one
            addExtension( filters[i] );
        }
        if ( description != null ) setDescription( description );
    }

    /**
     * Return true if this file should be shown in the directory pane,
     * false if it shouldn't.
     *
     * Files that begin with "." are ignored.
     *
     * @see FileFilter#accept
     */
    public boolean accept( File f )
    {
        if ( f != null ) {
            if ( f.isDirectory() ) {
                return true;
            }
            String extension = getExtension( f );
            if ( extension != null && filters.get( extension ) != null ) {
                return true;
            }
        }
        return false;
    }

    /**
     * Return the extension of a file's name.
     */
    public static String getExtension( String name )
    {
        if ( name != null ) {
            int i = name.lastIndexOf( '.' );
            if ( i > 0 && i < name.length() - 1 ) {
                return name.substring( i + 1 ).toLowerCase();
            }
        }
        return null;
    }

    /**
     * Return the extension of a file's name.
     *
     * @param f the File.
     */
    public static String getExtension( File f )
    {
        if ( f != null ) {
            return getExtension( f.getName() );
        }
        return null;
    }

    /**
     * Adds a filetype "dot" extension to filter against.
     *
     * For example: the following code will create a filter that filters
     * out all files except those that end in ".jpg" and ".tif":
     *
     *   BasicFileFilter filter = new BasicFileFilter();
     *   filter.addExtension("jpg");
     *   filter.addExtension("tif");
     *
     * Note that the "." before the extension is not needed and will be ignored.
     */
    public void addExtension( String extension )
    {
        if ( filters == null ) {
            filters = new Hashtable<String,Object>( 5 );
        }
        filters.put( extension.toLowerCase(), this );
        fullDescription = null;
    }


    /**
     * Returns the human readable description of this filter. For
     * example: "JPEG and GIF Image Files (*.jpg, *.gif)"
     */
    public String getDescription()
    {
        if ( fullDescription == null ) {
            if ( description == null || isExtensionListInDescription() ) {
                fullDescription = description;
                if ( description != null ) {
                    fullDescription += " (";
                }
                else {
                    fullDescription = " (";
                }

                // build the description from the extension list
                Enumeration<String> extensions = filters.keys();
                if ( extensions != null ) {
                    fullDescription += " ." + extensions.nextElement();
                    while ( extensions.hasMoreElements() ) {
                        fullDescription += ", ." + extensions.nextElement();
                    }
                }
                fullDescription += ")";
            }
            else {
                fullDescription = description;
            }
        }
        return fullDescription;
    }

    /**
     * Sets the human readable description of this filter. For
     * example: filter.setDescription("Gif and JPG Images");
     */
    public void setDescription( String description )
    {
        this.description = description;
        fullDescription = null;
    }

    /**
     * Determines whether the extension list (.jpg, .gif, etc) should
     * show up in the human readable description.
     *
     * Only relevent if a description was provided in the constructor
     * or using setDescription();
     */
    public void setExtensionListInDescription( boolean b )
    {
        useExtensionsInDescription = b;
        fullDescription = null;
    }

    /**
     * Returns whether the extension list (.jpg, .gif, etc) should
     * show up in the human readable description.
     *
     * Only relevent if a description was provided in the constructor
     * or using setDescription();
     */
    public boolean isExtensionListInDescription()
    {
        return useExtensionsInDescription;
    }

    // FilenameFilter implementation.
    public boolean accept( File dir, String name )
    {
        if ( dir != null ) {
            String extension = getExtension( name );
            if ( extension != null && filters.get( extension ) != null ) {
                return true;
            }

            //  Could be a directory.
            File maybeDir = new File( dir, name );
            if ( maybeDir.isDirectory() ) {
                return true;
            }
        }
        return false;
    }
}
