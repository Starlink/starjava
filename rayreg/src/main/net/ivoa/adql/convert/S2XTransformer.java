/*
 * Created by Ray Plante for the National Virtual Observatory
 * c. 2005, 2006
 */
package net.ivoa.adql.convert;

import net.ivoa.util.Configuration;

import javax.xml.transform.Result;
import javax.xml.transform.TransformerException;

/**
 * a general interface for converting ADQL/s to ADQL/x.  The input of the 
 * conversion need not strictly by ADQL/s, but rather just a string rendering,
 * depending on the implementation.
 */
public interface S2XTransformer {

    /**
     * configure this transformer.  This needs to be called once before a 
     * call to transform().  
     */
    public void init(Configuration conf) throws TransformerException;

    /**
     * return the version string for the ADQL standard that this transformer
     * understands and can convert.  This is only guaranteed to be correct
     * after init() is called.
     */
    public String getADQLVersion();

    /**
     * transform the input ADQL/x
     * @param adqls   a string representation of ADQL
     * @param out     the XML result to write into (either a SAXResult, 
     *                  a DOMResult, or a StreamResult).
     * @throws IllegalStateException if the the transformer has not been 
     *   properly configured
     */
    public void transform(String adqls, Result out) 
         throws TransformerException, IllegalStateException;

}
