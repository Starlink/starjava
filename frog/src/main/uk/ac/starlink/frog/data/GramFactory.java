package uk.ac.starlink.frog.data;

import java.io.File;

import uk.ac.starlink.frog.data.TimeSeries;
import uk.ac.starlink.frog.util.FrogException;
import uk.ac.starlink.frog.util.FrogDebug;

import uk.ac.starlink.frog.gram.FourierTransform;
import uk.ac.starlink.frog.gram.ChisqPeriodogram;

/**
 * This class creates and clones instances of Gram that are
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
 * @see GramImpl
 * @see Gram
 * @see InputNameParser
 * @see "The Singleton Design Pattern"
 */
public class GramFactory
{

   /**
     *  Application wide debug manager
     */
    protected FrogDebug
        debugManager = FrogDebug.getReference(); 
        
    /**
     *  Create the single class instance.
     */
    private static final GramFactory instance = new GramFactory();

    /**
     *  Hide the constructor from use.
     */
    private GramFactory() {}

    /**
     *  Return reference to the only allowed instance of this class.
     *
     *  @return reference to only instance of this class.
     */
    public static GramFactory getReference()
    {
        return instance;
    }

    /**
     *  Check the format of the incoming specification and create an
     *  instance of Gram for it.
     *
     *  @param specspec the specification of the series to be
     *                  opened (i.e. file.fits, file.sdf,
     *                  file.fits[2], file.more.ext_1 etc.).
     *
     *  @return the Gram object created from the given
     *          specification.
     *
     *  @exception FrogException thrown if specification does not
     *             specify a series that can be accessed.
     */
    public Gram get( String specspec ) throws FrogException
    {
        GramImpl impl = null;
        debugManager.print( "            GramFactory.get()");

        // Look for plain format.
        InputNameParser namer = new InputNameParser( specspec );
        if ( namer.exists() ) {
        
            String type = namer.format();
          //  if ( type.equals( "NDF" ) ) {
          //       impl = new NDFGramImpl( namer.ndfname() );
          //   }
          //   else if ( type.equals( "FITS" ) ) {
           // if ( type.equals( "FITS" ) ) {
           //     impl = new FITSGramImpl( namer.ndfname() );
          //  } else if ( type.equals( "TEXT" ) ) {
            if ( type.equals( "TEXT" ) ) {
                impl = new TXTGramImpl( namer.ndfname() );
            } else {
                throw new FrogException( "Periodogram '" + specspec +
                                       "' has an unknown type or format." );
            }
          
 
            if ( impl == null ) {
                // If specspec if just a file, then we're having some issues
                // with accessing it using the known schemes.
                File testFile = new File( specspec );
                if ( testFile.exists() ) {
                    if ( testFile.canRead() ) {
                        throw new FrogException( "s '" + specspec +
                                                  "' has an unknown type, "+
                                                  "format or name syntax." );
                    }
                    else {
                        throw new FrogException( "Cannot read: " + specspec );
                    }
                }
                else {
                    
                    //  Just a file that doesn't exist.
                    throw new FrogException( "Periodogram not found: " +
                                              specspec ); 
                }
            }
        }
  //      else {
            //  try the HdxInputNameParser to see if the file is an HDX
            //  one.
   //         HdxInputNameParser hdxNamer = new HdxInputNameParser( specspec );
  //          if ( hdxNamer.format().equals( "XML" ) ) {
   //             impl = new NDXGramImpl( specspec );
   //         }
  //      }
        if ( impl == null ) {
            throw new FrogException( "Unable to open " + specspec );
        }
        
        //  Wrap the implementation in a Gram object.
        Gram specData = new Gram( impl );
        return specData;
    }
   
   /**
     *  Generate a periodogram from a TimeSeries object
     *
     *  @param series TimeSeries object
     *  @param window whether we want a window function (FT only)
     *  @param min Minimum frequency (default 0.0)
     *  @param max Maximum frequency (default nyquist)
     *  @param interval Frequency interval ( 1/ (4 X total time interval) )
     *  @param type the type of periodogram to generate
     *
     *  @return the Gram object created from the given
     *          specification.
     *
     *  @exception FrogException thrown if specification does not
     *             specify a series that can be accessed.
     */
     public Gram get( TimeSeries series, boolean window,
                      double minFreq, double maxFreq, 
                      double freqInterval, String type ) throws FrogException
     {
        GramImpl impl = null;
        debugManager.print( "            GramFactory.get()");

        if ( type == "FOURIER" ) {
           impl = 
              FourierTransform.make(series,window,minFreq,maxFreq,freqInterval);
        } 
        else if ( type == "CHISQ" ) {
           impl = 
            ChisqPeriodogram.make(series,window,minFreq,maxFreq,freqInterval);
        }
        
        if ( impl == null ) {
            throw new FrogException( "Unable to build " + type );
        }
        
        //  Wrap the implementation in a Gram object.
        Gram gram = new Gram( impl );
        return gram;
     } 

    /**
     * Create an clone of an existing series by transforming it into
     * another implementation format. The destination format is
     * decided using the usual rules on specification string.
     *
     * @param source Gram object to be cloned.
     * @param specspec name of the resultant clone (defines
     *                 implementation type).
     *
     * @return the cloned Gram object.
     * @exception FrogException maybe thrown if there are problems
     *            creating the new Gram object or the implementation.
     */
    public Gram getClone( Gram source, String specspec )
        throws FrogException
    {
        InputNameParser namer = new InputNameParser( specspec );
        String targetType = namer.format();
        String sourceType = source.getDataFormat();

        //  Create an implementation object using the source to
        //  provide the content (TODO: could be more efficient?).
        GramImpl impl = null;
       // if ( targetType.equals( "NDF" ) ) {
       //     impl = new NDFGramImpl( namer.ndfname(), source );
       // }
       // if ( targetType.equals( "FITS" ) ) {
        //    impl = new FITSGramImpl( namer.ndfname(), source );
       // }
        if ( targetType.equals( "TEXT" ) ) {
            impl = new TXTGramImpl( namer.ndfname(), source );
        }
        Gram specData = new Gram( impl );
        specData.setType( source.getType() );
        return specData;
    }
}
