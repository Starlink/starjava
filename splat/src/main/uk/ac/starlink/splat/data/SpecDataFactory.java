/*
 * Copyright (C) 2000-2003 Central Laboratory of the Research Councils
 *
 *  History:
 *     01-SEP-2000 (Peter W. Draper):
 *       Original version.
 */
package uk.ac.starlink.splat.data;

import java.io.File;

import uk.ac.starlink.splat.util.SplatException;
import uk.ac.starlink.splat.imagedata.NDFJ;

/**
 * This class creates and clones instances of SpecData and derived
 * classes. The type of the spectrum supplied is determined either by
 * heuristics based on the specification, or by a given, known, type.
 * <p>
 * The known types are identified by public variables and are
 * described using short and long names.
 * <p>
 * If an untyped specification is given then the usual the file name
 * plus extension and any qualifiers (such as HDS object path, or FITS
 * extension number). This is parsed and identified by the InputNameParser
 * class.
 * <p>
 * EditableSpecData and LineIDSpecData instances can also be created
 * and copied.
 *
 * @author Peter W. Draper
 * @version $Id$
 * @see SpecDataImpl
 * @see SpecData
 * @see InputNameParser
 * @see "The Singleton Design Pattern"
 */
public class SpecDataFactory
{
    //
    // Enumeration of the "types" of spectrum that can be created.
    //

    /** The type should be determined only using file name rules */
    public final static int DEFAULT = 0;

    /** The file is a FITS file. */
    public final static int FITS = 1;

    /** The file is a HDS file. */
    public final static int HDS = 2;

    /** The file is a TEXT file. */
    public final static int TEXT = 3;

    /** The file is an HDX file. */
    public final static int HDX = 4;

    /** The file is an line identifier file. */
    public final static int IDS = 5;

    /**
     * Short descriptions of each type.
     */
    public final static String[] shortNames = {
        "default", "fits", "hds", "text", "hdx", "line ids"
    };

    /**
     * Long descriptions of each type.
     */
    public final static String[] longNames = {
        "File extension rule", "FITS file", "HDS container file", 
        "TEXT files", "HDX/NDX XML files", "Line identification files"
    };

    /**
     * File extensions for each type.
     */
    public final static String[][] extensions = {
        {"*"}, {"fits", "fit"}, {"sdf"}, {"txt", "lis"}, {"xml"}, {"ids"}
    };


    /**
     * Treeview icons symbolic names for each data type. XXX may want
     * to extend this with own (line identifiers clearly not available).
     */
    public final static String[] treeviewIcons = {
        "FILE", "FITS", "NDF", "DATA", "NDX", "ARY2"
    };

    /**
     *  Create the single class instance.
     */
    private static final SpecDataFactory instance = new SpecDataFactory();

    /**
     *  Hide the constructor from use.
     */
    private SpecDataFactory() {}

    /**
     *  Return reference to the only allowed instance of this class.
     *
     *  @return reference to only instance of this class.
     */
    public static SpecDataFactory getReference()
    {
        return instance;
    }

    /**
     *  Attempt to open a given specification as a known type. If this
     *  fails then a SplatException will be thrown. Note that if the
     *  type is DEFAULT then this is equivalent to calling 
     *  {@link get(String)}. 
     *
     *  @param specspec the specification of the spectrum to be opened.
     *  @param type the type of the spectrum (one of types defined in
     *              this class).
     *  @return the SpecData object created from the given
     *          specification.
     *  @exception SplatException thrown if specification does not
     *             specify a spectrum that can be accessed.
     */
    public SpecData get( String specspec, int type )
        throws SplatException
    {
        if ( type == DEFAULT ) {
            return get( specspec );
        }

        SpecDataImpl impl = null;
        switch (type) 
        {
            case FITS: {
                impl = new FITSSpecDataImpl( specspec );
            }
            break;
            case HDS: {
                impl = new NDFSpecDataImpl( specspec );
            }
            break;
            case TEXT: {
                impl = new TXTSpecDataImpl( specspec );
            }
            break;
            case HDX: {
                impl = new NDXSpecDataImpl( specspec );
            }
            break;
            case IDS: {
                impl = new LineIDTXTSpecDataImpl( specspec );
            }
            break;
            default: {
                throw new SplatException( "Spectrum '" + specspec + "' supplied"+
                                          " with an unknown type: " + type );
            }
        }
        if ( impl == null ) {
            throwReport( specspec, true );
        }
        return makeSpecDataFromImpl( impl );
    }

    /**
     *  Check the format of the incoming specification and create an
     *  instance of SpecData for it.
     *
     *  @param specspec the specification of the spectrum to be
     *                  opened (i.e. file.fits, file.sdf,
     *                  file.fits[2], file.more.ext_1 etc.).
     *
     *  @return the SpecData object created from the given
     *          specification.
     *
     *  @exception SplatException thrown if specification does not
     *             specify a spectrum that can be accessed.
     */
    public SpecData get( String specspec ) 
        throws SplatException
    {
        SpecDataImpl impl = null;

        // Look for plain format.
        InputNameParser namer = new InputNameParser( specspec );
        if ( namer.exists() ) {
            String type = namer.format();
            if ( type.equals( "NDF" ) && NDFJ.supported() ) {
                impl = new NDFSpecDataImpl( namer.ndfname() );
            }
            else if ( type.equals( "FITS" ) ) {
                impl = new FITSSpecDataImpl( namer.ndfname() );
            }
            else if ( type.equals( "TEXT" ) ) {
                impl = new TXTSpecDataImpl( namer.ndfname() );
            }
            else if ( type.equals( "XML" ) ) {
                impl = new NDXSpecDataImpl( namer.ndfname() );
            }
            else {
                throw new SplatException( "Spectrum '" + specspec +
                                          "' has an unknown type or format." );
            }
        }
        else {

            //  File doesn't exist. Could still be an HDX sent down
            //  the wire, check the format and try this for all XML
            //  types.
            if ( namer.format().equals( "XML" ) ) {
                impl = new NDXSpecDataImpl( specspec );
            }
            else {

                //  Or it could be a line identifier file, with type
                //  ".ids". XXX Note such a file will be misidentified
                //  as an NDF with path ".ids".
                if ( ".ids".equals( namer.path() ) ) {
                    impl = new LineIDTXTSpecDataImpl( specspec );
                }
            }
        }

        // Occasionally 
        if ( impl == null ) {
            throwReport( specspec, false );
        }
        return makeSpecDataFromImpl( impl );
    }

    /**
     * Make a suitable SpecData for a given implementation.x
     */
    protected SpecData makeSpecDataFromImpl( SpecDataImpl impl )
        throws SplatException
    {
        SpecData specData = null;
        if ( impl instanceof LineIDTXTSpecDataImpl ) {
            specData = new LineIDSpecData( (LineIDTXTSpecDataImpl) impl );
        }
        else {
            specData = new SpecData( impl );
        }
        return specData;
    }

    /**
     * Make up a suitable report for a spectrum that cannot be
     * processed into a implementation and throw a SplatException.
     */
    private void throwReport( String specspec, boolean typed )
        throws SplatException
    {
        // If specspec if just a file, then we're having some issues
        // with accessing it using the known schemes or defined type.
        File testFile = new File( specspec );
        if ( testFile.exists() ) {
            if ( testFile.canRead() ) {
                if ( typed ) {
                    //  An explicit type was specified.
                    throw new SplatException( "Spectrum '" + specspec +
                                              "' cannot be matched to " +
                                              "the requested type " );
                }
                else {
                    throw new SplatException( "Spectrum '" + specspec +
                                              "' has an unknown type, "+
                                              "format or name syntax." );
                }
            }
            else {
                throw new SplatException( "Cannot read: " + specspec );
            }
        }
        else {
            //  Just a file that doesn't exist.
            throw new SplatException( "Spectrum not found: " + specspec ); 
        }
    }

    /**
     * Create an clone of an existing spectrum by transforming it into
     * another implementation format. The destination format is
     * decided using the usual rules on specification string.
     *
     * @param source SpecData object to be cloned.
     * @param specspec name of the resultant clone (defines
     *                 implementation type).
     *
     * @return the cloned SpecData object.
     * @exception SplatException maybe thrown if there are problems
     *            creating the new SpecData object or the implementation.
     */
    public SpecData getClone( SpecData source, String specspec )
        throws SplatException
    {
        InputNameParser namer = new InputNameParser( specspec );
        String targetType = namer.format();
        String sourceType = source.getDataFormat();

        //  Create an implementation object using the source to
        //  provide the content (TODO: could be more efficient?).
        SpecDataImpl impl = null;
        if ( targetType.equals( "NDF" ) ) {
            impl = new NDFSpecDataImpl( namer.ndfname(), source );
        }
        if ( targetType.equals( "FITS" ) ) {
            impl = new FITSSpecDataImpl( namer.ndfname(), source );
        }
        if ( targetType.equals( "TEXT" ) ) {
            impl = new TXTSpecDataImpl( namer.ndfname(), source );
        }
        SpecData specData = new SpecData( impl );
        specData.setType( source.getType() );
        return specData;
    }

    /**
     * Create spectrum that can have its values modified or
     * individually editted. Initially the object contains no
     * coordinates or data, so space for these must be allocated and
     * set using the {@link EditableSpecData.setData} method.
     *
     * @param shortname the short name to use for the spectrum.
     *
     * @return an EditableSpecData object.
     */
    public EditableSpecData createEditable( String shortname )
        throws SplatException
    {
        return new EditableSpecData( new MEMSpecDataImpl( shortname ) );
    }
    /**

     * Copy a spectrum into one that can have its values modified or
     * individually editted. If the SpecData is an instance if
     * LineIDSpecData then another LineIDSpecData instance will be
     * returned, otherwise a plain EditableSpecData instance will be
     * returned.
     *
     * @param shortname the short name to use for the spectrum copy.
     *
     * @return an EditableSpecData object.
     */
    public EditableSpecData createEditable( String shortname, 
                                            SpecData specData )
        throws SplatException
    {
        // Check the actual type to see if this needs special
        // handling.
        if ( specData instanceof LineIDSpecData ) {
            return new LineIDSpecData
                ( new LineIDMEMSpecDataImpl( shortname, specData ) );
        }
        else {
            return new EditableSpecData
                ( new MEMSpecDataImpl( shortname, specData ) );
        }
    } 

}
