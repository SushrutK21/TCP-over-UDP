import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutput;
import java.io.ObjectOutputStream;
import java.io.RandomAccessFile;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;

/**
 * Class capable of serving requested file or command packet in a separate 
 * thread
 * 
 * @author sSushrut
 */
public class Server extends Thread {
    
    final int actualPacketLossRate = 100;
    InetAddress clientIp;
    static boolean master = false;
    int clientPort;
    SeqNo seqNo;
    int ssThresh = 75;
    int rate = 2000;
    
    /**
     * Constructor setting details of client which is to be served
     * @param clientIp
     * @param clientPort
     */
    Server( String clientIp, int clientPort, SeqNo seqNo ) {
        try {
            this.clientIp = InetAddress.getByName(clientIp);
            this.clientPort = clientPort;
            this.seqNo = seqNo;
        } catch ( Exception e ) {
            e.printStackTrace();
        }
    }
    
    void retransmit(long seq) {        
        seqNo.set(seq);
        ssThresh = rate + 100;
        rate = 2000;
        this.interrupt();
    }
    
    /**
     * Serves file or cmd packet in separate thread
     */
    public void run() {
        //read file chunk by chunk and send to client
        DatagramPacket udpPacket;
        DatagramSocket socket;
        ByteArrayOutputStream out;
        ObjectOutput os;
        Packet p;
        byte[] temp, packetInBytes;
        RandomAccessFile raf;
                       
        try {
            temp = new byte[500];
            socket = new DatagramSocket();                        
            raf = new RandomAccessFile("shaks.txt", "r");
            while ( raf.read(temp) != -1 ) {
                seqNo.incr();
                
                //interrupted coz of nack. retransmit from last correct packet
                if ( Thread.interrupted() ) {
                    raf.seek(seqNo.get()*500);
                    raf.read(temp);
                }

                p = new Packet(temp, false, "127.0.0.1", socket.getLocalPort(), "127.0.0.1", clientPort, seqNo);                

                //convert packet to bytes
                out = new ByteArrayOutputStream();
                os = new ObjectOutputStream(out);
                os.writeObject(p);
                packetInBytes = out.toByteArray();
                out.close();
                os.close();

                //create and send Datagram packet
                udpPacket = new DatagramPacket(packetInBytes, packetInBytes.length, clientIp, clientPort);                                
                
                if ( rate > actualPacketLossRate ) {
                    if ( (seqNo.get() != 7) || ( master == true ) ) {
                        System.out.println("Packet seq no: " + seqNo.get());
                        socket.send(udpPacket);                        
                    }
                }
                
                if ( (seqNo.get() == 7) && (master == false) )
                    master = true;
                
                try {
                    Thread.sleep(rate);
                } catch ( Exception e ) {
                    raf.seek(seqNo.get()*500);                    
                }
                
                if ( rate > actualPacketLossRate ) {
                    rate -= 100;
                    if ( rate < ssThresh ) 
                        ssThresh = rate;
                }
            }

            //send packet indicating file transfer is over
            p = new Packet(null, false, "127.0.0.1", socket.getLocalPort(), "127.0.0.1", clientPort, seqNo);
            seqNo.incr();
            //convert packet to bytes
            out = new ByteArrayOutputStream();
            os = new ObjectOutputStream(out);
            os.writeObject(p);
            packetInBytes = out.toByteArray();
            //create and send Datagram packet
            udpPacket = new DatagramPacket(packetInBytes, packetInBytes.length, clientIp, clientPort);
            socket.send(udpPacket);
            System.out.println("File sending over");
            
        } catch ( Exception e ) {
            e.printStackTrace();
        }
    }
    
    /**
     * Main method, has server listening for file req or cmd
     * @param args
     */
    public static void main(String args[]) {
        byte[] buff;
        DatagramPacket udpPacket;
        DatagramSocket socket;
        ByteArrayInputStream bis;
        ObjectInputStream in;
        Packet p;

        try {
            //initial setup for server socket
            buff = new byte[1024];
            udpPacket = new DatagramPacket(buff, buff.length);
            socket = new DatagramSocket(5000);

            while ( true ) {
                socket.receive(udpPacket);
                //retrieve packet check if its cmd or file req
                bis = new ByteArrayInputStream(buff);
                in = new ObjectInputStream(bis);
                p = (Packet) in.readObject();
                bis.close();
                in.close();

                if ( p.cmdPacket ) {
                    System.out.println("Command packet received");
                } else { //serve file in new thread
                    System.out.println("starting new thread for file transfer");
                    SeqNo commonSeqNo = new SeqNo(0);
                    Server s = new Server(p.sourceIp, p.sourcePort, commonSeqNo);
                    NackReceiver ns = new NackReceiver(s, commonSeqNo);
                    s.start();
                    ns.start();
                }
            }
        } catch ( Exception e ) {
            e.printStackTrace();
        }
    }
}
