/*
========================================================================
class ProblemSelector
 version: 9/2/97
 copyright 1997, Peter Signell
--------------------------------------------------------------------------
*/
import java.applet.Applet;
import java.awt.*;
import java.lang.Math;
import gjt.*;

public class ProblemSelector implements GeneralData {

    // define the problems' variables
    int      numForces, numEquations, tailPosX, tailPosY;
	int      maxTriesEachForce;
    int[]    matchForceComps = new int[4];
    double   scale;
    String   units;
    String[] resultsSpacingStrings = new String[3];
    String[] blinkerStrings = new String[4];
    String   initialNoteString;
    String[] messageStrings = new String[2];
    String[] forceNames = new String[4];
    double[] cosSin = new double[2];
    int[][]  truAnsX = new int[4][2]; int[][] truAnsY = new int[4][2];
    int[]    truHdsX = new int[4]; int[] truHdsY = new int[4];

    // assign the problem object to the problems' interface type
    ProblemData problem;

    // the ProblemSelector constructor:
    ProblemSelector(int p, String an, String w, String ac){
        int problemNo = p;
        String wt = w;
        String angle = an;
        String accel = ac;

        // instantiate the selected problem
        if (problemNo ==  0) problem = new HangingBall0  ("90", "17", "0");
        if (problemNo ==  1) problem = new HangingBall   ("90", "17", "0");
        if (problemNo ==  2) problem = new PersonHorizS  ("0", "17", "0");
        if (problemNo ==  3) problem = new PersonHorizD  ("0", "16", "10");
        if (problemNo ==  4) problem = new PersonHorizD2 ("0", "16", "10");
        if (problemNo ==  5) problem = new BoxInclineS   ("23", "19", "0");
        if (problemNo ==  6) problem = new BoxInclineS2  ("23", "20", "0");
        if (problemNo ==  7) problem = new PersonIncliS  ("23", "20", "0");
        if (problemNo ==  8) problem = new PersonIncliD  ("23", "20", "10");
        if (problemNo ==  9) problem = new PulleyHorizSB ("00", "17", "0");
        if (problemNo == 10) problem = new PulleyHorizSP ("00", "17", "0");
        if (problemNo == 11) problem = new PulleyHorizSP4("00", "17", "0");

        // get the selected problem's variables
        numForces         = problem.getNumForces();
        matchForceComps   = problem.getMatchForceComps();
        maxTriesEachForce = problem.getMaxTriesEachForce();
        numEquations      = problem.getNumEquations();
        units             = problem.getUnits();
        scale             = problem.getScale();
        resultsSpacingStrings = problem.getResultsSpacingStrings();
        blinkerStrings        = problem.getBlinkerStrings();
        initialNoteString     = problem.getInitialNoteString();
        messageStrings        = problem.getMessageStrings();
        forceNames = problem.getForceNames();
        truAnsX    = problem.getTruAnsX()   ;
        truAnsY    = problem.getTruAnsY()   ;
        tailPosX   = problem.getTailPosX()  ;
        tailPosY   = problem.getTailPosY()  ;
        cosSin     = problem.getCosSin()    ;
        truHdsX    = problem.getTruHdsX()   ;
        truHdsY    = problem.getTruHdsY()   ;
    }

    // make available the selected problem's variables
    public int      getNumForces()            {return numForces            ;}
    public int[]    getMatchForceComps()      {return matchForceComps      ;}
    public int      getMaxTriesEachForce()    {return maxTriesEachForce    ;}
    public int      getNumEquations()         {return numEquations         ;}
    public String   getUnits()                {return units                ;}
    public double   getScale()                {return scale                ;}
    public String[] getResultsSpacingStrings(){return resultsSpacingStrings;}
    public String[] getBlinkerStrings()       {return blinkerStrings       ;}
    public String   getInitialNoteString()    {return initialNoteString    ;}
    public String[] getMessageStrings()       {return messageStrings       ;}
    public String[] getForceNames()           {return forceNames           ;}
    public int[][]  getTruAnsX()              {return truAnsX              ;}
    public int[][]  getTruAnsY()              {return truAnsY              ;}
    public int      getTailPosX()             {return tailPosX             ;}
    public int      getTailPosY()             {return tailPosY             ;}
    public double[] getCosSin()               {return cosSin               ;}
    public int[]    getTruHdsX()              {return truHdsX              ;}
    public int[]    getTruHdsY()              {return truHdsY              ;}

    // make available the selected problem's method
	//     for drawing the apparatus in the UserCanvas:
    public void drawUserApparatus (Graphics g) {
        problem.drawUserApparatus (g);
    }

    // make available the selected problem's method
	//     for drawing the apparatus in the ProblemCanvas:
    public void drawProblemApparatus (Graphics g, CanvasEquation f) {
        // design width: 300 = 279 + 8 + 8
        // draw a border
        g.drawRect(0, 0, problem.problemFrameX, problem.problemFrameY);
        problem.drawProblem(g, f);
    }
}
//============= end ================