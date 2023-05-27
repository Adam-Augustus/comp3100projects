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

    boolean readQueue = false; //false = send to queue. true = send from queue to servers
    boolean noMoreJobs = false;
    int sizeOfQueue = 0;
    int sizeOfJobToEnqueue = 16;
    int maxQueueSize = 5;


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

    getServers();
    dataSend("ENQJ GQ");
    sizeOfQueue++;
    str = dataRec();

    //MAIN DECISION LOOP
    while (!str.equals("NONE")) {
        String[] split = str.split(" "); //get first element of recieved data, if multiple parts

        //if rec OK, send REDY
        if (split[0].equals("OK")) {
            dataSend("REDY");
        }

        //if rec JOBN or JOBP, get job data, fill currServers with capable servers for that job, run findBestServerForJob, assign job
        else if (split[0].equals("JOBN") || split[0].equals("JOBP")) {
            System.out.println("Running job allocator MAIN");
            int subTime = Integer.parseInt(split[1]);
            int jobID = Integer.parseInt(split[2]);
            int estRuntime = Integer.parseInt(split[3]);
            int reqCores = Integer.parseInt(split[4]);
            int reqMem = Integer.parseInt(split[5]);
            int reqDisk = Integer.parseInt(split[6]);

            getCapServer(reqCores, reqMem, reqDisk);

            int thisServer = findBestServerForJob(reqCores, reqMem, reqDisk);
            if (thisServer != -1) { //assign job to selected server
                String serverToUse = currServers[thisServer].serverType;
                int serverNoToUse = currServers[thisServer].serverID;
                dataSend("SCHD " + split[2] + " " + serverToUse + " " + serverNoToUse);
            }
            if (thisServer == -1) { //assign job to GQ
                sizeOfQueue++;
                dataSend("ENQJ GQ");
            }
        }

        //if job complete, send REDY
        else if (split[0].equals("JCPL")) {
            dataSend("REDY");
        }

        //if no jobs and queue has jobs, turn on noMoreJobs, turn on useQueue, send OK
        else if (split[0].equals("CHKQ")) {
            readQueue = true;
            noMoreJobs = true;
            dataSend("DEQJ GQ 0");
            sizeOfQueue--;
        }
        else if (readQueue) {
            dataSend("DEQJ GQ 0");
            dataSend("REDY");
        }

        else if (str.equals("")) System.out.println("im broken");;

        //System.out.println("READING NEXT");
        str = dataRec();

        if (sizeOfQueue >= maxQueueSize) {
            readQueue = true;
            System.out.println("readQueue is "+readQueue);
        }
        if (sizeOfQueue <= 0 && !noMoreJobs) readQueue = false;
    }

    dataSend("QUIT");

    str = dataRec();

    dout.close();  
    s.close();  

    }

    //get servers and add to array of type server, then fill data as required
    void getServers() throws IOException {
        //System.out.println("Getting Servers");
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
            //System.out.println("Setting server " + x + " to " + s);
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
        //System.out.println("Server collection complete!");
        //System.out.println("Largest server called " + largestServerName + " has " + largestServerCores + " cores and there are " + noLargestServers + " of them");
        dataSend("OK");
    }

    void getCapServer(int core, int mem, int disk) throws IOException {
        dataSend("GETS Capable " + core + " " + mem + " " + disk);
        String s = dataRec();
        String[] a = s.split(" ");
        // while (!a[0].equals("DATA")) {
        //     s = dataRec();
        //     a = s.split(" ");
        //     System.out.println("FAILED "+a[0]);
        // }
        
        noOfCurrServers = Integer.parseInt(a[1]);
        currServers = new server[noOfCurrServers];

        for (int i = 0; i < noOfCurrServers; i++) {
            currServers[i] = new server();
        }
        dataSend("OK");
        int x = 0;
        while(x < noOfCurrServers) {
            s = din.readLine();
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
        if (!readQueue){
            if (reqCores <= sizeOfJobToEnqueue) return -1;
        }
        int bestScore = 99999999; //start best score functionally infinite
        int bestRatio = 99999999;
        int serverToUse = 0;
        for (int i = 0; i < noOfCurrServers; i++) {
            int equivalentServerInMainList = findServerData(currServers[i].serverType, currServers[i].serverID);
            //create a fitness score for each server based on amount of jobs
            int check = 0; //initialize check as 0
                check = (check + currServers[i].rJobs + currServers[i].wJobs)*100;
                check = check/Servers[equivalentServerInMainList].cores;   

            //get ratio of reqUsage to availUsage (from 0 uses no resources, to 1 uses all available resources)
            int ratio = 0;
            int coreRatio = 0;
            //int memRatio = 0;
            //int diskRatio = 0;
            if (currServers[i].cores != 0) coreRatio = (reqCores*100)/Servers[equivalentServerInMainList].cores*100;
            //if (currServers[i].memory != 0) memRatio = (reqMem*100)/currServers[i].memory*100;
            //if (currServers[i].disk != 0) diskRatio = (reqDisk*100)/currServers[i].disk*100;
            //ratio = coreRatio + memRatio + diskRatio / 3;
            ratio = coreRatio;

            //normalize score based on avaliable cores in server (server with more cores can have more jobs in queue)
            //search array of all servers for currently pointed at server

            //System.out.println("Checking server " + currServers[i].serverType + " " + currServers[i].serverID + " with score " + Check);
            if (check < bestScore) {
                bestScore = check;
                serverToUse = i;
            }
            // if (check == bestScore) {
            //     if (ratio < bestRatio) {
            //         bestScore = check;
            //         serverToUse = i;
            //         bestRatio = ratio;
            //     }
            // }
        }
        return serverToUse;
    }

    int findServerData(String name, int n) {
        for (int i = 0; i < noOfServers; i++) {
            if (name.equals(Servers[i].serverType)) {
                if (n == Servers[i].serverID) return i;
            }
        }
        return -1;
    }

    //write s to given dout, and print to console (removed print for submission)
    void dataSend(String s) throws IOException {
        dout.write((s+"\n").getBytes()); 
        System.out.println("Sent: " + s);
        dout.flush();
    }

    //recieve from din, print to console (removed for submission), return str
    String dataRec() throws IOException {
        String str;
        str = din.readLine();
        System.out.println("Rec: " + str);
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
