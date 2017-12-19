import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.BindException;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.util.logging.Level;
import java.util.logging.Logger;

public class MsgReceiver extends Thread{
    private appFunc appfunc;
    private Boolean loop;
    ServerSocket receiverSocket=null;

	public MsgReceiver(appFunc app)
	{
            System.out.println("Listening for followers' messages at "+app.anonDNS_port);
            this.loop=true;
            try {	
                this.receiverSocket = new ServerSocket(app.anonDNS_port);
            }catch(BindException e){
                System.err.println("Application is already opened!");
                System.exit(0);
            } catch (IOException e) {
                e.printStackTrace();
                System.exit(0);
            }
            this.appfunc=app;
	}
	
    public void run() {
        String sentence;
        Socket connectionSocket = null;
        while (loop)
        {
            DataOutputStream outToClient = null;
            try {
            	//TODO encryption with symmetric key
            	connectionSocket = this.receiverSocket.accept();
                BufferedReader inFromClient =new BufferedReader(new InputStreamReader(connectionSocket.getInputStream()));
                outToClient = new DataOutputStream(connectionSocket.getOutputStream());
                sentence = inFromClient.readLine();
                outToClient.writeBytes(sentence); //Write Back message (ACK) 
                String userIP=connectionSocket.getInetAddress().getHostAddress();
                String user=this.appfunc.fromFollowingIPtoName(userIP);
                if(user==null)
                	System.out.println("\nNew message from uknown user: "+userIP+": " + sentence);
                else
                	System.out.println("\nNew message from "+user+": " + sentence);
            }catch(SocketException e){
            	System.out.println("Bye Bye!");
            	System.exit(0);
            } catch (IOException ex) {
                Logger.getLogger(anonDNS.class.getName()).log(Level.SEVERE, null, ex);
            } finally {
                try {
                	if(outToClient!=null)
                		outToClient.close();
                } catch (IOException ex) {
                    Logger.getLogger(anonDNS.class.getName()).log(Level.SEVERE, null, ex);
                }
            }
        }
    }
}