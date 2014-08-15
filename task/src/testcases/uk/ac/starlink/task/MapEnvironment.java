package uk.ac.starlink.task;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.util.Map;

public class MapEnvironment implements Environment {

    private final Map<String,String> map_;
    private ByteArrayOutputStream out_ = new ByteArrayOutputStream();
    private PrintStream pout_ = new PrintStream( out_ );

    public MapEnvironment( Map<String,String> map ) {
        map_ = map;
    }

    public void clearValue( Parameter par ) {
        map_.remove( par.getName() );
    }

    public void acquireValue( Parameter par ) throws TaskException {
        par.setValueFromString( this, map_.get( par.getName() ) );
    }

    public String[] getNames() {
        return map_.keySet().toArray( new String[ 0 ] );
    }

    public PrintStream getOutputStream() {
        return pout_;
    }

    public PrintStream getErrorStream() {
        return System.err;
    }

    public String getOutputText() {
        pout_.flush();
        return new String( out_.toByteArray() );
    }
}
