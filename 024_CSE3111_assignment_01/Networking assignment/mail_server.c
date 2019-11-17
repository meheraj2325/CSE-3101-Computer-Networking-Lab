#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <netinet/in.h>
#include <sys/socket.h>
#include <sys/types.h>
#include <netinet/in.h>
#include <arpa/inet.h>
#define PORT 2424

char command[200] = {0};

void clearcommand();
char* addString(char *c,char *d);
char* extractClientUsername(char *c);

int main(int argc, char const *argv[])
{
    printf("Port no: %d\n",PORT);
    clearcommand();

    struct sockaddr_in address;
    int opt = 1,serverFileDescriptor, sockett, readValue,addressLength;
    addressLength = sizeof(address);
    char hostname[100];
    gethostname(hostname, 100);

    if ((serverFileDescriptor = socket(AF_INET, SOCK_STREAM, 0)) == 0)
    {
        perror("Socket creation failed.");
        return -1;
    }

    if (setsockopt(serverFileDescriptor, SOL_SOCKET, SO_REUSEADDR | SO_REUSEPORT,&opt, sizeof(opt)))
    {
        perror("Setsockopt failed.");
        return -1;
    }

    address.sin_family = AF_INET;
    address.sin_addr.s_addr = INADDR_ANY;
    address.sin_port = htons( PORT );

    if (bind(serverFileDescriptor, (struct sockaddr *)&address, sizeof (address ))<0)
    {
        perror("Bind failed.");
        return -1;
    }
    if (listen(serverFileDescriptor, 3) < 0)
    {
        perror("Listen failed.");
        return -1;
    }
	
	// Accepts the client request
    while((sockett = accept(serverFileDescriptor, (struct sockaddr *)&address,(socklen_t*) & addressLength))>0)
    {
        printf("Connected.\n");
        clearcommand();
        char *peerIP;
        peerIP=inet_ntoa(address.sin_addr);
        char* invalidCommand="500 Syntax error or Invalid command";

        char *ready = "220 The server is ready";
        send(sockett,ready, strlen(ready), 0 );
        printf("Sent to client : %s\r\n",ready );
        clearcommand();
        readValue = read( sockett, command, 1024); // reading the helo command
        if(command[0] != 'H' || command[1] != 'E' || command[2] != 'L' || command[3] != 'O')
        {	
            send(sockett,invalidCommand, strlen(invalidCommand), 0 );
            printf("Command : %s \n Error status : %s\r\n\n\n",command,invalidCommand );
            close(sockett);
            continue;
        }
        printf("Received command : %s\r\n",command );
	
        char *welcome = "250 Hello ";
        welcome = addString(welcome,peerIP);
        welcome = addString(welcome,", pleased to meet you");
        send(sockett,welcome, strlen(welcome), 0 ); // sending the reponse message for helo command
        printf("Sent to client : %s\r\n",welcome);
        clearcommand();
        readValue = read( sockett, command, 1024); // reading the mail from command
        if(command[0]!='M'||command[1]!='A'||command[2]!='I'||command[3]!='L'||command[4]!=' '||command[5]!='F'||command[6]!='R'||command[7]!='O'||command[8]!='M')
        {

            send(sockett,invalidCommand, strlen(invalidCommand), 0 );
            printf("Command : %s \n Error status : %s\r\n\n\n",command,invalidCommand );
            close(sockett);
            continue;
        }
        printf("Received command : %s\r\n",command );

        char *mailFromResponse ="250 Sender OK";
        send(sockett,mailFromResponse, strlen(mailFromResponse), 0 );  // sending the reponse message for mail from command
        printf("Sent to client : %s\r\n",mailFromResponse);
        clearcommand();
        readValue = read( sockett, command, 1024); // reading the rcpt to command
        if(command[0]!='R'||command[1]!='C'||command[2]!='P'||command[3]!='T'||command[4]!=' '||command[5]!='T'||command[6]!='O')
        {

            send(sockett,invalidCommand, strlen(invalidCommand), 0 );
            printf("Command : %s \n Error status : %s\r\n\n\n",command,invalidCommand );
            close(sockett);
            continue;
        }
        printf("Received command : %s\r\n",command );
        char* clientUsername = extractClientUsername(command); // extracting the receiver name from rcpt to command
        clientUsername = addString(clientUsername,".txt");
        ///printf("clientUserName : %s\n",clientUsername);

        FILE *userMailboxCheck;
        userMailboxCheck=fopen(clientUsername, "r");
        if(userMailboxCheck==NULL)
        {
            char* notExists="550 No such user here";
            send(sockett,notExists, strlen(notExists), 0 );
            printf("Error Status : %s\r\n\n\n\n",notExists );
            close(sockett);
            continue;
        }
        fclose(userMailboxCheck);

        char *rcptToResponse="250 Recipient OK";
        send(sockett,rcptToResponse, strlen(rcptToResponse), 0 );  // sending the reponse message for rcpt to command
        printf("Sent to Client : %s\r\n",rcptToResponse );
        clearcommand();
        readValue = read( sockett, command, 1024);
        if(command[0]!='D'||command[1]!='A'||command[2]!='T'||command[3]!='A')
        {
            send(sockett,invalidCommand, strlen(invalidCommand), 0 );
            printf("Command : %s \n Error status : %s\r\n\n\n",command,invalidCommand );
            close(sockett);
            continue;
        }
        printf("Received command : %s\r\n",command );

        char *dataResponse ="354 Enter mail, end with \".\" on a line by itself";
        send(sockett,dataResponse, strlen(dataResponse), 0 ); // sending the reponse message for DATA command
        printf("Sent to client : %s\r\n",dataResponse);

        FILE *userMailbox;
        userMailbox=fopen(clientUsername, "a");

	// reading the mail message header and body        
	while(1)
        {
            clearcommand();
            readValue = read( sockett, command, 1024);
            printf("Received -> %s\r\n",command );
            if(command[0]=='.' && command[1]=='\0') break;
            else
            {
                send(sockett,"250 OK", strlen("250 OK"), 0 );
                fprintf(userMailbox, "%s\r\n", command);
            }
        }
        fprintf(userMailbox, "\r\n\n\n\n" );
        fclose(userMailbox);

        char *msgAccepted="250 Message accepted for delivery";
        send(sockett,msgAccepted, strlen(msgAccepted), 0 );
        printf("Sent to client : %s\r\n",msgAccepted);
        clearcommand();
        readValue = read( sockett, command, 1024);
        if(command[0]!='Q'||command[1]!='U'||command[2]!='I'||command[3]!='T')
        {
            send(sockett,invalidCommand, strlen(invalidCommand), 0 );
            printf("Command : %s \n Error status : %s\r\n\n\n",command,invalidCommand );
            close(sockett);
            continue;
        }
        printf("Received command : %s\r\n",command );

        char *closing_connection="221 ";
        closing_connection = addString(closing_connection,hostname);
        closing_connection=addString(closing_connection," closing connection");
        send(sockett,closing_connection, strlen(closing_connection), 0 );
        printf("Sent to client : %s\r\n\n\n\n",closing_connection);
        close(sockett);
    }

    return 0;
}

// This method extracts the reciever name from rcpt to command
char* extractClientUsername(char *c)
{
    char *result = (char *)malloc(strlen(c)*sizeof(c));
    int i=0,j=0;
    while(c[i]!='<') i++;
    i++;
    while(c[i]!='@')
    {
        result[j] = c[i];
        i++,j++;
    }
    result[j] = '\0';
    return result;
}

// It concatenates 2 char arrays and stores the result in a new char array
char* addString(char *c,char *d)
{
    int len_c = strlen(c);
    int len_d = strlen(d);
    char *result = (char *)malloc((len_c + len_d + 5) * sizeof (char));
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

void clearcommand()
{
    for(int i=0; i<200; i++) command[i] = 0;
}
