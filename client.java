import java.net.*;
import java.io.*; 

public class client{

    //Elements to create network connection
    Socket s;
    BufferedReader din;
    DataOutputStream dout;

    //Elements for super cool super awesome implementation
    server[] Servers;
    int noOfServers;
    server[] currServers;
    int noOfCurrServers;


    public client(String a, int p) {
        try {
            //open connection
            s = new Socket(a, p);
            //System.out.println("socket opened!");
            //System.out.println("Local IP: " + s.getLocalAddress() + " Local port:" + s.getLocalPort());
            //System.out.println("Remote IP: " + s.getInetAddress() + " Remote port:" + s.getPort());

            din = new BufferedReader(new InputStreamReader(s.getInputStream()));
            dout = new DataOutputStream(s.getOutputStream());
        }
        catch(UnknownHostException e) {
            System.out.println("Unknown Host!: " + e);
        }
        catch(IOException e) {
            System.out.println("IOExepction!: " + e);
        }
    }

    public void run() throws IOException{
    String str;             //String to hold job requests when buffer is overwritten (such as when executing GETS)
    //System.out.println("Handshake success?:)) " + handshake());
    handshake();

    dataSend("REDY");

    str = dataRec();

    //getServers();

    while (!str.equals("NONE")) {
        String[] split = str.split(" ");
        if (split[0].equals("OK")) {
            dataSend("REDY");
        }
        //Read job data into variables
        if (split[0].equals("JOBN")) {
            //System.out.println("Rec Job " + str);
            //int subTime = Integer.parseInt(split[1]);
            //int jobID = Integer.parseInt(split[2]);
            //int estRuntime = Integer.parseInt(split[3]);
            int reqCores = Integer.parseInt(split[4]);
            int reqMem = Integer.parseInt(split[5]);
            int reqDisk = Integer.parseInt(split[6]);

            getCapServer(reqCores, reqMem, reqDisk); //fill currServer array with servers capable of given job
            int thisServer = findBestServerForJob(reqCores, reqMem, reqDisk);
            String serverToUse = currServers[thisServer].serverType;
            int serverNoToUse = currServers[thisServer].serverID;

            dataSend("SCHD " + split[2] + " " + serverToUse + " " + serverNoToUse);
            //System.out.println("Scheduled job " + jobID + " to " + serverToUse + " " + serverNoToUse);
        }
        if (split[0].equals("JCPL")) {
            dataSend("REDY");
        }
        //System.out.println("Reading next command");
        str = dataRec();
        //System.out.println("Rec: " + str);
    }

    dataSend("QUIT");

    str = dataRec();

    dout.close();  
    s.close();  

    }

    //get servers and add to array of type server, then fill data as required
    void getServers() throws IOException {
        System.out.println("Getting Servers");
        dataSend("GETS All");
        String s = dataRec();
        String[] a = s.split(" ");
        noOfServers = Integer.parseInt(a[1]);
        Servers = new server[noOfServers];
        for (int i = 0; i < noOfServers; i++) {
            Servers[i] = new server();
        }
        dataSend("OK");
        int x = 0;
        while(x < noOfServers) {        //collect all data for future complex implementations
            s = din.readLine();
            System.out.println("Setting server " + x + " to " + s);
            String[] temp = s.split(" ");
            Servers[x].serverType = temp[0];
            Servers[x].serverID = Integer.parseInt(temp[1]);
            Servers[x].state = temp[2];
            Servers[x].currStartTime = Integer.parseInt(temp[3]);
            Servers[x].cores = Integer.parseInt(temp[4]);
            Servers[x].memory = Integer.parseInt(temp[5]);
            Servers[x].disk = Integer.parseInt(temp[6]);
            Servers[x].wJobs = Integer.parseInt(temp[7]);
            Servers[x].rJobs = Integer.parseInt(temp[8]);

            x++;
        }
        System.out.println("Server collection complete!");
        //System.out.println("Largest server called " + largestServerName + " has " + largestServerCores + " cores and there are " + noLargestServers + " of them");
        dataSend("OK");
    }

    void getCapServer(int core, int mem, int disk) throws IOException {
        //System.out.println("Getting Capable Servers!");
        dataSend("GETS Capable " + core + " " + mem + " " + disk);
        String s = dataRec();
        //System.out.println("Recieved " + s);
        String[] a = s.split(" ");
        noOfCurrServers = Integer.parseInt(a[1]);
        currServers = new server[noOfCurrServers];

        for (int i = 0; i < noOfCurrServers; i++) {
            currServers[i] = new server();
        }
        dataSend("OK");
        int x = 0;
        while(x < noOfCurrServers) {
            s = din.readLine();
            //System.out.println("Setting server " + x + " to " + s);
            String[] temp = s.split(" ");
            currServers[x].serverType = temp[0];
            currServers[x].serverID = Integer.parseInt(temp[1]);
            currServers[x].state = temp[2];
            currServers[x].currStartTime = Integer.parseInt(temp[3]);
            currServers[x].cores = Integer.parseInt(temp[4]);
            currServers[x].memory = Integer.parseInt(temp[5]);
            currServers[x].disk = Integer.parseInt(temp[6]);
            currServers[x].wJobs = Integer.parseInt(temp[7]);
            currServers[x].rJobs = Integer.parseInt(temp[8]);
            x++;
        }
        dataSend("OK");
    }

    int findBestServerForJob(int reqCores, int reqMem, int reqDisk) {
        //modified (improved) version of a Best Fit algorithm. Not currently using the global Queue.
        //get cap server with lowest 'working score' ("req / avail") (aka fitness score)
        //assign job to that server
        //--
        //score needs to be averaged, i.e. a large server running at 50% has to have a lower score than a tiny server running 60%
        //needs to return an Int that represents an index to chosen server in currServers.
        //--
        //cores is cores avaliable to use not avaliable overall
        int bestScore = 99999999; //start best score functionally infinite
        int serverToUse = 0;
        for (int i = 0; i < noOfCurrServers; i++) {
            //create a fitness score for each server based on amount of jobs
            int check = 0; //initialize check as 0
            check = check + currServers[i].rJobs + 10000; //valuate having running jobs as super unfavourable, all jobs same weight
            check = check + currServers[i].wJobs; //valuate waiting jobs regularly

            //normalize score based on avaliable cores in server (server with more cores can have more jobs in queue)


            //System.out.println("Checking server " + currServers[i].serverType + " " + currServers[i].serverID + " with score " + Check);
            if (check < bestScore) {
                bestScore = check;
                serverToUse = i;
            }
        }
        return serverToUse;
    }

    //write s to given dout, and print to console (removed print for submission)
    void dataSend(String s) throws IOException {
        dout.write((s+"\n").getBytes()); 
        //System.out.println("Sent: " + s);
        dout.flush();
    }

    //recieve from din, print to console (removed for submission), return str
    String dataRec() throws IOException {
        String str;
        str = din.readLine();
        //System.out.println("Rec: " + str);
        return str;
    }

    //automate handshake, return true if success, false if fail
    boolean handshake() throws IOException {
        String temp;
        dataSend("HELO");
        temp = dataRec();
        if(!temp.equals("OK")) {
            System.out.println("Handshake failure! Expected 'OK', recieved: " + temp);
            return false;
        }
        dataSend("AUTH "+System.getProperty("user.name"));
        temp = dataRec();
        if(!temp.equals("OK")) {
            System.out.println("Handshake failure! Expected 'OK', recieved: " + temp);
            return false;
        }
        return true;
    }

    public static void main(String args[]) throws IOException {
        client client = new client("127.0.0.1", 50000);
        client.run();
    }
}
