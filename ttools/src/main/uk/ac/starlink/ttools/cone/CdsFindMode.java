package uk.ac.starlink.ttools.cone;

/**
 * Enumeration of ways to run an upload match using the CDS Xmatch service.
 *
 * @author   Mark Taylor
 * @since    14 May 2014
 */
public enum CdsFindMode {
            
    /** All matches. */
    ALL( "all", "All matches", "all", true ),

    /** Best CDS table match for each uploaded table row. */
    BEST0( "best", "Best match in remote table for each local table row",
           "best", true ),
    
    /** Best uploaded match for each CDS table row. */
    BEST1( "best-remote", "Best match in local table for each remote table row",
           "best", false );
        
    private final String name_;
    private final String summary_;
    private final String selectionValue_;
    private final boolean uploadFirst_;
    
    /**
     * Constructor.
     *
     * @param  name  mode name
     * @param  summary  mode summary
     * @param  selectionValue  value of service "selection" parameter
     * @param  uploadFirst  whether uploaded table is table 1
     */
    CdsFindMode( String name, String summary,
                 String selectionValue, boolean uploadFirst ) { 
        name_ = name;
        summary_ = summary;
        selectionValue_ = selectionValue;
        uploadFirst_ = uploadFirst;
    }

    /**
     * Returns the one-word name for this mode.
     *
     * @return  mode name
     */
    public String getName() {
        return name_;
    }

    /**
     * Returns a short text summary of the meaning of this mode.
     *
     * @return  mode summary
     */
    public String getSummary() {
        return summary_;
    }

    /**
     * Returns the value of the CDS Xmatch service "<code>selection<code>"
     * parameter to use for this mode.
     *
     * @return  "best" or "all"
     */
    public String getSelectionValue() {
        return selectionValue_;
    }

    /**
     * Indicates the assignment of the uploaded and remote tables as
     * "first" or "second", for the purpose of setting Xmatch service
     * parameters
     * (<code>cat1</code>/</code>2</code>,
     *  <code>colDec1</code>/</code>2</code> etc).
     *
     * @return  if true, uploaded table is table 1 and remote is table 2;
     *          if false, it's the other way round
     */
    public boolean isUploadFirst() {
        return uploadFirst_;
    }

    @Override
    public String toString() {
        return name_;
    }    
}
