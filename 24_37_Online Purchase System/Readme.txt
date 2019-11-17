
/** Following commands are needed to execute the programs **/

Bank program:
	javac Bank.java
	java Bank Bank_port(Example : java Bank 3737)

Store Program:
	javac Store.java
	java Store Store_port Bank_Host_IP Bank_Port (Example : java Store 3434 localhost 3737)

Brower Program : http://localhost:3434/index.html

** makefile can also be used to compile the programs.
command : make

** We assumed that user will fill up the provided form in the index.html web page correctly.


