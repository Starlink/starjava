package uk.ac.starlink.jpcs;

public class ParameterAbortException extends ParameterException {

ParameterAbortException( String keyword ) {
   super( "Parameter " + keyword + " is 'abort'" );
}

}
