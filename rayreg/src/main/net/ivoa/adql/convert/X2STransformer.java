/*
 * Created by Ray Plante for the National Virtual Observatory
 * c. 2005, 2006
 */
package net.ivoa.adql.convert;

import net.ivoa.util.Configuration;

import javax.xml.transform.Source;
import javax.xml.transform.TransformerException;

/**
 * a general interface for converting ADQL/x to ADQL/s.  The output of the 
 * conversion need not strictly by ADQL/s, but rather just a string rendering,
 * depending on the implementation.
 */
public interface X2STransformer {

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
     * @throws IllegalStateException if the the transformer has not been 
     *   properly configured
     */
    public String transform(Source adqlx) 
         throws TransformerException, IllegalStateException;

}
