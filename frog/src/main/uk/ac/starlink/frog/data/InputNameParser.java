package uk.ac.starlink.frog.data;

import java.io.File;
import uk.ac.starlink.frog.util.FrogDebug;

/**
 *  InputNameParser - class for encapsulating naming strategies that
 *                    differentiate the various data formats used to
 *                    store series.
 * <p>
 *  These names may be simple disk file names, such as the top-level
 *  containers of NDF, FITS and TEXT data, or more complex ones that
 *  include NDF slices, FITS extensions and possibly HDS component
 *  paths.
 *  <p>
 *  So the sort of names that we might get for series are:
 *  <ul>
 *    <li> file.fits -- FITS may also be .fit .fits.gz .fits.Z etc.
 *
 *    <li> file.fits[1] -- first FITS extension.
 *
 *    <li> file -- an NDF, which is expanded to file.sdf
 *
 *    <li> file.txt or file.lis -- a text file containing columns of
 *                                 coordinates and values.
 *
 *    <li> file.imh -- IRAF file to be processed by NDF, other types
 *                     are listed in the environment variable $NDF_FORMATS_IN.
 *
 *    <li> file(1:100,2:200) -- NDF with slice, expanded to
                                file.sdf(1:100,2:200).
 *
 *    <li> file.sdf(1:100,2:200) -- same as above.
 *
 *    <li> file.sdf -- simple NDF
 *
 *    <li> file.ndf -- NDF stored in a container file,
 *                     expanded to file.sdf.ndf.
 *
 *    <li> file.sdf.ndf(1:100,2:100) -- slice of an NDF stored in a
 *                                      container file. This name is fully
 *                                      expanded.
 *  </ul>
 *  To use this class create an object with the series specification,
 *  then use its methods to get fully qualified names, slices, paths
 *  and disk file names.
 *
 * @since $Date$
 * @since 13-JAN-2003, original version.
 * @author Peter W. Draper
 * @author Alasdair Allan
 * @version $Id$
 */
public class InputNameParser 
{
   /**
     *  Application wide debug manager
     */
    protected FrogDebug debugManager = FrogDebug.getReference();
    
    /**
     *  Constructor - single argument, the series specification.
     */
    public InputNameParser( String specspec ) 
    {
        debugManager.print( "              InputNameParser(" + specspec + ")");
        this.specspec = specspec;
        parseName();
    }

    /**
     *  Default Constructor.
     */
    public InputNameParser() 
    {
        //  Do nothing.
    }

    /**
     *  Set the name.
     */
    public void setName( String specspec ) 
    {
        this.specspec = specspec;
        parseName();
    }

    /**
     *  Get the fully expanded name. Do not add the FITS extension,
     *  unless it is needed (i.e. we have a slice) when requested.
     */
    public String fullname() 
    {
        if ( fitsext_.equals( "" ) || ! slice_.equals( "" ) ) {
            return fullname_;
        } else {
            return diskfile_ + path_ + slice_;
        }
    }

    /**
     *  Get the diskfile name.
     */
    public String diskfile() 
    {
        return diskfile_;
    }

    /**
     *  Get the NDF slice.
     */
    public String slice() 
    {
        return slice_;
    }

    /**
     *  Get the FITS extension.
     */
    public String fitsext() 
    {
        return fitsext_;
    }

    /**
     *  Get the FITS extension number. Note this is 0, if an NDF slice
     *  is available.
     */
    public int fitshdunum() 
    {
        if ( slice_.equals( "" ) ) {
            return fitshdu_;
        } else {
            return 0;
        }
    }
    
    /**
     *  Get the HDS path.
     */
    public String path() 
    {
        return path_;
    }

    /** 
     *  Get the diskfile type.
     */
    public String type() 
    {
        return type_;
    }

    /**
     *  Get the "data format" of the file. This is one of "NDF",
     *  "FITS" or "TEXT", depending on what the file extension and
     *  suitability to process are. If the file cannot be recognised
     *  then "UNKNOWN" is returned;
     */
    public String format() 
    {
        if ( type_.indexOf( ".fit" ) > -1 ||
             type_.indexOf( ".FIT" ) > -1 ||
             type_.indexOf( ".fts" ) > -1 ) {
            if ( slice_.equals( "" ) ) {
                return "FITS";
            }
        }
        if ( type_.indexOf( ".txt" ) > -1 ||
             type_.indexOf( ".lis" ) > -1 ||
             type_.indexOf( ".cat" ) > -1 ||
             type_.indexOf( ".dat" ) > -1 ||
             type_.indexOf( ".bjd" ) > -1 ) {
            return "TEXT";
        }
        if ( type_.equals( ".sdf" ) ) {
            return "NDF";
        }

        //  If the file type matches a string in NDF_FORMATS_IN,
        //  that's great. Note this needs to be set using:
        //  -Dndf.formats.in=$NDF_FORMATS_IN on the command-line.
        String ndfFormatsIn = System.getProperty( "ndf.formats.in", "" );
        if ( ! ndfFormatsIn.equals( "" ) ) {
            if ( ndfFormatsIn.indexOf( type_ ) > -1 ) {
                return "NDF";
            }
        }
        return "UNKNOWN";
    }

    /**
     *  Get the filename as used by Starlink applications. This is the
     *  fullname without the ".sdf". TODO: do we need single quotes to get
     *  any FITS extensions out to ADAM tasks?
     */
    public String ndfname() 
    {
        if ( type_.equals( ".sdf" ) ) {
            int i1 = fullname_.indexOf( ".sdf" );
            if ( i1 > -1 ) {
                String name = fullname_.substring( 0, i1 ) + 
                              fullname_.substring( i1 + 4 );
                return name;
            } else {
                return "";
            }
        }
        

        //  Foreign format. Always return these in single quotes to protect
        //  special characters.
        if ( fitshdu_ != 0 ) {
            //return "'" + diskfile_ + "[" + fitshdu_ + "]" + slice_ + "'";
            return diskfile_ + "[" + fitshdu_ + "]" + slice_;
        }
        //return "'" + fullname_ + "'";
        return fullname_;
   }
    
    /**  
     *  Check if diskfile exists, is readable and a plain file.
     */
    public boolean exists () 
    {
        File file = new File( diskfile_ );
        return ( file.canRead() && file.isFile() );
    }

    /**
     *  Make name absolute (i.e. start with leading "/").
     */
    public String absolute() 
    {
        File file = new File( specspec );
        specspec = file.getAbsolutePath();
        parseName();
        return specspec;
    }

    /**  
     *  Parse the spectrum specification, obtaining the fully expanded
     *  name, the diskfile name and any NDF extended parts.
     */
    protected void parseName() 
    {
        reset();
        getSlice();
        getFitsext();
        getType();
        if ( ! checkType() ) {
            getPath();
        }
        getDiskfile();
        getFullname();
    }

    /** 
     *  Get any slice information from the image name.
     */
    protected void getSlice () 
    {
        int i1 = specspec.lastIndexOf( "(" );
        int i2 = specspec.lastIndexOf( ")" );
        if ( i1 > -1 && i2 > -1 ) {
            slice_ = specspec.substring( i1, i2 + 1 );
        } else {
            slice_ = "";
        }
    }

    /**
     *  Get any FITS extension information from the image name. Extract
     *  the extension number as we need to decrement this for FITS files
     *  that are to be processed by CONVERT & NDF.
     */
    protected void getFitsext() 
    {
        int i1 = specspec.lastIndexOf( "[" );
        int i2 = specspec.lastIndexOf( "]" );
        if ( i1 > -1 && i2 > -1 ) {
            fitsext_ = specspec.substring( i1, i2 + 1 );
            try {
                fitshdu_ = Integer.parseInt( 
                    specspec.substring( i1 + 1, i2 ) );
            } catch ( Exception e ) {
                fitshdu_ = 0;
            }
        } else {
            fitsext_ = "";
        }
    }

    /**
     *  Get the file type. This is the string (minus any FITS extension
     *  or NDF slice) after the first "."  in the string after the last
     *  directory separator. If no type is given then it defaults to
     * ".sdf".
     */
    protected void getType() 
    {
        File file = new File( specspec );
        String name = file.getName();
        int i1 = name.indexOf( "." );
        if ( i1 > -1 ) {
            if ( ! fitsext_.equals( "") ) {
                int i2 = name.indexOf( fitsext_ );
                type_ = name.substring( i1, i2 );
            } else if ( ! slice_.equals( "" ) ) {
                int i2 = name.indexOf( slice_ );
                type_ = name.substring( i1, i2 );
            } else {
                type_ = name.substring( i1 );
            }
        } else {
            type_ = ".sdf";
        }
    }

    /**  
     *  Check if the file type is known to the NDF system, or is a FITS
     *  or TEXT description.
     */
    protected boolean checkType () 
    {
        if ( format().equals( "UNKNOWN" ) ) {
            return false;
        }
        return true;
    }

    /**
     *  Construct name of diskfile. Assumes type_ already set.
     */
    protected void getDiskfile() 
    {
        int i1 = specspec.indexOf( type_ );
        if ( i1 > -1 ) {
            diskfile_ = specspec.substring( 0, i1 ) + type_;
        } else {

            //  Type not in specspec, so fallback to path_.
            i1 = specspec.indexOf( path_ );
            if ( i1 > -1 && ! path_.equals( "" ) ) {
                diskfile_ = specspec.substring( 0, i1 ) + type_;
            } else {

                //  No type or path, so name must be complete, just remove
                //  slice.
                int i2;
                if ( ! slice_.equals( "" ) ) {
                    i2 = specspec.indexOf( slice_ );
                    diskfile_ = specspec.substring( 0, i2 ) + type_;
                } else {
                    diskfile_ = specspec + type_;
                }
            }
        }
    }

    /**  
     *  Construct the full name from the various parts.
     */
    protected void getFullname() 
    {
        fullname_ = diskfile_ + path_ + fitsext_ + slice_;
    }

    /**
     *  Get the path component from the name. Assumes a checkType has
     *  been failed, so missing type information implies an NDF and the
     *  current potential path is stored in "type_".
     */
    protected void getPath() 
    {
        int i1 = type_.indexOf( ".sdf" );
        int i2;
        if ( i1 > -1 ) {
            i1 += 4;
            if ( ! slice_.equals( "" ) ) {
                i2 = type_.indexOf( slice_ ) - 1;
                if ( i2 > -1 ) {
                    path_ = type_.substring( i1, i2 );
                } else {
                    path_ = type_.substring( i1 );
                }
            } else {
                path_ = type_.substring( i1 );
            }
        } else {

            //  No ".sdf", so assume what looks like a file extension is a
            //  path.
            if ( ! slice_.equals( "" ) ) {
                i2 = type_.indexOf( slice_ ) - 1;
                if ( i2 < 0 ) {
                    path_ = type_;
                } else {
                    path_ = type_.substring( 0, i2 );
                }
            } else {
                path_ = type_;
            }
        }
        type_ = ".sdf";
    }

    /**
     *  Reset internal configuration (when new name supplied).
     */
    protected void reset() 
    {
        fullname_ = "";
        diskfile_ = "";
        slice_ = "";
        fitsext_ = "";
        fitshdu_ = 0;
        path_ = "";
        type_ = ".sdf";
    }

    /**
     *  Name of the spectrum as supplied by the user.
     */
    protected String specspec = "";

    /**
     *  Fully expanded name.
     */
    protected String fullname_ = "";

    /**
     *  Disk file name
     */
    protected String diskfile_ = "";

    /**
     *  NDF slice.
     */
    protected String slice_ = "";

    /**
     *  FITS extension specification ([int]).
     */
    protected String fitsext_ = "";

    /**
     *  HDU number of FITS extension.
     */
    protected int fitshdu_  = 0;

    /**
     *  HDS path
     */
    protected String path_ = "";

    /**
     *  Disk file type.
     */
    protected String type_ = ".sdf";

    /**
     *  Test method.
     */
    public static void main( String[] args ) 
    {
        InputNameParser namer = new InputNameParser();
        String names[] = {
            "file.fits", 
            "file.fits[1]", 
            "file", 
            "file.txt",
            "file.lis", 
            "file.imh",
            "file(1:100,2:200)",
            "file.sdf(1:100,2:200)",
            "file.sdf",
            "file.ndf",
            "file.sdf.ndf(1:100,2:100)",
            "file.fits(1:100,3:300)",
            "file.fits[2](1:100,3:300)" };
        for ( int i = 0; i < names.length; i++ ) {
            System.out.println( "" );
            System.out.println( "Parsing: " + names[i] );
            System.out.println( "======== ");
            namer.setName( names[i] );
            System.out.println( "fullname = " + namer.fullname() );
            System.out.println( "diskfile = " + namer.diskfile() );
            System.out.println( "slice = " + namer.slice() );
            System.out.println( "fitsext = " + namer.fitsext() );
            System.out.println( "fitshdunum = " + namer.fitshdunum() );
            System.out.println( "path = " + namer.path() );
            System.out.println( "type = " + namer.type() );
            System.out.println( "ndfname = " + namer.ndfname() );
            System.out.println( "exists = " + namer.exists () );
            System.out.println( "absolute = " + namer.absolute() );
            System.out.println( "format = " + namer.format() );
        }
    }
}
