#!/home/perl/bin/perl

  use SOAP::Lite;
  use Getopt::Long;

  unless ( scalar @ARGV >= 1 ) {
     die "USAGE: $0 [-host host] [-port port] -file filename\n";
  }
    
  my $status = GetOptions( "host=s"       => \$host,
                           "port=s"       => \$port,
                           "file=s"       => \$file );
  
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
  
  my $data;
  if( defined $file ) {
     unless ( open ( FILE, "<$file") ) {
        die "ERROR: Cannot open $file\n";
     }
     undef $/;
     $data = <FILE>;
     close FILE;
  } else {
     die "ERROR: No data file specified.\n";
  }
  
  my $service = SOAP::Lite->service(
      "http://$host:$port/services/FrogSOAPServices?wsdl" );
    
  print $service->displaySeries( $data ) . "\n";    


             
             
             
             
