package uk.ac.starlink.table.formats;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import uk.ac.starlink.table.ColumnInfo;
import uk.ac.starlink.table.TableFormatException;

/**
 * Class which reads the header of a WDC text file and is then capable
 * of answering questions about it and reading rows of the actual data.
 */
class WDCReader {

    private static final Pattern SPACES_PATTERN = 
        Pattern.compile( "\\s*(\\S+)" );
    private static Logger logger = 
        Logger.getLogger( "uk.ac.starlink.table.formats" );


    private ColumnInfo[] colinfos;
    private Eater[] eaters;

    public WDCReader( BufferedInputStream strm ) throws IOException {

        /* Bail out if the static parts of the header look unfamiliar. */
        String l1 = WDCTableBuilder.readLine( strm );
        if ( l1 == null && ! l1.startsWith( "Column formats and units" ) ) {
            throw new TableFormatException( "Doesn't look quite " +
                                            "like a WDC file" );
        }
        String l2 = WDCTableBuilder.readLine( strm );
        if ( l2 == null && ! l2.startsWith( "---" ) ) {
            throw new TableFormatException( "Doesn't look quite " +
                                            "like a WDC file" );
        }

        /* Loop over header lines. */
        List colinfoList = new ArrayList();
        List eaterList = new ArrayList();
        while ( true ) {
            String line = WDCTableBuilder.readLine( strm );

            /* Check for end of file. */
            if ( line == null ) {
                throw new TableFormatException( "End of file in WDC headers" );
            }

            /* Parse the line. */

            /* Look for special date format. */
            if ( line.toLowerCase()
                .matches( "^\\s*datetime.*yy+\\s+mm\\s+dd\\s+hhmmss.*" ) ) {
                ColumnInfo colinfo = 
                    new ColumnInfo( "Date", Date.class, "Date of observation" );
                Eater eater = new Eater() {
                    public Object eat( Matcher matcher ) {
                        Calendar cal = Calendar.getInstance();

                        matcher.find();
                        int year = Integer.parseInt( matcher.group( 1 ) );
                        cal.set( Calendar.YEAR, year );

                        matcher.find();
                        int month = Integer.parseInt( matcher.group( 1 ) );
                        cal.set( Calendar.MONTH, month );

                        matcher.find();
                        int day = -1 + Integer.parseInt( matcher.group( 1 ) );
                        cal.set( Calendar.DAY_OF_MONTH, day );

                        matcher.find();
                        String hhmmss = matcher.group( 1 );
                        int hour = Integer.parseInt( hhmmss.substring( 0, 2 ) );
                        int min = Integer.parseInt( hhmmss.substring( 2, 4 ) );
                        int sec = Integer.parseInt( hhmmss.substring( 4, 6 ) );
                        cal.set( Calendar.HOUR_OF_DAY, hour );
                        cal.set( Calendar.MINUTE, min );
                        cal.set( Calendar.SECOND, sec );

                        return cal.getTime();
                    }
                };
                colinfoList.add( colinfo );
                eaterList.add( eater );
            }

            /* Separator descriptor. */
            else if ( line.matches( "^\\s+%[0-9]+[a-z]\\s*$" ) ) {
                // ignore it
            }

            /* Heading line. */
            else if ( line
                     .matches( "^\\S.*\\s%[0-9\\.\\+\\-]+[a-z]\\s+[^%]*$" ) ) {
                Pattern pat = 
                    Pattern.compile( "^(\\S.*?)\\s*%[0-9\\.\\+\\-]+([a-z]).*" );
                Matcher matcher = pat.matcher( line );
                assert matcher.find();
                String colname = matcher.group( 1 );
                char fmt = matcher.group( 2 ).charAt( 0 );
                Class clazz;
                Eater eater;
                switch ( fmt ) {
                    case 'd':
                    case 'i':
                        clazz = Integer.class;
                        eater = new Eater() {
                            public Object eat( Matcher matcher ) {
                                matcher.find();
                                String word = matcher.group( 1 );
                                return word.equals( "-" ) 
                                     ? null
                                     : Integer.valueOf( word );
                            }
                        };
                        break;
                    case 'e':
                    case 'f':
                    case 'g':
                        clazz = Float.class;
                        eater = new Eater() {
                            public Object eat( Matcher matcher ) {
                                matcher.find();
                                String word = matcher.group( 1 );
                                return word.equals( "-" ) 
                                     ? null
                                     : Float.valueOf( word );
                            }
                        };
                        break;
                    case 's':
                        // better hope there are no spaces
                        clazz = String.class;
                        eater = new Eater() {
                            public Object eat( Matcher matcher ) {
                                matcher.find();
                                String word = matcher.group( 1 );
                                return word;
                            }
                        };
                        break;
                    default:
                        throw new TableFormatException( "Unknown data format '" 
                                                      + fmt + "'" );
                }
                colinfoList.add( new ColumnInfo( colname, clazz, null ) );
                eaterList.add( eater );
            }

            /* Empty line signals end of header. */
            else if ( line.trim().length() == 0 ) {
                break;
            }

            else {
                logger.warning( "Ignoring strange format line \"" 
                              + line + "\"" );
            }
        }

        /* Store these as arrays for efficiency. */
        colinfos = (ColumnInfo[]) colinfoList.toArray( new ColumnInfo[ 0 ] );
        eaters = (Eater[]) eaterList.toArray( new Eater[ 0 ] );
        assert colinfos.length == eaters.length;
    }

    public ColumnInfo[] getColumnInfos() {
        return colinfos;
    }

    public Object[] decodeLine( String line ) {
        int ipos = 0;
        int ncol = colinfos.length;
        Object[] row = new Object[ ncol ];
        Matcher matcher = SPACES_PATTERN.matcher( line );
        for ( int icol = 0; icol < ncol; icol++ ) {
            row[ icol ] = eaters[ icol ].eat( matcher );
        }
        return row;
    }

    /**
     * Helper interface to define the behaviour of a column reader.
     */
    private static interface Eater {

        /**
         * This eater should read as many groups (space-delimited words)
         * from the matcher as required and return an object created
         * from the values it finds.
         * 
         * @param  matcher  the matcher holding the string data
         * @return  the read object got from the words
         */
        public Object eat( Matcher matcher );
    }

}
