import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.math.BigInteger;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.PublicKey;
import java.security.SecureRandom;
import java.security.Security;
import java.security.SignatureException;
import java.security.cert.CertificateEncodingException;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.HashMap;
import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKey;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import javax.security.auth.x500.X500Principal;
import org.apache.commons.codec.DecoderException;
import org.apache.commons.codec.binary.Hex;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.util.encoders.Base64;
import org.bouncycastle.x509.X509V3CertificateGenerator;

public class cryptoFunc {
    
    private static final String PROVIDER = BouncyCastleProvider.PROVIDER_NAME;
    private ProfileInfo prof;
    private final SecretKey myKey;
    private final HashMap<Integer,byte[]>ivTable;
    private final static String[] filesPostfix={"_followers.anondns","_followings.anondns","_certLists.anondns","_userBinds.anondns","_profileinfo.anondns","_myIV.anondns","_myCert.cert"};
    

    public cryptoFunc(String username,String pswd) throws anondnsException, InvalidKeyException, ClassNotFoundException, NoSuchAlgorithmException, NoSuchProviderException, NoSuchPaddingException, IllegalBlockSizeException, BadPaddingException, InvalidAlgorithmParameterException, IOException
    {
    	Security.addProvider(new org.bouncycastle.jce.provider.BouncyCastleProvider());
    	if(utilities.checkDir(username,false)!=0)
            throw new anondnsException("There is no such dir");
        this.prof=new ProfileInfo(username, null ,null ,null, pswd, null,null);
    	this.myKey=makeKey(String.valueOf(pswd));
    	this.ivTable=loadIVTable();
    	ProfileInfo pr = loadProfileInfo(username);
    	if(pr==null)
            throw new anondnsException("Could not load profile infos correctly");
    	else
            this.prof=pr;
    }
    
    public cryptoFunc(String username, String pswd, String telNo,String domain, String apikey) throws anondnsException, InvalidKeyException, IllegalBlockSizeException, BadPaddingException, NoSuchAlgorithmException, NoSuchPaddingException, InvalidAlgorithmParameterException, IOException, CertificateEncodingException, IllegalStateException, NoSuchProviderException, SignatureException {
    	Security.addProvider(new org.bouncycastle.jce.provider.BouncyCastleProvider());
    	if(utilities.checkDir(username,true)!=1)
            throw new anondnsException("Could not make dir");	
    	this.myKey=makeKey(String.valueOf(pswd));
        KeyPair keypair = generateAsymmetricKeys();
    	X509Certificate mycert= createCert(keypair,username);
        this.prof=new ProfileInfo(username, domain,telNo ,apikey, pswd, keypair,mycert); 
        saveCertToFile(mycert,getFilename(7));
    	this.ivTable=new HashMap<Integer,byte[]>();
    	if(!saveProfileInfo(username,this.prof))
            throw new anondnsException("Could not save profile infos");	
    }
  
    public ProfileInfo getProf(){
    	return this.prof;
    }
    
    public String decryptIP(String response,PublicKey friendsPub) throws NoSuchAlgorithmException, DecoderException,NoSuchProviderException, NoSuchPaddingException, InvalidKeyException, IllegalBlockSizeException, BadPaddingException {
    	byte[] ciphertext=Hex.decodeHex(response.toCharArray());
        //decrypt
        byte[] plaintxt=decryptRSA(ciphertext);
        //verify signature
        byte[] ip=verifyData(plaintxt,friendsPub);
    	return new String(ip);
    }
    
    public String encryptMyIP(PublicKey friendsPub) throws InvalidKeyException, IllegalBlockSizeException, BadPaddingException, NoSuchAlgorithmException, NoSuchPaddingException, SignatureException, NoSuchProviderException{
        String ip = utilities.whatsMyIP();
        byte[] msg=ip.getBytes();
        //sign
        byte[] plaintext=signData(msg);  
        //encrypt    
        byte[] cipherText=encryptRSA(plaintext,friendsPub);
        return Hex.encodeHexString(cipherText);
    }   
    
    /*
    ------------------------------------------------------------------------------------
        ASYMMETRIC CRYPTOGRAPHIC FUNCTIONS
    RSA encrypts only an IP address=4 bytes
    one cryptographic block is 8 bytes so ECB mode with no IV works fine!
    */
    
    public static String makeToken(String followersTelNum) throws NoSuchAlgorithmException, UnsupportedEncodingException
    {
    	MessageDigest md = MessageDigest.getInstance("SHA-1");
    	md.update(followersTelNum.getBytes("UTF-8")); // Change this to "UTF-16" if needed
    	byte[] digest = md.digest();
    	return Hex.encodeHexString(digest);
    }
    
    public static KeyPair generateAsymmetricKeys() throws NoSuchAlgorithmException, NoSuchProviderException
    {
    	Security.addProvider(new BouncyCastleProvider());
    	KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance("RSA", PROVIDER);
        keyPairGenerator.initialize(1024, new SecureRandom());       
        return keyPairGenerator.generateKeyPair();
    }    
    
    /*public static KeyPair generateAsymmetricKeys(String alias) throws NoSuchAlgorithmException, NoSuchProviderException
    {
    	KeyPairGenerator kpg = KeyPairGenerator.getInstance("RSA", "AndroidKeyStore");
    	Calendar expiryCal=new GregorianCalendar();
    	expiryCal.set(Calendar.YEAR, expiryCal.get(Calendar.YEAR)+1);
    	kpg.initialize(new KeyPairGeneratorSpec.Builder("anonDNS")
    	        .setAlias(alias)
    	        .setStartDate(new GregorianCalendar().getTime())
    	        .setEndDate(expiryCal.getTime())
    	        .setSerialNumber(BigInteger.valueOf(1))
    	        .setSubject(new X500Principal("CN=test1"))
    	        .build());

    	KeyPair kp = kpg.generateKeyPair();
    }    */
    
    public static X509Certificate createCert(KeyPair keyPair,String holder) throws CertificateEncodingException, InvalidKeyException, IllegalStateException, NoSuchProviderException, NoSuchAlgorithmException, SignatureException, UnsupportedEncodingException{
    	X509V3CertificateGenerator certGen = new X509V3CertificateGenerator();
    	X500Principal dnName = new X500Principal("CN="+holder);
    	certGen.setSerialNumber(BigInteger.valueOf(System.currentTimeMillis()));
    	X500Principal issuer = new X500Principal("CN=anondns");
    	certGen.setIssuerDN(issuer);
    	Date startDate=new GregorianCalendar().getTime();
    	Calendar expiryCal=new GregorianCalendar();
    	expiryCal.set(Calendar.YEAR, expiryCal.get(Calendar.YEAR)+1);
    	certGen.setNotBefore(startDate);
    	certGen.setNotAfter(expiryCal.getTime());
    	certGen.setSubjectDN(dnName);                       // note: same as issuer
    	certGen.setPublicKey(keyPair.getPublic());
    	certGen.setSignatureAlgorithm("SHA256WithRSAEncryption");
        X509Certificate cert = certGen.generate(keyPair.getPrivate(), PROVIDER);
       // saveCertToFile(cert,holder); <------ Do we need this?
    	return cert;
    }

    public static X509Certificate loadCertFromFile(String path) throws CertificateException, FileNotFoundException{
        CertificateFactory fact = CertificateFactory.getInstance("X.509");
        File f=new File(path);
        if (!f.exists()||f.isDirectory()){
            System.err.println("No such file");
            return null;
        }
        FileInputStream is = new FileInputStream(path);
        X509Certificate cer = (X509Certificate) fact.generateCertificate(is);
        return cer;
    }
    
    private static Boolean saveCertToFile(X509Certificate cert,String filename) throws UnsupportedEncodingException, CertificateEncodingException{
        FileOutputStream os = null;
        try {
            os = new FileOutputStream(filename);
            os.write("-----BEGIN CERTIFICATE-----\n".getBytes("US-ASCII"));
            os.write(Base64.encode(cert.getEncoded()));
            os.write("-----END CERTIFICATE-----\n".getBytes("US-ASCII"));
            os.close();
            return true;
        } catch (IOException ex) {
            System.err.println("Could not save certificate to file");
            return false; 
        }
        finally{
            if(os!=null)
                try {
                os.close();
            } catch (IOException ex) {
                System.err.println("Could not save certificate to file");
                return false;         
            }
        }
    }
    
    private static byte[] verifyData(byte[] ciphertxt, PublicKey pub) throws InvalidKeyException, IllegalBlockSizeException, BadPaddingException, NoSuchAlgorithmException, NoSuchProviderException, NoSuchPaddingException {
        Cipher cipher = Cipher.getInstance("RSA/None/NoPadding", PROVIDER);
        cipher.init(Cipher.DECRYPT_MODE, pub);
        return cipher.doFinal(ciphertxt);
    }
    
    private byte[] signData(byte[] data) throws NoSuchPaddingException, IllegalBlockSizeException, BadPaddingException, InvalidKeyException, NoSuchAlgorithmException, NoSuchProviderException{
        Cipher cipher = Cipher.getInstance("RSA/None/NoPadding", PROVIDER);
        cipher.init(Cipher.ENCRYPT_MODE, this.prof.keypair.getPrivate());
        byte[] signatureBytes = cipher.doFinal(data);
        return signatureBytes;
    }
    
    private byte[] decryptRSA(byte[] ciphertxt) throws NoSuchAlgorithmException, NoSuchProviderException, NoSuchPaddingException, InvalidKeyException, IllegalBlockSizeException, BadPaddingException {
        final Cipher cipher = Cipher.getInstance("RSA/None/NoPadding", PROVIDER);
        cipher.init(Cipher.DECRYPT_MODE, this.prof.keypair.getPrivate());
     //   System.out.println(ciphertxt.length);
        return cipher.doFinal(ciphertxt);
    }
    
    private static byte[] encryptRSA(byte[] plaintext,PublicKey pub) throws InvalidKeyException, IllegalBlockSizeException, BadPaddingException, NoSuchAlgorithmException, NoSuchPaddingException, NoSuchProviderException 
     {       
        final Cipher cipher = Cipher.getInstance("RSA/None/NoPadding", PROVIDER);
        cipher.init(Cipher.ENCRYPT_MODE, pub);
        return cipher.doFinal(plaintext);
    }
    
    /*
        --------------------------------------------------------------------------------
        SYMMETRIC CRYPTOGRAPHIC FUNCTIONS
        For AES use CBC mode and store IV to disk with AES ECB mode
    */
    public byte[] decryptAES(byte[] ciphertxt,int type) throws NoSuchAlgorithmException, NoSuchProviderException, NoSuchPaddingException, InvalidKeyException, IllegalBlockSizeException, BadPaddingException, InvalidAlgorithmParameterException, IOException {
        final Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
        if(this.ivTable.get(type)==null)
            return null;
        cipher.init(Cipher.DECRYPT_MODE, this.myKey, new IvParameterSpec(this.ivTable.get(type)));
        return cipher.doFinal(ciphertxt);
    }
    
    public byte[] encryptAES(byte[] plaintext,int type) throws InvalidKeyException, IllegalBlockSizeException, BadPaddingException, NoSuchAlgorithmException, NoSuchPaddingException, InvalidAlgorithmParameterException, IOException 
    {       
        final Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
        cipher.init(Cipher.ENCRYPT_MODE, this.myKey);
        this.ivTable.put(type, cipher.getIV());
        if(storeIVTable())
            return cipher.doFinal(plaintext);
        else
            return null;
    }
     
    private Boolean storeIVTable() throws InvalidKeyException, InvalidAlgorithmParameterException, NoSuchAlgorithmException, NoSuchPaddingException, IllegalBlockSizeException, BadPaddingException, IOException
    {
    	byte[] plaintext=utilities.toByteArray(this.ivTable);
        final Cipher cipher = Cipher.getInstance("AES/ECB/PKCS5Padding");
        cipher.init(Cipher.ENCRYPT_MODE, this.myKey);
        byte[] cipherIv=cipher.doFinal(plaintext);
        return utilities.saveToFile(getFilename(6),cipherIv);
    } 
    
    private HashMap<Integer,byte[]> loadIVTable()throws IllegalBlockSizeException, InvalidKeyException, InvalidAlgorithmParameterException, NoSuchAlgorithmException, BadPaddingException, NoSuchPaddingException, IOException, ClassNotFoundException
    {
        byte[] cipherIV=utilities.loadFromFile(getFilename(6));
        final Cipher cipher = Cipher.getInstance("AES/ECB/PKCS5Padding");
        cipher.init(Cipher.DECRYPT_MODE, this.myKey);
        byte[] data=cipher.doFinal(cipherIV);
        return (HashMap<Integer,byte[]>)utilities.toHashMap(data);
    }

    private static SecretKey makeKey(String pass) throws NoSuchAlgorithmException
    {
        MessageDigest md = MessageDigest.getInstance("MD5");
        md.update(pass.getBytes());
        byte[] mdbytes = md.digest();
        SecretKey key= new SecretKeySpec(mdbytes, "AES");
        return key;
    }
    
    /*
     * Rest functions
     */
    public String getFilename(int type){
    	if(type>filesPostfix.length)
            throw new ArrayIndexOutOfBoundsException("Error: Out of bounds in filename type");
    	return this.prof.username+"/"+filesPostfix[type-1];
    }
    
    public Boolean encrBeforeStore(HashMap h,int type) throws IOException, InvalidKeyException, IllegalBlockSizeException, BadPaddingException, NoSuchAlgorithmException, NoSuchPaddingException, InvalidAlgorithmParameterException{
    	byte[] bytes=utilities.toByteArray(h);
        byte[] cypherTxt=encryptAES(bytes,type);
    	return utilities.saveToFile(getFilename(type),cypherTxt);
    }
    
	private Boolean saveProfileInfo(String username, ProfileInfo pr) throws IOException, InvalidKeyException, IllegalBlockSizeException, BadPaddingException, NoSuchAlgorithmException, NoSuchPaddingException, InvalidAlgorithmParameterException {
    	byte[] bytes=utilities.toByteArray(pr);
        byte[] cypherTxt=encryptAES(bytes,5);
    	return utilities.saveToFile(getFilename(5),cypherTxt);
    }
	
    private ProfileInfo loadProfileInfo(String username) throws InvalidKeyException, ClassNotFoundException, NoSuchAlgorithmException, NoSuchProviderException, NoSuchPaddingException, IllegalBlockSizeException, BadPaddingException, InvalidAlgorithmParameterException, IOException{
        String filename=username+"/"+filesPostfix[4];
    	byte[] cipherTxt=utilities.loadFromFile(filename);
    	ProfileInfo ret=null;
        if(cipherTxt!=null)
        {
	        ret = (ProfileInfo)utilities.toProfileInfo(decryptAES(cipherTxt,5));
	    	if(ret==null)
	    	{
	    		System.err.println(filename+" file found but could not load content");
	    		return null;
        	}
        	else
	    		System.out.println(filename+" have been loaded succesfully");
	    }
        //else//certs
        //	System.out.println("Warning: No file "+filename+" to load profile");
        return ret;
    }
}
