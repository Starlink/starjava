package uk.ac.starlink.jpcs;

public class ParameterNullException extends ParameterException {

ParameterNullException( String keyword ) {
   super( "Parameter " + keyword + " is Null" );
}

}
