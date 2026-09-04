/* MessageDisplay.java
 copyright, Peter Signell, 5/5/97
 responsibilities (desired):
     input:  n-string message array (not including the blinking string)
             m-string array of successive blinking strings
             a canvas on which to write the blinking message
             a position on the canvas at which to write the message
             the color of the background
             the color of the non-blinking strings
             the color of the blinking strings
             mechanism to change the message
             mechanism to stop the blinking
     output: blinking message on the canvas
----------------------------------------------------------------------
*/
import java.awt.*;
import java.lang.Math;
/*
----------------------------------------------------------------------
*/
class MessageDisplay implements Runnable, GeneralData {

    private String forceFirstPart;
    private String forceLastPart;
    private int    forceFirstPartWidth;
    private int    forceLastPartWidth;
    private String text1, text2, text3;
    private String oldText1, oldText2, oldText3;
    private String blinkerString, doneString, dataString;
    private int    blinkerStringWidth;
    private int    numBlinks, blinkNum;
    private FontMetrics fm;
    private Color  blinkerStringColor, mColor;
    private int    blinkerStringNumber;
    private String[] messageStrings;
    private String[] forceStrings;

    private boolean ifDone, ifUpAndOk, ifDown, ifInit;

    // objects to be referenced:
    private MessageCanvas messageCanvas;
    private Thread runner;

    //-----------------------------------------------------------------
    // this is the class constructor (note that it has the graphics object):
    public MessageDisplay(String[] s, Graphics g) {
        forceStrings =  new String[2];
        forceStrings = s;
        fm=g.getFontMetrics(g.getFont());
        numBlinks = 10;
        text1 = forceStrings[0];
        text2 = "xxx";
        text3 = forceStrings[1];
        doneString = "done";
        dataString = "data";
        blinkNum = 1;
        ifDone = false;
    }    
    //-----------------------------------------------------------------
    // This method is called by messageCanvas 
    public void setMessageCanvasHandle(MessageCanvas m) {
        messageCanvas = m;
    }
    //-----------------------------------------------------------------
    public void run() {
        // this method is called only when a runnable this.thread is created
        // the thread stops when this method completes due to either ifDown or the loop completing
        Graphics g = messageCanvas.getGraphics();
        blinkerStringColor = messageBlinkerColor;
        paint(g);
        while ((blinkNum < numBlinks) && (!ifDown)) {
            try {runner.sleep(500);}
            catch (InterruptedException e) {}
            blinkerStringColor = backgroundColor;
            paint(g);
            try {runner.sleep(500);}
            catch (InterruptedException e) {}
            blinkerStringColor = messageBlinkerColor;
            paint(g);
            blinkNum++;
            }
        runner = null;
    }
    //-----------------------------------------------------------------
    public void paint(Graphics g) {
        int x, y;

        // draw a black box around the message
        g.setColor(backgroundColor);
        g.fillRect(0,10,messageFrameX-1,messageFrameY);
        g.setColor(Color.black);
        g.drawRect(0,10,messageFrameX-1,messageFrameY);

        // construct and display the new message
        x = messagePosX; y = messagePosY;
        if (blinkerString.equals(doneString)) {
            text1 = messageDoneString;
            text2 = messageDoneBlinker;
            text3 = "";
            mColor = messageDoneColor;
        }
        else if (blinkerString.equals(dataString)) {
            text1 = messageDataString;
            text2 = messageDataBlinker;
            text3 = "";
            mColor = messageDoneColor;
        }
        else {
            text1 = forceStrings[0];
            text2 = blinkerString;
            text3 = forceStrings[1];
            mColor = messageColor;
        }
        // plot the new message
        g.setColor(mColor);
        g.drawString(text1,x,y);
        x += fm.stringWidth(text1);
        g.setColor(blinkerStringColor);
        g.drawString(text2,x,y);
        x += fm.stringWidth(text2);
        g.setColor(messageColor);
        g.drawString(text3,x,y);
    }
    //-----------------------------------------------------------------
    public void stopRunner() {
        if (runner != null) {
            if (runner.isAlive()) {
                runner.stop();
                runner = null;
            }
        }
    }
    //-----------------------------------------------------------------
    public void notifyMessagesDone() {
        ifDone = true;
    }
    //-----------------------------------------------------------------
    public void setMouseActionInMessageDisplay
               (String b, boolean i, boolean d, boolean u, Graphics g) {
        ifDown = d;
        ifUpAndOk = u;
        ifInit = i;
        blinkerString = b;

        // if the 'run' loop is still going at 'click', the next command restarts it
        //     since the rest of the method is then not used
        blinkNum = 1;

        if ((ifDown) && (!ifInit)) {
            stopRunner();
            blinkerStringColor = messageBlinkerColor;
            paint(g);
        }
        if (ifUpAndOk || ifInit) {
            if (runner == null) {
                // tell the Thread constructor that *this* object's run method is to be used
                runner = new Thread(this);
            }
            if (!runner.isAlive()) {
                runner.start();
            }
        }
    }
    //-----------------------------------------------------------------
}
//---------------------------------------------------------------------
