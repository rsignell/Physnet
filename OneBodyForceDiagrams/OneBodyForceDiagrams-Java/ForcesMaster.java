/*  5/21/97, pss; 4/14/08, pss
======================================================================
ForcesMaster requires these classes:
     Problemcanvas
     UserCanvas
     ResultsCanvas
     MessageCanvas
     RowLayout
     ColumnLayout
----------------------------------------------------------------------
 A new problem can be added to this class:
     ProblemSelector
----------------------------------------------------------------------
 author: Peter Signell
 version: 5/21/97
 copyright 1997, Peter Signell
----------------------------------------------------------------------
*/
import java.applet.Applet;
import java.awt.*;
import java.lang.Math;
import gjt.*;
import corejava.Console;
/*
----------------------------------------------------------------------
*/
public class ForcesMaster extends java.applet.Applet implements GeneralData {
    ProblemCanvas   problemCanvas;
    UserCanvas      userCanvas;
    ResultsCanvas   resultsCanvas;
    MessageCanvas   messageCanvas;
    Panel           leftPanel, rightPanel;
    RowLayout       appletLayout;
    ColumnLayout    leftLayout, rightLayout;
    private boolean ifInit, ifRestart;
    private int     problemNo, maxTriesEachForce, problemMode;
    private String  weightIn, angleIn, accelerationIn, cgiScript;
    //----------------------------------------------------------------
    public void init() {
        ifInit = true;

        problemNo = Integer.parseInt(getParameter("problemNo"));
        maxTriesEachForce = Integer.parseInt(getParameter("maxTriesEachForce"));
        problemMode = Integer.parseInt(getParameter("problemMode"));
        weightIn = getParameter("weight");
        angleIn = getParameter("angle");
        accelerationIn = getParameter("acceleration");
        if (weightIn.indexOf("*10^") > -1) {weightIn = stringToRoundToString(weightIn);}
        if (angleIn.indexOf("*10^") > -1) {angleIn = stringToRoundToString(angleIn);}
        if (accelerationIn.indexOf("*10^") > -1) {accelerationIn = stringToRoundToString(accelerationIn);}
        cgiScript = getParameter("cgiScript");

        problemCanvas = new ProblemCanvas(problemNo);
        userCanvas    = new UserCanvas(this, problemNo, maxTriesEachForce, problemMode,
                                       weightIn, angleIn, accelerationIn, cgiScript,
                                       problemCanvas);
        resultsCanvas = new ResultsCanvas();
        messageCanvas = new MessageCanvas();
        userCanvas.setMessageCanvasHandle(messageCanvas);
        userCanvas.setResultsCanvasHandle(resultsCanvas);

        leftPanel  = new Panel();
        rightPanel = new Panel();

        int gap = 5;
        setBackground(backgroundColor);

        appletLayout = new RowLayout(gjt.Orientation.LEFT,gjt.Orientation.TOP,gap);
        setLayout(appletLayout);
        add(leftPanel);
        add(rightPanel);

        leftLayout = new ColumnLayout(gjt.Orientation.LEFT,gjt.Orientation.TOP,gap);
        leftPanel.setLayout(leftLayout);
        leftPanel.add(problemCanvas);
        leftPanel.add(resultsCanvas);

        rightLayout = new ColumnLayout(gjt.Orientation.LEFT,gjt.Orientation.TOP,gap);
        rightPanel.setLayout(rightLayout);
        rightPanel.add(messageCanvas);
        rightPanel.add(userCanvas);

        //int xmax = problemFrameX + messageFrameX + gap + 8;
        //int ymax = problemFrameY + resultsY + gap + 3;

        //resize(xmax,ymax);
    }
    //----------------------------------------------------------------
    //public void stop() {
    //    messageCanvas.suspendRunner();
    //}
    //----------------------------------------------------------------
    public void paint(Graphics g){

        if (ifInit == false){
            problemCanvas.repaint();
            resultsCanvas.repaint();
            userCanvas.browserPaint();
            messageCanvas.browserPaint();
        }
        ifInit = false;
    }
    //----------------------------------------------------------------
    public void start(){

        //System.out.println("ForcesMaster start()");

        //if (ifInit == false){
        //    repaint();
        //}
        //ifInit = false;
        ifRestart = false;
    }
    //----------------------------------------------------------------
    public String stringToRoundToString(String s) {
        String string = s;
        double value = Double.valueOf(string.substring(0,string.indexOf("*10^"))).doubleValue();
        double power = Double.valueOf(string.substring(string.indexOf("*10^")+4)).doubleValue();
        value = value * Math.pow(10,power);
        value = Math.round(value);
        return Double.toString(value);
    }
    //----------------------------------------------------------------
}
//--------------------------------------------------------------------
