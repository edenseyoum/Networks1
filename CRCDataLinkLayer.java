// =============================================================================
// IMPORTS

import java.util.Iterator;
import java.util.LinkedList;
import java.util.Queue;
// =============================================================================


// =============================================================================
/**
 * @file   DumbDataLinkLayer.java
 * @author Scott F. Kaplan (sfkaplan@cs.amherst.edu)
 * @date   August 2018, original September 2004
 *
 * A data link layer that uses start/stop tags and byte packing to frame the
 * data, and that performs no error management.
 */
public class CRCDataLinkLayer extends DataLinkLayer {
// =============================================================================



    // =========================================================================
    /**
     * Embed a raw sequence of bytes into a framed sequence.
     *
     * @param  data The raw sequence of bytes to be framed.
     * @return A complete frame.
     */
    protected byte[] createFrame (byte[] data) {

	byte[] messag = data; 

	Queue<Byte> framingData = new LinkedList<Byte>();
	
	// Begin with the start tag.
	framingData.add(startTag);

	// Add each byte of original data.
	for (int i = 0; i < data.length; i += 1) {

	    // If the current data byte is itself a metadata tag, then precede
	    // it with an escape tag.
	    byte currentByte = data[i];
	    if ((currentByte == startTag) ||
		(currentByte == stopTag) ||
		(currentByte == escapeTag)) {

		framingData.add(escapeTag);

	    }

	    // Add the data byte itself.
	    framingData.add(currentByte);

	}

	byte[] g = new byte[6];
	String a = "101001";
	g = a.getBytes();
	//g = {1, 0, 1, 0, 0, 1};

	

	for (int i =0; i< 5 ; i++){
	    messag[data.length + i] = g[i];
	}

	
	byte[] r = div(messag, g); 

	for(int i =0; i< r.length; i++){
	    framingData.add(r[i]);
	}
	
	

	// End with a stop tag.
	framingData.add(stopTag);

	// Convert to the desired byte array.
	byte[] framedData = new byte[framingData.size()];
	Iterator<Byte>  i = framingData.iterator();
	int             j = 0;
	while (i.hasNext()) {
	    framedData[j++] = i.next();
	}

	return framedData;
	
    } // createFrame ()
    // =========================================================================


        @Override
       public void send (byte[] data) {
	System.out.println(data.length);

	if (data.length % 8 == 0){
	    int mul = (int) data.length/8;
	    for(int i = 0; i < mul; i++){
		byte[] byEight = new byte[8];
		for(int j = 0; j< 8; j++){
		    byEight[j] = data[j + (i*8)]; 
		}
		    byte[] framedData = createFrame(byEight);
		
		for (int k = 0; k < framedData.length; k += 1) {
		    transmit(framedData[k]);
		}
	    }
	}
	
	//assuming the data is always divisible by 8 because the else statement below wouldn't compile.
	else{
	    for(int i = 0; i < (data.length/8)+1; i++){
		   byte[] byEight = new byte[8];
		   for(int j = 0; j< 8; j++){
		       	if((j + (i*8)) < data.length) {
			    byEight[j] = data[j + (i*8)]; 
			}
		       
		   }
		       byte[] framedData = createFrame(byEight);
		   

	    for (int k = 0; k < framedData.length; k += 1) {
		transmit(framedData[k]);
	    }
	    }
	
	
	    }
    }
 
    // =========================================================================


    public byte[] div(byte[] m, byte[] g){
	byte [] mes = m;
	byte[] gen = g;
	byte[] rem = new byte[5];
	for(int i=0; i< m.length; i++){
	    if(m[i] == 1){
		byte[] d = new byte[6];
		for(int j = 0; j < 6; j++){
		    d[j] = m[i+j];
		}

		for(int j= 1; j<6; j++){
		    rem[j-1] = xorr(d[j], gen[j]);
		    }

		byte[] mnew = new byte[m.length];
		for(int j= 0; j< 6; j++){
		    mnew[i+j] = rem[j];
		}
		div(mnew, g);
	    }
	}
	return rem;
    }

    public byte xorr(byte a, byte b){
	if(a == b)
	    return 0;
	else
	    return 1;
    }




    
    protected byte[] processFrame () {

	// Search for a start tag.  Discard anything prior to it.
	boolean        startTagFound = false;
	Iterator<Byte>             i = byteBuffer.iterator();
	while (!startTagFound && i.hasNext()) {
	    byte current = i.next();
	    if (current != startTag) {
		i.remove();
	    } else {
		startTagFound = true;
	    }
	}

	// If there is no start tag, then there is no frame.
	if (!startTagFound) {
	    return null;
	}
	
	// Try to extract data while waiting for an unescaped stop tag.
	Queue<Byte> extractedBytes = new LinkedList<Byte>();
	boolean       stopTagFound = false;
	while (!stopTagFound && i.hasNext()) {

	    // Grab the next byte.  If it is...
	    //   (a) An escape tag: Skip over it and grab what follows as
	    //                      literal data.
	    //   (b) A stop tag:    Remove all processed bytes from the buffer and
	    //                      end extraction.
	    //   (c) A start tag:   All that precedes is damaged, so remove it
	    //                      from the buffer and restart extraction.
	    //   (d) Otherwise:     Take it as literal data.
	    byte current = i.next();
	    if (current == escapeTag) {
		if (i.hasNext()) {
		    current = i.next();
		    extractedBytes.add(current);
		} else {
		    // An escape was the last byte available, so this is not a
		    // complete frame.
		    return null;
		}
	    } else if (current == stopTag) {
		cleanBufferUpTo(i);
		stopTagFound = true;
	    } else if (current == startTag) {
		cleanBufferUpTo(i);
		extractedBytes = new LinkedList<Byte>();
	    } else {
		extractedBytes.add(current);
	    }

	}

	// If there is no stop tag, then the frame is incomplete.
	if (!stopTagFound) {
	    return null;
	}

	// Convert to the desired byte array.
	if (debug) {
	    System.out.println("DumbDataLinkLayer.processFrame(): Got whole frame!");
	}

	//extractedData includes cheking bits at the end
	byte[] extractedData = new byte[extractedBytes.size()];
	int                j = 0;
	i = extractedBytes.iterator();
	while (i.hasNext()) {
	    extractedData[j] = i.next();
	    if (debug) {
		System.out.printf("DumbDataLinkLayer.processFrame():\tbyte[%d] = %c\n",
				  j,
				  extractedData[j]);
	    }
	    j += 1;
	}



	byte[] g = new byte[6];
	String a = "101001";
	g = a.getBytes();
	//g = {1, 0, 1, 0, 0, 1};


	//check

	byte[] rcheck = div(extractedData, g);
	for(int l= 0; l < rcheck.length; l++){
	if(rcheck[l] != 0){
	    System.out.println("remainder not zero");
	    return null;
	}
	}
	
	int onlydata = extractedData.length - 5; 
	byte[] justData = new byte[onlydata];

	    for(int k = 0; k< onlydata; k++){
		justData[k] = extractedData[k];
	    }
	    

	return justData;

    } // processFrame ()
    // ===============================================================



    // ===============================================================
    private void cleanBufferUpTo (Iterator<Byte> end) {

	Iterator<Byte> i = byteBuffer.iterator();
	while (i.hasNext() && i != end) {
	    i.next();
	    i.remove();
	}

    }


    // ===============================================================



    // ===============================================================
    // DATA MEMBERS
    // ===============================================================



    // ===============================================================
    // The start tag, stop tag, and the escape tag.
    private final byte startTag  = (byte)'{';
    private final byte stopTag   = (byte)'}';
    private final byte escapeTag = (byte)'\\';
    // ===============================================================



// ===================================================================
} // class DumbDataLinkLayer
// ===================================================================
