// PulleyHorizSP4.java
// copyright, Peter Signell, 5/19/97
//-------------------------------------------------------------------------------
import java.awt.*;
import java.lang.Math;
//-------------------------------------------------------------------------------
class PulleyHorizSP4 implements ProblemData, Forceable {

    private String title = "Stationary person holds block over pulley";

    int[][] truAnsX = new int[4][2]; int[][] truAnsY = new int[4][2];
    int[]   truHdsX = new int[4]; int[] truHdsY = new int[4];
    private int uOriginX = 0;
    private int uOriginY = 0;

    int    gForce    = 17;
    int    mA        = 0;
    double  thetaDeg  = 0f;
    int    problemDotHalfW = 1;
    int    userDotHalfW = 2;
    double  userOriginX = 70f;
    double  userOriginY = 170f;
    String units     = "N";
    double  scale     = 1f/2.6f;
    double  problemScale = 0.6f;
    int    numForces = 4;
    int[]  matchForceComps = {1,1,1,1};
    int    maxTriesEachForce = 1;
    int    numEquations = 1;
    String initialNoteString = "At left note weight & unit vectors, then click";
    String[] resultsSpacingStrings = {"friction","17","34"};
    String[] blinkerStrings = {"gravity","string","normal surface","frictional surface"};
    String[] messageStrings = {"Mouse-draw the "," force on the person:"};
    String[] forceNames = {"gravity","string","normal","friction"};
    double problemOriginX = 62f * problemScale;
    double problemOriginY = 190f * problemScale;
    double inclineL = 160f;
    double inclineHalfL = inclineL/2;
    int tailPosX = 146;
    int tailPosY = 110;
    double  theta, cost, sint;
    double[] cosSin = new double[2];
    int wHang = gForce;
    int wPers = 2 * wHang;
    double fAngle = Math.atan2(wHang,wPers);
    
    double fCos = Math.cos(fAngle);
    double fSin = Math.sin(fAngle);

    //---------------------------------------------------------------------------
    // the PulleyHorizSP4 constructor:
    public PulleyHorizSP4 (String an, String w, String ac) {
        String angleIn = an;
        String weightIn = w;
        String accelerationIn = ac;
        if (!angleIn.equals("none")) {thetaDeg = (double) Integer.parseInt(angleIn);}
        if (!weightIn.equals("none")) {gForce = Integer.parseInt(weightIn);}
        if (!accelerationIn.equals("none")) {mA = Integer.parseInt(accelerationIn);}
        theta = thetaDeg * 3.14159 / 180;
        cost = Math.cos(theta);
        sint = Math.sin(theta);
        cosSin[0] = cost; cosSin[1] = sint;
        // gravity force
        truAnsX[0][0] = 0;
        truAnsY[0][0] = wPers;
        // string force
        truAnsX[1][0] = wHang;
        truAnsY[1][0] = 0;
        // normal force
        truAnsX[2][0] = 0;
        truAnsY[2][0] = -wPers;
        // friction force
        truAnsX[3][0] = -wHang;
        truAnsY[3][0] = 0;
        if (numEquations > 1) {
            // gravity force
            truAnsX[0][1] = (int) Math.round(-gForce * sint);
            truAnsY[0][1] = (int) Math.round( gForce * cost);
            // normal (anti-compression) force
            truAnsX[1][1] = 0;
            truAnsY[1][1] = (int) Math.round(-gForce * cost);
            // friction force
            truAnsX[2][1] = (int) Math.round( gForce * sint + mA);
            truAnsY[2][1] = 0;
        }
        // get the screen positions of the force components
        for (int n=0;n<=numForces-1;n++) {
            truHdsX[n] = (int) Math.round(tailPosX + truAnsX[n][0]/scale);
            truHdsY[n] = (int) Math.round(tailPosY + truAnsY[n][0]/scale);
        }
    }
    //---------------------------------------------------------------------------
    public void drawProblem (Graphics g, CanvasEquation f) {
        // draw a border
        g.drawRect(0, 0, problemFrameX, problemFrameY);
        // draw the header
        g.drawString(title+":", 10, 16);
        // draw the ground line
        g.setColor(Color.black);
        // draw the supporting surface
        uOriginX = (int)(problemOriginX+35);
        uOriginY = (int)(problemOriginY-75);
        // body
        lPlot(g, (int)(8+16*fSin),(int)(-26-16*fCos), (int)(8-34*fSin),(int)(-26+34*fCos));
        // CM
        fcPlot(g, (int)(8-13*fSin),(int)(-26+13*fCos), 2);
        // head
        cPlot(g, (int)(8-34*fSin),(int)(-20+34*fCos), 6);
        // eye
        fcPlot(g, (int)(10-34*fSin),(int)(-19+34*fCos), 1);
        // arms
        lPlot(g, (int)(8-26*fSin),(int)(-26+26*fCos), (int)(30-26*fSin),(int)(-26+13*fCos));
        // hands
        cPlot(g, (int)(30-26*fSin),(int)(-26+13*fCos), 2);
        // string
        lPlot(g, (int)(8-13*fSin),(int)(-26+13*fCos), (int)(128-13*fSin),(int)(-26+13*fCos));
        // pulley
        cPlot(g, (int)(128-13*fSin),(int)(-36+13*fCos), 10);
        cPlot(g, (int)(128-13*fSin),(int)(-36+13*fCos), 1);
        // pulley support
        lPlot(g, (int)(126-34*fSin),(int)(-26-16*fCos), (int)(128-13*fSin),(int)(-36+13*fCos));
        // vert string
        lPlot(g, (int)(138-13*fSin),(int)(-36+13*fCos), (int)(138-13*fSin),(int)(-66+13*fCos));
        // block
        rPlot(g, (int)(128-13*fSin),(int)(-66+13*fCos), 20,20);
        fcPlot(g, (int)(138-13*fSin),(int)(-76+13*fCos), 2);
        // ground
        lPlot(g, (int)(-28+16*fSin),(int)(-26-16*fCos), (int)(126-34*fSin),(int)(-26-16*fCos));
        //lPlot(g, (int)(126-34*fSin),(int)(-26-16*fCos), (int)(146.6-26*fSin),(int)(-37.4+26*fCos));
        lPlot(g, (int)(126-34*fSin),(int)(-26-16*fCos), (int)(126-34*fSin),(int)(-66-16*fCos));
        // write the weight
        String string = "Block's weight = " + wHang + units;
        g.setColor(Color.black);
        g.drawString(string,problemEqnX-40,problemEqnY);
        string = "Person's weight = " + wPers + units;
        g.drawString(string,problemEqnX-40,problemEqnY+20);
        
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
    // shift a rectangle for plotting
    private void rPlot (Graphics g, int x1, int y1, int wX, int hY) {
        int nx1, ny1;
        nx1 = (int)( x1 + uOriginX);
        ny1 = (int)(-y1 + uOriginY);
        g.drawRect(nx1,ny1, wX, hY);
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
    public double    getScale()                {return scale                ;}
    public String[] getResultsSpacingStrings(){return resultsSpacingStrings;}
    public String[] getBlinkerStrings()       {return blinkerStrings       ;}
    public String   getInitialNoteString()    {return initialNoteString    ;}
    public String[] getMessageStrings()       {return messageStrings       ;}
    public String[] getForceNames()           {return forceNames           ;}
    public int[][]  getTruAnsX()              {return truAnsX              ;}
    public int[][]  getTruAnsY()              {return truAnsY              ;}
    public int      getTailPosX()             {return tailPosX             ;}
    public int      getTailPosY()             {return tailPosY             ;}
    public double[]  getCosSin()               {return cosSin               ;}
    public int[]    getTruHdsX()              {return truHdsX              ;}
    public int[]    getTruHdsY()              {return truHdsY              ;}
    //---------------------------------------------------------------------------
    // draw apparatus for UserCanvas
    public void drawUserApparatus (Graphics g) {
        g.setColor(Color.black);
        uOriginX = (int)(tailPosX-7+13*fSin);
        uOriginY = (int)(tailPosY-2-13*fCos);
        // body
        lPlot(g, (int)(8+16*fSin),(int)(-26-16*fCos), (int)(8-34*fSin),(int)(-26+34*fCos));
        // CM
        fcPlot(g, (int)(8-13*fSin),(int)(-26+13*fCos), 2);
        // head
        cPlot(g, (int)(8-34*fSin),(int)(-20+34*fCos), 6);
        // eye
        fcPlot(g, (int)(10-34*fSin),(int)(-19+34*fCos), 1);
        // arms
        lPlot(g, (int)(8-26*fSin),(int)(-26+26*fCos), (int)(30-26*fSin),(int)(-26+13*fCos));
        // hands
        cPlot(g, (int)(30-26*fSin),(int)(-26+13*fCos), 2);
        // string
        lPlot(g, (int)(8-13*fSin),(int)(-26+13*fCos), (int)(38-13*fSin),(int)(-26+13*fCos));
        // ground
        lPlot(g, (int)(-28+16*fSin),(int)(-26-16*fCos), (int)(38+16*fSin),(int)(-26-16*fCos));
    }
    //---------------------------------------------------------------------------
    // add a carot over x and y to make unit vector symbols
    public void addXyCarot(Graphics g, int x, int y){
        g.drawLine(x + 1, y -9, x + 3, y -11);
        g.drawLine(x + 5, y -9, x + 3, y -11);
    }
    //---------------------------------------------------------------------------
}
