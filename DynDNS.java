import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.UnknownHostException;
import java.security.InvalidKeyException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.SignatureException;
import java.security.UnrecoverableEntryException;
import java.security.cert.Certificate;
import java.util.HashMap;

import javax.crypto.BadPaddingException;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;

import org.apache.commons.codec.DecoderException;
import org.bouncycastle.util.encoders.Base64;
import org.xbill.DNS.Lookup;
import org.xbill.DNS.Record;
import org.xbill.DNS.TXTRecord;
import org.xbill.DNS.TextParseException;
import org.xbill.DNS.Type;

public class DynDNS {
	private final ProfileInfo prof;
	private final cryptoFunc crypto;
	private final KeyStore ks;
	private final HashMap <String,DNSRecord>  followingsList;
	
    public DynDNS(ProfileInfo pr,cryptoFunc cr,KeyStore certs,HashMap <String,DNSRecord> flist){
        this.prof=pr;
        this.crypto=cr;
        this.ks=certs;
        this.followingsList=flist;
    }

    public HashMap <String,DNSRecord> getFollowingsList()
    {
        return this.followingsList;
    }
	
    public boolean deleteRecord(Long recordID){	
    	HttpURLConnection connection=null;
    	try {
        // Connect to DynDNS
            connection =initializeHttp(getURL(this.prof.domain,recordID),"DELETE");
            OutputStream os = connection.getOutputStream();
            String update="";
            os.write(update.getBytes());
            os.flush();

         // Get Response
            int responseCode = connection.getResponseCode();
            System.out.println(responseCode + ":" + connection.getResponseMessage());
            BufferedReader br = new BufferedReader(new InputStreamReader((connection.getInputStream())));

         //Print response
            String output;
            while ((output = br.readLine()) != null) {
                if ((responseCode>=200)&&(responseCode<=202)) //succesfull request
                {
                    String str=output.split("status")[1].split(":")[1].split("}")[0].replace("\"","");	
                    if (str=="OK")
                        return true;
                }
                else
                    return false;
            }
            return false;
        } 
        catch (IOException ex) {
            ex.printStackTrace();
        }
        finally {
            if(connection != null) {
                connection.disconnect(); 
            }
        }
    	return false;
    }
	
    private URL getURL(String zone,long recordID) throws MalformedURLException
    {
    	//System.out.println("URL: https://pointhq.com/zones/"+zone+"/records/"+recordID);
    	if(zone!=null){
            if (recordID==-1)
                    return new URL("https://pointhq.com/zones/"+zone+"/records/");
            else if (recordID==-2)
                    return new URL("https://pointhq.com/zones/"+zone);
            else
                    return new URL("https://pointhq.com/zones/"+zone+"/records/"+recordID);
    	}
    	else
            return new URL("https://pointhq.com/zones/");
    }
    
    
    public String getZone(String zone) throws MalformedURLException
    {
    	return pointdnsGET(getURL(zone,-2));
    }
  
    public String getZones() throws MalformedURLException
    {
    	return pointdnsGET(getURL(null,-1));
    }
    
    public String getZoneRecords(String zone) throws MalformedURLException
    {
    	return pointdnsGET(getURL(zone,-1));
    }
    
    public String getRecord(String zone,Long recordID) throws MalformedURLException
    {
    	return pointdnsGET(getURL(zone,recordID));
    }
    
    public boolean updateDNSRecord(DNSRecord recToChange, String randomIP) throws InvalidKeyException, SignatureException, NoSuchPaddingException, NoSuchAlgorithmException, IllegalBlockSizeException, BadPaddingException, NoSuchProviderException
    {
        HttpURLConnection connection=null;
        try {
        // Connect to DynDNS
            connection = initializeHttp(getURL(this.prof.domain,recToChange.getId()),"PUT");
            OutputStream os = connection.getOutputStream();
            String mycurrentIP=null;
            if(randomIP==null)
            	mycurrentIP=utilities.whatsMyIP();
            else
            	mycurrentIP=randomIP;
            String update="{\"zone_record\":{\"data\":\""+this.crypto.encryptMyIP(this.ks.getCertificate(recToChange.getSubdomain()).getPublicKey())+"\"}}";
            os.write(update.getBytes());
            os.flush();
            long starttime=System.nanoTime();

       //Print response
            int responseCode = connection.getResponseCode();
            if ((responseCode>=200)&&(responseCode<=202)) //succesfull request
                recToChange.setCurrentIpAddr(mycurrentIP);//set my currentIP
            else
                System.err.println(responseCode+" "+connection.getResponseMessage());	
            long stoptime=System.nanoTime();
            System.out.println("Update reg "+recToChange.getSubdomain()+" Time: "+(stoptime-starttime)/(double)1000000+" ms");
            return true;
        } 
        catch (IOException | KeyStoreException ex) {
            ex.printStackTrace();
        }
        finally {
            if(connection != null) {
                connection.disconnect(); 
            }
        }
        return false;
    }
    
    private HttpURLConnection initializeHttp(URL url, String Method) throws IOException
    {
    // Connect
    	String credentials=this.prof.username+":"+this.prof.apiKey;
    	HttpURLConnection connection = (HttpURLConnection)url.openConnection();
        String encodedAuthorization = Base64.toBase64String(credentials.getBytes());
        connection.setRequestProperty("Authorization", "Basic "+encodedAuthorization);

    //HTTP Headers
        connection.setRequestMethod(Method);
        connection.setRequestProperty("User-Agent", "anonDNS client");
        connection.setRequestProperty("Accept", "application/json");
        connection.setRequestProperty("Content-type","application/json");

    //PUT parameters
        connection.setDoInput(true);
        connection.setDoOutput(true);
        connection.connect();
    	return connection;
    }
    
    public long createRecord(DNSRecord recToCreate) {
    	HttpURLConnection connection=null;
        String ttl="0";
        String type="TXT";
        try {
            connection=initializeHttp(getURL(this.prof.domain,-1),"POST");
            OutputStream os = connection.getOutputStream();
            String mycurrIP=utilities.whatsMyIP();
            String encIP;
            try{
                encIP=this.crypto.encryptMyIP(this.ks.getCertificate(recToCreate.getSubdomain()).getPublicKey());
            }catch(NullPointerException e) {
                System.out.println("Could not found follower's certificate");
                return -1;
            }
            String update="{\"zone_record\":{\"name\":\""+recToCreate.getSubdomain().split("\\.")[0]+"\",\"record_type\":\""+type+"\",\"data\":\""+encIP+"\",\"ttl\":\""+ttl+"\"}}";
            os.write(update.getBytes());
            os.flush();
        // Get Response
            int responseCode = connection.getResponseCode();
            System.out.println(responseCode + ":" + connection.getResponseMessage());
            if (responseCode==422){
                    System.out.println("Error: Your PointHQ quota is Full");
                    return -1;
            }
            BufferedReader br = new BufferedReader(new InputStreamReader((connection.getInputStream())));
            String zone_id=null;
        //Print response
            String output;
            while ((output = br.readLine()) != null) {
            	if((responseCode>=200)&&(responseCode<=202))
            	{
                //    System.out.println("OUTPUT: "+output);
                    String id=output.split("id")[1].split(":")[1].split(",")[0].replace("\"","");
                    recToCreate.setCurrentIpAddr(mycurrIP);
                    recToCreate.setId(Long.parseLong(id));
                    System.out.println("New record\n-----------\nDomain: "+recToCreate.getSubdomain()+"\nRecordID: "+recToCreate.getId()+"\nIP: "+recToCreate.getCurrentIpAddr().getHostAddress()+"\nenc(IP): "+encIP);
                    zone_id=output.split("zone_id")[1].split(":")[1].split("}")[0].replace("\"",""); 
            	}
            	else
            	{
                    System.err.println(responseCode+" "+connection.getResponseMessage());
                    return -1;
            	}
            }
            return Long.parseLong(zone_id);
        } 
        catch (Exception ex) {
            ex.printStackTrace(); 
        }
        finally {
            if(connection != null) {
                connection.disconnect(); 
            }        
        }
        return -1;
    }
    
    private String pointdnsGET(URL url)
    {
        HttpURLConnection connection=null;
        try{
       // Connect to DynDNS
            connection = initializeHttp(url,"GET");
        // Get Response
            int responseCode = connection.getResponseCode();
            System.out.println(responseCode + ":" + connection.getResponseMessage());
            BufferedReader br = new BufferedReader(new InputStreamReader((connection.getInputStream())));

        //Print response
            String  str="";
            String output;
            while ((output = br.readLine()) != null) {
            if ((responseCode>=200)&&(responseCode<=202)) //succesfull request
                    str+=output;
            }
            return str;
        } 
        catch (Exception ex) {
            ex.printStackTrace();
        }
        finally {
            if(connection != null) {
                connection.disconnect(); 
            } 
        }
	    return null;
    }
    
    
    public String getDNSRecord(String subdomain,boolean update) throws NoSuchAlgorithmException, UnrecoverableEntryException, KeyStoreException, NoSuchPaddingException, InvalidKeyException, IllegalBlockSizeException, BadPaddingException, NoSuchProviderException, DecoderException
    {
        Record [] records;
        try {
            records = new Lookup(subdomain, Type.TXT).run();
        } catch (TextParseException ex) {
             ex.printStackTrace();
            return null;
        }
        if (records==null)   
            return null;
        TXTRecord rec=null;
        for (Record record : records)	
        	rec = (TXTRecord) record;
    	Certificate certEntry = this.ks.getCertificate(subdomain);
        String ip=utilities.extractTXTresult(rec.getStrings());
        if(this.followingsList.get(subdomain)!=null)
        {
            String hisCurrentIP=this.followingsList.get(subdomain).getCurrentIpAddr().getHostAddress();
            ip=this.crypto.decryptIP(ip,certEntry.getPublicKey());
//            System.out.println("New IP of Following: "+ip);
            if((hisCurrentIP==null)||((update)&&(!hisCurrentIP.equals(ip))))
            {    
                //System.out.println("DNS lookup time "+(stoptTime-startTime)/(double)1000000+" ms");
                try {
                    this.followingsList.get(subdomain).setCurrentIpAddr(ip);
                } catch (UnknownHostException ex) {
                    ex.printStackTrace();
                    return null;
                }
                System.out.println("Update was needed from "+hisCurrentIP+" to "+ip+" with TTL "+rec.getTTL());
            }
        }
        return ip;
    }
}
