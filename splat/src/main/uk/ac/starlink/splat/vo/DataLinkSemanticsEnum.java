package uk.ac.starlink.splat.vo;

import java.util.HashMap;


/**
 * Enum of possible datalink semantic types recognized by SPLAT.
 * 
 * 
 * @author Margarida Castro Neves
 *
 */

//
// semantics:  http://www.ivoa.net/rdf/datalink/core
//


public enum DataLinkSemanticsEnum  {	
	preview ("#preview"), 
	thisdata ("#this"), 
	auxiliary ("#auxiliary"), 
	proc ("#proc"), 
	cutout ("#cutout"),
	_default ("");

	private final String semanticsString;
	DataLinkSemanticsEnum(String s) {
		semanticsString = s;
	}

	public static DataLinkSemanticsEnum getSemantics(String input) {
		for (DataLinkSemanticsEnum dse : DataLinkSemanticsEnum.values())
			if (dse.semanticsString.equals(input))
				return dse;
		return _default;
	}

};
