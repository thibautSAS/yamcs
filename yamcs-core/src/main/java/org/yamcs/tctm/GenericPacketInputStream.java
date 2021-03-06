package org.yamcs.tctm;

import java.io.DataInputStream;
import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.util.Map;

import org.yamcs.ConfigurationException;
import org.yamcs.YConfiguration;
import org.yamcs.utils.ByteArrayUtils;

/**
 * Generic packet reader that splits the stream into packets based on the length of the packet
 * <p>
 * Inspired from Netty's LengthFieldBasedFrameDecoder
 * 
 * <p>
 * The following configuration variables are used
 * <ul>
 * <li>maxPacketLength - the maximum packet length; if a packet with the length greater than this would be received, the
 * input stream is closed and an exception is raised. The length of the packet considered here is the number of data
 * bytes read from the
 * stream - that is including the length field itself and the bytes to strip at the beginning if set (see below)</li>
 * <li>lengthFieldOffset - the offset in the packet where the length is read from</li>
 * <li>lengthFieldLength - the size in bytes of the length field</li>
 * <li>lengthAdjustment - after reading the length from the configured offset, this variable is added to it to determine
 * the real length</li>
 * <li>initialBytesToStrip - after reading the packet, strip this number of bytes from the beginning</li>
 * </ul>
 * 
 * <p>
 * All the above configuration parameters have to be set, otherwise a ConfigurationException will be thrown.
 * 
 * @author nm
 *
 */
public class GenericPacketInputStream implements PacketInputStream {
    private final int maxPacketLength;
    private final int lengthFieldOffset;
    private final int lengthFieldLength;
    private final int lengthFieldEndOffset;
    private final int lengthAdjustment;
    private final int initialBytesToStrip;
    DataInputStream dataInputStream;

    public GenericPacketInputStream(InputStream inputStream, Map<String, Object> args) {
        this.dataInputStream = new DataInputStream(inputStream);
        this.maxPacketLength = YConfiguration.getInt(args, "maxPacketLength");
        this.lengthFieldOffset = YConfiguration.getInt(args, "lengthFieldOffset");
        this.lengthFieldLength = YConfiguration.getInt(args, "lengthFieldLength");
        this.lengthAdjustment = YConfiguration.getInt(args, "lengthAdjustment");
        this.initialBytesToStrip = YConfiguration.getInt(args, "initialBytesToStrip");
        lengthFieldEndOffset = lengthFieldOffset + lengthFieldLength;

        if (lengthFieldLength != 1 && lengthFieldLength != 2 && lengthFieldLength != 3 && lengthFieldLength != 4) {
            throw new ConfigurationException("Unsupported legnthFieldLength, supported values are 1,2,3 or 4");
        }
    }

    @Override
    public byte[] readPacket() throws IOException, PacketTooLongException {
        byte[] b = new byte[lengthFieldEndOffset];
        dataInputStream.readFully(b);
        int length;
        switch (lengthFieldLength) {
        case 1:
            length = 0xFF & b[lengthFieldOffset];
            break;
        case 2:
            length = ByteArrayUtils.decodeShort(b, lengthFieldOffset);
            break;
        case 3:
            length = (ByteArrayUtils.decodeShort(b, lengthFieldOffset) << 16) + (0xFF & b[lengthFieldOffset + 2]);
            break;
        case 4:
            length = ByteArrayUtils.decodeInt(b, lengthFieldOffset);
            break;
        default:
            throw new IllegalStateException();
        }
        length+=lengthAdjustment;
        
        if(length>maxPacketLength) {
            throw new PacketTooLongException(maxPacketLength, length);
        }
        byte[] packet = new byte[length-initialBytesToStrip];
        int offset;
        if(initialBytesToStrip<=lengthFieldEndOffset) { 
            offset = lengthFieldEndOffset-initialBytesToStrip;
            System.arraycopy(b, initialBytesToStrip, packet, 0, offset);
        } else {
            offset = 0;
            skipFully(dataInputStream, initialBytesToStrip-lengthFieldEndOffset);
        }
        dataInputStream.readFully(packet, offset, packet.length-offset);
        
        return packet;
    }

    
    static void skipFully(InputStream in, int n) throws IOException {
        while (n > 0) {
            long skipped = in.skip(n);
            if (skipped == 0) throw new EOFException("Tried to skip " + n + " but reached EOF");
            n -= skipped;
        }
    }
}
