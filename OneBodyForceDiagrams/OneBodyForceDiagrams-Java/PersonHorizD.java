// PersonHorizD.java
// copyright, Peter Signell, 5/19/97
//-------------------------------------------------------------------------------
import java.awt.*;
import java.lang.Math;
//-------------------------------------------------------------------------------
class PersonHorizD implements ProblemData, Forceable {

    private String title = "Horizontally accelerating person";
    int[][] truAnsX = new int[4][2]; int[][] truAnsY = new int[4][2];
    int[]   truHdsX = new int[4]; int[] truHdsY = new int[4];
    private int uOriginX = 0;
    private int uOriginY = 0;

    int    gForce    = 17;
    int    mA        = 10;
    double thetaDeg  = 0f;
    int    problemDotHalfW = 1;
    int    userDotHalfW = 2;
    double userOriginX = 70f;
    double userOriginY = 170f;
    String units     = "N";
    double scale     = 1.0/5.0;
    double problemScale = 0.6;
    int    numForces = 3;
    int[]  matchForceComps = {1,1,1};
    int    maxTriesEachForce = 2;
    int    numEquations = 1;
    String initialNoteString = "At left note weight & unit vectors, then click";
    String[] resultsSpacingStrings = {"resultant","9","19"};
    String[] blinkerStrings = {"resultant","gravity","surface"};
    String[] messageStrings = {"Mouse-draw the "," force on the person:"};
    String[] forceNames = {"resultant","gravity","surface"};
    double problemOriginX = 62f * problemScale;
    double problemOriginY = 190f * problemScale;
    double inclineL = 160f;
    double inclineHalfL = inclineL/2;
    int tailPosX = 146;
    int tailPosY = 110;
    double   theta, cost, sint;
    double[] cosSin = new double[2];
    double phi, cosp, sinp;
    double xOffset, yOffset;
    //---------------------------------------------------------------------------
    // the PersonHorizD constructor:
    public PersonHorizD (String an, String w, String ac) {
        String angleIn = an;
        String weightIn = w;
        String accelerationIn = ac;
        if (!angleIn.equals("none")) {thetaDeg = (double) Integer.parseInt(angleIn);}
        if (!weightIn.equals("none")) {gForce = Integer.parseInt(weightIn);}
        if (!accelerationIn.equals("none")) {mA = Integer.parseInt(accelerationIn);}
        phi = Math.atan((double)mA/(double)gForce);
        cosp = Math.cos(phi);
        sinp = Math.sin(phi);
        theta = thetaDeg * 3.14159 / 180;
        cost = Math.cos(theta);
        sint = Math.sin(theta);
        cosSin[0] = cost; cosSin[1] = sint;
        xOffset = 0.4*inclineL*cost;
        yOffset = 0.4*inclineL*sint+26;
        // resultant force
        truAnsX[0][0] = (int) Math.round(mA);
        truAnsY[0][0] = 0;
        // gravity force
        truAnsX[1][0] = 0;
        truAnsY[1][0] = gForce;
        // sutface force
        truAnsX[2][0] = (int) Math.round(mA);
        truAnsY[2][0] = (int) Math.round(-gForce);
        // friction force (not asked for)
        truAnsX[3][0] = (int) Math.round(( gForce * sint + mA) * cost);
        truAnsY[3][0] = (int) Math.round((-gForce * sint - mA) * sint);
        if (numEquations > 1) {
            // resultant force
            truAnsX[0][1] = (int) Math.round(-gForce * sint);
            truAnsY[0][1] = (int) Math.round( gForce * cost);
            // gravity force
            truAnsX[1][1] = (int) Math.round(-gForce * sint);
            truAnsY[1][1] = (int) Math.round( gForce * cost);
            // normal (anti-compression) force
            truAnsX[2][1] = 0;
            truAnsY[2][1] = (int) Math.round(-gForce * cost);
            // friction force
            truAnsX[3][1] = (int) Math.round( gForce * sint + mA);
            truAnsY[3][1] = 0;
        }
        // get the screen positions of the force components
        truHdsX[0] = (int) Math.round(tailPosX + truAnsX[0][0]/scale);
        truHdsY[0] = (int) Math.round(tailPosY + truAnsY[0][0]/scale);
        truHdsX[1] = (int) Math.round(tailPosX + truAnsX[1][0]/scale);
        truHdsY[1] = (int) Math.round(tailPosY + truAnsY[1][0]/scale);
        truHdsX[2] = (int) Math.round(tailPosX + truAnsX[2][0]/scale);
        truHdsY[2] = (int) Math.round(tailPosY + truAnsY[2][0]/scale);
    }
    //---------------------------------------------------------------------------
    public void drawProblem (Graphics g, CanvasEquation f) {
        // draw a border
        g.drawRect(0, 0, problemFrameX, problemFrameY);
        // draw the header
        g.drawString(title+":", 10, 16);
        // draw the ground line
        g.drawLine((int)problemOriginX,
                   (int)problemOriginY,
                   (int)(problemOriginX+inclineL*cost),
                   (int)(problemOriginY));
        // draw the incline line
        g.drawLine((int)problemOriginX,
                   (int)problemOriginY,
                   (int)(problemOriginX+inclineL*cost),
                   (int)(problemOriginY-inclineL*sint));
        g.setColor(Color.black);
        // draw the person: uOrigin is c.g.
        uOriginX = (int)(problemOriginX+xOffset);
        uOriginY = (int)(problemOriginY-yOffset);
        // torso
        lPlot(g, (int)(-8*sinp),(int)(-8*cosp), (int)(14*sinp),(int)(14*cosp));
        // head
        cPlot(g, (int)(20*sinp),(int)(20*cosp), 6);
        // eye
        fcPlot(g, (int)(20*sinp+4),(int)(20*cosp+2), 1);
        // c.g.
        fcPlot(g, 0,0, 2);
        // forward leg
        lPlot(g, (int)(-8*sinp),(int)(-8*cosp), 12,-4);
        lPlot(g, 12,-4, 12,-14);
        lPlot(g, 12,-14, 15,-12);
        // back leg
        lPlot(g, (int)(-8*sinp),(int)(-8*cosp), (int)(-22*sinp),(int)(-22*cosp));
        lPlot(g, (int)(-22*sinp),(int)(-22*cosp), (int)(-yOffset*sinp/cosp-8),(int)(-yOffset));
        // write the weight
        String string = "Person's weight = " + gForce + units;
        g.setColor(Color.black);
        g.drawString(string,problemEqnX-40,problemEqnY);
		// write ma, the mass times acceleration, as a vector
        string = "Person's mass times acceleration = " + mA;
        g.drawString(string,problemEqnX-40,problemEqnY + 20);
        FontMetrics fm;
        fm=g.getFontMetrics(g.getFont());
        int y =  problemEqnY + 20;
        int x = problemEqnX-40 + fm.stringWidth(string);
        string = "i";
        g.drawString(string, x, y);
        addXyCarot(g, x, y);
        x += fm.stringWidth(string);
        g.drawString(units, x, y);

        // title the unit vectors
        uOriginX = problemEqnX + 112;
        uOriginY = problemEqnY + 55;
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

        // draw the y and x unit vectors
        if (numEquations > 1) {
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
    }
    //---------------------------------------------------------------------------
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
    //---------------------------------------------------------------------------
    // shift a line for plotting
    private void lPlot (Graphics g, int x1, int y1, int x2, int y2) {
        int nx1, ny1, nx2, ny2;
        nx1 = (int)( x1 + uOriginX);
        ny1 = (int)(-y1 + uOriginY);
        nx2 = (int)( x2 + uOriginX);
        ny2 = (int)(-y2 + uOriginY);
        g.drawLine(nx1, ny1, nx2, ny2);
    }
    //---------------------------------------------------------------------------
    // shift a circle for plotting
    private void cPlot (Graphics g, int x1, int y1, int r) {
        int nx1, ny1, nx2, ny2, x2, y2;
        nx1 = (int)( x1 + uOriginX);
        ny1 = (int)(-y1 + uOriginY);
        g.drawOval((int)(nx1-r), (int)(ny1-r), (int)(2*r), (int)(2*r));
    }
    //---------------------------------------------------------------------------
    // shift a filled circle for plotting
    private void fcPlot (Graphics g, int x1, int y1, int r) {
        int nx1, ny1;
        nx1 = (int)( x1 + uOriginX);
        ny1 = (int)(-y1 + uOriginY);
        g.fillOval((int)(nx1-r), (int)(ny1-r), (int)(2*r), (int)(2*r));
    }
    //---------------------------------------------------------------------------
    // shift a string for plotting
    private void sPlot (Graphics g, String s, int x, int y) {
        int nx, ny;
        nx = (int)( x + uOriginX);
        ny = (int)(-y + uOriginY);
        g.drawString(s, nx, ny);
    }
    //---------------------------------------------------------------------------
    // shift a string for plotting
    private void sRPlot (Graphics g, String s, int x, int y) {
        int nx, ny;
        nx = (int)(x*cost - y*sint);
        ny = (int)(x*sint + y*cost);
        nx = (int)( nx + uOriginX);
        ny = (int)(-ny + uOriginY);
        g.drawString(s, nx, ny);
    }
    //---------------------------------------------------------------------------
    // shift an ij carot for plotting
    private void cIjPlot (CanvasEquation f, Graphics g, int x, int y) {
        int nx, ny;
        nx = (int)( x + uOriginX);
        ny = (int)(-y + uOriginY);
        f.addIjCarot(g, nx, ny);
    }
    //---------------------------------------------------------------------------
    // shift an xy carot for plotting
    private void cRXyPlot (CanvasEquation f, Graphics g, int x, int y) {
        int nx, ny;
        nx = (int)(x*cost - y*sint);
        ny = (int)(x*sint + y*cost);
        nx = (int)( nx + uOriginX);
        ny = (int)(-ny + uOriginY);
        f.addXyCarot(g, nx, ny);
    }
    //---------------------------------------------------------------------------
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
    //---------------------------------------------------------------------------
    // draw apparatus for UserCanvas
    public void drawUserApparatus (Graphics g) {
        g.setColor(Color.black);
        // draw the person
        uOriginX = tailPosX;
        uOriginY = tailPosY;
        // torso
        lPlot(g, (int)(-8*sinp),(int)(-8*cosp), (int)(14*sinp),(int)(14*cosp));
        // head
        cPlot(g, (int)(20*sinp),(int)(20*cosp), 6);
        // eye
        fcPlot(g, (int)(20*sinp+4),(int)(20*cosp+2), 1);
        // c.g.
        fcPlot(g, 0,0, 2);
        // forward leg
        lPlot(g, (int)(-8*sinp),(int)(-8*cosp), 12,-4);
        lPlot(g, 12,-4, 12,-14);
        lPlot(g, 12,-14, 15,-12);
        // back leg
        lPlot(g, (int)(-8*sinp),(int)(-8*cosp), (int)(-22*sinp),(int)(-22*cosp));
        lPlot(g, (int)(-22*sinp),(int)(-22*cosp), (int)(-yOffset*sinp/cosp-8),(int)(-yOffset));
        // draw the incline line
        lPlot(g, 0,-26, (int)( 0.3*inclineL*cost), (int)(-26+0.3*inclineL*sint));
        lPlot(g, 0,-26, (int)(-0.2*inclineL*cost), (int)(-26-0.2*inclineL*sint));
    }
    //---------------------------------------------------------------------------
    // add a carot over x and y to make unit vector symbols
    public void addXyCarot(Graphics g, int x, int y){
        g.drawLine(x + 1, y -9, x + 3, y -11);
        g.drawLine(x + 5, y -9, x + 3, y -11);
    }
    //---------------------------------------------------------------------------
}
