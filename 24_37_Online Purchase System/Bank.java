

import java.io.*;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.StringTokenizer;

public class Bank {
    public static String workingDirectory = System.getProperty("user.dir");
    public static String databasePath = new String( workingDirectory + "/database.txt");

    public static void main(String[] args) throws Exception {
        int bankPort = Integer.parseInt(args[0]);
        //int bankPort = 3737; // for testing purpose

        ServerSocket serverSocket = null;
        Socket clientSocket = null;
        PrintWriter printWriter = null;
        BufferedReader bufferedReader = null;

        try {
            serverSocket = new ServerSocket(bankPort); // Bank server will run on the given bankPort
            System.out.println("Server Listening......");

        } catch (Exception e) {
            e.printStackTrace();
        }
        while (true) {
            try {
                clientSocket = serverSocket.accept();// It accepts the request from Store Server.
                System.out.println("Successfully connected to " + clientSocket.getInetAddress());
                printWriter = new PrintWriter(clientSocket.getOutputStream(), true);
                bufferedReader = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));

                String userInfo = bufferedReader.readLine().trim();
                System.out.println("UserInfo sent by store server : " + userInfo);
                int productPrice = Integer.parseInt(bufferedReader.readLine());
                System.out.println("productPrice sent by store server : " + productPrice);
                bufferedReader.readLine(); // extra line just for checking

                ArrayList<String> databaseContents = retrieveDatabase(); // Storing the records of the databases into an arraylist
                int userIndex = -1;
                boolean noSuchUser = false, insufficientCredit = false, transactionApproved = false;

		// Checking whether any record matches with the user info sent by the Store program
                for(int i=0;i<databaseContents.size();i++){
                    String content = databaseContents.get(i);
                    if(content.startsWith(userInfo)){
                        userIndex = i;
                    }
                }

                if(userIndex==-1) noSuchUser = true; // No user info matches with the user info sent by the Store program
                else{
                    StringTokenizer stringTokenizer = new StringTokenizer(databaseContents.get(userIndex),"|");
                    String userFirstName = stringTokenizer.nextToken(); // user first name
                    String userFamilyName = stringTokenizer.nextToken(); // user family name
                    String userPostCode = stringTokenizer.nextToken(); // user post code
                    String userCreditCard = stringTokenizer.nextToken(); // user credit card number

                    int userBalance = Integer.parseInt(stringTokenizer.nextToken()); // user balance
                    int userCredit = Integer.parseInt(stringTokenizer.nextToken()); // user credit

                    if(userCredit<productPrice) insufficientCredit = true;
                    else{
                        transactionApproved = true;
                        userBalance += productPrice;
                        userCredit -= productPrice;
                        String userData = userFirstName + "|" + userFamilyName + "|" + userPostCode + "|" + userCreditCard + "|" + userBalance + "|" +userCredit;
                        databaseContents.set(userIndex,userData);
                    }
                }
                String message = null; // response message needed to be delivered to the Store Server Program indicating purchase information
                if(noSuchUser){
                    message = "NO_USER";
                }
                else if(insufficientCredit){
                    message = "INSUFFICIENT_CREDIT";
                }
                else if(transactionApproved){
                    updateDatabase(databaseContents); // updating the database after the transaction
                    message = "TRANSACTION_APPROVED";
                }

                System.out.println(message);
                printWriter.println(message);
                printWriter.flush();

            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                System.err.println("Closing connection with client");
                printWriter.close();
                bufferedReader.close();
                clientSocket.close();
            }
        }
    }


	 //This method reads the whole database file and stores the records of the databases into an arraylist
    public static ArrayList<String> retrieveDatabase() throws IOException{
        ArrayList<String> data = new ArrayList<>();

        File file = new File(databasePath);
        FileReader fileReader = new FileReader(file);
        BufferedReader bufferedReader = new BufferedReader(fileReader);
        String string;
        while((string=bufferedReader.readLine())!=null){
            System.out.println(string);
            data.add(string);
        }
        bufferedReader.close();
        fileReader.close();
        return data;
    }

	// This method updates the database after the transaction
    public  static void updateDatabase(ArrayList<String> data) throws IOException
    {
        File file = new File(databasePath);
        FileWriter fileWriter = new FileWriter(file);
        BufferedWriter bufferedWriter = new BufferedWriter(fileWriter);

        for(int i=0;i<data.size();i++){
            bufferedWriter.write(data.get(i));
            bufferedWriter.newLine();
            bufferedWriter.flush();
        }

        bufferedWriter.close();
        fileWriter.close();
    }
}
