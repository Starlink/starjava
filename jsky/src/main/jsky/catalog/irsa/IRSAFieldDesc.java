/*
 * Copyright 2002 Association for Universities for Research in Astronomy, Inc.,
 * Observatory Control System, Gemini Telescopes Project.
 *
 * $Id: IRSAFieldDesc.java,v 1.1 2002/08/04 21:48:50 brighton Exp $
 */

package jsky.catalog.irsa;

import java.net.MalformedURLException;
import java.net.URL;

import jsky.catalog.FieldDescAdapter;
import jsky.catalog.QueryResult;
import jsky.catalog.TableQueryResult;
import jsky.catalog.URLQueryResult;



/**
 * Describes a column in the tabular result of an IRSACatalog query.
 *
 * @version $Revision: 1.1 $
 * @author Allan Brighton
 */
public class IRSAFieldDesc extends FieldDescAdapter {

    // These correspond to the fields in the IRSA data dictionary XML file

    private int _index;             // column index attribute
    private String _colname;        // column name
    private String _desc;           // column description
    private String _units;          // units of column values
    private String _dbtype;         // database data date
    private String _format;         // FORTRAN type format string
    private boolean _nulls;         // true if value can be null
    private boolean _mini;          // true if this col belongs to the "mini" column list
    private boolean _short;         // true if this col belongs to the "short" column list
    private boolean _std;           // true if this col belongs to the "standard" column list


    /** Constructor */
    public IRSAFieldDesc() {
    }

    /** Return true if this field contains a world coordinates RA value. */
    public boolean irsa() {
        return getName().equalsIgnoreCase("ra");
    }

    /** Return true if this field contains a world coordinates Dec value. */
    public boolean isDec() {
        return getName().equalsIgnoreCase("dec");
    }

    public void setMini(boolean b) {_mini = b;}          
    public boolean isMini() {return _mini;}          

    public void setShort(boolean b) {_short = b;}          
    public boolean isShort() {return _short;}         

    public void setStd(boolean b) {_std = b;}          
    public boolean isStd() {return _std;}           
}
