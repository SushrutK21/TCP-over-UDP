import java.io.ByteArrayInputStream;
import java.io.ObjectInputStream;
import java.net.DatagramPacket;
import java.net.DatagramSocket;

/**
 * Listens to NACK's and informs the server if any appear.
 * @author Sushrut
 */
public class NackReceiver extends Thread {
    
    final int nackServerPort = 5005;
    Server s;
    SeqNo seqNo;
    
    /**
     * Constructor initializing nack server 
     * @param s
     * @param seqNo
     */
    NackReceiver(Server s, SeqNo seqNo) {
        this.s = s;
        this.seqNo = seqNo;
    }
    
    /**
     * listen for NACK and inform its server 
     */
    public void run() {
        byte[] buff;
        DatagramSocket socket;
        DatagramPacket udpPacket;
        ByteArrayInputStream bis;
        ObjectInputStream in;
        Packet x;
        
        try {
            buff = new byte[1500];
            udpPacket = new DatagramPacket(buff, buff.length);
            socket = new DatagramSocket(nackServerPort);
        
            while ( true ) {
                //receives seq no. of last packet which was received correctly
                socket.receive(udpPacket);
                bis = new ByteArrayInputStream(buff);
                in = new ObjectInputStream(bis);
                x = (Packet) in.readObject();
                System.out.println("*** NACK received: " + x.seqNo.get());

                s.retransmit(x.seqNo.get());
            }
        } catch ( Exception e ) {
            e.printStackTrace();
        }
    }        
}
