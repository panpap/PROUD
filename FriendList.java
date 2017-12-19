import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.UnknownHostException;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.SignatureException;
import java.security.UnrecoverableEntryException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;

import javax.crypto.BadPaddingException;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;

import org.apache.commons.codec.DecoderException;
import org.xbill.DNS.TextParseException;
import java.util.Random;


public class FriendList{
    
    private final long N;
    private final ProfileInfo prof;
    private HashMap <String,DNSRecord> followingsList=null;
    private HashMap <String,DNSRecord> followersList=null; 
    private final HashMap <String,String> userToName;
    private final KeyStore ks;
    private final DynDNS dns;
    private final cryptoFunc crypto;
    public PrintWriter out;

    public FriendList(cryptoFunc cr,long delay) throws NoSuchAlgorithmException, CertificateException, KeyStoreException, IOException, ClassNotFoundException, NoSuchPaddingException, NoSuchProviderException, InvalidKeyException, BadPaddingException, IllegalBlockSizeException, InvalidAlgorithmParameterException, anondnsException{
        this.prof=cr.getProf();
        this.crypto=cr;
        this.N=delay;
        this.ks=retrieveCertificates(this.prof.myPass);
        if (this.ks!=null)
        {	       
            this.followersList=retrieveFriendsList(1);
            this.followingsList=retrieveFriendsList(2); 
            this.userToName=retrieveUsersList();
    	}
    	else
            throw new anondnsException("Wrong password, While unlocking followers' certificates");
        this.dns=new DynDNS(this.prof,this.crypto,this.ks,this.followingsList);
    }
        
    public void newFollower(String followersTelNum, X509Certificate cert) throws KeyStoreException, NoSuchAlgorithmException, CertificateException, IOException, InvalidKeyException, IllegalBlockSizeException, BadPaddingException, NoSuchPaddingException, InvalidAlgorithmParameterException, NoSuchProviderException, SignatureException{
    	String token=cryptoFunc.makeToken(followersTelNum);
    	String subdomain=token+"."+this.prof.domain;
    	this.userToName.put(subdomain,cert.getSubjectDN().getName());
    	DNSRecord rec=new DNSRecord(subdomain,followersTelNum,null,-1);
        this.followersList.put(subdomain,rec);
        this.ks.setCertificateEntry(subdomain, cert);
        this.prof.zone_id=this.dns.createRecord(rec);
        saveCertificates();
        this.crypto.encrBeforeStore(this.followersList,1);
        this.crypto.encrBeforeStore(this.userToName,4);
    }
    
    public Boolean newFollowing(String name,String hisdomain,String userTel,X509Certificate cert) throws IOException, NoSuchAlgorithmException, UnrecoverableEntryException, KeyStoreException, NoSuchPaddingException, InvalidKeyException, IllegalBlockSizeException, BadPaddingException, NoSuchProviderException, InvalidAlgorithmParameterException, DecoderException, CertificateException {
    	String token=cryptoFunc.makeToken(this.prof.telNo);
    	String subdomain=token+"."+hisdomain;
        String encIP = this.dns.getDNSRecord(subdomain,false);
        if(encIP==null){
            System.err.println("Cannot find "+subdomain+" record. New Following process aborted.");
            return false;
        }
        this.ks.setCertificateEntry(subdomain, cert);
        saveCertificates();
    	this.followingsList.put(subdomain,new DNSRecord(subdomain,userTel,null,-1)); 
        this.followingsList=this.dns.getFollowingsList();
        String ip = this.crypto.decryptIP(encIP, cert.getPublicKey());
        System.out.println("Your friend's "+cert.getSubjectX500Principal().getName().split("=")[1]+" IP is "+ip);
        this.followingsList.get(subdomain).setCurrentIpAddr(ip);
        this.userToName.put(subdomain,name);
        this.crypto.encrBeforeStore(this.followingsList,2);
        this.crypto.encrBeforeStore(this.userToName,4);
        return true;
    }
    
    private void saveCertificates() throws KeyStoreException, NoSuchAlgorithmException, CertificateException, IOException 
    {        
        FileOutputStream fos = null;
        String filename=this.crypto.getFilename(3);
        try {
            fos = new FileOutputStream(filename);
            this.ks.store(fos, this.prof.myPass);
        } catch (FileNotFoundException e) {
            System.err.println("Warning! Could not save Certificates to "+filename+" correctly!");
        }
        if (fos != null) {
            fos.close();
        }
    }
    
    private KeyStore retrieveCertificates(char[] myPass) throws IOException, NoSuchAlgorithmException, CertificateException, KeyStoreException {
    	KeyStore ks = KeyStore.getInstance(KeyStore.getDefaultType());//KeyStore.getInstance("AndroidKeyStore");
        String filename=this.crypto.getFilename(3);
        FileInputStream fis = null;
        try {
            fis = new FileInputStream(filename);
            ks.load(fis,myPass);
        } catch (FileNotFoundException ex) {
            System.out.println("Warning: No file "+filename+" to load");
            ks.load(null);
            if(fis!=null)
            	fis.close();
            return ks;
        }catch(IOException e){  //Wrong password
            if(fis!=null)
                fis.close();
            return null;
        }
        fis.close();
        System.out.println("Followers' Certificates list have been loaded succesfully");
        return ks;
    }
    
    //delete cert, name bind and stored record  
    public void deleteFollower(String telnum) throws IOException, IllegalBlockSizeException, BadPaddingException, NoSuchAlgorithmException, NoSuchPaddingException, InvalidKeyException, KeyStoreException, InvalidAlgorithmParameterException, SignatureException, NoSuchProviderException{
        DNSRecord todelete = searchFriend(this.followersList, telnum);
        if (todelete==null){
            System.err.println("Error: Follower was not found!");
            return;
        }
        this.dns.updateDNSRecord(todelete,utilities.randomIP());
        (new Thread(new Deleter(this.dns,todelete,this.N))).start();//delete record after N seconds
        this.followersList.remove(todelete.getSubdomain());
        this.userToName.remove(todelete.getSubdomain());
        this.ks.deleteEntry(todelete.getSubdomain());
        this.crypto.encrBeforeStore(this.followersList,1);
        this.crypto.encrBeforeStore(this.userToName,4);
        System.out.println("Follower has been deleted!");
    }
    
    //delete cert, name bind and stored record
    public void unfollow(String telnum) throws IOException, KeyStoreException, InvalidKeyException, IllegalBlockSizeException, BadPaddingException, NoSuchAlgorithmException, NoSuchPaddingException, InvalidAlgorithmParameterException{
        DNSRecord todelete = searchFriend(this.followingsList, telnum);
        if (todelete==null){
            System.err.println("Error: Followed User not found!");
            return;
        }
        this.followingsList.remove(todelete.getSubdomain());
        this.userToName.remove(todelete.getSubdomain());
        this.ks.deleteEntry(todelete.getSubdomain());
        this.crypto.encrBeforeStore(this.followingsList,2);
        this.crypto.encrBeforeStore(this.userToName,4);
        System.out.println("User has been unfollowed!");
    }
      
    private HashMap <String,DNSRecord> retrieveFriendsList(int type) throws InvalidKeyException, ClassNotFoundException, NoSuchAlgorithmException, NoSuchProviderException, NoSuchPaddingException, IllegalBlockSizeException, BadPaddingException, InvalidAlgorithmParameterException, IOException{
    	String filename=this.crypto.getFilename(type);
    	byte[] cipherTxt=utilities.loadFromFile(filename);
    	HashMap<String, DNSRecord> ret=null;
        if(cipherTxt!=null)
        {
            byte[] plaintext = this.crypto.decryptAES(cipherTxt,type);
            if(plaintext==null)
                return new HashMap<String, DNSRecord>();
            ret = (HashMap<String, DNSRecord>)utilities.toHashMap(plaintext);
            if(ret==null)
            {
                System.err.println(filename+" file found but could not load content");
                return new HashMap<String, DNSRecord>();
            }
            else
                System.out.println(filename+" have been loaded succesfully");
	    }
        else
        {
            ret=new HashMap<String, DNSRecord>();
            System.out.println("Warning: No file "+filename+" to load");
        }
        return ret;
    }
    
    private HashMap <String,String> retrieveUsersList() throws IOException, InvalidKeyException, ClassNotFoundException, NoSuchAlgorithmException, NoSuchProviderException, NoSuchPaddingException, IllegalBlockSizeException, BadPaddingException, InvalidAlgorithmParameterException
    {
    	String filename=this.crypto.getFilename(4);
    	byte[] cipherTxt=utilities.loadFromFile(filename);
    	HashMap<String, String> ret=null;
        if(cipherTxt!=null)
        {
            byte[] plaintext=this.crypto.decryptAES(cipherTxt,4);
            if(plaintext==null)
                return new HashMap<String, String>();
            ret = (HashMap<String, String>)utilities.toHashMap(plaintext);
            if(ret==null)
            {
                System.err.println(filename+" file found but could not load content");
                return new HashMap<String, String>();
            }
            else
                System.out.println(filename+" have been loaded succesfully");
	    }
        else
        {
            ret=new HashMap<String, String>();
            System.out.println("Warning: No file "+filename+" to load");
        }
        return ret;
    }
    
    public void showContacts(int which)
    {
        if ((which==1)||(which==0)){
            System.out.println("------------------------- Followers  -------------------------\n");
            utilities.printList(this.followersList,this.userToName,1);
        }
        if ((which==2) || (which==0)){
            System.out.println("------------------------- Followings -------------------------\n");
            utilities.printList(this.followingsList,this.userToName,2);
        }
    }

    public Boolean updateFollowerRecordsTimer() throws UnknownHostException, TextParseException, NoSuchAlgorithmException, UnrecoverableEntryException, KeyStoreException, InvalidKeyException, IllegalBlockSizeException, BadPaddingException, NoSuchPaddingException, SignatureException, NoSuchProviderException, IOException, InvalidAlgorithmParameterException, DecoderException{
        if(!this.followersList.isEmpty()){
            String oldIP=searchFriend(this.followersList,null).getCurrentIpAddr().getHostAddress();
            String testSubjectUsername=searchFriend(this.followersList,null).getSubdomain();
            long startTime = System.currentTimeMillis();   
            Boolean ret=updateFollowerRecords();
            long stoptTime = System.currentTimeMillis();
            System.out.println("IP update time "+(stoptTime-startTime)+" ms");
   /*         if(ret)
            {
                String currentIP="";
                startTime = System.nanoTime();   
                do  //Wait untill the first friendrecord is updated
                {
                    // Thread.sleep(1000);
                    currentIP=dns.getDNSRecord(testSubjectUsername,false);
                }
                while (!utilities.whatsMyIP().equals(currentIP));
                this.followingsList=this.dns.getFollowingsList();
                stoptTime = System.nanoTime();
                System.out.println("IP change propagation time "+(stoptTime-startTime)/(double)1000000+" ms");
                System.out.println("Host "+testSubjectUsername+" changed from "+oldIP+" to "+ currentIP);
            }*/
            //System.exit(43);
            return ret;
        }
        else
            return false;
    }
           
    public boolean updateFollowerRecords() throws UnknownHostException, NoSuchAlgorithmException, UnrecoverableEntryException, KeyStoreException, InvalidKeyException, IllegalBlockSizeException, BadPaddingException, SignatureException, NoSuchPaddingException, NoSuchProviderException, IOException, InvalidAlgorithmParameterException, DecoderException 
    {
    	System.out.println("Updating the followers' records...");
        DNSRecord recToChange;
        Boolean changeNeeded=false;
    	Collection<DNSRecord> c = this.followersList.values();
    	Iterator<DNSRecord> traverse = c.iterator();
        while(traverse.hasNext())
        {    
            recToChange = (DNSRecord) traverse.next();
           /* String ip=this.dns.getDNSRecord(recToChange.getSubdomain(),false);
            this.followingsList=this.dns.getFollowingsList();
            System.out.println(ip+" "+recToChange.getCurrentIpAddr().getHostAddress());
            if(!ip.equals(recToChange.getCurrentIpAddr().getHostAddress())) //check if the ip at the DNS needs update*/
            {    
            	changeNeeded=true;
            	while(!dns.updateDNSRecord(recToChange,null))
                    System.err.println("DNS Record update failed. Trying again...");
            }
        }
        if(changeNeeded)
        	this.crypto.encrBeforeStore(this.followersList,1);
        return changeNeeded;
    }

    public DNSRecord getFollowing(String sendTo) {
        return this.followingsList.get(sendTo);
    }
    
    public HashMap <String,DNSRecord>  getFollowingList(){
        return this.followingsList;
    }
                        
    public boolean updateFollowings() throws UnknownHostException, NoSuchAlgorithmException, UnrecoverableEntryException, KeyStoreException, InvalidKeyException, IllegalBlockSizeException, BadPaddingException, NoSuchPaddingException, NoSuchProviderException, DecoderException 
    {
        DNSRecord recToGet;
    	Collection<DNSRecord> c = this.followingsList.values();
    	Iterator<DNSRecord> traverse = c.iterator();
        int count=0;
        while(traverse.hasNext())
        {    
            recToGet = (DNSRecord) traverse.next();
            String ip=this.dns.getDNSRecord(recToGet.getSubdomain(),true);
            if(ip==null)
                System.err.println("Cannot find "+recToGet.getSubdomain()+" record");
            else
                out.println("Record: ");//+recToGet.getSubdomain()+"\n of TelNum"+recToGet.getTel_num()+" has new IP: "+ip); 
            count++;
        }
        this.followingsList=this.dns.getFollowingsList();
        out.println(count+" followed Users now are up to date!"); 
        return true;
    }

    public String fromFollowingIPtoName(String hostAddress){
        return utilities.fromIPtoName(this.followingsList,this.userToName,hostAddress);
    }
    
    public String fromFollowingsIPtoName(String hostAddress){
        String receiverName=utilities.fromIPtoName(this.followingsList,this.userToName,hostAddress);
        if (receiverName.contains("="))
            receiverName=receiverName.split("=")[1];
        return receiverName;
    }
    
    public static DNSRecord searchFriend(HashMap <String,DNSRecord> h,String key)
    {
        DNSRecord reg;
    	Collection<DNSRecord> c = h.values();
    	Iterator<DNSRecord> traverse = c.iterator();
        while(traverse.hasNext())
        {    
            reg = (DNSRecord) traverse.next();
            if (key==null)
                return reg;
            else if (key.equals(reg.getCurrentIpAddr().getHostAddress()))
                return reg;
            else if (key.equals(reg.getTel_num()))
            	return reg;
            else
            	;
        }
        return null;
    }

    public long NumOfFollowings() {
        return this.followingsList.size();
    }

    void setOut(PrintWriter out) {
        this.out=out;
    }
}

class Deleter implements Runnable {
    private final DynDNS dns;
    private final DNSRecord toDelete;
    private final long N;

    public Deleter(DynDNS ddns,DNSRecord todelete,long delay){
        this.dns=ddns;
        this.toDelete=todelete;
        this.N=delay;
    }
	
    public void run() {
    	Random randomGenerator = new Random();
    	int randomInt = randomGenerator.nextInt(10000);
        try {
            Thread.sleep(this.N+randomInt);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        this.dns.deleteRecord(this.toDelete.getId());
     }
}
