/*
 * Copyright (C) 2004 Central Laboratory of the Research Councils
 *
 *  History:
 *     27-AUG-2004 (Peter W. Draper):
 *       Original version.
 */

package uk.ac.starlink.splat.data;

import java.net.URL;
import java.net.URI;
import java.io.File;
import java.io.IOException;

import uk.ac.starlink.splat.util.SplatException;
import uk.ac.starlink.util.URLUtils;

/**
 * Parser for any spectral name specifications in SPLAT. This class should
 * deal with the case of simple local file names (paths) and file names
 * expressed as URLs.
 *
 * @author Peter W. Draper
 * @version $Id$
 */
public class NameParser
{
    private boolean isLineIdentifier = false;
    private boolean isRemote = false;
    private PathParser pathParser = new PathParser();
    private PathParser urlPathParser = new PathParser();
    private URL url = null;

    /**
     * Default constructor.
     */
    public NameParser()
    {
        //  Do nothing.
    }

    /**
     * Create an instance for the given specification.
     */
    public NameParser( String specspec )
        throws SplatException
    {
        setSpecSpec( specspec );
    }

    /**
     * Parse the given specification.
     */
    public void setSpecSpec( String specspec )
        throws SplatException
    {
        isRemote = false;
        isLineIdentifier = false;
        
        boolean inError = false;
        try {
            url = URLUtils.makeURL( specspec );
        }
        catch ( Exception e ) {
            //  Parse to URL failed, that should never happen.
            inError = true;
        }
        if ( url == null || inError ) {
            throw new SplatException( "Cannot make sense of " +
                                      "specification: " + specspec );
        }
        pathParser.setPath( specspec );

        if ( ! pathParser.exists() ) {

            //  A line identifier file, with type ".ids" isn't understood by
            //  the PathParser and will be misidentified as an NDF with path
            //  ".ids", check for this special case.
            if ( ".ids".equals( pathParser.path() ) ) {
                isLineIdentifier = true;
            }
            else {
                // Name could be specified as a URL and still be local. It
                // might also not exist on purpose (file to be created). This
                // latter case is covered when the scheme comes out as "file".
                File testFile = null;
                boolean localFile = false;
                try {
                    URI uri = URLUtils.urlToUri( url );
                    testFile = new File( uri );
                    localFile = uri.getScheme().equals( "file" );
                }
                catch ( Exception e ) {
                    //  Not serious yet, just means uri isn't a filename,
                    //  unless it's local that's always true.
                }
                if ( testFile != null && localFile ) {
                    try {
                        pathParser.setPath( testFile.getCanonicalPath() );
                    }
                    catch (IOException e) {
                        throw new SplatException( e );
                    }
                }
                else {
                    //  Must take URL to specify a remote resource.
                    isRemote = true && ! localFile;
                    
                    //  Parse the path part of the URL to get format etc.
                    urlPathParser.setPath( url.getPath() );
                }
            }
        }
    }

    /**
     * Return if the specification given was considered to be for a remote
     * resource. Note this hasn't been tested.
     */
    public boolean isRemote()
    {
        return isRemote;
    }

    /**
     * Return the URL of the specification.
     */
    public URL getURL()
    {
        return url;
    }

    /**
     * Return the "name" of the specification. For a local file this will be
     * the filename, plus any qualifying characteristics (HDS paths, FITS
     * extensions). For a remote file this will be the full URL.
     */
    public String getName()
    {
        if ( isLineIdentifier ) {
            return pathParser.ndfname();
        }
        if ( isRemote ) {
            return url.toString();
        }
        return pathParser.ndfname();
    }

    /**
     *  Get the "data format". This is one of "NDF", "FITS", "TEXT", "XML" or
     *  "IDS", depending on what the file extension and suitability  to
     *  process are. If the file cannot be recognised then "UNKNOWN" is
     *  returned;
     */
    public String getFormat()
    {
        if ( isLineIdentifier ) {
            return "IDS";
        }
        if ( isRemote ) {
            return urlPathParser.format();
        }
        return pathParser.format();
    }

    /**
     *  Get the file extension.
     */
    public String getType()
    {
        if ( isLineIdentifier ) {
            return ".ids";
        }
        if ( isRemote ) {
            return urlPathParser.type();
        }
        return pathParser.type();
    }

    /**
     *  Does the backing file exist?
     */
    public boolean exists()
    {
        return pathParser.exists();
    }
}
