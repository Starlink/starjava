// Copyright 2002 Association for Universities for Research in Astronomy, Inc.,
// Observatory Control System, Gemini Telescopes Project.
//
// $Id: IRSAQueryArgs.java,v 1.1 2002/08/04 21:48:50 brighton Exp $

package jsky.catalog.irsa;

import java.util.Vector;

import jsky.catalog.BasicQueryArgs;
import jsky.catalog.FieldDesc;
import jsky.catalog.FieldDescAdapter;
import jsky.catalog.SearchCondition;
import jsky.catalog.ValueRange;


/**
 * Adds an SQL select statement to the basic query arguments.
 *
 * @author Allan Brighton 
 */
public final class IRSAQueryArgs extends BasicQueryArgs {

    private String _sqlString;

    /**
     * Constructor
     */
    public IRSAQueryArgs(IRSACatalog catalog) {
	super(catalog);
    }

    public String getSQLString() {return _sqlString;}
    public void setSQLString(String s) {_sqlString = s;}
}

