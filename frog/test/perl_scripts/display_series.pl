#!/home/perl/bin/perl

  use SOAP::Lite;

  my $service = SOAP::Lite->service(
      "http://dastardly.astro.ex.ac.uk:8084/services/FrogSOAPServices?wsdl" );
    
  print $service->displaySeries( "0.0 23.0 0.1\n0.1 21.0 0.2\n" ) . "\n";    

