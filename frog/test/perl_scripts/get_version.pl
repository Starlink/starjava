#!/home/perl/bin/perl

  use SOAP::Lite;
  use Getopt::Long;
    
  my $status = GetOptions( "host=s" => \$host, "port=s" => \$port );
  
  # default hostname
  unless ( defined $host ) {
     # localhost.localdoamin
     $host = "127.0.0.1";
  } 

  # default port
  unless( defined $port ) {
     # default port for the user agent
     $port = 8084;   
  } 
  
  my $service = SOAP::Lite->service(
      "http://$host:$port/services/Version?wsdl" );
    
  print $service->getVersion() . "\n";    

