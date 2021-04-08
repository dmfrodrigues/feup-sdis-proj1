package sdis.Messages;

import sdis.Peer;

import java.net.DatagramPacket;
import java.net.InetSocketAddress;

abstract public class Message {
    private final String version;
    private final String messageType;
    private final int senderId;
    private final String fileId;
    private final InetSocketAddress inetSocketAddress;

    public Message(String version, String messageType, int senderId, String fileId, InetSocketAddress inetSocketAddress){
        if(version           == null) throw new NullPointerException("version");
        if(messageType       == null) throw new NullPointerException("messageType");
        if(fileId            == null) throw new NullPointerException("fileId");
        if(inetSocketAddress == null) throw new NullPointerException("inetSocketAddress");

        this.version = version;
        this.messageType = messageType;
        this.senderId = senderId;
        this.fileId = fileId;
        this.inetSocketAddress = inetSocketAddress;
    }

    /**
     * Returns common part of headers:
     *
     * <Version> <MessageType> <SenderId> <FileId>
     *
     * @return  Common part of headers
     */
    public byte[] getBytes(){
        return (version + " " + messageType + " " + senderId + " " + fileId).getBytes();
    }

    public int getLength(){
        return this.getBytes().length;
    }

    public DatagramPacket getPacket(){
        return new DatagramPacket(getBytes(), getLength(), inetSocketAddress);
    }

    public abstract void process(Peer peer);

    public String getVersion(){
        return version;
    }

    public String getFileId() {
        return fileId;
    }

    public Integer getSenderId(){
        return senderId;
    }

    public InetSocketAddress getSocketAddress(){
        return inetSocketAddress;
    }
}
