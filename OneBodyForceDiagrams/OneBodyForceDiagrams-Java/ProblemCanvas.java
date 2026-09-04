/*
ProblemCanvas.java
 copyright, Peter Signell, 5/5/97
==========================================================================
*/
import java.applet.Applet;
import java.awt.*;
import java.lang.Math;
import gjt.*;
/*
==========================================================================
*/
class ProblemCanvas extends Canvas implements GeneralData {
    // define the relevant objects    
    CanvasEquation canvasEquation;
    ProblemSelector problemSelector;

    private int problemNo;

    // the ProblemCanvas constructor:
    ProblemCanvas(int p) {
        problemNo = p;
    }

    public Dimension preferredSize() {
        return new Dimension(problemFrameX+1,problemFrameY+1);
    }

    public Dimension minimumSize() {
        return preferredSize();
    }

    public void setProblemSelectorHandle(ProblemSelector u){
        problemSelector = u;
    }

    // paint without clearing the entire canvas (saves time)
    public void update(Graphics g){
        // this update does not clear the screen before painting
        paint(g);
    }

    // paint the canvas
    public void paint(Graphics g){
        // create the Problem object and pass it needed handles
        canvasEquation = new CanvasEquation(g);
        problemSelector.drawProblemApparatus(g, canvasEquation);
    }
}
//==========================================================================