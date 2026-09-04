/*
 MessageCanvas.java
 copyright, Peter Signell, 4/9/97
==========================================================================
*/
import java.applet.Applet;
import java.awt.*;
import java.lang.Math;
import gjt.*;
/*
==========================================================================
*/
class MessageCanvas extends Canvas implements GeneralData {

    MessageDisplay messageDisplay;

    private boolean  ifBrowserPaint;
    private boolean  ifInit;
    private int      forceNumber;
    private boolean  ifDown;
    private boolean  ifUpAndOk;
    private String   blinkerString;
    private String[] messageStrings;

    //------------------------------------------------------------
    // the class constructor:
    public MessageCanvas() {
        ifInit = true;
        blinkerString = "data";
        messageStrings = new String[2];
        ifDown = ifUpAndOk = ifBrowserPaint = false;
    }
    //------------------------------------------------------------------
    public void browserPaint() {
        //System.out.println("browserPaint");
        ifBrowserPaint = true;
        repaint();
        ifBrowserPaint = false;
    }
    //------------------------------------------------------------
    public Dimension preferredSize() {
        return new Dimension(messageFrameX+1,messageFrameY+11);
    }
    //------------------------------------------------------------
    public Dimension minimumSize() {
        return preferredSize();
    }
    //------------------------------------------------------------
    public void setMessageStrings (String[] s) {
        messageStrings = s;
    }
    //------------------------------------------------------------
    // notification of: mUp + OK judgment
    public void notifyMouseUpOk(String b) {
        blinkerString = b;
        ifInit = false;
        ifUpAndOk = true;
        ifDown = false;
        repaint();
    }
    //------------------------------------------------------------
    // notification of: done with display of messages
    public void notifyMessagesDone() {
        messageDisplay.notifyMessagesDone();
    }
    //------------------------------------------------------------
    // notification of: dataMessage + mUp; same consequences as above
    public void notifyMouseClick(String b) {
        blinkerString = b;
        ifInit = false;
        ifUpAndOk = true;
        ifDown = false;
        repaint();
    }
    //------------------------------------------------------------
    // notification of: mDown or mDrag and a real force number
    public void notifyMouseDown(String b) {
        blinkerString = b;
        ifInit = false;
        ifDown = true;
        ifUpAndOk = false;
        repaint();
    }
    //------------------------------------------------------------
    // paint without clearing the entire canvas (saves time)
    public void update(Graphics g){
        // this update does not clear the screen before painting
        paint(g);
    }
    //------------------------------------------------------------
    public void paint(Graphics g){
        if (messageDisplay == null) {
            // create messageDisplay
            messageDisplay = new MessageDisplay(messageStrings, g);
            // pass it this canvas's handle so it can get *this* graphics when needed
            messageDisplay.setMessageCanvasHandle(this);
        }
        messageDisplay.setMouseActionInMessageDisplay(blinkerString, ifInit, ifDown, ifUpAndOk, g);
    }
    //------------------------------------------------------------
}
/*
==========================================================================
*/
