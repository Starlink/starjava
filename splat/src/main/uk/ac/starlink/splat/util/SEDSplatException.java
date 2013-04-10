package uk.ac.starlink.splat.util;

import java.lang.Exception;
import java.awt.Component;
import javax.swing.JOptionPane;

/**
 *  Exception thrown when it is suspected that a table contains an SED, not
 *  just a single spectrum.
 */
public class SEDSplatException
    extends SplatException
{
    /** The number of rows in the table (should be number of spectra) */
    private int rows = 0;
    private int type = 0;
    private String spec=null;

    public SEDSplatException( int rows )
    {
        super();
        this.rows = rows;
    }
    
    
  
    
    public SEDSplatException( int rows, String message, Throwable cause )
    {
        super( message, cause );
        this.rows = rows;
    }

    public SEDSplatException( int rows, Throwable cause )
    {
        super( cause );
        this.rows = rows;
    }

    public SEDSplatException ( int rows, String message )
    {
        super( message );
        this.rows = rows;
    }

    /**
     * Get the number of rows in the suspected SED table.
     */
    public int getRows()
    {
        return rows;
    }
    
    public int getType() 
    {
        return type;
    }
    
    public void setType(int t) {
        this.type=t;
    }
    
    public void setSpec(String spec) {
        this.spec=spec;
    }
    
    public String getSpec() {
        return this.spec;
    }
    
}
