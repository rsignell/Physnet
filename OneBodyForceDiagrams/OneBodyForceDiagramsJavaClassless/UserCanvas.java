/*
========================================================================
class UserCanvas requires these classes:
    CanvasEquation
    CanvasVector
    UserDisplay
    GeneralData
    ResultsMaxWidth (not yet implemented ... might be done in *this* constructor
                     because it needs the font metrics for the widths; note that
                     it will be valid for any problem.) *This* will communicate the
                     max widths to CanvasEquation so it can be made
                     problem-independent.
    variable problemMode: 1=demo, 2=homework, 3=pre- and post-test
 version: 9/2/97
 copyright 1997, Peter Signell
--------------------------------------------------------------------------
*/
import java.applet.Applet;
import java.awt.*;
import java.lang.*;
import java.net.*;
import java.io.*;
/*
--------------------------------------------------------------------------
*/
class UserCanvas extends Canvas implements GeneralData {

    // non-apparatus variables
    private boolean fInit, mUp, mDown, mDrag, ifBrowserPaint;
    private int     forceNumber, maxTriesEachForce, problemMode, numTries;
    private String  forceName;
    private int     userPosX, userPosY;
    private int[]   forceX = new int[2];
    private int[]   forceY = new int[2];
    private int     newUserPosX, newUserPosY;
    private boolean ifDataMessage;
    private int     maxForceNumber;
    private int     problemNo;
    private String  mouseUpsString;
    private String  cgiScript;
    private String  weightIn, angleIn, accelerationIn;

    // userCanvas problem-specific variables:
    private int    tailPosX, tailPosY;
    private int    numForces, numEquations, forceTolerance;
    private double scale, cost, sint;
    private String units;
    private int[]    truHdsX = new int[4];
    private int[]    truHdsY = new int[4];
    private double[] cosSin = new double[2];
    private int[][]  truAnsX = new int[4][2];
    private int[][]  truAnsY = new int[4][2];
    private String[] forceNames = new String[4];
    private int[]    matchForceComps = new int[4];
    private String[] resultsSpacingStrings = new String[3];
    private String[] blinkerStrings = new String[4];
    private String[] messageStrings = new String[3];

    // define the objects to be placed on the UserCanvas:
    private MessageCanvas messageCanvas;
    private ResultsCanvas resultsCanvas;

    // classes referenced:
    ProblemSelector problemSelector;
    CanvasEquation canvasEquation;
    CanvasVector canvasVector;
    ProblemCanvas problemCanvas;
    ForcesMaster forcesMaster;

    //------------------------------------------------------------------
    // the UserCanvas constructor
    UserCanvas(ForcesMaster f, int p, int m, int pM,
               String w, String an, String ac, String cS, ProblemCanvas v){

        problemNo = p;
        maxTriesEachForce = m;
        problemMode = pM;
        cgiScript = cS;
        problemCanvas = v;
        forcesMaster = f;
        weightIn = w;
        angleIn = an;
        accelerationIn = ac;
        problemSelector = new ProblemSelector(problemNo, angleIn, weightIn, accelerationIn);
        problemCanvas.setProblemSelectorHandle(problemSelector);

        numForces           = problemSelector.getNumForces();
        matchForceComps     = problemSelector.getMatchForceComps();
        numEquations        = problemSelector.getNumEquations();
        units     = problemSelector.getUnits();
        scale     = problemSelector.getScale();
        //if (!weightIn.equals("none")) {scale = 18 * scale /(double)(Integer.parseInt(weightIn));}
        forceTolerance = (int) Math.round(scale * userTolerance);
        resultsSpacingStrings = problemSelector.getResultsSpacingStrings();
        blinkerStrings = problemSelector.getBlinkerStrings();
        messageStrings = problemSelector.getMessageStrings();
        forceNames = problemSelector.getForceNames();
        truAnsX    = problemSelector.getTruAnsX()   ;
        truAnsY    = problemSelector.getTruAnsY()   ;
        tailPosX   = problemSelector.getTailPosX()  ;
        tailPosY   = problemSelector.getTailPosY()  ;
        cosSin     = problemSelector.getCosSin()    ;
        truHdsX    = problemSelector.getTruHdsX()   ;
        truHdsY    = problemSelector.getTruHdsY()   ;
        cost = cosSin[0];
        sint = cosSin[1];

        // initialize the force variables
        maxForceNumber = numForces - 1;
        mouseUpsString = "";
        for (int n=0;n<=numForces;n++) {
            mouseUpsString += "0";
        }

        ifBrowserPaint = false;

        // initialize to the "see data" message
        ifDataMessage = true;
        forceNumber = -1;
        fInit = mUp = mDown = mDrag = false;
    }
    //------------------------------------------------------------------
    public void setMessageCanvasHandle(MessageCanvas m) {
        messageCanvas = m;
        messageCanvas.setMessageStrings(messageStrings);
    }
    //------------------------------------------------------------------
    public void browserPaint() {
        ifBrowserPaint = true;
        repaint();
    }
    //------------------------------------------------------------------
    public void setResultsCanvasHandle(ResultsCanvas r) {
        resultsCanvas = r;
    }
    //------------------------------------------------------------------
    public Dimension preferredSize() {
        return new Dimension(userFrameX+1, userFrameY+1);
    }
    //------------------------------------------------------------------
    public Dimension minimumSize() {
        return preferredSize();
    }
    //------------------------------------------------------------------
    // mouse button just went from down to up: repaint the canvas;
    public boolean mouseUp(Event evt, int x, int y) {
        mUp = true; mDown = mDrag = fInit = false;
        newUserPosX = x; newUserPosY = y;
        repaint();
        return true;
    }
    //------------------------------------------------------------------
    // mouse button just went from up to down: repaint the canvas
    public boolean mouseDown(Event evt, int x, int y) {
        mDown = true; mUp = mDrag = fInit = false;
        newUserPosX = x; newUserPosY = y;
        repaint();
        return true;
    }
    //------------------------------------------------------------------
    // mouse button down and mouse just moved: repaint the canvas
    public boolean mouseDrag(Event evt, int x, int y) {
        mDrag = true; mUp = mDown = fInit = false;
        newUserPosX = x; newUserPosY = y;
        repaint();
        return true;
    }
    //------------------------------------------------------------------
    // paint without clearing the entire canvas (saves time)
    public void update(Graphics g){
        paint(g);
    }
    //---------------------------------------------------------------------------
    int p2fX(int x, int y) {
        return (int) Math.round(scale * (x - tailPosX));
    }
    //---------------------------------------------------------------------------
    int p2fY(int x, int y) {
        return (int) Math.round(scale * (y - tailPosY));
    }
    //------------------------------------------------------------------
    // initialize the canvas contents or judge the user's mouse events and respond
    public void paint(Graphics g) {

        // if forceNumber < 0 and !mUp: do nothing until mUp

        // if forceNumber < 0 and mUp (click): start the forces
        if (forceNumber < 0 && mUp) {
            fInit = true;
            mDown = false;
            // next line added 4/17/08 (correction for case of zero force true answer)
            mUp = false;
            forceNumber = 0;
            forceName = forceNames[forceNumber];
            userPosX = tailPosX; userPosY = tailPosY;
            numTries = 0;
            messageCanvas.notifyMouseClick(blinkerStrings[forceNumber]);
            // create the force equation and vector objects
            canvasEquation = new CanvasEquation(g);
            canvasVector   = new CanvasVector(g);
        }

        // if mouseDown and all forces finished: do end game
        if (mDown && (forceNumber == maxForceNumber+1)) {
            endGame("OK", cgiScript, problemNo, mouseUpsString);
        }

        // if mouseDown or mouseDrag and really a force:
        //     erase user's old vector & eqn, update position
        if ((mDown || mDrag) && (forceNumber >= 0) && (forceNumber <= maxForceNumber)) {
            messageCanvas.notifyMouseDown(blinkerStrings[forceNumber]);
            eraseUserStuff(g,forceX[0],forceY[0],0);
            if (numEquations == 2) {
                eraseUserStuff(g,forceX[1],forceY[1],1);
            }
            userPosX = newUserPosX; userPosY = newUserPosY;
        }

        // if browserPaint or forceInit or mouseDown or mouseDrag and really a force:
        //     plot fixed & user's new stuff
        if ((ifBrowserPaint || fInit || mDown || mDrag)
                            && (forceNumber >= 0) && (forceNumber <= maxForceNumber)) {

            // redraw the userCanvas bounding box and the apparatus
            g.setColor(Color.black);
            g.drawRect(0, 0, userFrameX, userFrameY);
            problemSelector.drawUserApparatus(g);

            // draw the user's previously-done correct vectors
            if (forceNumber > 0) {
                drawDoneVectors(forceNumber-1,g);
            }

            // draw the user's new vector and equation
            canvasVector.plotVector(tailPosX, tailPosY, userPosX, userPosY, g);
            forceX[0] = p2fX(userPosX,userPosY);
            forceY[0] = p2fY(userPosX,userPosY);
            canvasEquation.plotEquation (g, forceName, units, forceX[0], forceY[0], 0);
            if (numEquations == 2) {
                forceX[1] = (int) Math.round(cost*forceX[0]-sint*forceY[0]);
                forceY[1] = (int) Math.round(sint*forceX[0]+cost*forceY[0]);
                canvasEquation.plotEquation (g, forceName, units, forceX[1], forceY[1], 1);
            }

            // get ready up for new input
            fInit = mDown = mDrag = false;
            if (ifBrowserPaint) {ifBrowserPaint = false;}
        }

        // if mouseUp, evaluate the user's answer
        if (mUp && (forceNumber >= 0) && (forceNumber < numForces)) {

            // keep track of the mouse ups
            String leftStr = "";
            String rightStr = "";
            String midStr = mouseUpsString.substring(forceNumber,forceNumber+1);
            if (forceNumber>0) {
                leftStr  = mouseUpsString.substring(0,forceNumber);
            }
            if (forceNumber<numForces-1) {
                rightStr = mouseUpsString.substring(forceNumber+1,numForces);
            }
            int numTries = Integer.parseInt(midStr);
            numTries++;
            if (numTries<=9) {midStr = Integer.toString(numTries);}
            else {midStr = "9";}
            mouseUpsString = leftStr+midStr+rightStr;

            // if correct, cycle to the next force or to the trailer message
            int matchIndex = matchForceComps[forceNumber]-1;

            int depX = Math.abs(forceX[matchIndex] - truAnsX[forceNumber][matchIndex]);
            int depY = Math.abs(forceY[matchIndex] - truAnsY[forceNumber][matchIndex]);
            if ((depX <= forceTolerance) && (depY <= forceTolerance)) {

                // erase the user's stuff
                for (int n=0;n<=numEquations-1;n++) {
                    eraseUserStuff(g,forceX[n],forceY[n],n);
                }

                // draw the apparatus
                problemSelector.drawUserApparatus(g);

                // draw the set of correctly done vectors
                drawDoneVectors(forceNumber, g);

                // notify the resultsCanvas of the user's correctly drawn force
                boolean ifResultsDone = false;
                if (forceNumber == maxForceNumber) {ifResultsDone = true;}
                resultsCanvas.notifyMouseUpOk(ifResultsDone, forceNumber, forceNames, units,
                                              truAnsX, truAnsY,
                                              resultsSpacingStrings, numEquations, numForces);

                // cycle the force or do the end game
                if (forceNumber < maxForceNumber) {
                    forceNumber++;
                    numTries = 0;
                    forceName = forceNames[forceNumber];
                    userPosX = tailPosX; userPosY = tailPosY;
                    forceX[0] = forceY[0] = forceX[1] = forceY[1] = 0;
                    // draw the equation and message for the *next* force
                    for (int n=0;n<=numEquations-1;n++) {
                        canvasEquation.plotEquation(g,forceName,units,forceX[n],forceY[n],n);
                    }
                    // notify the messageCanvas that there is a new force
                    messageCanvas.notifyMouseUpOk(blinkerStrings[forceNumber]);
                }
                else {
                    g.setColor(userTrailerColor);
                    g.drawString(userTrailer,userTrailerPosX,userTrailerPosY);
                    // notify the messageCanvas that we are all done
                    messageCanvas.notifyMouseUpOk("done");
                    forceNumber++;
                }
            }
            // if not correct and mode > 1, check # of tries
            else {
                if (problemMode>1 && numTries >= maxTriesEachForce) {
                    endGame("notOK", cgiScript, problemNo, mouseUpsString);
                }
            }
            fInit = mUp = mDown = false;
        }
    }
    //------------------------------------------------------------------
    private void endGame (String i, String cS, int p, String m) {
        messageCanvas.notifyMessagesDone();
        String ifOK = i;
        cgiScript = cS;
        int problemNo = p;
        String mouseUpsString = m;
        URL theURL = null;
        //String url = "file:///H|/applets/ForcesMaster/Test.html";
        String url = "file:///C|/Physnet/dev/ForcesMasterWeb/Java/ForcesMasterMenu.html";
        //String url = "http://www.physnet.org/modules/supportprograms/OneBodyForceDiagrams/ForcesMasterMenu.html";
        //String url = cgiScript+"A"+mouseUpsString;
        try {theURL = new URL(url);}
        catch (MalformedURLException e) {
            theURL = null;
            //System.out.println("Bad URL: "+theURL);
        }
        if (theURL != null) {
            System.out.println("message sent");
            forcesMaster.getAppletContext().showDocument(theURL);
        }
        else{System.out.println("");
             System.out.println("message not sent to "+url);
        }
    }
    //------------------------------------------------------------------
    private void eraseUserStuff(Graphics g, int fX, int fY, int n) {
        if (n == 0) {
            canvasVector.plotVector(backgroundColor, tailPosX, tailPosY, userPosX, userPosY, g);
        }
        canvasEquation.plotEquation (g, forceName, units, backgroundColor, fX, fY, n);
    }
    //------------------------------------------------------------------
    private void drawDoneVectors (int f, Graphics g) {
        int nmax = f;
        for (int n=0;n<=nmax;n++) {
            canvasVector.plotVector(canvasEquation, n, forceNames[n],
                                    tailPosX, tailPosY,
                                    truHdsX[n], truHdsY[n], g);
        }
    }
    //------------------------------------------------------------------
}
/*
==========================================================================
*/
