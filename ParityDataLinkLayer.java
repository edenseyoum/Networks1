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
public class ParityDataLinkLayer extends DataLinkLayer {
// =============================================================================



    // =========================================================================
    /**
     * Embed a raw sequence of bytes into a framed sequence.
     *
     * @param  data The raw sequence of bytes to be framed.
     * @return A complete frame.
     */
    protected byte[] createFrame (byte[] data) {

	Queue<Byte> framingData = new LinkedList<Byte>();
	
	// Begin with the start tag.
	framingData.add(startTag);
	//byte parity;

	if(evenOnes(data) == true)
	    parity = 0;
	else
	    parity = 1;


	framingData.add(parity);

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
    public void send (byte[] dataa) {
	
	byte[] data = new byte[dataa.length - 1];
	
	for (int i=0; i< data.length; i++){
	    data[i] = dataa[i];
	}
	
	
	System.out.println("length of data is " + data.length);
	System.out.println("data is " + new String(data));

	/*for(int i =0; i< data.length; i++){
	  System.out.println(i + " and " +  );
	    }*/
	
	int n = data.length;
	
	if (n % 8 == 0){
	    System.out.println("n is " + n);
	    int mul = (int) n/8;
	    System.out.println("mul is " + mul);
	    for(int i = 0; i < mul; i++){
		byte[] byEight = new byte[8];
		for(int j = 0; j< 8; j++){
		    byEight[j] = data[j + (i*8)]; 
		}
		
		byte[] framedData = createFrame(byEight);
		    
		    System.out.println("framed data: " + new String(framedData));
		    
		    for (int k = 0; k < framedData.length; k += 1) {
			transmit(framedData[k]);
		    }
		    
	    }
	}
	
	else{
		for(int i = 0; i < (n/8)+1; i++){
		byte[] byEight = new byte[9];
		   for(int j = 0; j< 9; j++){
		       if((j + (i*9)) < data.length) {
			    byEight[j] = data[j + (i*9)]; 
			}
		   } 
		   
		   byte[] framedData = createFrame(byEight);
		       
		       
		   /*else{
		     byte[] byEight = new byte[(n % 8)];
			    for(int j = 0; j< byEight.length; j++){
			    byEight[j] = data[j + (i*9)];
				}
			    byte[] framedData = createFrame(byEight);
			    
			    }*/
		       
		       for (int k = 0; k < framedData.length; k += 1) {
		       transmit(framedData[k]);
		       
		   }
	    }
	    
	    
	    }
    }
    
    
    // =========================================================================
    /**
     * Determine whether the received, buffered data constitutes a complete
     * frame.  If so, then remove the framing metadata and return the original
     * data.  Note that any data preceding an escaped start tag is assumed to be
     * part of a damaged frame, and is thus discarded.
     *
     * @return If the buffer contains a complete frame, the extracted, original
     * data; <code>null</code> otherwise.
     */


    public boolean evenOnes(byte[] b){
	boolean evenOnes;
	int k = 0;
	for(int i =0; i < b.length; i++){
	    for(int j=0; j< 8; j++){
		k = k + ((b[i] >>> j) & 1);
	    }
	}
	if ((k % 2) == 0)
	    evenOnes = true;
	else
	    evenOnes = false;
	
	return evenOnes;
	

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

	byte parityCheck = extractedData[0];
	byte [] justData = new byte[extractedData.length-1];
	for(int k = 0; k < extractedData.length-1 ; k++){
	    justData[k] = extractedData[k+1];
	}

	boolean evenOnes = evenOnes(justData);

	if(evenOnes == false && parityCheck == (byte)0){
	    return null;
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
    private byte parity;
    
    // ===============================================================



// ===================================================================
} // class DumbDataLinkLayer
// ===================================================================
