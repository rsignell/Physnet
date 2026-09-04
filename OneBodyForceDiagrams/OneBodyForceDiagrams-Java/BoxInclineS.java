// BoxInclineS.java
// copyright, Peter Signell, May 1, 1997
//-------------------------------------------------------------------------------
import java.awt.*;
import java.lang.Math;
//-------------------------------------------------------------------------------
class BoxInclineS implements ProblemData, Forceable {

    private String title = "Stationary block on an inclined plane";

    int[][] truAnsX = new int[4][2]; int[][] truAnsY = new int[4][2];
    int[] truHdsX = new int[4]; int[] truHdsY = new int[4];

    int    gForce    = 20;
    double thetaDeg  = 25f;
    int    problemDotHalfW = 1;
    int    userDotHalfW = 2;
    double userOriginX = 110f;
    double userOriginY = 170f;
    String units     = "N";
    double scale     = 1f/5;
    double problemScale = 0.6f;
    int    numForces = 3;
    int    maxTriesEachForce = 1;
    int    numEquations = 1;
    int[]  matchForceComps = {1,1,1};
    String   initialNoteString = "At left note weight & unit vectors, then click";
    String[] resultsSpacingStrings = {"resultant","11","19"};
    String[] blinkerStrings = {"resultant","gravity","surface"};
    String[] messageStrings = {"Mouse-draw the "," force on the block:"};
    String[] forceNames = {"resultant","gravity","surface"};
    double   problemOriginX = 82f * problemScale;
    double   problemOriginY = 190f * problemScale;
    double   inclineL = 160f;
    double   boxHalfL = 20f;
    double   boxHalfH = 14f;
    double   inclineHalfL = inclineL/2;
    double   theta, cost, sint;
    double[] cosSin = new double[2];
    int   accelX = 0;
    int[] problemXs = new int[5];
    int[] problemYs = new int[5];
    int[] userXs = new int[5];
    int[] userYs = new int[5];
    int problemBoxCenterX;
    int problemBoxCenterY;
    int userBoxCenterX;
    int userBoxCenterY;
    int tailPosX;
    int tailPosY;
    int problemDotLeftX;
    int problemDotTopY;
    int userDotLeftX;
    int userDotTopY;

    // the BoxInclineS constructor:
    public BoxInclineS (String an, String w, String ac) {
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
        truAnsX[0][0] = 0;
        truAnsY[0][0] = 0;
        truAnsX[1][0] = 0;
        truAnsY[1][0] = gForce;
        truAnsX[2][0] = 0;
        truAnsY[2][0] = -gForce;
        truHdsX[0] = (int) Math.round(tailPosX + truAnsX[0][0]/scale);
        truHdsY[0] = (int) Math.round(tailPosY + truAnsY[0][0]/scale);
        truHdsX[1] = (int) Math.round(tailPosX + truAnsX[1][0]/scale);
        truHdsY[1] = (int) Math.round(tailPosY + truAnsY[1][0]/scale);
        truHdsX[2] = (int) Math.round(tailPosX + truAnsX[2][0]/scale);
        truHdsY[2] = (int) Math.round(tailPosY + truAnsY[2][0]/scale);
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
                   (int)(problemOriginX+inclineL*Math.cos(theta)),
                   (int)(problemOriginY));
        // draw the incline line
        g.drawLine((int)problemOriginX,
                   (int)problemOriginY,
                   (int)(problemOriginX+inclineL*Math.cos(theta)),
                   (int)(problemOriginY-inclineL*Math.sin(theta)));
        g.setColor(Color.green);
        g.fillPolygon(problemXs,problemYs,5);
        // draw the center dot
        g.setColor(Color.black);
        g.drawOval(problemDotLeftX,problemDotTopY,
                  (int)(2*problemDotHalfW),(int)(2*problemDotHalfW));
        // write the weight
        String string = "Block's weight = " + gForce + units;
        g.setColor(Color.black);
        g.drawString(string,problemEqnX,problemEqnY);

        // title the unit vectors
        int uOriginx = problemEqnX + 112;
        int uOriginy = problemEqnY + 35;
        g.drawString("Unit vectors: ",uOriginx - 110,uOriginy - 5);

        // set up to draw the unit vectors
        g.setColor(Color.blue);
        int lineL = 20;
        int arroL = 7;
        int arroS = 3;

        // draw the y unit vector
        g.drawLine(uOriginx, uOriginy, uOriginx, uOriginy - lineL);
        g.drawLine(uOriginx - arroS, uOriginy - lineL + arroL, uOriginx,uOriginy - lineL);
        g.drawLine(uOriginx + arroS, uOriginy - lineL + arroL, uOriginx,uOriginy - lineL);
        g.drawString("j", uOriginx - lineL/2 + 2, uOriginy - lineL/2);
        f.addIjCarot(g, uOriginx - lineL/2 + 2, uOriginy - lineL/2);

        // draw the x unit vector
        g.drawLine(uOriginx, uOriginy, uOriginx + lineL,uOriginy);
        g.drawLine(uOriginx + lineL - arroL, uOriginy - arroS, uOriginx + lineL,uOriginy);
        g.drawLine(uOriginx + lineL - arroL, uOriginy + arroS, uOriginx + lineL,uOriginy);
        g.drawString("i",uOriginx + lineL + 5, uOriginy + 3);
        f.addIjCarot(g,uOriginx + lineL + 5, uOriginy + 3);
        g.drawString(" ", uOriginx, uOriginy + 13);
    }
    // methods for UserCanvas
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

    // draw UserDisplay
    public void drawUserApparatus (Graphics g) {
        // draw the incline line
        g.setColor(Color.black);
        g.drawLine((int)userOriginX,
                   (int)userOriginY,
                   (int)(userOriginX+inclineL*cost),
                   (int)(userOriginY-inclineL*sint));
        g.setColor(Color.green);
        g.fillPolygon(userXs,userYs,5);
        // draw the center dot
        g.setColor(Color.black);
        g.fillOval(userDotLeftX,userDotTopY,(int)(2*userDotHalfW),(int)(2*userDotHalfW));
    }
}
