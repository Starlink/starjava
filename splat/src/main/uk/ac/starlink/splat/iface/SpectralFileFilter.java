package uk.ac.starlink.splat.iface;

import java.io.File;
import java.io.FilenameFilter;
import java.util.Enumeration;
import java.util.Hashtable;

import javax.swing.filechooser.FileFilter;
import uk.ac.starlink.splat.util.Utilities;

/**
 * A convenience implementation of FileFilter that filters out
 * all files except for those type extensions that it knows about.
 *
 * Extensions are of the type ".foo", which is typically found on
 * Windows and Unix boxes, but not on Macinthosh. Case is ignored.
 *
 * Example - create a new filter that filters out all files
 * but gif and jpg image files:
 *
 *     JFileChooser chooser = new JFileChooser();
 *     SpectralFileFilter filter = new SpectralFileFilter(
 *                   new String{"gif", "jpg"}, "JPEG & GIF Images")
 *     chooser.addChoosableFileFilter(filter);
 *     chooser.showOpenDialog(this);
 *
 * @version 1.1 10/14/99
 * @author Jeff Dinkins
 * @author Peter W. Draper
 */
public class SpectralFileFilter 
    extends FileFilter
    implements FilenameFilter
{
    private static String TYPE_UNKNOWN = "Type Unknown";
    private static String HIDDEN_FILE = "Hidden File";

    private Hashtable filters = null;
    private String description = null;
    private String fullDescription = null;
    private boolean useExtensionsInDescription = true;

    /**
     * Creates a file filter. If no filters are added, then all
     * files are accepted.
     *
     * @see #addExtension
     */
    public SpectralFileFilter()
    {
        this.filters = new Hashtable();
    }

    /**
     * Creates a file filter that accepts files with the given extension.
     * Example: new SpectralFileFilter("jpg");
     *
     * @see #addExtension
     */
    public SpectralFileFilter( String extension )
    {
        this( extension,null );
    }

    /**
     * Creates a file filter that accepts the given file type.
     * Example: new SpectralFileFilter("jpg", "JPEG Image Images");
     *
     * Note that the "." before the extension is not needed. If
     * provided, it will be ignored.
     *
     * @see #addExtension
     */
    public SpectralFileFilter( String extension, String description )
    {
        this();
        if ( extension != null ) addExtension( extension );
        if ( description != null ) setDescription( description );
    }

    /**
     * Creates a file filter from the given string array.
     * Example: new SpectralFileFilter(String {"gif", "jpg"});
     *
     * Note that the "." before the extension is not needed adn
     * will be ignored.
     *
     * @see #addExtension
     */
    public SpectralFileFilter( String[] filters )
    {
        this( filters, null );
    }

    /**
     * Creates a file filter from the given string array and description.
     * Example: new SpectralFileFilter(String {"gif", "jpg"}, "Gif and JPG Images");
     *
     * Note that the "." before the extension is not needed and will be ignored.
     *
     * @see #addExtension
     */
    public SpectralFileFilter( String[] filters, String description )
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
     * @see FileFilter#accepts
     */
    public boolean accept( File f )
    {
        if ( f != null ) {
            if ( f.isDirectory() ) {
                return true;
            }
            String extension = Utilities.getExtension( f );
            if ( extension != null && filters.get( extension ) != null ) {
                return true;
            }
        }
        return false;
    }

    /**
     * Adds a filetype "dot" extension to filter against.
     *
     * For example: the following code will create a filter that filters
     * out all files except those that end in ".jpg" and ".tif":
     *
     *   SpectralFileFilter filter = new SpectralFileFilter();
     *   filter.addExtension("jpg");
     *   filter.addExtension("tif");
     *
     * Note that the "." before the extension is not needed and will be ignored.
     */
    public void addExtension( String extension )
    {
        if ( filters == null ) {
            filters = new Hashtable( 5 );
        }
        filters.put( extension.toLowerCase(), this );
        fullDescription = null;
    }


    /**
     * Returns the human readable description of this filter. For
     * example: "JPEG and GIF Image Files (*.jpg, *.gif)"
     *
     * @see setDescription
     * @see setExtensionListInDescription
     * @see isExtensionListInDescription
     * @see FileFilter#getDescription
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
                Enumeration extensions = filters.keys();
                if ( extensions != null ) {
                    fullDescription += " ." + (String)extensions.nextElement();
                    while ( extensions.hasMoreElements() ) {
                        fullDescription += ", ." + 
                            (String)extensions.nextElement();
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
     *
     * @see setDescription
     * @see setExtensionListInDescription
     * @see isExtensionListInDescription
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
     *
     * @see getDescription
     * @see setDescription
     * @see isExtensionListInDescription
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
     *
     * @see getDescription
     * @see setDescription
     * @see setExtensionListInDescription
     */
    public boolean isExtensionListInDescription()
    {
        return useExtensionsInDescription;
    }
    
    // FilenameFilter implementation.
    public boolean accept( File dir, String name )
    {
        if ( dir != null ) {
            String extension = Utilities.getExtension( name );
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
