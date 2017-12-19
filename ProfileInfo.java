import java.io.Serializable;
import java.security.KeyPair;
import java.security.cert.X509Certificate;

public class ProfileInfo implements Serializable{

    private static final long serialVersionUID = 1L;
    public final String username;
    public final char[] myPass;
    public final String apiKey;
    public final String domain;
    public final X509Certificate mycert;
    public final KeyPair keypair;
    public long zone_id;
    public final String telNo;

    public ProfileInfo(String user, String dom,String telNo ,String apik, String pswd, KeyPair kp,X509Certificate cert){
    	this.myPass = pswd.toCharArray();
        this.username=user;
        this.apiKey=apik;
        this.domain=dom;
        this.telNo=telNo;
        this.mycert=cert;
        this.keypair=kp;
        this.zone_id=-1;
    }
}