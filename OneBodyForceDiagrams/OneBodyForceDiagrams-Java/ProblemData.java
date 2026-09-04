// ProblemData.java
// copyright, Peter Signell, 4/30/97

import java.awt.*;
import java.lang.Math;

//-------------------------------------------------------------------------------
// classes using this database: user -> user, results, problem, canvasEquation
//-------------------------------------------------------------------------------
interface ProblemData extends GeneralData {

    public int      getNumForces()            ;
    public int[]    getMatchForceComps()      ;
    public int      getMaxTriesEachForce()    ;
    public int      getNumEquations()         ;
    public String   getUnits()                ;
    public double   getScale()                ;
    public String[] getResultsSpacingStrings();
    public String[] getBlinkerStrings()       ;
    public String   getInitialNoteString()    ;
    public String[] getMessageStrings()       ;
    public String[] getForceNames()           ;
    public int[][]  getTruAnsX()              ;
    public int[][]  getTruAnsY()              ;
    public int      getTailPosX()             ;
    public int      getTailPosY()             ;
    public double[] getCosSin()               ;
    public int[]    getTruHdsX()              ;
    public int[]    getTruHdsY()              ;
    public void drawProblem (Graphics g, CanvasEquation f);
    public void drawUserApparatus (Graphics g);
}
//-------------------------------------------------------------------------------
