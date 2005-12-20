package uk.ac.starlink.topcat;

import java.io.File;
import java.util.Arrays;
import javax.swing.filechooser.FileFilter;

/**
 * File filter that matches according to filename suffix.
 * Any file which ends in the same string as one of the suffices supplied
 * at construction time is considered to match.  This matching is 
 * case insensitive.  Any directory is considered to match also 
 * (this permits navigation in the JFileChooser).
 *
 * @author   Mark Taylor
 * @since    19 Dec 2005
 */
public class SuffixFileFilter extends FileFilter {

    private final String[] suffices_;
    private final String descrip_;

    /**
     * Constructs a new file filter which selects on the given suffices.
     * These are matched case-insensitively.
     *
     * @param  suffices  array of suitable suffix strings
     */
    public SuffixFileFilter( String[] suffices ) {
        StringBuffer sbuf = new StringBuffer();
        for ( int i = 0; i < suffices.length; i++ ) {
            if ( i > 0 ) {
                sbuf.append( ", " );
            }
            sbuf.append( suffices[ i ] );
        }
        descrip_ = sbuf.toString();

        suffices_ = new String[ suffices.length ];
        for ( int i = 0; i < suffices.length; i++ ) {
            suffices_[ i ] = suffices[ i ].toLowerCase();
        }
    }

    public String getDescription() {
        return descrip_;
    }

    public boolean accept( File file ) {
        if ( file.isDirectory() ) {
            return true;
        }
        String name = file.getName().toLowerCase();
        for ( int i = 0; i < suffices_.length; i++ ) {
            if ( name.endsWith( suffices_[ i ] ) ) {
                return true;
            }
        }
        return false;
    }

    public boolean equals( Object other ) {
        return other instanceof SuffixFileFilter
            && Arrays.equals( suffices_, ((SuffixFileFilter) other).suffices_ );
    }

    public int hashCode() {
        int code = 23;
        for ( int i = 0; i < suffices_.length; i++ ) {
            code = code * 99 + suffices_[ i ].hashCode();
        }
        return code;
    }
}
