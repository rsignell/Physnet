// BoxInclineS2.java
// copyright, Peter Signell, May 1, 1997
//-------------------------------------------------------------------------------
import java.awt.*;
import java.lang.Math;
//-------------------------------------------------------------------------------
class BoxInclineS2 implements ProblemData, Forceable {

    private String title = "Stationary block on an inclined plane";

    int[][] truAnsX = new int[4][2]; int[][] truAnsY = new int[4][2];
    int[]   truHdsX = new int[4]; int[] truHdsY = new int[4];
    private int uOriginX = 0;
    private int uOriginY = 0;

    int    gForce    = 20;
    double thetaDeg  = 23f;
    int    problemDotHalfW = 1;
    int    userDotHalfW = 2;
    double userOriginX = 110f;
    double userOriginY = 170f;
    String units     = "N";
    double scale     = 1f/5;
    double problemScale = 0.6f;
    int    numForces = 4;
    int    maxTriesEachForce = 1;
    int    numEquations = 2;
    int[]  matchForceComps = {1,1,2,2};
    String initialNoteString = "At left note weight & unit vectors, then click";
    String[] resultsSpacingStrings = {"resultant","9","19"};
    String[] blinkerStrings = {"resultant","gravity","normal surface","frictional surface"};
    String[] messageStrings = {"Mouse-draw the "," force on the block:"};
    String[] forceNames = {"resultant","gravity","normal","friction"};
    double problemOriginX = 82f * problemScale;
    double problemOriginY = 190f * problemScale;
    double inclineL = 160f;
    double boxHalfL = 20f;
    double boxHalfH = 14f;
    double inclineHalfL = inclineL/2;
    double theta, cost, sint;
    double[] cosSin = new double[2];
    int   accelX = 0;
    int[] problemXs = new int[5];
    int[] problemYs = new int[5];
    int[] userXs = new int[5];
    int[] userYs = new int[5];
    int problemBoxCenterX, problemBoxCenterY, userBoxCenterX, userBoxCenterY;
    int tailPosX, tailPosY;
    int problemDotLeftX, problemDotTopY, userDotLeftX, userDotTopY;

    // the BoxInclineS2 constructor:
    public BoxInclineS2 (String an, String w, String ac) {
        String angleIn = an;
        String weightIn = w;
        String accelerationIn = ac;
        if (!angleIn.equals("none")) {thetaDeg = (double) Integer.parseInt(angleIn);}
        if (!weightIn.equals("none")) {gForce = Integer.parseInt(weightIn);}
        if (!accelerationIn.equals("none")) {accelX = Integer.parseInt(accelerationIn);}
        theta = thetaDeg * 3.14159 / 180;
        cost = Math.cos(theta);
        sint = Math.sin(theta);
        cosSin[0] = cost; cosSin[1] = sint;
        // get the box's corners
        int PLLX = (int)(problemOriginX+(inclineHalfL-boxHalfL)*cost);
        int PLLY = (int)(problemOriginY-(inclineHalfL-boxHalfL)*sint);
        int PLRX = (int)(problemOriginX+(inclineHalfL+boxHalfL)*cost);
        int PLRY = (int)(problemOriginY-(inclineHalfL+boxHalfL)*sint);
        int PULX = PLLX - (int)(2*boxHalfH*sint);
        int PULY = PLLY - (int)(2*boxHalfH*cost);
        int PURX = PLRX - (int)(2*boxHalfH*sint);
        int PURY = PLRY - (int)(2*boxHalfH*cost);
        problemXs[0] = PLLX; problemXs[1] = PLRX; problemXs[2] = PURX;
        problemXs[3] = PULX; problemXs[4] = PLLX;
        problemYs[0] = PLLY; problemYs[1] = PLRY; problemYs[2] = PURY;
        problemYs[3] = PULY; problemYs[4] = PLLY;
        int ULLX = (int)(userOriginX+(inclineHalfL-boxHalfL)*cost);
        int ULLY = (int)(userOriginY-(inclineHalfL-boxHalfL)*sint);
        int ULRX = (int)(userOriginX+(inclineHalfL+boxHalfL)*cost);
        int ULRY = (int)(userOriginY-(inclineHalfL+boxHalfL)*sint);
        int UULX = ULLX - (int)(2*boxHalfH*sint);
        int UULY = ULLY - (int)(2*boxHalfH*cost);
        int UURX = ULRX - (int)(2*boxHalfH*sint);
        int UURY = ULRY - (int)(2*boxHalfH*cost);
        userXs[0]=ULLX; userXs[1]=ULRX; userXs[2]=UURX; userXs[3]=UULX; userXs[4]=ULLX;
        userYs[0]=ULLY; userYs[1]=ULRY; userYs[2]=UURY; userYs[3]=UULY; userYs[4]=ULLY;
        // get the box's center
        problemBoxCenterX = (int)Math.round((PULX+PLRX)/2);
        problemBoxCenterY = (int)Math.round((PULY+PLRY)/2);
        userBoxCenterX    = (int)Math.round((UULX+ULRX)/2);
        userBoxCenterY    = (int)Math.round((UULY+ULRY)/2);
        tailPosX = userBoxCenterX;
        tailPosY = userBoxCenterY;
        problemDotLeftX = problemBoxCenterX-problemDotHalfW;
        problemDotTopY  = problemBoxCenterY-problemDotHalfW;
        userDotLeftX    = userBoxCenterX   -userDotHalfW;
        userDotTopY     = userBoxCenterY   -userDotHalfW;

        // resultant force
        truAnsX[0][0] = 0;
        truAnsY[0][0] = 0;
        // gravity force
        truAnsX[1][0] = 0;
        truAnsY[1][0] = gForce;
        // normal force
        truAnsX[2][0] = (int) Math.round(-gForce * cost * sint);
        truAnsY[2][0] = (int) Math.round(-gForce * cost * cost);
        // friction force
        truAnsX[3][0] = (int) Math.round( gForce * sint * cost);
        truAnsY[3][0] = (int) Math.round(-gForce * sint * sint);

        // resultant force
        truAnsX[0][1] = 0;
        truAnsY[0][1] = 0;
        // gravity force
        truAnsX[1][1] = (int) Math.round(-gForce * sint);
        truAnsY[1][1] = (int) Math.round( gForce * cost);
        // normal (anti-compression) force
        truAnsX[2][1] = 0;
        truAnsY[2][1] = (int) Math.round(-gForce * cost);
        // friction force
        truAnsX[3][1] = (int) Math.round( gForce * sint);
        truAnsY[3][1] = 0;

        // get the screen positions of the force components
        truHdsX[0] = (int) Math.round(tailPosX + truAnsX[0][0]/scale);
        truHdsY[0] = (int) Math.round(tailPosY + truAnsY[0][0]/scale);
        truHdsX[1] = (int) Math.round(tailPosX + truAnsX[1][0]/scale);
        truHdsY[1] = (int) Math.round(tailPosY + truAnsY[1][0]/scale);
        truHdsX[2] = (int) Math.round(tailPosX + truAnsX[2][0]/scale);
        truHdsY[2] = (int) Math.round(tailPosY + truAnsY[2][0]/scale);
        truHdsX[3] = (int) Math.round(tailPosX + truAnsX[3][0]/scale);
        truHdsY[3] = (int) Math.round(tailPosY + truAnsY[3][0]/scale);
    }

    public void drawProblem (Graphics g, CanvasEquation f) {
        // draw a border
        g.drawRect(0, 0, problemFrameX, problemFrameY);
        // draw the header
        g.drawString(title+":", 10, 16);
        // draw the ground line
        g.setColor(Color.black);
        g.drawLine((int)problemOriginX,
                   (int)problemOriginY,
                   (int)(problemOriginX+inclineL*cost),
                   (int)(problemOriginY));
        // draw the incline line
        g.drawLine((int)problemOriginX,
                   (int)problemOriginY,
                   (int)(problemOriginX+inclineL*cost),
                   (int)(problemOriginY-inclineL*sint));
        g.setColor(Color.green);
        g.fillPolygon(problemXs,problemYs,5);
        // draw the center dot
        g.setColor(Color.black);
        g.fillOval(problemDotLeftX,problemDotTopY,
                  (int)(2*problemDotHalfW),(int)(2*problemDotHalfW));
        // write the weight
        String string = "Block's weight = " + gForce + units;
        g.setColor(Color.black);
        g.drawString(string,problemEqnX,problemEqnY);

        // title the unit vectors
        uOriginX = problemEqnX + 112;
        uOriginY = problemEqnY + 35;
        sPlot(g, "Unit vectors: ", -110, 5);

        // set up to draw the unit vectors
        g.setColor(Color.blue);
        int lineL = 20;
        int arroL = 7;
        int arroS = 3;

        // draw the j unit vector
        // draw arrow shank
        lPlot(g, 0,0, 0,lineL);
        // draw arrowhead
        lPlot(g, 0,lineL, +arroS,lineL-arroL);
        lPlot(g, 0,lineL, -arroS,lineL-arroL);
        sPlot(g, "j", -lineL/2+1,lineL/2);
        cIjPlot(f, g, -lineL/2+1,lineL/2);

        // draw the i unit vector
        // draw arrow shank
        lPlot( g, 0,0, lineL,0);
        // draw arrowhead
        lPlot(g, lineL,0, lineL-arroL, arroS);
        lPlot(g, lineL,0, lineL-arroL,-arroS);
        // draw letter
        sPlot(g, "i", lineL+3,-4);
        cIjPlot(f, g, lineL+3,-4);

        uOriginX += 3*lineL+7;

        // draw the y unit vector
        // draw arrow shank
        uPlot(g,0,0,0,lineL);
        // draw arrowhead
        uPlot(g, 0,lineL, +arroS,lineL-arroL);
        uPlot(g, 0,lineL, -arroS,lineL-arroL);
        // draw letter
        sRPlot(g, "y", -lineL/2-5,lineL/2+5);
        cRXyPlot(f, g, -lineL/2-5,lineL/2+5);

        // draw the x unit vector
        // draw arrow shank
        uPlot(g, 0,0, lineL,0);
        // draw arrowhead
        uPlot(g, lineL,0, lineL-arroL,+arroS);
        uPlot(g, lineL,0, lineL-arroL,-arroS);
        // draw letter
        sRPlot(g, "x", lineL+3,-4);
        cRXyPlot(f, g, lineL+3,-4);
    }

    // rotate and shift a point for plotting
    private void uPlot (Graphics g, int x1, int y1, int x2, int y2) {
        int nx1, ny1, nx2, ny2;
        nx1 = (int)(x1*cost - y1*sint);
        ny1 = (int)(x1*sint + y1*cost);
        nx2 = (int)(x2*cost - y2*sint);
        ny2 = (int)(x2*sint + y2*cost);
        nx1 = (int)( nx1 + uOriginX);
        ny1 = (int)(-ny1 + uOriginY);
        nx2 = (int)( nx2 + uOriginX);
        ny2 = (int)(-ny2 + uOriginY);
        g.drawLine(nx1, ny1, nx2, ny2);
    }

    // shift a line for plotting
    private void lPlot (Graphics g, int x1, int y1, int x2, int y2) {
        int nx1, ny1, nx2, ny2;
        nx1 = (int)( x1 + uOriginX);
        ny1 = (int)(-y1 + uOriginY);
        nx2 = (int)( x2 + uOriginX);
        ny2 = (int)(-y2 + uOriginY);
        g.drawLine(nx1, ny1, nx2, ny2);
    }

    // shift a string for plotting
    private void sPlot (Graphics g, String s, int x, int y) {
        int nx, ny;
        nx = (int)( x + uOriginX);
        ny = (int)(-y + uOriginY);
        g.drawString(s, nx, ny);
    }

    // shift a string for plotting
    private void sRPlot (Graphics g, String s, int x, int y) {
        int nx, ny;
        nx = (int)(x*cost - y*sint);
        ny = (int)(x*sint + y*cost);
        nx = (int)( nx + uOriginX);
        ny = (int)(-ny + uOriginY);
        g.drawString(s, nx, ny);
    }

    // shift an ij carot for plotting
    private void cIjPlot (CanvasEquation f, Graphics g, int x, int y) {
        int nx, ny;
        nx = (int)( x + uOriginX);
        ny = (int)(-y + uOriginY);
        f.addIjCarot(g, nx, ny);
    }

    // shift an xy carot for plotting
    private void cRXyPlot (CanvasEquation f, Graphics g, int x, int y) {
        int nx, ny;
        nx = (int)(x*cost - y*sint);
        ny = (int)(x*sint + y*cost);
        nx = (int)( nx + uOriginX);
        ny = (int)(-ny + uOriginY);
        f.addXyCarot(g, nx, ny);
    }

    // "get" methods for UserCanvas
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

    // draw apparatus for UserCanvas
    public void drawUserApparatus (Graphics g) {
        // draw the incline line
        g.setColor(Color.black);
        g.drawLine((int)(userOriginX+0.18*inclineL*cost),
                   (int)(userOriginY-0.18*inclineL*sint),
                   (int)(userOriginX+0.8*inclineL*cost),
                   (int)(userOriginY-0.8*inclineL*sint));
        g.setColor(Color.green);
        g.fillPolygon(userXs,userYs,5);
        // draw the center dot
        g.setColor(Color.black);
        g.fillOval(userDotLeftX,userDotTopY,(int)(2*userDotHalfW),(int)(2*userDotHalfW));
    }
}
//-------------------------------------------------------------------------------
