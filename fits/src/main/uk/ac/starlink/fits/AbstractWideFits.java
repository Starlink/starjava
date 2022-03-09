package uk.ac.starlink.fits;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

/**
 * Implementations of the WideFits interface.
 * This class fills in the details of the general idea defined in
 * WideFits.  Static methods provide concrete implementations.
 *
 * <p>The Wide FITS convention is defined in the file
 * (fits/src/docs/)wide-fits.txt
 *
 * @author   Mark Taylor
 * @since    27 Jul 2017
 */
public abstract class AbstractWideFits implements WideFits {

    private final int icolContainer_;
    private final int extColMax_;
    private final CardFactory cardFactory_;
    private final String name_;

    /** Index of container column hosting extended column data. */
    public static final String KEY_ICOL_CONTAINER = "XT_ICOL";

    /** Header key for extended column count - includes standard ones. */
    public static final String KEY_NCOL_EXT = "XT_NCOL";

    private static final Logger logger_ =
        Logger.getLogger( "uk.ac.starlink.fits" );

    /**
     * Constructor.
     *
     * @param  icolContainer  1-based index of container column
     *                        used for storing extended column data;
     *                        usually 999
     * @param  extColMax    maximum number of extended columns
     *                      (including standard columns) that can be
     *                      represented by this convention
     * @param  cardFactory  object which should be used to construct
     *                      header cards associated with this implementation
     * @param  implName   base name of this implementation
     */
    protected AbstractWideFits( int icolContainer, int extColMax,
                                CardFactory cardFactory, String implName ) {
        icolContainer_ = icolContainer;
        if ( icolContainer_ > MAX_NCOLSTD ) {
            throw new IllegalArgumentException( "Container column index > "
                                              + MAX_NCOLSTD );
        }
        extColMax_ = extColMax;
        cardFactory_ = cardFactory;
        name_ = implName
              + ( icolContainer == MAX_NCOLSTD ? "" : icolContainer );
    }

    public int getContainerColumnIndex() {
        return icolContainer_;
    }

    public int getExtColumnMax() {
        return extColMax_;
    }

    public CardImage[] getContainerColumnCards( long nbyteExt, long nslice ) {

        /* Work out how we will declare the data type for this column
         * in the FITS header.  Since this column data is not supposed
         * to have any meaning, it doesn't matter what format we use
         * as long as it's the right length.
         * The obvious thing would be to use 'B' format (byte),
         * and that does work.  However, FITS bytes are unsigned,
         * and java bytes are signed.  To get round that,
         * the STIL FITS reading code usually turns FITS bytes into java
         * shorts on read.  That doesn't break anything, but it means that
         * if a non-WideFits-aware STIL reader encounters a WideFits table,
         * it may end up doing expensive and useless conversions of
         * large byte arrays to large short arrays.
         * (Other software may or may not have similar issues with
         * FITS unsigned bytes, I don't know).
         * So, if the element size is an even number of bytes, write it
         * using TFORM = 'I' (16-bit signed integer) instead.
         * Since the FITS and java 16-bit types match each other, this
         * avoids the problem.  If it's an odd number, we still have to
         * go with bytes. */
        final char formChr;
        final short formSiz;
        long elSize = nslice > 0 ? nbyteExt / nslice : nbyteExt;
        if ( elSize % 2 == 0 ) {
            formChr = 'I';
            formSiz = 2;
        }
        else {
            formChr = 'B';
            formSiz = 1;
        }
        long nEl = nbyteExt / formSiz;
        assert nEl * formSiz == nbyteExt;

        /* If requested, prepare to write a TDIM header. */
        final String dimStr;
        if ( nslice > 0 ) {
            if ( nEl % nslice == 0 ) {
                dimStr = new StringBuffer()
                    .append( '(' )
                    .append( nEl / nslice )
                    .append( ',' )
                    .append( nslice )
                    .append( ')' )
                    .toString();
            }
            else {
                logger_.severe( nEl + " not divisible by " + nslice
                              + " - no TDIM" + icolContainer_ );
                dimStr = null;
            }
        }
        else {
            dimStr = null;
        }

        /* Add the relevant entries to the header. */
        BintableColumnHeader colhead =
            BintableColumnHeader.createStandardHeader( icolContainer_ );
        List<CardImage> cards = new ArrayList<>();
        String forcol = " for column " + icolContainer_;
        cards.add( cardFactory_
                  .createStringCard( colhead.getKeyName( "TTYPE" ),
                                     "XT_MORECOLS",
                                     "label" + forcol ) );
        cards.add( cardFactory_
                  .createStringCard( colhead.getKeyName( "TFORM" ),
                                     Long.toString( nEl ) + formChr,
                                     "format" + forcol ) );
        if ( dimStr != null ) {
            cards.add( cardFactory_
                      .createStringCard( colhead.getKeyName( "TDIM" ),
                                         dimStr,
                                         "dimensions" + forcol ) );
        }
        cards.add( cardFactory_
                  .createStringCard( colhead.getKeyName( "TCOMM" ),
                                     "Extension buffer for columns beyond "
                                   + icolContainer_, null ) );
        return cards.toArray( new CardImage[ 0 ] );
    }

    public CardImage[] getExtensionCards( int ncolExt ) {
        return new CardImage[] {
            cardFactory_.createIntegerCard( KEY_ICOL_CONTAINER, icolContainer_,
                                            "index of container column" ),
            cardFactory_.createIntegerCard( KEY_NCOL_EXT, ncolExt,
                                            "total columns including extended" )
        };
    }

    public int getExtendedColumnCount( FitsHeader hdr, int ncolStd ) {
        Integer icolContainerValue = hdr.getIntValue( KEY_ICOL_CONTAINER );
        Integer ncolExtValue = hdr.getIntValue( KEY_NCOL_EXT );
        if ( icolContainerValue == null && ncolExtValue == null ) {
            return ncolStd;
        }
        else if ( icolContainerValue == null || ncolExtValue == null ) {
            logger_.warning( "FITS header has one but not both of "
                           + KEY_ICOL_CONTAINER + " and " + KEY_NCOL_EXT
                           + " - no extended columns" );
            return ncolStd;
        }
        int icolContainer = icolContainerValue.intValue();
        int ncolExt = ncolExtValue.intValue();
        if ( icolContainer != ncolStd ) {
            logger_.warning( "FITS header " + KEY_ICOL_CONTAINER + "="
                           + icolContainer
                           + " != standard column count (TFIELDS) " + ncolStd
                           + " - no extended columns" );
            return ncolStd;
        }
        for ( String tkey : new String[] { "TTYPE", "TFORM", "TCOMM" } ) {
            hdr.useKey( tkey + icolContainer );
        }
        logger_.config( "Located extended columns in wide FITS file" );
        return ncolExt;
    }

    @Override
    public String toString() {
        return name_;
    }

    /**
     * Returns a WideFits instance that uses normal TFORMaaa headers
     * where aaa is a 3-digit base-26 integer (each digit is [A-Z]).
     *
     * <p><strong>Note:</strong> this implementation is a historical relic.
     * It could be removed if its maintenance becomes problematic.
     *
     * @param  icolContainer  1-based index of container column
     *                        used for storing extended column data;
     *                        usually 999
     * @return  WideFits implementation
     */
    public static WideFits createAlphaWideFits( int icolContainer ) {
        return new AlphaWideFits( icolContainer );
    }

    /**
     * Returns a WideFits instance that uses headers of the form
     * HIERARCH XT TFORMnnnnn, using the ESO HIERARCH convention.
     *
     * @param  icolContainer  1-based index of container column
     *                        used for storing extended column data;
     *                        usually 999
     * @return  WideFits implementation
     */
    public static WideFits createHierarchWideFits( int icolContainer ) {
        return new HierarchWideFits( icolContainer );
    }

    /**
     * Utility method to write a log message indicating that this
     * convention is being used to write a FITS file.
     *
     * @param  logger   logger
     * @param  nStdcol  number of standard FITS columns
     * @param  nAllcol  total number of columns including extended
     */
    public static void logWideWrite( Logger logger, int nStdcol, int nAllcol ) {
        if ( nAllcol > nStdcol ) {
            logger.warning( "Using non-standard extended column convention" );
            logger.warning( "Other FITS software may not see columns "
                          + nStdcol + "-" + nAllcol );
        }
    }

    /**
     * Utility method to write a log message indicating that this
     * convention is being used to read a FITS file.
     *
     * @param  logger   logger
     * @param  nStdcol  number of standard FITS columns
     * @param  nAllcol  total number of columns including extended
     */
    public static void logWideRead( Logger logger, int nStdcol, int nAllcol ) {
        if ( nAllcol > nStdcol ) {
            logger.info( "Using non-standard extended column convention "
                       + "for columns " + nStdcol + "-" + nAllcol );
        }
    }

    /**
     * WideFits implementation based on using 3-digit base-26 numbers
     * to label extended columns in normal 8-character FITS keywords.
     *
     * <p><strong>Note:</strong> this implementation is a historical relic.
     * It could be removed if its maintenance becomes problematic.
     */
    static class AlphaWideFits extends AbstractWideFits {

        /** First digit used for extended column indexing. */
        private static final char DIGIT0 = 'A';

        /** Number of digits used for extended column indexing.
         * This is the base used for the index value encoding.
         * All characters in the range DIGIT0..NDIGIT must be legal FITS
         * keyword characters, and must not be decimal digits. */
        private static final int NDIGIT = 26;

        /**
         * Constructor.
         *
         * @param  icolContainer  1-based index of container column
         *                        used for storing extended column data
         */
        public AlphaWideFits( int icolContainer ) {
            super( icolContainer,
                   icolContainer - 1 + NDIGIT * NDIGIT * NDIGIT,
                   CardFactory.DEFAULT, "alpha" );
        }

        public BintableColumnHeader createExtendedHeader( int icolContainer,
                                                          int jcol ) {
            final String jcolStr = encodeInteger( jcol - icolContainer );
            return new BintableColumnHeader( CardFactory.DEFAULT ) {
                public String getKeyName( String stdName ) {
                    return stdName + jcolStr;
                }
            };
        }

        /**
         * Encodes an integer so it can be used as an extended column index.
         * This uses base 26, with the digits A-Z.
         *
         * <p>This must give a unique result of not more than 3 characters
         * for each input value in the allowed range,
         * which is legal for inclusion in a FITS keyword,
         * and which is not capable of interpretation as a decimal integer.
         *
         * @param  ix  input value in range 0&lt;ix&lt;17576
         * @return   string representation in base 26
         * @throws  NumberFormatException if input value is out of range
         */
        public String encodeInteger( int ix ) {
            int base = NDIGIT;
            int max = base * base * base;
            if ( ix >= 0 && ix < max ) {
                char[] digits = new char[ 3 ];
                int j = ix;
                for ( int k = 0; k < 3; k++ ) {
                    digits[ 2 - k ] = (char) ( DIGIT0 + ( j % base ) );
                    j = j / base;
                }
                return new String( digits );
            }
            else {
                String msg = "Out of range (0-" + ( max - 1 ) + "): " + ix;
                throw new NumberFormatException( msg );
            }
        }
    }

    /**
     * WideFits implementation based on the non-standard HIERARCH convention.
     */
    static class HierarchWideFits extends AbstractWideFits {

        public static final String NAMESPACE = "XT";

        /**
         * Constructor.
         *
         * @param  icolContainer  1-based index of container column
         *                        used for storing extended column data
         */
        public HierarchWideFits( int icolContainer ) {
            super( icolContainer, Integer.MAX_VALUE, CardFactory.HIERARCH,
                   "hierarch" );
        }

        public BintableColumnHeader createExtendedHeader( int icolContainer,
                                                          final int jcol ) {
            return new BintableColumnHeader( CardFactory.HIERARCH ) {
                public String getKeyName( String stdName ) {
                    return new StringBuffer()
                        .append( "HIERARCH" )
                        .append( " " )
                        .append( NAMESPACE )
                        .append( " " )
                        .append( stdName )
                        .append( jcol )
                        .toString();
                }
            };
        }
    }
}
