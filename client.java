import java.net.*;  
import java.io.*; 

public class client{

    //Elements to create network connection
    Socket s;
    BufferedReader din;
    DataOutputStream dout;

    //Elements to get largest server type for lrr implementation
    server[] Servers;
    int largestServerCores = 0;
    String largestServerName;
    int noLargestServers = 0;

    public client(String a, int p) {
        try {
            //open connection
            s = new Socket(a, p);
            System.out.println("socket opened!");
            System.out.println("Local IP: " + s.getLocalAddress() + " Local port:" + s.getLocalPort());
            System.out.println("Remote IP: " + s.getInetAddress() + " Remote port:" + s.getPort());

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
    System.out.println("Handshake success?: " + handshake());

    dataSend("REDY");

    str = dataRec();

    getServers();

    int serverToUse = 0;
    while (!str.equals("NONE")) {
        String[] split = str.split(" ");
        if (split[0].equals("OK")) {
            dataSend("REDY");
        }
        if (split[0].equals("JOBN")) {
            dataSend("SCHD " + split[2] + " " + largestServerName + " " + serverToUse);
            serverToUse++;
        }
        if (split[0].equals("JCPL")) {
            dataSend("REDY");
        }
        if (serverToUse >= noLargestServers) serverToUse = 0;
        //System.out.println("Reading next command");
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
        int noOfServers = Integer.parseInt(a[1]);
        Servers = new server[noOfServers];
        for (int i = 0; i < noOfServers; i++) {
            Servers[i] = new server();
        }
        dataSend("OK");
        int x = 0;
        while(x < noOfServers) {        //collect all data in anticipation that its required for stage 2
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

            if (Servers[x].cores > largestServerCores) {    //retain name of first server type with most cores
                largestServerName = Servers[x].serverType;
                largestServerCores = Servers[x].cores;
                noLargestServers = 0;
            }
            if (Servers[x].cores == largestServerCores && Servers[x].serverType.equals(largestServerName)) {    //iterate counter for each subsequent matching server
                noLargestServers++;
            }

            x++;
        }
        //System.out.println("Server collection complete!");
        //System.out.println("Largest server called " + largestServerName + " has " + largestServerCores + " cores and there are " + noLargestServers + " of them");
        dataSend("OK");
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