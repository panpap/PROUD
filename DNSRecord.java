import java.io.Serializable;
import java.net.InetAddress;
import java.net.UnknownHostException;

public class DNSRecord implements Serializable{

	private static final long serialVersionUID = 1L;
	private String subdomain;
    private String tel_num;
    private InetAddress ipAddr;
    private long recordID;

    public DNSRecord(String subd,String tel,String ip,long id) throws UnknownHostException
    {
        this.subdomain=subd;
        this.tel_num=tel;
    	if (ip==null)
    		this.ipAddr = InetAddress.getByName(utilities.whatsMyIP());
    	else
    		this.ipAddr = InetAddress.getByName(ip);
    	this.recordID=id;
    }      
    
    public InetAddress getCurrentIpAddr() {
        return this.ipAddr;
    }
    
    public void setCurrentIpAddr(String ip) throws UnknownHostException {
    	if (ip==null)
    		this.ipAddr = InetAddress.getByName(utilities.whatsMyIP());
    	else
    		this.ipAddr = InetAddress.getByName(ip);
    }

    public long getId() {
        return this.recordID;
    }

    public void setId(long id){
        this.recordID = id;
    }
    
    public String getSubdomain() {
        return this.subdomain;
    }

    public void setSubdomain(String subdomain) {
        this.subdomain = subdomain;
    }

    public String getTel_num() {
        return this.tel_num;
    }

    public void setTel_num(String tel_num) {
        this.tel_num = tel_num;
    }
}
