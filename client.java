import java.net.*;
import java.io.*; 

public class client{

    //Elements to create network connection
    Socket s;
    BufferedReader din;
    DataOutputStream dout;

    //Elements for super cool super awesome implementation
    server[] Servers; //full list of servers
    int noOfServers; //how many of those there are
    server[] currServers; //list of servers that can do job
    int noOfCurrServers; //how many there

    public client(String a, int p) {
        try {
            //open connection
            s = new Socket(a, p);
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
    handshake();
    dataSend("REDY");
    str = dataRec();
    getServers();
    dataSend("ENQJ GQ");
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
        }

        //if job complete, send REDY
        else if (split[0].equals("JCPL")) {
            dataSend("REDY");
        }

        //if no jobs and queue has jobs, turn on noMoreJobs, turn on useQueue, send OK
        else if (split[0].equals("CHKQ")) {
            dataSend("DEQJ GQ 0");
        }

        else if (str.equals("")) System.out.println("im broken");;
        str = dataRec();
    }

    dataSend("QUIT");

    str = dataRec();

    dout.close();  
    s.close();  

    }

    //get servers and add to array of type server, then fill data as required
    void getServers() throws IOException {
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
        while(x < noOfServers) {        //collect all data on Servers
            s = din.readLine();
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
        dataSend("OK");
    }

    void getCapServer(int core, int mem, int disk) throws IOException {
        dataSend("GETS Capable " + core + " " + mem + " " + disk);
        String s = dataRec();
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
        for (int i = noOfCurrServers-1; i > 0; i--) {
            int equivalentServerInMainList = findServerData(currServers[i].serverType, currServers[i].serverID);

            //create a fitness score for each server based on amount of jobs
            //running and waiting jobs INCREASE
            //more cores DECREASE
            //more available cores DECREASE
            int check = 0; //initialize check as 0
                check = (check + currServers[i].rJobs + currServers[i].wJobs)*100000;
                //check = check/Servers[equivalentServerInMainList].cores;
                if (currServers[i].cores != 0) check = check/currServers[i].cores;   

            //System.out.println("Checking server " + currServers[i].serverType + " " + currServers[i].serverID + " with score " + check);
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

    //Find in Servers[] index that matches input server from currServers, if fail return -1
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
