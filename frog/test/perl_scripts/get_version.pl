#!/home/perl/bin/perl

  use SOAP::Lite;

  my $service = SOAP::Lite->service(
      "http://dastardly.astro.ex.ac.uk:8084/services/Version?wsdl" );
    
  print $service->getVersion() . "\n";    

