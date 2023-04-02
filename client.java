import java.net.*;  
import java.io.*; 

//TODO create server class to make data recieved from GETS usable

public class client{

    Socket s;
    BufferedReader din;
    DataOutputStream dout;

    String[] Servers;

    public client(String a, int p) {
        try {
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
    String str;

    System.out.println(handshake());

    dataSend("REDY");

    str = dataRec();

    getServers();

    str = dataRec();

    dataSend("OK");

    str = dataRec();

    dataSend("QUIT");

    dout.close();  
    s.close();  

    }

    //get servers and add to arraylist
    boolean getServers() throws IOException {
        dataSend("GETS All");
        String s = dataRec();
        String[] a = s.split(" ");
        Servers = new String[Integer.parseInt(a[1])];
        dataSend("OK");
        int x = 0;
        while((s = din.readLine()) != null) {
            Servers[x] = s;
            x++;
            System.out.println("Setting server " + x + " to " + s);
        }
        return false;
    }

    //write s to given dout, and print to console
    void dataSend(String s) throws IOException {
        dout.write((s+"\n").getBytes()); 
        System.out.println("Sent: " + s);
        dout.flush();
    }

    //recieve from din, print to console, return str
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
        client client = new client("localhost", 50000);
        client.run();
    }
}