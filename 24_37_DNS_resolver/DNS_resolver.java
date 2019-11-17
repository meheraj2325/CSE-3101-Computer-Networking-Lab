import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.util.ArrayList;

public class DNS_resolver{
    private static String[] dnsRootServerAddress;
    private static final int dnsPort = 53;
    private static String aliasHostName = "www.google.com";
    static boolean ipFlag = false,cnameFlag=false,soaFlag=false;
    //ipFlag -> true, when type A record found
    //cnameFlag -> true, when canonical name found for a domain name
    //soaFlag -> true, when SOA type record responded by the server
    static String cnameForAliasHostName = ""; //cnameForAliasHostName contains canonical name for a domain name
    static ArrayList<String> ipArrayList = new ArrayList<>(); // list to record all the A type values
    static ArrayList<String> extraArrayList = new ArrayList<>();

    public static void main(String[] args) throws IOException {
        initRootServers(); // configuring the ip addresses of the 13 root DNS servers
        if(args.length==0){
	    System.out.println("Please run the program using following command : java DNS_resolver hostname");
	    return;
	}
	aliasHostName = args[0];
        DatagramSocket socket = new DatagramSocket();
        // dnsRootServerAddress[8] = "192.36.148.17"
        dnsRequestSender(socket, dnsRootServerAddress[8], aliasHostName);
        DataInputStream dataInputStream = getResponsePacket(socket);
        printResponsePacket(dataInputStream); /* This function prints the dns header and answer RRs,
        authority RRs, additional RRs from response packet */

        //Function printResponsePacket finds TLD server

        if(soaFlag){
            System.out.println(aliasHostName + " does not exist");
            return;
        }
        else if(ipFlag) {
            ipFlag = false;
            extraArrayList.clear();
            // copying the all A type values from ipArrayList to extraArrayList
            for(int i=0;i<ipArrayList.size();i++){
                extraArrayList.add(ipArrayList.get(i));
            }
            ipArrayList.clear();

            for(int j=0;j<extraArrayList.size();j++) {
                dnsRequestSender(socket, extraArrayList.get(j), aliasHostName);
                dataInputStream = getResponsePacket(socket);
                printResponsePacket(dataInputStream); /* This function prints the dns header and answer RRs,
                authority RRs, additional RRs from response packet */
                if(ipFlag) break;
            }
        }
        else {
            if(cnameFlag){
                while(true){  // finds A type record following the canonical name chain
                    if(cnameFlag){
                        cnameFlag=false;
                        dnsRequestSender(socket, cnameForAliasHostName, aliasHostName);
                        dataInputStream = getResponsePacket(socket);
                        printResponsePacket(dataInputStream);
                    }
                    else if(ipFlag) {
                        if (ipArrayList.size() != 0) System.out.println( aliasHostName + " = " + ipArrayList.get(0));
                        else System.out.println("DNS Error Occurred");
                        return ;
                    }
                    else break;

                }
            }
            else System.out.println("DNS Error Occurred");
            return ;
        }

        // Above portion finds Authoritative Servers from TLD server

        if(soaFlag){
            System.out.println(aliasHostName + " does not exist");
            return;
        }
        if(ipFlag) {
            ipFlag = false;
            extraArrayList.clear();
            // copying the all A type values from ipArrayList to extraArrayList
            for(int i=0;i<ipArrayList.size();i++){
                extraArrayList.add(ipArrayList.get(i));
            }
            ipArrayList.clear();
            for(int j=0;j<extraArrayList.size();j++) {
                dnsRequestSender(socket, extraArrayList.get(j), aliasHostName);
                dataInputStream = getResponsePacket(socket);
                boolean isSOAFlagTrue = printResponsePacket2(dataInputStream);
                if(isSOAFlagTrue) return;
                if(ipFlag) break;
            }
        }
        else{
            if(cnameFlag){
                while(true){ // finds A type record following the canonical name chain
                    if(cnameFlag){
                        cnameFlag=false;
                        dnsRequestSender(socket, cnameForAliasHostName, aliasHostName);
                        dataInputStream = getResponsePacket(socket);
                        boolean isSOAFlagTrue = printResponsePacket2(dataInputStream);
                        if(isSOAFlagTrue) return;
                    }
                    else if(ipFlag) {
                        if (ipArrayList.size() != 0) System.out.println( aliasHostName + " = " + ipArrayList.get(0));
                        else System.out.println("DNS Error Occurred");
                        return ;
                    }
                    else break;

                }
            }
            else System.out.println("DNS Error Occurred");
            return ;
        }
        if(ipArrayList.size()!=0)
            System.out.println(aliasHostName+" = "+ipArrayList.get(0));
        else
            System.out.println("DNS Error Occurred");

        // Above portion resolves ip address for aliasHostName
    }

    private static void dnsRequestSender(DatagramSocket socket, String DNSAddress, String aliasHostName) throws IOException {

        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        DataOutputStream dataOutputStream = new DataOutputStream(byteArrayOutputStream);

        /* *** Build a DNS Request Frame *** */
        dataOutputStream.writeShort(0x1234);			// Identifier
        dataOutputStream.writeShort(0x0000);			// Write Query ipFlags
        dataOutputStream.writeShort(0x0001);			// Question Count
        dataOutputStream.writeShort(0x0000);			// Number of Answer Records
        dataOutputStream.writeShort(0x0000);			// Number of Authority Records
        dataOutputStream.writeShort(0x0000);			// Number of Additional Records

        String[] aliasHostNameParts = aliasHostName.split("\\.");
        for (int i = 0; i < aliasHostNameParts.length; i++) {
            byte[] aliasHostNameBytes = aliasHostNameParts[i].getBytes("UTF-8");
            dataOutputStream.writeByte(aliasHostNameBytes.length);
            dataOutputStream.write(aliasHostNameBytes);
        }

        dataOutputStream.writeByte(0x00);			// Zero byte to end the header

        dataOutputStream.writeShort(0x0001);			// Write Type 0x01 = A
        dataOutputStream.writeShort(0x0001);			// Write Class 0x01 = IN

        byte[] dnsFrameByteArray = byteArrayOutputStream.toByteArray();

        /* *** Send DNS Request Frame *** */
        DatagramPacket datagramPacket = new DatagramPacket(dnsFrameByteArray, dnsFrameByteArray.length,
                InetAddress.getByName(DNSAddress), dnsPort);
        socket.send(datagramPacket);
    }

    private static DataInputStream getResponsePacket(DatagramSocket socket) throws IOException {
        byte[] bufferPacketByteArray = new byte[1024];
        DatagramPacket packet = new DatagramPacket(bufferPacketByteArray, bufferPacketByteArray.length);
        socket.receive(packet);

        ByteArrayInputStream newByteArrayInputStream = new ByteArrayInputStream(bufferPacketByteArray);
        DataInputStream newDataInputStream = new DataInputStream(newByteArrayInputStream);

        return newDataInputStream;
    }

    private static int[] dnsResponseHeaderPrint(DataInputStream dataInputStream) throws IOException {
        // System.out.println("Transaction ID: 0x" + String.format("%04x",  dataInputStream.readShort()));
        dataInputStream.readShort();
        // System.out.println("ipFlags: 0x" + String.format("%04x", dataInputStream.readShort()));
        dataInputStream.readShort();
        // System.out.println("Questions: 0x" + String.format("%04x", dataInputStream.readShort()));
        dataInputStream.readShort();
        short answerRR = dataInputStream.readShort();
        // System.out.println("Answers RRs: 0x" + String.format("%04x", answerRR));
        short authorityRR = dataInputStream.readShort();
        //  System.out.println("Authority RRs: 0x" + String.format("%04x", authorityRR));
        short additionalRR = dataInputStream.readShort();
        //  System.out.println("Additional RRs: 0x" + String.format("%04x", additionalRR));

        String responseAliasHostName = getAliasHostName(dataInputStream);
        //  System.out.println("aliasHostName: " + responseAliasHostName);
        dataInputStream.readInt();

        int[] responses = new int[3];
        responses[0] = (int)answerRR;
        responses[1] = (int)authorityRR;
        responses[2] = (int)additionalRR;
        return responses;
    }

    public static String getAliasHostName(DataInputStream dataInputStream) throws IOException {
        int aliasHostNamePartsLength;
        StringBuffer stringBuffer = new StringBuffer();
        while ( (aliasHostNamePartsLength = dataInputStream.readByte()) > 0 ) { byte[] aliasHostNameBytes = new byte[aliasHostNamePartsLength];
            for (int i=0; i<aliasHostNamePartsLength; i++) aliasHostNameBytes[i] = dataInputStream.readByte();
            String aliasHostName = new String(aliasHostNameBytes, "UTF-8");
            if (stringBuffer.length() > 0) stringBuffer.append(".");
            stringBuffer.append(aliasHostName);
        }
        //System.out.println(stringBuffer.toString());
        return stringBuffer.toString();
    }

    private static String dnsAnswerPrint(DataInputStream dataInputStream) throws IOException {
        try{
            //System.out.println("Name: 0x" + String.format("%04x", dataInputStream.readShort()));
            dataInputStream.readShort();
            short type = dataInputStream.readShort();
            // System.out.println("Type: 0x" + String.format("%04x", type));
            // System.out.println("Class: 0x" + String.format("%04x", dataInputStream.readShort()));
            dataInputStream.readShort();
            //  System.out.println("TTL: 0x" + String.format("%08x", dataInputStream.readInt()));
            dataInputStream.readInt();

            try {
                if (type == 0x0001 || type == 0x0028) {
                    short addressLength = dataInputStream.readShort();
                    // System.out.println("Length: 0x" + String.format("%04x", addressLength));
                    //  System.out.print("Address: ");
                    byte[] address = new byte[addressLength];
                    dataInputStream.read(address, 0, addressLength);
                    if (!ipFlag) {
                        ipArrayList.add(convertByteArrayToIP(address, addressLength));
                        // ans = convertByteArrayToIP(address, addressLength);
                        ipFlag = true;
                        return ipArrayList.get(ipArrayList.size() - 1);
                    } else {
                        ipArrayList.add(convertByteArrayToIP(address, addressLength));
                    }
                } else if (type == 0x0005) {
                    if (!cnameFlag) {
                        cnameForAliasHostName = getCanonicalHostName(dataInputStream);
                        cnameFlag = true;
                        return cnameForAliasHostName;
                    } else
                        return getCanonicalHostName(dataInputStream);
                }
                else if(type == 0x0006) {
                    short addressLength = dataInputStream.readShort();
                    byte[] address = new byte[addressLength];
                    dataInputStream.read(address, 0, addressLength);
                    soaFlag = true;
                    return null;
                }
                else {
                    short addressLength = dataInputStream.readShort();
                    byte[] address = new byte[addressLength];
                    dataInputStream.read(address, 0, addressLength);
                    return null;
                }
            }
            catch (Exception e){
                System.out.println(e);
            }
        }catch(Exception e)
        {
            System.out.println(e);
        }
        return null;
    }

    public static String getCanonicalHostName(DataInputStream dataInputStream) throws IOException{
        int domainPartsLength;
        StringBuffer stringBuffer = new StringBuffer();
        domainPartsLength = dataInputStream.readShort();
        byte[] domainBytes = new byte[domainPartsLength];
        for (int i=0; i<domainPartsLength; i++) domainBytes[i] = dataInputStream.readByte();
        String domain = new String(domainBytes, "UTF-8");
        if (stringBuffer.length() > 0) stringBuffer.append(".");
        stringBuffer.append(domain);
        String cname = stringBuffer.toString();
        //System.out.println(cname);
        return cname;
    }

    private static void printResponsePacket(DataInputStream dataInputStream) throws IOException
    {
        int[] outputs = dnsResponseHeaderPrint(dataInputStream);
        int answerRR = outputs[0], authorityRR = outputs[1], additionalRR = outputs[2];
        // System.out.println("Answer:");
        for (int i=0; i<answerRR; i++) dnsAnswerPrint(dataInputStream);
        // System.out.println("authorityRR:");
        for (int i=0; i<authorityRR; i++) dnsAnswerPrint(dataInputStream);
        //System.out.println("additionalRR:");
        for (int i=0; i<additionalRR; i++) dnsAnswerPrint(dataInputStream);
    }

    private static boolean printResponsePacket2(DataInputStream dataInputStream) throws IOException
    {
        int[] outputs = dnsResponseHeaderPrint(dataInputStream);
        int answerRR = outputs[0], authorityRR = outputs[1],additionalRR = outputs[2];
        // System.out.println("Answer:");
        for (int i = 0; i < answerRR; i++) dnsAnswerPrint(dataInputStream);
        if(soaFlag){
            System.out.println(aliasHostName + " does not exist");
            return true;
        }
        //  System.out.println("authorityRR:");
        for (int i = 0; i < authorityRR; i++) dnsAnswerPrint(dataInputStream);
        if(soaFlag){
            System.out.println(aliasHostName + " does not exist");
            return true;
        }
        //  System.out.println("additionalRR:");
        for (int i = 0; i < additionalRR; i++) dnsAnswerPrint(dataInputStream);
        if(soaFlag){
            System.out.println(aliasHostName + " does not exist");
            return true;
        }
        return false;
    }

    private static String convertByteArrayToIP(byte[] address, short addressLength) {
        StringBuffer stringBuffer = new StringBuffer();
        for (int i = 0; i < addressLength; i++ ) {
            if (i != 0) stringBuffer.append(".");
            stringBuffer.append( String.format("%d", (address[i] & 0xFF)) );
        }
        String output = stringBuffer.toString();
        // System.out.println(output);
        return output;
    }

    public static void initRootServers()
    {
        dnsRootServerAddress = new String[15];
        dnsRootServerAddress[0] = "198.41.0.4";
        dnsRootServerAddress[1] = "199.9.14.201";
        dnsRootServerAddress[2] = "192.33.4.12";
        dnsRootServerAddress[3] = "199.7.91.13[";
        dnsRootServerAddress[4] = "192.203.230.10";
        dnsRootServerAddress[5] = "192.5.5.241";
        dnsRootServerAddress[6] = "192.112.36.4";
        dnsRootServerAddress[7] = "198.97.190.53";
        dnsRootServerAddress[8] = "192.36.148.17";
        dnsRootServerAddress[9] = "192.58.128.30";
        dnsRootServerAddress[10] = "193.0.14.129";
        dnsRootServerAddress[11] = "199.7.83.42";
        dnsRootServerAddress[12] = "202.12.27.33";
    }
}