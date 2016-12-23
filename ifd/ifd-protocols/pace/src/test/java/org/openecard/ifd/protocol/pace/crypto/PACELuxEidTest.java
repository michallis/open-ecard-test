package org.openecard.ifd.protocol.pace.crypto;

import org.openecard.ifd.protocol.pace.PACETest;

/**
 * Created by michallispashidis on 23/12/16.
 */
public class PACELuxEidTest {
    public static void main(String []args){
        PACELuxEidTest paceTest = new PACELuxEidTest();
        try {
            paceTest.execPACE();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void execPACE(){
        //do something
        System.out.println("hi there");
    }
}
