/*
========================================================================
interface Forceable
 version: 4/30/97
 copyright 1997, Peter Signell
--------------------------------------------------------------------------
*/
import java.applet.Applet;
import java.awt.*;
import java.lang.Math;
import gjt.*;

interface Forceable {
    public int      getNumForces();
    public int[]    getMatchForceComps();
    public int      getMaxTriesEachForce();
    public int      getNumEquations();
    public String   getUnits();
    public double   getScale();
    public String[] getResultsSpacingStrings();
    public String[] getBlinkerStrings();
    public String   getInitialNoteString();
    public String[] getMessageStrings();
    public String[] getForceNames();
    public int[][]  getTruAnsX();
    public int[][]  getTruAnsY();
    public int      getTailPosX();
    public int      getTailPosY();
    public double[] getCosSin();
    public int[]    getTruHdsX();
    public int[]    getTruHdsY();
    public void drawUserApparatus (Graphics g);
    public void drawProblem (Graphics g, CanvasEquation f);
}
