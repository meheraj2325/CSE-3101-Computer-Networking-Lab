
import java.net.InetAddress;
import java.net.Socket;
import java.net.ServerSocket;
import java.io.*;
import java.util.Date;
import java.util.StringTokenizer;

public class Store {

    public static void main(String[] args) throws Exception {

        int storePort = Integer.parseInt(args[0]); // Store_PORT
        // int storePort = 3434;  // for testing purpose
        InetAddress bankHostIp = InetAddress.getByName(args[1]); // BANK_HOST_IP
        // InetAddress bankHostIp = InetAddress.getByName("localhost");  // for testing purpose
        int bankPort = Integer.parseInt(args[2]); // Bank_PORT
        // int bankPort = 3737; // for testing purpose

        ServerSocket serverSocket = null;
        Socket clientSocket = null;
        PrintWriter printWriter = null;
        BufferedReader bufferedReader = null;
        BufferedOutputStream bufferedOutputStream = null;

        try {
            serverSocket = new ServerSocket(storePort); // Store server will run on the given storePort
            System.out.println("Server Listening......");
        } catch (Exception e) {
            e.printStackTrace();
        }

        while (true) {
            try {
                clientSocket = serverSocket.accept(); // It accepts the request from client browser.
                System.out.println("Successfully connected to " + clientSocket.getInetAddress());
                printWriter = new PrintWriter(clientSocket.getOutputStream(), true); 
                bufferedReader = new BufferedReader(new InputStreamReader(clientSocket.getInputStream())); 
                bufferedOutputStream = new BufferedOutputStream(clientSocket.getOutputStream());

                String requestHeaderFirstLine = bufferedReader.readLine(); // Takes the first line the of the request message header

                System.out.println("Request Message : ");
                System.out.println("----------------------------------------------");
                System.out.println("requestHeaderFirstLine = " + requestHeaderFirstLine); // prints the first line the of the request message header

                String string;
                int contentLength = 0;
		// Now reading the whole request message header here
                while ((string = bufferedReader.readLine())!=null) {
                    if(string.equals("")) break;
                    if(string.startsWith("Content-Length:")){
                        String parts[] = string.split(" ");
                        contentLength = Integer.valueOf(parts[1]);//takes the value of the content-length to get the content sent by the POST method
                        System.out.println("Found content-length = " + contentLength);
                    }
                    System.out.println(string);
                }
                System.out.println(string);
                System.out.println("---------------------------------------------");

                if (requestHeaderFirstLine != null) {
                    if (requestHeaderFirstLine.toUpperCase().startsWith("GET")) {
						// The functionality of HTTP GET request message is performed on this section of the code.
                        httpGetMethodFunctionality(requestHeaderFirstLine, bufferedReader, printWriter, bufferedOutputStream);
                    } else if (requestHeaderFirstLine.toUpperCase().startsWith("POST")) {
						// The functionality of HTTP POST request message is performed on this section of the code.
                        httpPostMethodFunctionality(bankHostIp,bankPort,requestHeaderFirstLine,contentLength,bufferedReader, printWriter,bufferedOutputStream);
                    } else {
                        System.out.println("501 Not Implemented.");
                    }
                }

            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                System.err.println("Closing connection with client");
                printWriter.close();
                bufferedOutputStream.close();
                bufferedReader.close();
                clientSocket.close();
            }
        }

    }


		// This method handles the HTTP GET requests
    public static void httpGetMethodFunctionality(String firstLine, BufferedReader bufferedReader, PrintWriter printWriter, BufferedOutputStream bufferedOutputStream) {
        StringTokenizer stringTokenizer = new StringTokenizer(firstLine, " "); // Splits the request header first line into many tokens
        String methodName = stringTokenizer.nextToken(); // Gets the method name
        String workingDirectory = System.getProperty("user.dir"); // Gets the location of the directory of Store.java program 
	    String requestedWebPage = stringTokenizer.nextToken(); // Gets the name of the requested web page/html file  
	    if(requestedWebPage.equals("/"))  requestedWebPage = "/index.html"; 
		String fileAddress = workingDirectory + requestedWebPage; // Generates the location of the requested html file
        String httpVersion = stringTokenizer.nextToken(); // Gets the httpVersion

        try {
		// Sending the requested html file with proper response message
                sendHtmlMessage(fileAddress,httpVersion,printWriter,bufferedOutputStream); 
        } catch (IOException e) {
            System.out.println(e.getMessage());
        }
    }


	// This method handles the HTTP POST requests
    public static void httpPostMethodFunctionality(InetAddress bankHostIP,int bankPort,String firstLine,int contentLength, BufferedReader bufferedReader, PrintWriter printWriter,BufferedOutputStream bufferedOutputStream) throws IOException{
        System.out.println("Inside Post method");
        System.out.println(firstLine); 
        char[] postMethodReceivedData = new char[contentLength+5];
        String postMethodReceivedDataString = "";
        try{
			// Reading the contents received from POST request message 
            for(int i=0;i<contentLength;i++){
                postMethodReceivedData[i] = (char) bufferedReader.read();
            }

            postMethodReceivedDataString = new String(postMethodReceivedData);
            System.out.println("Received Data : " + postMethodReceivedDataString);
        }catch(IOException e){
            e.printStackTrace();
        }

        int itemQuantity = 0, itemNumber = 0;
        String userInfo = ""; // This strig will concatenate all the user informations recieved from POST message

        try{
        	StringTokenizer stringTokenizer = new StringTokenizer(postMethodReceivedDataString,"&="); // Splits the recieved POST request messsage line into many tokes
		    System.out.println(stringTokenizer.nextToken()); // Html Input name = itemNumber
		    itemNumber = Integer.parseInt(stringTokenizer.nextToken()); // itemNumber value
		    System.out.println("itemNumber : " + itemNumber);

		    System.out.println(stringTokenizer.nextToken()); // Html input name = quantity
		    itemQuantity = Integer.parseInt(stringTokenizer.nextToken()); // quantity value
		    System.out.println("itemQuantity : " + itemQuantity);

		    System.out.println(stringTokenizer.nextToken()); // Html input name = firstName
		    userInfo += stringTokenizer.nextToken(); // firstName value
		    System.out.println("userInfo : " + userInfo);

		    System.out.println(stringTokenizer.nextToken()); // Html input name = familyName
		    userInfo += "|";
		    userInfo += stringTokenizer.nextToken(); // familyName value
		    System.out.println("userInfo : " + userInfo);

		    System.out.println(stringTokenizer.nextToken()); // Html input name = postCode
		    userInfo += "|";
		    userInfo += stringTokenizer.nextToken(); // postCode value
		    System.out.println("userInfo : " + userInfo);

		    System.out.println(stringTokenizer.nextToken()); // Html input name = creditCard
		    userInfo += "|";
		    userInfo += stringTokenizer.nextToken(); // creditCard value
		    System.out.println("userInfo : " + userInfo);
		    
        }catch(RuntimeException e){
        	e.printStackTrace();
        }

        // To get the required credit for user's desired transaction
        int productPrice = getProductPrice(itemQuantity,itemNumber);

        // To connect to the bank server
        // and to send the userInfo and required credit for the transaction
        // and to get the response message from bank server
        String responseMessage = connectToBankServer(bankHostIP,bankPort,userInfo,productPrice);

        if(responseMessage.equalsIgnoreCase("NO_USER")){
            String workingDirectory = System.getProperty("user.dir");
            String fileAddress = workingDirectory + "/no_user.html";
            String httpVersion = "HTTP/1.1";
			
			// Sending appropiate response message containing the status of the transaction
            sendHtmlMessage(fileAddress,httpVersion,printWriter,bufferedOutputStream);
        }
        else if(responseMessage.equalsIgnoreCase("INSUFFICIENT_CREDIT")){
            String workingDirectory = System.getProperty("user.dir");
            String fileAddress = workingDirectory + "/insufficient_credit.html";
            String httpVersion = "HTTP/1.1";

			// Sending appropiate response message containing the status of the transaction
            sendHtmlMessage(fileAddress,httpVersion,printWriter,bufferedOutputStream);
        }
        else if(responseMessage.equalsIgnoreCase("TRANSACTION_APPROVED")){
            String workingDirectory = System.getProperty("user.dir");
            String fileAddress = workingDirectory + "/transaction_approved.html";
            String httpVersion = "HTTP/1.1";

			// Sending appropiate response message containing the status of the transaction
            sendHtmlMessage(fileAddress,httpVersion,printWriter,bufferedOutputStream);
        }
    }


	// This method sends the requested html file with proper response message
    public static void sendHtmlMessage(String fileAddress,String httpVersion,PrintWriter printWriter, BufferedOutputStream bufferedOutputStream) throws IOException{
        File baseHtmlFile = new File(fileAddress); 
        StringBuffer stringBuffer = new StringBuffer();
        stringBuffer.append(httpVersion);

        if (baseHtmlFile.exists()) {
            int fileLength = (int) baseHtmlFile.length();
	    
	        // creating response message
            stringBuffer.append(" 200 OK\n");
            stringBuffer.append("Connection : close\n");
            stringBuffer.append("Date: " + new Date().toString() + '\n');
            stringBuffer.append("Server: Custom Java HTTP Server\n");
            stringBuffer.append("Content-Length: " + fileLength + "\n");
            stringBuffer.append("Content-Type: text/html\n\r");
            printWriter.println(stringBuffer.toString()); // Sending the response message header to the browser
            printWriter.println();
            printWriter.flush();

            System.out.println("Response Message : ");
            System.out.println("~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~");
            System.out.println(stringBuffer.toString());
            System.out.println("~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~");

            byte[] fileData = readHtmlFileData(baseHtmlFile, fileLength); //Reading the html file data from the file location
            bufferedOutputStream.write(fileData, 0, fileLength); // Sending the requested html file data to the browser
            bufferedOutputStream.flush();

        } else {
            int fileLength = (int) baseHtmlFile.length();

            stringBuffer.append(" 404 Not Found\n");
            System.out.println("404 not found");
            stringBuffer.append("Connection : close\n");
            stringBuffer.append("Date: " + new Date().toString() + '\n');
            stringBuffer.append("Server: Custom Java HTTP Server\n");
            stringBuffer.append("Content-Length: " + fileLength + "\n");
            stringBuffer.append("Content-Type: text/html\n\r");
            printWriter.println(stringBuffer.toString());
            printWriter.println();
            printWriter.flush();
        }
    }
	

	// This method reads the html file data from the specified file location
    public static byte[] readHtmlFileData(File file, int fileLength) throws IOException {
        FileInputStream fileInputStream = null;
        BufferedInputStream bufferedInputStream = null;
        byte[] fileData = new byte[fileLength];

        try {
            fileInputStream = new FileInputStream(file);
            bufferedInputStream = new BufferedInputStream(fileInputStream);
            bufferedInputStream.read(fileData);
        } finally {
            bufferedInputStream.close();
            fileInputStream.close();
        }

        return fileData;
    }

	// This method will establish a TCP connection with bank server and return a response message about the transaction
    public static String connectToBankServer(InetAddress bankHostIp,int bankPort,String userInfo,int productPrice) throws IOException {
        Socket storeClientSocket = null;
        PrintWriter storeClientPrintWiter = null;
        BufferedReader storeClientBuffredReader = null;
        String responseMessage = null;

        try{
            storeClientSocket = new Socket(bankHostIp, bankPort);
            storeClientPrintWiter = new PrintWriter(storeClientSocket.getOutputStream(), true);
            storeClientBuffredReader = new BufferedReader(new InputStreamReader(storeClientSocket.getInputStream()));

            storeClientPrintWiter.println(userInfo); // Sending the userinfo to the bank server
            storeClientPrintWiter.println(productPrice); // Sending the productPrice to the bank server
            storeClientPrintWiter.println("$$$$$");

            responseMessage = storeClientBuffredReader.readLine(); // Reading the response message containing the status of the transaction received from BAnk server 
        }catch (IOException e){
            e.printStackTrace();
        }
        finally {
            storeClientBuffredReader.close();
            storeClientPrintWiter.close();
            storeClientSocket.close();
        }
        return responseMessage;
    }


	// This method will return the product price
    public static int getProductPrice(int quantity, int itemNumber){
        int totalPrice = 0;
        if(itemNumber==1 || itemNumber==2) totalPrice = 120 * quantity ;
        else if(itemNumber==3) totalPrice = 1100 * quantity ;
        else if(itemNumber==4) totalPrice = 900  * quantity ;
        else if(itemNumber==5) totalPrice = 800  * quantity ;
        else if(itemNumber==6) totalPrice = 1300 * quantity ;
        return totalPrice;
    }
}
