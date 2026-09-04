/*
==================================================================
 ResultsCanvas requires this class:
     CanvasEquation
 version: 5/4/97, pss
 copyright 1997, Peter Signell
------------------------------------------------------------------
*/
import java.applet.Applet;
import java.awt.*;
import java.lang.Math;
import gjt.*;
/*
------------------------------------------------------------------
*/
class ResultsCanvas extends Canvas implements GeneralData {

    private int      forceNumber, numEquations, numForces;
    private boolean  ifInit, ifDown, ifDone, ifUpAndOk;
    private int[][]  ansX, ansY;
    private String   forceName, units, doneString;
    private String[] spacingStrings, forceNames;

    CanvasEquation canvasEquation;

    //------------------------------------------------------------
    // the class constructor:
    public ResultsCanvas() {
        ifInit = true;
        doneString = "done";
        forceName = "xxx";
    }
    //------------------------------------------------------------
    public Dimension preferredSize() {
		// last number below was 11
        return new Dimension(resultsFrameX+1,resultsFrameY+11);
    }
    //------------------------------------------------------------
    public Dimension minimumSize() {
        return preferredSize();
    }
    //------------------------------------------------------------
    public void notifyMouseUpOk(boolean i, int f, String[] fN, String u,
                              int[][] x, int[][] y, String[] s, int n, int nF) {
        ifDone = i;
        forceNumber = f;
        forceNames = fN;
        forceName = forceNames[forceNumber];
        numEquations = n;
        numForces = nF;
        units = u;
        ansX = x;
        ansY = y;
        spacingStrings = s;

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
        // draw the heading

        if (ifInit) {
            // create the canvas-equation creator
            canvasEquation = new CanvasEquation(g);

            // draw the text-enclosing box
            g.setColor(Color.black);
            g.drawRect(0,0,resultsFrameX,resultsFrameY);

            // draw the header
            g.setColor(resultsHeaderColor);
            g.drawString(resultsHeader,resultsHeaderPosX,resultsHeaderPosY);
        }

        // if a force, draw the canvas stuff
        if ((!ifInit) && (!forceName.equals(doneString))) {
             // draw the text-enclosing box
            g.setColor(Color.black);
            g.drawRect(0,0,resultsFrameX,resultsFrameY);

            // draw the header
            g.setColor(resultsHeaderColor);
            g.drawString(resultsHeader,resultsHeaderPosX,resultsHeaderPosY);

            // draw the equations
            for (int m=0;m<=forceNumber;m++) {
                // call the equation draw-er to draw forcename and answers at the correct place
                for (int n=0;n<=numEquations-1;n++) {
                    canvasEquation.plotEquation(g, m, forceNames, units,
                                            ansX[m][n], ansY[m][n],
                                            spacingStrings, n, numForces);
                }
                if (numEquations == 2) {
                    g.setColor(Color.black);
                    int x = resultsEqnPosX[0];
                    int y = resultsEqnPosY[0] + 20*numForces - 12;
                    g.drawLine(x, y, x+180, y);
                }
            }
        }

        // if we are done, add the trailer
        //if ((!ifInit) && (forceName.equals(doneString))) {
        if ((!ifInit) && (ifDone)) {
            g.setColor(resultsTrailerColor);
            int y = resultsEqnPosY[0] +
                    20*numForces*numEquations +
                    7*(numEquations-1);
            g.drawString(resultsTrailer, resultsTrailerPosX, y);
        }

        ifInit = false;
    }
    //------------------------------------------------------------
}
/*
==================================================================
*/
