package uk.ac.starlink.frog.data;

import java.io.File;

import uk.ac.starlink.frog.util.FrogException;
import uk.ac.starlink.frog.util.FrogDebug;

/**
 * This class creates and clones instances of TimeSeries that are
 * appropriate to the data format of the given series specification.
 * <p>
 * The specification is usual the file name plus extension and any
 * qualifiers (such as HDS object path, or FITS extension
 * number). This is parsed and identified by the InputNameParser
 * class.
 *
 * @author Peter W. Draper
 * @author Alasdair Allan
 * @version $Id$
 * @since $Date$
 * @since 01-SEP-2000
 * @see TimeSeriesImpl
 * @see TimeSeries
 * @see InputNameParser
 * @see "The Singleton Design Pattern"
 */
public class TimeSeriesFactory
{

   /**
     *  Application wide debug manager
     */
    protected FrogDebug
        debugManager = FrogDebug.getReference(); 
        
    /**
     *  Create the single class instance.
     */
    private static final TimeSeriesFactory instance = new TimeSeriesFactory();

    /**
     *  Hide the constructor from use.
     */
    private TimeSeriesFactory() {}

    /**
     *  Return reference to the only allowed instance of this class.
     *
     *  @return reference to only instance of this class.
     */
    public static TimeSeriesFactory getReference()
    {
        return instance;
    }

    /**
     *  Check the format of the incoming specification and create an
     *  instance of TimeSeries for it.
     *
     *  @param specspec the specification of the series to be
     *                  opened (i.e. file.fits, file.sdf,
     *                  file.fits[2], file.more.ext_1 etc.).
     *
     *  @return the TimeSeries object created from the given
     *          specification.
     *
     *  @exception FrogException thrown if specification does not
     *             specify a series that can be accessed.
     */
    public TimeSeries get( String specspec ) throws FrogException
    {
        TimeSeriesImpl impl = null;
        debugManager.print( "            TimeSeriesFactory.get()");

        // Look for plain format.
        InputNameParser namer = new InputNameParser( specspec );
        if ( namer.exists() ) {
        
            String type = namer.format();
          //  if ( type.equals( "NDF" ) ) {
          //       impl = new NDFTimeSeriesImpl( namer.ndfname() );
          //   }
          //   else if ( type.equals( "FITS" ) ) {
            if ( type.equals( "FITS" ) ) {
                impl = new FITSTimeSeriesImpl( namer.ndfname() );
            } else if ( type.equals( "TEXT" ) ) {
                impl = new TXTTimeSeriesImpl( namer.ndfname() );
            } else {
                throw new FrogException( "Series '" + specspec +
                                       "' has an unknown type or format." );
            }
          
 
            if ( impl == null ) {
                // If specspec if just a file, then we're having some issues
                // with accessing it using the known schemes.
                File testFile = new File( specspec );
                if ( testFile.exists() ) {
                    if ( testFile.canRead() ) {
                        throw new FrogException( "Series '" + specspec +
                                                  "' has an unknown type, "+
                                                  "format or name syntax." );
                    }
                    else {
                        throw new FrogException( "Cannot read: " + specspec );
                    }
                }
                else {
                    
                    //  Just a file that doesn't exist.
                    throw new FrogException( "Series not found: " +
                                              specspec ); 
                }
            }
        }
  //      else {
            //  try the HdxInputNameParser to see if the file is an HDX
            //  one.
   //         HdxInputNameParser hdxNamer = new HdxInputNameParser( specspec );
  //          if ( hdxNamer.format().equals( "XML" ) ) {
   //             impl = new NDXTimeSeriesImpl( specspec );
   //         }
  //      }
        if ( impl == null ) {
            throw new FrogException( "Unable to open " + specspec );
        }
        
        //  Wrap the implementation in a TimeSeries object.
        TimeSeries specData = new TimeSeries( impl );
        return specData;
    }

    /**
     * Create an clone of an existing series by transforming it into
     * another implementation format. The destination format is
     * decided using the usual rules on specification string.
     *
     * @param source TimeSeries object to be cloned.
     * @param specspec name of the resultant clone (defines
     *                 implementation type).
     *
     * @return the cloned TimeSeries object.
     * @exception FrogException maybe thrown if there are problems
     *            creating the new TimeSeries object or the implementation.
     */
    public TimeSeries getClone( TimeSeries source, String specspec )
        throws FrogException
    {
        InputNameParser namer = new InputNameParser( specspec );
        String targetType = namer.format();
        String sourceType = source.getDataFormat();

        //  Create an implementation object using the source to
        //  provide the content (TODO: could be more efficient?).
        TimeSeriesImpl impl = null;
       // if ( targetType.equals( "NDF" ) ) {
       //     impl = new NDFTimeSeriesImpl( namer.ndfname(), source );
       // }
        if ( targetType.equals( "FITS" ) ) {
            impl = new FITSTimeSeriesImpl( namer.ndfname(), source );
        }
        if ( targetType.equals( "TEXT" ) ) {
            impl = new TXTTimeSeriesImpl( namer.ndfname(), source );
        }
        TimeSeries specData = new TimeSeries( impl );
        specData.setType( source.getType() );
        return specData;
    }
}
