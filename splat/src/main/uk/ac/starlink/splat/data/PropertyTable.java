/*
 * Copyright (C) 2008 Science and Technology Facilities Council
 *
 *  History:
 *     17-DEC-2008 (Peter W. Draper):
 *        Original version.
 */

package uk.ac.starlink.splat.data;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import uk.ac.starlink.table.ColumnInfo;
import uk.ac.starlink.table.RandomStarTable;
import uk.ac.starlink.table.DefaultValueInfo;
import uk.ac.starlink.table.DescribedValue;

import nom.tam.fits.Header;
import nom.tam.fits.HeaderCard;

/**
 * Table implementation that wraps the properties of a {@link SpecData}
 * instance so that they appear to be the columns in a table. Naturally
 * this arrangement has a single row.
 * <p>
 * The expected use of this class is when performing expression evaluations
 * on the table meta-data to determine things like an offset when drawing
 * stacked spectra. Having the properties as the table columns (instead of the
 * table parameters) avoids the need to prefix the property names with
 * "param$". Names that have characters not allowed in a Java identifier 
 * will have the characters replaced with underscores.
 *
 * @author Peter W. Draper
 * @version $Id:$
 */
public class PropertyTable
    extends RandomStarTable
{
    /** The SpecData instance. */
    private SpecData specData = null;

    /** The table row. */
    private ArrayList row = null;

    /** Column infos. */
    private ArrayList colInfo = null;

    /** Compiled pattern for matching non-word characters. */
    private Pattern pattern = null;

    /**
     * Construct an instance wrapping the given {@link SpecData}.
     *
     * @param  specData the SpecData.
     */
    public PropertyTable( SpecData specData )
    {
        this.specData = specData;

        //  Get all parameters of the spectrum and add to the table columns.
        addHeaders();
    }

    public int getColumnCount()
    {
        if ( row != null ) {
            return row.size();
        }
        return 0;
    }

    public ColumnInfo getColumnInfo( int icol )
    {
        if ( colInfo != null ) {
            return (ColumnInfo) colInfo.get( icol );
        }
        return null;
    }

    public long getRowCount()
    {
        if ( row != null ) {
            return 1;
        }
        return 0;
    }

    public Object getCell( long lrow, int icol )
    {
        if ( row != null ) {
            return row.get( icol );
        }
        return null;
    }

    /**
     *  Add the SpecData headers as the table columns and row.
     */
    protected void addHeaders()
    {
        Header headers = specData.getHeaders();
        if ( headers == null ) {
            return;
        }

        //  The data types of any headers isn't really known, except strings
        //  should be identifiable. We'll let everything else be a double.
        DefaultValueInfo info = null;
        Double dvalue = null;
        HeaderCard card = null;
        String comment = null;
        String key = null;
        String svalue = null;

        colInfo = new ArrayList();
        row = new ArrayList();

        Iterator it = headers.iterator();
        while ( it.hasNext() ) {
            card = (HeaderCard) (it.next());
            key = card.getKey();
            svalue = card.getValue();
            comment = card.getComment();

            //  A blank keyword is really a comment line, skip those.
            //  Also nothing to be done with valueless keywords.
            if ( "".equals( key ) || "".equals( svalue ) || 
                 key == null || svalue == null ) {
                continue;
            }

            //  Make key conformant.
            key = cleanKey( key );

            if ( card.isStringValue() ) {
                colInfo.add ( new ColumnInfo( key, String.class, comment ) );
                row.add( svalue );
            }
            else {
                try {
                    dvalue = Double.valueOf( svalue );
                    colInfo.add ( new ColumnInfo( key, Double.class, comment ) );
                    row.add( dvalue );
                }
                catch (NumberFormatException e) {
                    //  Just ignore silently. There could be a number of these.
                }
            }
        }
    }

    /**
     * Clean a FITS key of any characters not allowed in an identifier.
     */
    private String cleanKey( String key )
    {
        //  Replace all non-word characters with underscore.
        if ( pattern == null ) {
            pattern = Pattern.compile( "\\W" );
        }
        Matcher matcher = pattern.matcher( key );
        return matcher.replaceAll( "_" );
    }
}
