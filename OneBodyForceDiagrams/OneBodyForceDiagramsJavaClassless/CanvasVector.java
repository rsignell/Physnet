/*
==========================================================================
 CanvasVector.java
 copyright, Peter Signell, 4/4/97
==========================================================================
*/
import java.awt.*;
import java.lang.Math;
/*
==========================================================================
*/
class CanvasVector implements GeneralData {

    private double headHalfWidth;
    private double headLength;
    private int    forceNumber;

    private CanvasEquation canvasEquation;

    // calculated here:
    private double offsetFac, shankRed, offsetx, offsety, shankx, shanky;
    private double vRelx, vRely, vectorL;
    //---------------------------------------------------------------------------
    // the class constructor:
    public CanvasVector (Graphics g) {
        headHalfWidth = 6f;
        headLength = 20f;
    }
    //---------------------------------------------------------------------------
    public void setheadHalfW(double w) {
        headHalfWidth = w;
    }
    //---------------------------------------------------------------------------
    public void headL(double l) {
        headLength = l;
    }
    //---------------------------------------------------------------------------
    public void plotVector(CanvasEquation ce, int f, String fN,
                           int tx, int ty, int hx, int hy, Graphics g) {
        // sets up to plot a "correct answer" vector for force "f"; plotForceLabel is in CE
        canvasEquation = ce;
        forceNumber = f;
        int tailPosX = tx;
        int tailPosY = ty;
        int truHdsX = hx;
        int truHdsY = hy;
        plotVector(userTruVecColor, tailPosX, tailPosY, truHdsX, truHdsY, g);
        canvasEquation.plotForceLabel(fN, hx + 7, hy + 7, g);
    }
    //---------------------------------------------------------------------------
    public void plotVector(int tx, int ty, int hx, int hy, Graphics g) {
        // sets up to plot a "user generated" vector
        plotVector(userVectorColor, tx, ty, hx, hy, g);
    }
    //---------------------------------------------------------------------------
    public void plotVector(Color vectorColor, int tx, int ty, int hx, int hy, Graphics g) {
        // plots an "any source" vector
        int[] headsX = new int[4]; int[] headsY = new int[4];
        int tailPosX, tailPosY, headPosX, headPosY;
        double vectorDX, vectorDY;
        double shankX, shankY;
        double offsetX, offsetY;
        double vectorLength;
    
        tailPosX = tx; tailPosY = ty;
        headPosX = hx; headPosY = hy;
        
        vectorDX = headPosX - tailPosX;
        vectorDY = headPosY - tailPosY;
        vectorLength = Math.sqrt(vectorDX * vectorDX + vectorDY * vectorDY);
        offsetFac = headHalfWidth/vectorLength;
        shankRed =  headLength/vectorLength;
        if (vectorLength < headLength) {
            offsetFac = offsetFac*vectorLength/headLength;
            shankRed = 1f;
        }
        shankX = headPosX - vectorDX * shankRed;
        shankY = headPosY - vectorDY * shankRed;
        offsetX = vectorDY * offsetFac;
        offsetY = vectorDX * offsetFac;
        headsX[0] = (int) Math.round(shankX + offsetX);
        headsX[1] = (int) Math.round(shankX - offsetX);
        headsX[2] = (int) Math.round(headPosX);
        headsY[0] = (int) Math.round(shankY - offsetY);
        headsY[1] = (int) Math.round(shankY + offsetY);
        headsY[2] = (int) Math.round(headPosY);
        g.setColor(vectorColor);
        g.drawLine(tailPosX, tailPosY, headsX[2], headsY[2]);
        g.fillPolygon(headsX,headsY,3);
    }
    //---------------------------------------------------------------------------
}
/*
==========================================================================
*/
