
#  Send a message to the EchoServer application. Should respond with 
#  "tomato".

export CLASSPATH=".:../../build/classes:`../../../../lib/jetty/echo_classpath`"
export CLASSPATH="`../../../../lib/axis/echo_classpath`:${CLASSPATH}"

javac EchoClient.java

java EchoClient "tomato"
