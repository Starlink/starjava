// Copyright (C) 2002 Central Laboratory of the Research Councils

package uk.ac.starlink.soap;

import org.mortbay.util.LogSink;
import org.mortbay.util.Frame;

/**
 * A LogSink for embedded applications. This throws all messages away.
 *
 * @author Peter W. Draper
 * @version $Id$
 * @since 24-MAY-2002
 */
public class AppLogSink
    implements LogSink
{
    /** 
     * Constructor.
     */
    public AppLogSink()
    {
        //  Does nothing.
    }
        
    public void setOptions(String logOptions)
    {  
        //  Does nothing.
    }
    public String getOptions()
    {
        return "";
    }
    
    public void log(String tag, Object msg, Frame frame, long time )
    {
        // Does nothing.
    }
    
    public synchronized void log(String formattedLog)
    {
        // Does nothing.
    }

    //
    // LifeCycle.
    //
    public void start() 
    {
        // Does nothing.
    }

    public void stop() 
    {
        // Does nothing.
    }

    public boolean isStarted()
    {
        return true;
    }
}




