import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.DataOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.UnknownHostException;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.SignatureException;
import java.security.UnrecoverableEntryException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;

import javax.crypto.BadPaddingException;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;

import org.apache.commons.codec.DecoderException;

public class appFunc implements Runnable {
    private MsgReceiver t;
    private String username;
    public int anonDNS_port=6789;
    private static FriendList flist;
    public ServerSocket receiverSocket;
    private long delay=60*1000; // 1min
    private static boolean loop=true;
	
    public Boolean register() throws InvalidKeyException, UnknownHostException, NoSuchAlgorithmException, UnrecoverableEntryException, KeyStoreException, IllegalBlockSizeException, NoSuchPaddingException, BadPaddingException, NoSuchProviderException, DecoderException, IOException{
        try {
            this.anonDNS_port=utilities.randomPort();
            this.username=utilities.getInput("Give username:");
            String pswd=utilities.getInput("Give password (local use only):");
            String telNo=utilities.getInput("Give telephone number:");
            String apikey=utilities.getInput("Give pointHQ apikey (optional):");
            String domain=utilities.getInput("Give domain (optional):");
            if((pswd==null)||(this.username==null)||(telNo==null)||(pswd.isEmpty())||(this.username.isEmpty())||(telNo.isEmpty())){
                System.err.println("Error: Null input!");
                return false;
            }
            cryptoFunc cr=null;
        try{
            cr = new cryptoFunc(this.username, pswd, telNo,domain ,apikey); 
        }
        catch (anondnsException e)
        {
            System.err.println("Error: User already exists!\n"+e);
            return false;
        }
        System.out.println("Welcome "+this.username);
        flist=new FriendList(cr,delay);//make profile
        } catch (InvalidKeyException | BadPaddingException 
				| IllegalStateException | NoSuchProviderException
				| NoSuchAlgorithmException | SignatureException
				| CertificateException | KeyStoreException 
				| ClassNotFoundException | NoSuchPaddingException
				| IllegalBlockSizeException | InvalidAlgorithmParameterException 
				| IOException | anondnsException e) {
            e.printStackTrace();
            return false;
        }
        getFollowingsRecordsLoop();
        startListener();
        return true;
    }
    
    public Boolean login() throws UnrecoverableEntryException, DecoderException{//load profile
    	try {
            this.anonDNS_port=utilities.randomPort();
            this.username=utilities.getInput("Give username:");
            String pswd=utilities.getInput("and password:");
            if((pswd==null)||(this.username==null)||(pswd.isEmpty())||(this.username.isEmpty())){
                System.err.println("Error: Null input!");
                return false;
            }
            cryptoFunc cr;
            try{
                cr=new cryptoFunc(this.username,pswd);
            }
            catch(anondnsException e){
                return false;
            }
            System.out.println("Welcome "+this.username);
            flist=new FriendList(cr,delay);
            getFollowingsRecordsLoop();
            startListener();
            return true;
            } catch (InvalidKeyException | NoSuchAlgorithmException
                            | CertificateException | KeyStoreException
                            | ClassNotFoundException | NoSuchPaddingException
                            | NoSuchProviderException | BadPaddingException
                            | IllegalBlockSizeException
                            | InvalidAlgorithmParameterException | IOException
                            | anondnsException e) {
                e.printStackTrace();
                return false;
            }
	}

	private void startListener() {
        this.t=new MsgReceiver(this);
            this.t.start();//Run Message receiver
	}

	public void newFollower(String telnum, X509Certificate cert) throws KeyStoreException, NoSuchAlgorithmException, CertificateException, IOException, InvalidKeyException, IllegalBlockSizeException, BadPaddingException, NoSuchPaddingException, InvalidAlgorithmParameterException, NoSuchProviderException, SignatureException {
            flist.newFollower(telnum, cert);
	}

	public Boolean updateFollowerRecordsTimer() throws NoSuchAlgorithmException, UnrecoverableEntryException, KeyStoreException, InvalidKeyException, IllegalBlockSizeException, BadPaddingException, NoSuchPaddingException, SignatureException, NoSuchProviderException, InvalidAlgorithmParameterException, IOException, DecoderException {
            return flist.updateFollowerRecordsTimer();
	}

	public void newFollowing() throws NoSuchAlgorithmException, UnrecoverableEntryException, KeyStoreException, IOException, NoSuchPaddingException, InvalidKeyException, BadPaddingException, IllegalBlockSizeException, NoSuchProviderException, InvalidAlgorithmParameterException, DecoderException, CertificateException {
            String path=utilities.getInput("Give Path of User's Cert:");
            String userDomain=utilities.getInput("Give User's Domain:");
            String userTelNum=utilities.getInput("Give User's Telephone Number:");
            if((path==null)||(userDomain==null)||(path.isEmpty())||(userDomain.isEmpty())){
                System.err.println("Error: Null input!");
                return;
            }
            cryptoFunc cr = null;
            X509Certificate userCert = cr.loadCertFromFile(path);
            String userName=userCert.getSubjectDN().getName();
            flist.newFollowing(userName,userDomain,userTelNum,userCert);
	}
	
    /*send Msg*/
    public void senderThread() throws IOException
    {    
        if (flist.NumOfFollowings()==0)
        {
            System.err.println("Unfortunately, you don't follow any user");
            return;
        }
        String userTelNum=utilities.getInput("Give User's Telephone Number:");
        String userPort=utilities.getInput("Give User's Port:");
    	DNSRecord sendToUser = flist.searchFriend(flist.getFollowingList(), userTelNum);
        if (sendToUser==null)
        {
            System.err.println("There is no such user!");
            return;
        }
        System.out.print("Message:\n> ");
        BufferedReader inFromUser =  new BufferedReader(new InputStreamReader(System.in));
        String sentence = inFromUser.readLine();
        Socket clientSocket = new Socket(sendToUser.getCurrentIpAddr(),Integer.parseInt(userPort));
        DataOutputStream outToServer =  new DataOutputStream(clientSocket.getOutputStream());      
        outToServer.writeBytes(sentence+'\n');
        String receiverName=flist.fromFollowingsIPtoName(sendToUser.getCurrentIpAddr().getHostAddress());
        System.out.print("Sent to "+receiverName+": " + sentence); 
        BufferedReader inFromServer =  new BufferedReader(new InputStreamReader(clientSocket.getInputStream())); 
        try{
            String ack = inFromServer.readLine();
            if(ack.equals(sentence))
                System.out.println("........acked!");
        }catch(NullPointerException e){
            System.out.println("........failed!");
        }
        clientSocket.close(); 
    }

    private void getFollowingsRecordsLoop() throws UnknownHostException, NoSuchAlgorithmException, UnrecoverableEntryException, KeyStoreException, InvalidKeyException, IllegalBlockSizeException, NoSuchPaddingException, BadPaddingException, NoSuchProviderException, DecoderException, IOException {
        FileWriter fw = new FileWriter(this.username+"/"+"followedUsersPings.log", true);
        BufferedWriter bw = new BufferedWriter(fw);
        PrintWriter out = new PrintWriter(bw);
        flist.setOut(out);
        (new Thread(new appFunc())).start();
    }

    public void run() {
        while((loop)&&(flist.NumOfFollowings()>0)){
            try {
                flist.updateFollowings();
                Thread.sleep(delay);
            } catch (InvalidKeyException | UnknownHostException
                            | NoSuchAlgorithmException | UnrecoverableEntryException
                            | KeyStoreException | IllegalBlockSizeException
                            | BadPaddingException | NoSuchPaddingException
                            | NoSuchProviderException | InterruptedException| DecoderException e) {
                e.printStackTrace();
            }
            //updateFollowings every N sec in separate Thread.
        }
        if(flist.NumOfFollowings()==0)
            flist.out.println("No Followings");
    }

    public void showContacts(int i) {
        flist.showContacts(i);
    }

    public String fromFollowingIPtoName(String userIP) {
        return flist.fromFollowingIPtoName(userIP);
    }

    public void unfollow() throws KeyStoreException, BadPaddingException, IllegalBlockSizeException, NoSuchPaddingException, NoSuchAlgorithmException, InvalidKeyException, IOException, InvalidAlgorithmParameterException {
        String userTelNum=utilities.getInput("Give User's Telephone Number");
        flist.unfollow(userTelNum);
    }

    public void deleteFollower() throws IOException, IllegalBlockSizeException, NoSuchAlgorithmException, KeyStoreException, InvalidAlgorithmParameterException, InvalidKeyException, NoSuchPaddingException, SignatureException, BadPaddingException, NoSuchProviderException
    {
        String userTelNum=utilities.getInput("Give User's Telephone Number");
        flist.deleteFollower(userTelNum);
    }

    public void close() throws IOException {
        this.t.receiverSocket.close();
        loop=false;
        //close updatefollowings
    }

    void addFollower() throws KeyStoreException, NoSuchAlgorithmException, CertificateException, IllegalBlockSizeException, InvalidKeyException, NoSuchPaddingException, BadPaddingException, InvalidAlgorithmParameterException, NoSuchProviderException, IOException, SignatureException {
        String myTelNum=utilities.getInput("Give Follower's TelNo");
        String path=utilities.getInput("Give Path of Follower's Cert:");
            if((path==null)||(myTelNum==null)||(path.isEmpty())||(myTelNum.isEmpty())){
            System.err.println("Error: Null input!");
            return;
        }
        cryptoFunc cr = null;
        X509Certificate cert = cr.loadCertFromFile(path);
        if (cert==null) return;
        newFollower(myTelNum,cert);
    }
}
