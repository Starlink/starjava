package uk.ac.starlink.splat.data;

import java.io.File;

import uk.ac.starlink.splat.util.SplatException;
import uk.ac.starlink.splat.imagedata.NDFJ;

/**
 * This class creates and clones instances of SpecData that are
 * appropriate to the data format of the given spectrum specification.
 * <p>
 * The specification is usual the file name plus extension and any
 * qualifiers (such as HDS object path, or FITS extension
 * number). This is parsed and identified by the InputNameParser
 * class.
 *
 * @author Peter W. Draper
 * @version $Id$
 * @since $Date$
 * @since 01-SEP-2000
 * @see SpecDataImpl
 * @see SpecData
 * @see InputNameParser
 * @see "The Singleton Design Pattern"
 */
public class SpecDataFactory
{
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
    public SpecData get( String specspec ) throws SplatException
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

            if ( impl == null ) {
                // If specspec if just a file, then we're having some issues
                // with accessing it using the known schemes.
                File testFile = new File( specspec );
                if ( testFile.exists() ) {
                    if ( testFile.canRead() ) {
                        throw new SplatException( "Spectrum '" + specspec +
                                                  "' has an unknown type, "+
                                                  "format or name syntax." );
                    }
                    else {
                        throw new SplatException( "Cannot read: " + specspec );
                    }
                }
                else {
                    
                    //  Just a file that doesn't exist.
                    throw new SplatException( "Spectrum not found: " +
                                              specspec ); 
                }
            }
        }
        else {
            //  File doesn't exist. Could still be an HDX sent down
            //  the wire, check the format and try this for all XML
            //  types.
            if ( namer.format().equals( "XML" ) ) {
                impl = new NDXSpecDataImpl( specspec );
            }
        }
        if ( impl == null ) {
            throw new SplatException( "Do not recognise '" + 
                                      specspec +"' as a spectrum" );
        }
        
        //  Wrap the implementation in a SpecData object.
        SpecData specData = new SpecData( impl );
        return specData;
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
}
