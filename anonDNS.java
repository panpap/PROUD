import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.SignatureException;
import java.security.UnrecoverableEntryException;
import java.security.cert.CertificateEncodingException;
import java.security.cert.CertificateException;
import javax.crypto.BadPaddingException;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;

import org.apache.commons.codec.DecoderException;

public class anonDNS{ 
	private static appFunc app;
    public static void main(String[] args) throws NumberFormatException, InvalidKeyException, NoSuchAlgorithmException, UnrecoverableEntryException, KeyStoreException, IllegalBlockSizeException, NoSuchPaddingException, BadPaddingException, NoSuchProviderException, SignatureException, InvalidAlgorithmParameterException, CertificateEncodingException, CertificateException, IllegalStateException, DecoderException, IOException 
    {
    	app=new appFunc();
    	int c = 0;
    	Boolean res=false;
            BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
    	do{
            _printMenu(1);
            try{
            c=Integer.parseInt(br.readLine());
            }catch(NumberFormatException e){continue;}
            if(c==1)
                res=app.login();//login and retrieve profile data
            else if((c<1)||(c>3))
            {
                System.out.println("Wrong choice!");
                res=false;
            }
            else if(c==2)
                res=app.register();//signup and create profile data
            else 
                System.exit(1);
    	}
    	while(!res);
        app.updateFollowerRecordsTimer(); //update records for followers
        br = new BufferedReader(new InputStreamReader(System.in));
        do{
            _printMenu(2);
            try{
            c=Integer.parseInt(br.readLine());
            }catch(NumberFormatException e){continue;}
            if(c==1)
                app.showContacts(0);
            else if(c==2){
                app.addFollower();
                //_populateApp();
            }
            else if(c==3){
                app.newFollowing();
            }
            else if(c==4){
                app.showContacts(2);
                app.senderThread();
            }
            else if(c==5){
                app.deleteFollower();
            }
            else if(c==6)
                 app.unfollow();
            else if((c<1)||(c>7))
                System.out.println("Wrong choice!");
            else{
                System.out.println("Application is closing...");
                app.close();
            }
        }
        while(c!=7);
    }
    
    private static void _printMenu(int c){
    	if(c==1)
            System.out.print("\nChoose from menu:\n1. Login\n2. Sign up\n3. Exit\n> ");
    	else
            System.out.print("\nChoose from menu:\n1. Show Contacts\n2. Add New Follower\n3. Follow User\n4. Send Message to Followed User\n5. Delete Follower\n6. Unfollow User\n7. Exit\n> ");
    }
    
    private static void _populateApp() throws CertificateEncodingException, InvalidKeyException, KeyStoreException, NoSuchAlgorithmException, CertificateException, IllegalStateException, NoSuchProviderException, SignatureException, IOException, UnrecoverableEntryException, NoSuchPaddingException, BadPaddingException, IllegalBlockSizeException, InvalidAlgorithmParameterException, DecoderException{
    	//followers
    	long start = System.currentTimeMillis();
    	app.newFollower("6977111111",cryptoFunc.createCert(cryptoFunc.generateAsymmetricKeys(), "Andy Warhol"));
    	app.newFollower("6977222222",cryptoFunc.createCert(cryptoFunc.generateAsymmetricKeys(), "Abraham Lincoln"));
    	app.newFollower("6977444444",cryptoFunc.createCert(cryptoFunc.generateAsymmetricKeys(), "Ernesto Guevarra"));
    	long end = System.currentTimeMillis();
    	System.out.println("Addition of 1 Followers cost "+(end-start)+" ms");
    }      
} 