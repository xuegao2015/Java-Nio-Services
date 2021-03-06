package org.kaipan.www.socket.protocol.http;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;

import org.kaipan.www.socket.core.IMessageReader;
import org.kaipan.www.socket.core.Message;
import org.kaipan.www.socket.core.MessageBuffer;
import org.kaipan.www.socket.core.Socket;
import org.kaipan.www.socket.log.Logger;

public class HttpMessageReader implements IMessageReader
{
	protected MessageBuffer    messageBuffer = null;
	protected List<Message> completeMessages = null;
	protected Message		  	 nextMessage = null;
	
	protected HttpMessageReaderBuffer readBuffer = null;
	
	public HttpMessageReader()
	{
		readBuffer = new HttpMessageReaderBuffer();
		
		completeMessages = new ArrayList<Message>();
	}
	
	@Override
	public void initialize(MessageBuffer readMessageBuffer)
	{
		this.messageBuffer = readMessageBuffer;				
	}
	
	private boolean onlyBodyNotCompleted() 
	{
		if ( readBuffer.headerComplete == true 
				&& readBuffer.bodycomplete == false ) {
			/**
			 * arrived body content length
			 */
        	int realContentLength = nextMessage.length - readBuffer.prevBodyEndIndex;
        	
        	/**
        	 * check if the complete packet has arrived
        	 *     else waiting more data...
        	 */
            if ( readBuffer.expectContentLength <= realContentLength ) {
                readBuffer.bodycomplete = true;
                
                completeMessages.add(nextMessage);
                nextMessage = null;
            }
            
            return true;
        }
		
		return false;
	}
	
	private boolean operate()
	{
		// body isn't complete
		if ( onlyBodyNotCompleted() ) return true;
        
        HttpHeader metaData = (HttpHeader) nextMessage.metaData;
        
        readBuffer.headerComplete = HttpUtil.prepare(nextMessage.sharedArray, 
        		nextMessage.offset, nextMessage.length, metaData);
        
        // header was still unfinished
        if ( ! readBuffer.headerComplete ) {
            if ( nextMessage.length > HttpUtil.HTTP_HEAD_MAXLEN ) {
            	Logger.write("illegal request, header is too large");
                return false;
            }
            
        	return true;
        }
        else {
            int headerLength = metaData.endOfHeader - nextMessage.offset;
            if ( headerLength > HttpUtil.HTTP_HEAD_MAXLEN ) {
            	Logger.write("illegal request, header is too large");
                return false;
            }
            
            readBuffer.headerComplete = true;
        }
        
        int endIndex  = metaData.bodyEndIndex;
        int realIndex = nextMessage.offset + nextMessage.length;
        if ( endIndex <= realIndex ) {
            completeMessages.add(nextMessage);
            
            if ( realIndex > endIndex ) {
            	Message message  = messageBuffer.getMessage();
                message.metaData = new HttpHeader();
            	
            	message.writePartialMessageToMessage(nextMessage, endIndex - nextMessage.offset);
            	nextMessage = message;
            }
            else {
            	nextMessage = null;
            }
            
            readBuffer.bodycomplete = true;
        }
        else {
        	readBuffer.bodycomplete        = false;
        	readBuffer.prevBodyEndIndex    = endIndex;
        	readBuffer.expectContentLength = endIndex - realIndex;
        }
        
        return true;
	}
	
    @Override
    public boolean read(Socket socket, ByteBuffer byteBuffer)
    {
        try {
            socket.read(byteBuffer);
            byteBuffer.flip();
        } 
        catch (IOException e) {
        	Logger.write(e.getMessage());
        }
        
        if ( socket.endOfStreamReached == true ) {
        	return false;
        }
        
    	if ( nextMessage == null ) {
    		this.nextMessage 		  = messageBuffer.getMessage();
    		this.nextMessage.metaData = new HttpHeader();
    	}

        // max reading data must be less than 4M
        nextMessage.writeToMessage(byteBuffer);
        byteBuffer.clear();
        
        return operate();
    }

	@Override
	public List<Message> getMessages() 
	{
		return completeMessages;
	}
}
