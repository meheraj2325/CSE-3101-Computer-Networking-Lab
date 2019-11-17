#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <stdbool.h>
#include<time.h>
#include <unistd.h>
#include <sys/socket.h>
#include <sys/types.h>
#include <netinet/in.h>
#include <netdb.h>
#include <errno.h>

int port = 0;
char statusCode[200] = {0};

void clearStatusCode();
char* addString(char *c,char *d);
char* getClientMailAddress();
char* getMailMsgBody(char *file_name);
int extractPortNumber(char *c);
char* extractServerHostName(char *c);
char* extractServerMailAddress(char *c);
bool checkMailAddressFormat(char* c);

int main(int argc, char const *argv[])
{
    if(argc<4)
    {
        printf("Incorrect number of arguments : %d.\n",argc);
        return 0;
    }

    char* mailAddressWithPort=argv[1];
    if(!checkMailAddressFormat(mailAddressWithPort)) // Checking the mail address format 
    {
        printf("Incorrect mail format.\n");
        return 0;
    }

    char *actualServerMailAddress = extractServerMailAddress(mailAddressWithPort); // extracting the server mail address from first argument
    char *serverHostName = extractServerHostName(mailAddressWithPort); // extracting the server host name from first argument
    port = extractPortNumber(mailAddressWithPort); // extracting the port number from first argument

    struct sockaddr_in server_address;
    struct hostent *host;
    int sockett = 0, readValue;

    host = gethostbyname(serverHostName);
    if ((sockett = socket(AF_INET, SOCK_STREAM, 0)) < 0)
    {
        printf("\n Socket creation error. \n");
        return -1;
    }
    memset(&server_address, '0', sizeof(server_address));

    server_address.sin_family = AF_INET;
    server_address.sin_port = htons(port);
    server_address.sin_addr = *((struct in_addr *)host->h_addr);
    bzero(&(server_address.sin_zero),8);

    if(inet_pton(AF_INET, serverHostName, &server_address.sin_addr)<=0)
    {
        printf("\n Invalid address/ Address not supported \n");
        return -1;
    }

    if (connect(sockett, (struct sockaddr *)&server_address, sizeof(server_address)) < 0) //  connecting to the server
    {
        printf("\n421 Service not available \r\n");
        return -1;
    }

    clearStatusCode();
    readValue = read( sockett, statusCode, 1024);
    if(statusCode[0] != '2')
    {
        printf("Error occurred.\n Replied status code and phrase : %s\n",statusCode);
        return 0;
    }
    printf("Status code and phrase : %s\r\n",statusCode );

    char *helo = "HELO ";
    helo = addString(helo,serverHostName);
    send(sockett, helo, strlen(helo), 0 ); // Sending the helo request
    printf("Sent HELO request : %s\r\n",helo);
    clearStatusCode();
    readValue = read(sockett, statusCode, 1024);
    if(statusCode[0]!='2')
    {
        printf("Error occurred.\n Replied status code and phrase : %s\n",statusCode);
        return 0;
    }
    printf("Status code and phrase : %s\r\n",statusCode );

    char *clientMailAddress = getClientMailAddress(); // retrieving the user name and host name of a client linux machine.
    char *mail_from = "MAIL FROM: <";
    mail_from = addString(mail_from,clientMailAddress);
    mail_from=addString(mail_from,">");
    send(sockett, mail_from, strlen(mail_from), 0 ); // Sending the "mail from" request
    printf("Sent MAIL_FROM request : %s\r\n",mail_from);
    clearStatusCode();
    readValue = read( sockett, statusCode, 1024);
    if(statusCode[0] != '2')
    {
        printf("Error occurred.\n Replied status code and phrase : %s\n",statusCode);
        return 0;
    }
    printf("Status code and phrase : %s\r\n",statusCode );

    char *rcpt_to = "RCPT TO: <";
    rcpt_to = addString(rcpt_to,actualServerMailAddress);
    rcpt_to = addString(rcpt_to,">");
    send(sockett, rcpt_to, strlen(rcpt_to), 0 ); // Sending the "rcpt to" request
    printf("Sent RCPT_TO request : %s\r\n",rcpt_to);
    clearStatusCode();
    readValue = read( sockett, statusCode, 1024);
    if(statusCode[0]!='2')
    {
        printf("Error occurred.\n Replied status code and phrase : %s\n",statusCode);
        return 0;
    }
    printf("Status code and phrase : %s\r\n",statusCode );

    send(sockett, "DATA", 4 , 0 ); // Sending the "DATA" command
    printf("Sent DATA request : DATA\r\n");
    clearStatusCode();
    readValue = read( sockett, statusCode, 1024);
    if(statusCode[0]!='2' && statusCode[0]!='3')
    {
        printf("Error occurred.\n Replied status code and phrase : %s\n",statusCode);
        return 0;
    }
    printf("Status code and phrase : %s\r\n",statusCode );


    // Sending the message header
    char *from = "FROM: <";
    from = addString(from,clientMailAddress);
    from = addString(from,">");
    send(sockett, from, strlen(from), 0 );
    printf("Sent -> %s\r\n",from);
    clearStatusCode();
    readValue = read( sockett, statusCode, 1024);

    char *to = "TO: <";
    to = addString(to,actualServerMailAddress);
    to = addString(to,">");
    send(sockett, to, strlen(to), 0 );
    printf("Sent -> %s\r\n",to);
    clearStatusCode();
    readValue = read( sockett, statusCode, 1024);

    char dateAndTime[30];
     time_t now = time(NULL);
    struct tm *t = localtime(&now);
    strftime(dateAndTime, sizeof(dateAndTime)-1, "DATE %d-%m-%Y %H:%M", t);
    send(sockett, dateAndTime, strlen(dateAndTime), 0 );
    printf("Sent -> %s\r\n",dateAndTime);
    clearStatusCode();
    readValue = read( sockett, statusCode, 1024);

    char subject[100];
    int idx = 0;
    for(int i=2; i<argc-1; i++)
    {
        int subLen = strlen(argv[i]);
        for(int j=0; j<subLen; j++)
        {
            if(argv[i][j]=='"') continue;
            subject[idx] = argv[i][j];
            idx++;
        }
        subject[idx] = ' ';
        idx++;
    }
    subject[idx] = '\0';

    char *mail_subject = "Subject: ";
    mail_subject = addString(mail_subject,subject);
    send(sockett, mail_subject, strlen(mail_subject), 0 ); // Sending the mail subject
    printf("Sent -> %s\r\n",mail_subject);
    clearStatusCode();
    readValue = read( sockett, statusCode, 1024);

    send(sockett, " ", strlen(" "), 0 );
    printf("Sent -> A blank line between headers and message body.\r\n");
    clearStatusCode();
    readValue = read( sockett, statusCode, 1024);

    char* fileName = argv[argc-1];
    char *mailMsgBody = getMailMsgBody(fileName);
    ///printf("-----------------     %s\n",mailMsgBody);
    int mailBodyLength = strlen(mailMsgBody);
	printf("Mail body length = %d\n",mailBodyLength);
    char *fileBuffer = (char *)malloc(mailBodyLength * sizeof(char));
    int j = 0;
    for(int i=0; i<mailBodyLength; i++)
    {
        fileBuffer[j] = mailMsgBody[i];
        if(mailMsgBody[i]=='\n' || mailMsgBody[i]=='\0')
        {
			///printf("i = %d\n", i);
            fileBuffer[j] = '\0';
            j=0;
            send(sockett, fileBuffer, strlen(fileBuffer), 0 );
            printf("Sent : %s\r\n",fileBuffer);
            clearStatusCode();
            readValue = read( sockett, statusCode, 1024);
        }
        else
            j++;
    }

    send(sockett,".",strlen("."), 0 );
    printf("Sent : .\r\n");
    clearStatusCode();
    readValue = read( sockett, statusCode, 1024);
    if(statusCode[0]!='2')
    {
        printf("Error occurred.\n Replied status code and phrase : %s\n",statusCode);
        return 0;
    }
    printf("Status code and phrase : %s\r\n",statusCode );

    send(sockett, "QUIT", 4, 0 );
    printf("Sent: QUIT\r\n");
    clearStatusCode();
    readValue = read( sockett, statusCode, 1024);
    if(statusCode[0]!='2')
    {
        printf("Error occurred.\n Replied status code and phrase : %s\n",statusCode);
        return 0;
    }
    printf("Status code and phrase : %s\r\n",statusCode );
    return 0;
}


// This method checks the mail address and detect whether it is in correct format or not
bool checkMailAddressFormat(char* c)
{
    bool at_flag,colon_flag;
    int len = strlen(c);

    for(int i=0; i<len; i++)
    {
        if(!at_flag)
        {
            if(c[i] == '@')  at_flag = true;
        }
        else
        {
            if(c[i] == ':') colon_flag = true;
        }
    }
    printf("%s\n",c);
    return (at_flag && colon_flag);
}

// This method extracts the server mail address from first argument
char* extractServerMailAddress(char *c)
{
    int len = strlen(c);
    char *mail_address = (char *)malloc(len*sizeof(char));
    int i = 0;
    while(c[i] != ':')
    {
        mail_address[i] = c[i];
        i++;
    }
    mail_address[i] = '\0';
    return mail_address;
}

// This method extracts the server host name from first argument
char* extractServerHostName(char *c)
{
    int len = strlen(c);
    char* host = (char *)malloc(len*sizeof(char));
    int i = 0;
    while(c[i] != '@') i++;
    i++;

    int j=0;
    while(c[i] !=  ':')
    {
        host[j] = c[i];
        i++;
        j++;
    }
    host[j] = '\0';
    return host;
}

// This method extracts the server port number from first argument
int extractPortNumber(char *c)
{
    char* port = (char*)malloc(6 * sizeof(char));
    int i = 0;
    while(c[i] != ':') i++;
    i++;
    port[0] = c[i];
    port[1] = c[i+1];
    port[2] = c[i+2];
    port[3] = c[i+3];
    port[4] = '\0';
    return atoi(port); // atoi converts the char array to integer
}

// This method reads the body of the email message from the text file
char* getMailMsgBody(char *file_name)
{
    char *msg_body = malloc(1000 * sizeof(char));
    int temp;
    int i = 0;
    FILE *file;
    file = fopen(file_name, "r");

    if (file)
    {
        while ((temp = fgetc(file)) != EOF)
        {
            msg_body[i] = (char)temp;
            i++;
        }
        msg_body[i] = '\0';
        fclose(file);
    }
    else
    {
        perror("File opening failed.\n");
        exit(0);
    }
    return msg_body;
}

// This method determines the client mail address
char* getClientMailAddress()
{
    char *username = getenv("USER");
    char *client_mail_adr = strcat(username,"@");
    char client_hostname[50];
    gethostname(client_hostname, 50);
    client_mail_adr = strcat(client_mail_adr,client_hostname);
    return client_mail_adr;
}

// It concatenates 2 char arrays and stores the result in a new char array
char* addString(char *c,char *d)
{
    int len_c = strlen(c);
    int len_d = strlen(d);
    char *result = (char *)malloc((len_c + len_d + 5) * sizeof(char));
    for(int i=0; i<len_c; i++)
    {
        result[i] = c[i];
    }
    for(int i=0; i<=len_d; i++)
    {
        result[i+len_c]=d[i];
    }
    return result;
}

// This method clears the statuscode array which is used to recieve the status code from server 
void clearStatusCode()
{
    for(int i=0; i<200; i++) statusCode[i] = 0;
}
