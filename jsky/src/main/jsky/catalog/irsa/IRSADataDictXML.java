// Copyright 2002
// Association for Universities for Research in Astronomy, Inc.,
// Observatory Control System, Gemini Telescopes Project.
//
// $Id: IRSADataDictXML.java,v 1.2 2002/08/20 09:57:58 brighton Exp $

package jsky.catalog.irsa;

import java.io.File;
import java.lang.reflect.InvocationTargetException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Vector;

import jsky.catalog.CatalogDirectory;
import jsky.catalog.TablePlotSymbol;
import jsky.util.NameValue;
import jsky.util.Resources;
import jsky.util.SaxParserUtil;
import jsky.util.TclUtil;

import org.xml.sax.Attributes;


/**
 * Parses an IRSA Data Dictionary XML file and saves the table column 
 * definitions found there.
 * See http://irsa.ipac.caltech.edu/
 *
 * @version $Revision: 1.2 $
 * @author Allan Brighton
 */
public class IRSADataDictXML extends SaxParserUtil {

    // list of IRSAFieldDesc objects, one for each table column described in the XML file
    private List _columns;

    // These correspond to the fields in the XML file
    private IRSAFieldDesc _column;  // the current table column description


    /**
     * Default constructor. Call parse(urlStr) to do the actual parsing.
     */
    public IRSADataDictXML() {
    }

    /** Return an array IRSAFieldDesc definitions found in the XML file after parsing */
    public IRSAFieldDesc[] getColumns() {
	IRSAFieldDesc[] ar = new IRSAFieldDesc[_columns.size()];
	_columns.toArray(ar);
	return ar;
    }


    
    // -- these methods are called by reflection from the base class --
    //    (which is why they must be declared public) 
    
    
    // called for the <DataDictionary> start tag
    public void _DataDictionaryStart(Attributes attrs) {
        _columns = new Vector();
    }

    // called for the </DataDictionary> end tag
    public void _DataDictionaryEnd() {
    }


    // called for the <catalog> start tag
    public void _columnStart(Attributes attrs) {
	_column = new IRSAFieldDesc();
    }

    // called for the </catalog> end tag
    public void _columnEnd() {
	_columns.add(_column);
    }



    // called for the <cntr> start tag
    public void _cntrStart(Attributes attrs) {
    }
    // called for the </cntr> end tag
    public void _cntrEnd() {
    }



    // called for the <colname> start tag
    public void _colnameStart(Attributes attrs) {
    }
    // called for the </colname> end tag
    public void _colnameEnd() {
	String name = getCData();
	_column.setName(name);
	if (name.equalsIgnoreCase("ra"))
	    _column.setIsRA(true);
	else if (name.equalsIgnoreCase("dec"))
	    _column.setIsDec(true);
    }



    // called for the <desc> start tag
    public void _descStart(Attributes attrs) {
    }
    // called for the </desc> end tag
    public void _descEnd() {
	_column.setDescription(getCData());
    }




    // called for the <units> start tag
    public void _unitsStart(Attributes attrs) {
    }
    // called for the </units> end tag
    public void _unitsEnd() {
	_column.setUnits(getCData());
    }




    // called for the <dbtype> start tag
    public void _dbtypeStart(Attributes attrs) {
    }
    // called for the </dbtype> end tag
    public void _dbtypeEnd() {
    }




    // called for the <format> start tag
    public void _formatStart(Attributes attrs) {
    }
    // called for the </format> end tag
    public void _formatEnd() {
	_column.setFormat(getCData());
    }




    // called for the <nulls> start tag
    public void _nullsStart(Attributes attrs) {
    }
    // called for the </nulls> end tag
    public void _nullsEnd() {
    }




    // called for the <indx> start tag
    public void _indxStart(Attributes attrs) {
    }
    // called for the </indx> end tag
    public void _indxEnd() {
    }




    // called for the <mini> start tag
    public void _miniStart(Attributes attrs) {
    }
    // called for the </mini> end tag
    public void _miniEnd() {
	_column.setMini(getCData().equals("y"));
    }




    // called for the <short> start tag
    public void _shortStart(Attributes attrs) {
    }
    // called for the </short> end tag
    public void _shortEnd() {
	_column.setShort(getCData().equals("y"));
    }




    // called for the <std> start tag
    public void _stdStart(Attributes attrs) {
    }
    // called for the </std> end tag
    public void _stdEnd() {
	_column.setStd(getCData().equals("y"));
    }




    // called for the <nnulls> start tag
    public void _nnullsStart(Attributes attrs) {
    }
    // called for the </nnulls> end tag
    public void _nnullsEnd() {
    }




    // called for the <count> start tag
    public void _countStart(Attributes attrs) {
    }
    // called for the </count> end tag
    public void _countEnd() {
    }



    // called for the <maximum> start tag
    public void _maximumStart(Attributes attrs) {
    }
    // called for the </maximum> end tag
    public void _maximumEnd() {
    }




    // called for the <minimum> start tag
    public void _minimumStart(Attributes attrs) {
    }
    // called for the </minimum> end tag
    public void _minimumEnd() {
    }



    // called for the <mean> start tag
    public void _meanStart(Attributes attrs) {
    }
    // called for the </mean> end tag
    public void _meanEnd() {
    }



    // called for the <ERROR> start tag
    public void _ERRORStart(Attributes attrs) {
    }
    // called for the </ERROR> end tag
    public void _ERROREnd() {
	throw new RuntimeException(getCData() + ": while reading : " + getURL());
    }



    /**
     * Test cases
     */
    public static void main(String[] args) {
        try {
            URL url = new URL("file:test/npd-dd.xml");
            IRSADataDictXML irsaDDXML = new IRSADataDictXML();
            irsaDDXML.parse(url);
	    System.out.println("Parsed test/npd-dd.xml and found " + irsaDDXML.getColumns().length + " column descriptions");
        }
        catch (Exception e) {
            e.printStackTrace();
            System.exit(1);
        }
	System.out.println("Test passed");
    }
}
