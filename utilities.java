import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.InetAddress;
import java.net.MalformedURLException;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collection;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Random;
import java.util.Set;

public class utilities {
	
    private static final String path="./";
	
    public static String whatsMyIP() {
        String ip = null;
        try {
            Enumeration<NetworkInterface> interfaces = NetworkInterface.getNetworkInterfaces();
            while (interfaces.hasMoreElements()) {
                NetworkInterface iface = interfaces.nextElement();
                // filters out 127.0.0.1 and inactive interfaces
                if (iface.isLoopback() || !iface.isUp())
                    continue;

                Enumeration<InetAddress> addresses = iface.getInetAddresses();
                while(addresses.hasMoreElements()) {
                    InetAddress addr = addresses.nextElement();
                    ip = addr.getHostAddress();
                }
            }
        } catch (SocketException e) {
            throw new RuntimeException(e);
        }
        return ip;
    }
    
	public static String fromIPtoName(HashMap <String,DNSRecord> h,HashMap <String,String> names,String hostAddress) {
        Set<String> items = (Set<String>) h.keySet();
        for(String key:items){
            if(h.get(key).getCurrentIpAddr().getHostAddress().equals(hostAddress))
            return names.get(key);
        }
        return null;
    } 
    
    public static int checkDir(String username,boolean createIfneeded){
    	File f=new File(path+username+"/");
        if (f.exists()||f.isDirectory()) return 0; 
    	if (createIfneeded && ((!f.exists())||(!f.isDirectory()))) {
            f.mkdir();
            System.out.println("profile directory for "+username+" has been created succesfully");
    	}
    	return 1;
    }
    
    public static byte[] concatBytearrays(byte[] a,byte[] b){
    	byte[] c = new byte[a.length + b.length];
    	System.arraycopy(a, 0, c, 0, a.length);
    	System.arraycopy(b, 0, c, a.length, b.length);
    	return c;
    }
    
    public static void printList(HashMap <String,DNSRecord> list,HashMap <String,String> names,int type)
    {
    	DNSRecord reg=null;
    	Collection<DNSRecord> c = list.values();
    	Iterator<DNSRecord> itr = c.iterator();
        while(itr.hasNext())
        {    
            reg = (DNSRecord)itr.next();
            String name=names.get(reg.getSubdomain());
            if (name.contains("="))
                    name=name.split("=")[1];
            System.out.println("Name: "+name+
            		"\nToken: "+reg.getSubdomain());
            if (type==2)
                System.out.println("IP: "+reg.getCurrentIpAddr().getHostName());
            System.out.println("Tel. Num.: "+reg.getTel_num());
        }
    }
    
    public static String getInput(String msg) throws IOException
    {
    	System.out.print(msg+"\n> ");
    	BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
        String input=br.readLine();
        return input;
    }
    
    public static Boolean saveToFile(String filename, byte[] data)throws IOException {
        Path path = Paths.get(filename);
        Files.write(path, data); //creates, overwrites
        return true;
    }
    
    public static byte[] loadFromFile(String filename) throws IOException{
    	if((new File(filename)).exists())
    	{
    		Path path = Paths.get(filename);
        	return Files.readAllBytes(path);
    	}
    	else
    		return null;
    }
    
    public static byte[] toByteArray(Object obj) throws IOException {
        byte[] bytes = null;
        ByteArrayOutputStream bos = null;
        ObjectOutputStream oos = null;
        try {
            bos = new ByteArrayOutputStream();
            oos = new ObjectOutputStream(bos);
            oos.writeObject(obj);
            oos.flush();
            bytes = bos.toByteArray();
        } finally {
            if (oos != null) {
                oos.close();
            }
            if (bos != null) {
                bos.close();
            }
        }
        return bytes;
    }

    public static HashMap toHashMap(byte[] bytes) throws IOException, ClassNotFoundException {
        Object obj = null;
        ByteArrayInputStream bis = null;
        ObjectInputStream ois = null;
        try {
            bis = new ByteArrayInputStream(bytes);
            ois = new ObjectInputStream(bis);
            obj = ois.readObject();
        } finally {
            if (bis != null) {
                bis.close();
            }
            if (ois != null) {
                ois.close();
            }
        }
        return (HashMap)obj;
    }
    
    public static ProfileInfo toProfileInfo(byte[] bytes) throws IOException, ClassNotFoundException {
        Object obj = null;
        ByteArrayInputStream bis = null;
        ObjectInputStream ois = null;
        try {
            bis = new ByteArrayInputStream(bytes);
            ois = new ObjectInputStream(bis);
            obj = ois.readObject();
        } finally {
            if (bis != null) {
                bis.close();
            }
            if (ois != null) {
                ois.close();
            }
        }
        return (ProfileInfo)obj;
    }

    public static String extractTXTresult(List<String>strings) {
            String result="";
            for (String str:strings)
                    result+=str;
            return result;
    }

    public static int randomPort(){
        Random r = new Random();
	return r.nextInt(65536);
    }

    public static String randomIP() {
            Random r = new Random();
            return r.nextInt(256) + "." + r.nextInt(256) + "." + r.nextInt(256) + "." + r.nextInt(256);
    }
}
