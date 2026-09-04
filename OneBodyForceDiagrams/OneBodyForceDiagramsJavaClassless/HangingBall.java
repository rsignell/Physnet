// HangingBall.java
// copyright, Peter Signell, April 30, 1997, April 16, 2008
//-------------------------------------------------------------------------------
import java.awt.*;
import java.lang.Math;
//-------------------------------------------------------------------------------
class HangingBall implements ProblemData, Forceable {

    private String title = "A ball hanging on a string";

    int tailPosX, tailPosY;
    int[][] truAnsX = new int[4][2]; int[][] truAnsY = new int[4][2];
    int[] truHdsX = new int[4]; int[] truHdsY = new int[4];

    int    gForce    = 20;
    double thetaDeg  = 90f;
    double pithHalfW = 14f;
    double stringL   = 130f;
    double originX   = 130f;
    double originY   = 0f;
    String units     = "N";
    double scale     = 1f/5;
    int    maxTriesEachForce = 1;
    Color  pithballColor = Color.green;
    double problemScale = 0.6f;
    // the number of forces to cycle through
    int    numForces = 3;
    // the number of coordinate systems used  in this problem ("number of equations")
    int    numEquations = 1;
    // for each force in turn: the number of the coordinate system in which to match the answer (1 or 2)
    int[]  matchForceComps = {1,1,1};
    // message to appear before any forces are mentioned
    String initialNoteString = "At left note weight & unit vectors, then click";
    // {longest force name, longest x-component answer, longest y-component answer} among the complete set of true-answer vectors
    String[] resultsSpacingStrings = {"resultant","0","19"};
    // the force name to blink for each force in turn
    String[] blinkerStrings = {"resultant","gravity","string"};
    // the words to appear on each side of the blinking force name, each turn
    String[] messageStrings = {"Mouse-draw the "," force on the ball:"};
    // the names of the forces to draw, in turn
    String[] forceNames = {"resultant","gravity","string"};
    double problemStringL;
    double problemCeilHalfL;
    double problemPithHalfW;
    double spotHalfW;
    double problemPlusHW;
    double problemOriginX;
    double problemOriginY;
    double problemAnchorX;
    double problemAnchorY;
    int    pithPosX, pithPosY;
    int    pithExtentX, pithExtentY;
    double theta, cost, sint;
    double[] cosSin = new double[2];
    int    accelX = 0;

    // the HangingBall constructor:
    public HangingBall (String an, String w, String ac) {
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
        problemStringL = 100f * problemScale;
        problemCeilHalfL = 100f * problemScale;
        problemPithHalfW = 14f * problemScale;
        spotHalfW = 2f * problemScale;
        problemOriginX = 230f * problemScale;
        problemOriginY = 50f * problemScale;
        problemAnchorX = problemOriginX + problemStringL * Math.cos(theta);
        problemAnchorY = problemOriginY + problemStringL * Math.sin(theta);
        // first index is force number - 1, second index is coordinate system number - 1; Y signs get reversed!
		// the resultant forcanswer
        truAnsX[0][0] = 0;
        truAnsY[0][0] = 0;
        // the gravity force answer
        truAnsX[1][0] = 0;
        truAnsY[1][0] = gForce;
        // the string force answer
        truAnsX[2][0] = 0;
        truAnsY[2][0] = -gForce;

        tailPosX = (int) Math.round(originX + stringL * Math.cos(theta));
        tailPosY = (int) Math.round(originY + stringL * Math.sin(theta));
        // the resultant force
        truHdsX[0] = (int) Math.round(tailPosX + truAnsX[0][0]/scale);
        truHdsY[0] = (int) Math.round(tailPosY + truAnsY[0][0]/scale);
        // the gravity force
        truHdsX[1] = (int) Math.round(tailPosX + truAnsX[1][0]/scale);
        truHdsY[1] = (int) Math.round(tailPosY + truAnsY[1][0]/scale);
        // the string force
        truHdsX[2] = (int) Math.round(tailPosX + truAnsX[2][0]/scale);
        truHdsY[2] = (int) Math.round(tailPosY + truAnsY[2][0]/scale);
        // the ball bounding box upper left corner position
        pithPosX = tailPosX - (int) Math.round(pithHalfW);
        pithPosY = tailPosY - (int) Math.round(pithHalfW);
        // the ball diameter
        pithExtentX = (int) Math.round(2*pithHalfW);
        pithExtentY = (int) Math.round(2*pithHalfW);
    }

    public void drawProblem (Graphics g, CanvasEquation f) {
        // draw a border
        g.drawRect(0, 0, problemFrameX, problemFrameY);

        g.drawString(title+":", 10, 16);
        // draw ceiling
        g.setColor(Color.black);
        g.drawLine((int)(problemOriginX - problemCeilHalfL),(int)problemOriginY,
                   (int)(problemOriginX + problemCeilHalfL),(int)problemOriginY);

        // draw string, ball
        g.drawLine((int)problemOriginX, (int)problemOriginY,
                   (int)problemOriginX, (int)problemAnchorY);
        //           (int)(2*problemOriginX - problemAnchorX),(int)problemAnchorY);
        g.setColor(Color.green);
        g.fillOval((int)(2*problemOriginX - problemAnchorX-problemPithHalfW),
                   (int)(problemAnchorY - problemPithHalfW),
                   (int)(2*problemPithHalfW), (int)(2*problemPithHalfW));
        g.setColor(Color.black);
        g.drawLine((int)(2*problemOriginX - problemAnchorX - problemPlusHW),
                   (int)problemAnchorY,
                   (int)(2*problemOriginX - problemAnchorX + problemPlusHW),
                   (int)problemAnchorY);

        // present the pithball weight
        String string = "The ball's weight = " + gForce + units;
        g.setColor(Color.black);
        g.drawString(string,problemEqnX,problemEqnY);
        // display the unit vectors
        int uOriginx = problemEqnX + 112;
        int uOriginy = problemEqnY + 35;
        g.drawString("Unit vectors: ",uOriginx - 110,uOriginy - 5);

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
        g.setColor(Color.black);
        g.drawLine((int) originX, (int) originY, tailPosX,tailPosY);
        g.setColor(pithballColor);
        g.fillOval(pithPosX, pithPosY, pithExtentX, pithExtentY);
        g.setColor(Color.black);
        g.fillOval(pithPosX+(int)pithHalfW-(int)spotHalfW,
                   pithPosY+(int)pithHalfW-(int)spotHalfW,
                   (int)(2*spotHalfW),(int)(2*spotHalfW));
    }
}
