// Copyright 2002 Association for Universities for Research in Astronomy, Inc.,
// Observatory Control System, Gemini Telescopes Project.
//
// $Id: IRSACatalogQueryPanel.java,v 1.2 2002/08/20 18:03:25 brighton Exp $

package jsky.catalog.irsa;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.List;

import javax.swing.JComboBox;

import jsky.catalog.Catalog;
import jsky.catalog.FieldDesc;
import jsky.catalog.QueryArgs;
import jsky.catalog.gui.CatalogQueryPanel;
import jsky.util.gui.DialogUtil;


/**
 * Defines the panel used for querying an IRSACatalog. 
 *
 * @author Allan Brighton 
 */
public class IRSACatalogQueryPanel extends CatalogQueryPanel {

    // A reference to the target catalog
    private IRSACatalog _catalog;

    // A description of the catalog table columns, downloaded from the IRSA site as an
    // XML file, needed for the SQL parameter
    private IRSAFieldDesc[] _columns;
    
    // The SQL SELECT statement sent to the server
    private String _sqlString;

    // The parameter description for the select item (contains the option values)
    private FieldDesc _selectParam;
    
    // The Select combo box
    private JComboBox _selectComboBox;


    /**
     * Initialize a query panel for the given catalog.
     * 
     * @param catalog the catalog, for which a user interface component is being generated
     */
    public IRSACatalogQueryPanel(Catalog catalog) {
	super(catalog);

	_catalog = (IRSACatalog)getCatalog();
	
	try {
	    _columns = _catalog.getFieldDesc();
	}
	catch(Exception e) {
	    DialogUtil.error(e);
	}

	// arrange to have the _updateSQLString() method called with the value of the
	// "Select" menu option, whenever it changes.
	_selectParam = _catalog.getParamDesc(IRSACatalog.SELECT);
	_selectComboBox = (JComboBox)getComponentForLabel(IRSACatalog.SELECT);

	_selectComboBox.addActionListener(new ActionListener() {
		public void actionPerformed(ActionEvent e) {
		    _updateSQLString();
		}
	    });
	_updateSQLString();
    }


    /** Return the generated SQL string. */
    public String getSQLString() {
	return _sqlString;
    }

    /** Set the SQL string to use in the catalog query. */
    public void setSQLString(String s) {
	_sqlString = s;
    }

    
    // Update the SQL string based on the currently selected menu item, 
    // s = (Mini, Short, Standard, All)
    private void _updateSQLString() {
	int i = _selectComboBox.getSelectedIndex();
	Integer value = (Integer)_selectParam.getOptionValue(i);
	_sqlString = "select ";
	if (value == IRSACatalog.MINI_COLUMN_LIST) {
	    _sqlString += _getMiniColumnList();
	}
	else if (value == IRSACatalog.SHORT_COLUMN_LIST) {
	    _sqlString += _getShortColumnList();
	}
	else if (value == IRSACatalog.STANDARD_COLUMN_LIST) {
	    _sqlString += _getStandardColumnList();
	}
	else if (value == IRSACatalog.ALL_COLUMNS) {
	    _sqlString += _getAllColumnList();
	}
	else if (value == IRSACatalog.CUSTOM_SQL) {
	    _sqlString += _getCustomColumnList();
	}
	_sqlString += " from " + _catalog.getId();

	//System.out.println("XXX select = " + _sqlString);
    }


    // return a comma separated list of column names in the "mini" column list
    private String _getMiniColumnList() {
	List l = new ArrayList();
	for(int i = 0; i < _columns.length; i++)
	    if (_columns[i].isMini())
		l.add(_columns[i].getName());
	return _getColumnList(l);
    }

    // return a comma separated list of column names in the "short" column list
    private String _getShortColumnList() {
	List l = new ArrayList();
	for(int i = 0; i < _columns.length; i++)
	    if (_columns[i].isShort())
		l.add(_columns[i].getName());
	return _getColumnList(l);
    }

    // return a comma separated list of column names in the "standard" column list
    private String _getStandardColumnList() {
	List l = new ArrayList();
	for(int i = 0; i < _columns.length; i++)
	    if (_columns[i].isStd())
		l.add(_columns[i].getName());
	return _getColumnList(l);
    }

    // return a comma separated list of all column names
    private String _getAllColumnList() {
	List l = new ArrayList();
	for(int i = 0; i < _columns.length; i++)
	    l.add(_columns[i].getName());
	return _getColumnList(l);
    }

    // return a comma separated list of column names selected by the user
    private String _getCustomColumnList() {
	return null; // XXX not impl
    }

    // return a comma separated list of column names from the given list of Strings
    private String _getColumnList(List l) {
	StringBuffer sb = new StringBuffer();
	int n = l.size();
	for(int i = 0; i < n; i++) {
	    sb.append((String)l.get(i));
	    if (i < (n-1))
		sb.append(",");
	}
	return sb.toString();
    }

    /**
     * Return a QueryArgs object based on the current panel settings
     * that can be passed to the Catalog.query() method. 
     * (Redefined from the parent class to handle the extra instrument panels.)
     *
     * @return the QueryArgs object to use for a catalog query.
     */
    public QueryArgs getQueryArgs()  {
	IRSAQueryArgs queryArgs = new IRSAQueryArgs(_catalog);
	initQueryArgs(queryArgs);
	return queryArgs;
    }

    /**
     * Initialize a QueryArgs object based on the current panel settings
     * so that can be passed to the Catalog.query() method. 
     */
    public void initQueryArgs(QueryArgs queryArgs)  {
	super.initQueryArgs(queryArgs);
	((IRSAQueryArgs)queryArgs).setSQLString(getSQLString());
    }
}
