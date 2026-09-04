/*
=================================================================================
 CanvasEquation.java
 copyright, Peter Signell, 4/30/97
=================================================================================
*/
import java.awt.*;
import java.lang.Math;
/*
=================================================================================
*/
class CanvasEquation implements GeneralData {
    // calculated here:
    private FontMetrics fm;
    private String strFx, strFy, signy;
    private String F_paren, parens;
    private String[] char_i = new String[2];
    private String[] char_j_paren = new String[2];
    private int w_F_paren, w_parens;
    private int[] w_char_i = new int[2];
    private int userPosX, userPosY;
    private int userEquationNum;
    //---------------------------------------------------------------------------
    // the class constructor:
    public CanvasEquation (Graphics g) {
        F_paren         = "F("   ;
        parens          = ") = (";
        char_i[0]       = "i"    ;
        char_j_paren[0] = "j ) " ;
        char_i[1]       = "x"    ;
        char_j_paren[1] = "y ) " ;
        userEquationNum = 0;

        // get the widths of the constants
        setFonts(g);

        w_F_paren = fm.stringWidth(F_paren);
        w_parens  = fm.stringWidth(parens) ;
        w_char_i[0]  = fm.stringWidth(char_i[0]) ;
        w_char_i[1]  = fm.stringWidth(char_i[1]) ;
    }
    //---------------------------------------------------------------------------
    // This one is for plotting in the resultsCanvas
    public void plotEquation(Graphics g, int f, String[] fN, String u,
                             int x, int y, String[] wi, int e, int nF) {

        String forceName, signX, signY;
        int forceNumber, numForces, w_units;
        int forceCompX, forceCompY;
        String unit, fW, xW, yW;
        String[] forceNames = new String[4];
        
        forceNames = fN;
        numForces = nF;
        forceNumber = f;
        forceName = forceNames[forceNumber];
        userEquationNum = e;
        forceCompX = x;
        forceCompY = y;
        unit = u;
        w_units = (int) fm.stringWidth(unit);
        fW = wi[0]; xW = wi[1]; yW = wi[2];

        int eqnX = resultsEqnPosX[forceNumber];
        int eqnY = resultsEqnPosY[forceNumber]+
                   (int)(20*numForces*userEquationNum)+
                   (int)(7*userEquationNum);

        int beforeSign = 2;
        int afterSign = 0;
        int beforeUnitV = 0;

        if (forceCompX <  0) signX = "-"; else signX = "";
        if (forceCompY <= 0) signY = "+"; else signY = "-";
        String absCompX = Integer.toString(Math.abs(forceCompX));
        String absCompY = Integer.toString(Math.abs(forceCompY));
        int w_compX = (int) fm.stringWidth(absCompX);
        int w_compY = (int) fm.stringWidth(absCompY);
        int w_sign = (int) Math.max(fm.stringWidth("+"),fm.stringWidth("-"));
        int w_fW = (int) fm.stringWidth(fW);
        int w_xW = (int) fm.stringWidth(xW);
        int w_yW = (int) fm.stringWidth(yW);
        
        int[] w = new int[16];
        String[] s = new String[16];
        s[0]  = F_paren + forceName; w[0] = w_F_paren + w_fW;
        s[1]  = parens;        w[1]  = w_parens;
        s[2]  = "";            w[2]  = beforeSign;
        s[3]  = signX;         w[3]  = w_sign;
        s[4]  = "";            w[4]  = afterSign;
        s[5]  = "";            w[5]  = w_xW - w_compX;
        s[6]  = absCompX;      w[6]  = w_compX;
        s[7]  = "";            w[7]  = beforeUnitV;
        s[8]  = char_i[userEquationNum]; w[8]  = w_char_i[userEquationNum];
        s[9]  = "";            w[9]  = beforeSign;
        s[10] = signY;         w[10] = w_sign;
        s[11] = "";            w[11] = afterSign;
        s[12] = "";            w[12] = (int) fm.stringWidth(yW) - w_compY;
        s[13] = absCompY;      w[13] = w_compY;
        s[14] = "";            w[14] = beforeUnitV;
        s[15] = char_j_paren[userEquationNum]+unit;  w[15] = 0;

        // draw the equation
        g.setColor(resultsEquationColor);
        int sX, sY ;
        sX = eqnX; sY = eqnY;
        for (int n=0;n<=15;n++) {
            g.drawString(s[n],sX,sY);
            sX += w[n];
        }
        // add arrow and carots
        addVectorSymbol(g,eqnX,eqnY);
        int c1X = eqnX+w[0]+w[1]+w[2]+w[3]+w[4]+w[5]+w[6]+w[7];
        int c2X = c1X +w[8]+w[9]+w[10]+w[11]+w[12]+w[13];
        if (userEquationNum == 0) {addIjCarot(g,c1X,eqnY);
                                   addIjCarot(g,c2X,eqnY);}
        if (userEquationNum == 1) {addXyCarot(g,c1X,eqnY);
                                   addXyCarot(g,c2X,eqnY);}
    }
    //---------------------------------------------------------------------------
    // this is for plotting in the userCanvas with the default equation color
    public void plotEquation(Graphics g, String f, String u, int x, int y, int n) {
        plotEquation(g, f, u, userEquationColor, x, y, n);
    }
    //---------------------------------------------------------------------------
    // this is for plotting in the userCanvas with color specified for wrinting/erasing
    public void plotEquation(Graphics g, String f, String u, Color c, int x, int y, int n) {
        
        int forceCompX = x;
        int forceCompY = y;

        String forceName = f;
        userEquationNum = n;
        String units = u;

        strFx = Integer.toString(forceCompX);
        if (forceCompX >= 0) {strFx = " " + strFx;}

        if (forceCompY <= 0) signy = "+";
        else signy = " -";
        int absCompY = (int) Math.abs(forceCompY);
        strFy = signy + Integer.toString(absCompY);

        int w_forceName = fm.stringWidth(forceName);
        int w_strFx = fm.stringWidth(strFx);
        int w_strFy = fm.stringWidth(strFy);

        int eqnX = userEqnPosX;
        int eqnY = userEqnPosY[userEquationNum];

        Color equationColor;
        equationColor = c;
        g.setColor(equationColor);

        // draw "F("
        g.drawString(F_paren, eqnX, eqnY);
        // add a vector symbol over the "F"
        addVectorSymbol(g, eqnX, eqnY);
        eqnX += w_F_paren;
        
        // draw force name
        g.drawString(forceName, eqnX, eqnY);
        eqnX += w_forceName;

        // draw ") = ("
        g.drawString(parens, eqnX, eqnY);
        eqnX += w_parens;

        // draw the x-component
        g.drawString(strFx, eqnX, eqnY);
        eqnX += w_strFx;

        // draw "i"
        g.drawString(char_i[userEquationNum], eqnX, eqnY);
        // add a carot over the "i" or the "x"
        if (userEquationNum == 0) {addIjCarot(g, eqnX, eqnY);}
        if (userEquationNum == 1) {addXyCarot(g, eqnX, eqnY);}
        eqnX += w_char_i[userEquationNum];

        // draw the absolute value of the y-component
        g.drawString(strFy, eqnX, eqnY);
        eqnX += w_strFy;

        // draw "j ) " + units
        g.drawString(char_j_paren[userEquationNum]+units, eqnX, eqnY);
        // add a carot over the "j"
        if (userEquationNum == 0) {addIjCarot(g, eqnX, eqnY);}
        if (userEquationNum == 1) {addXyCarot(g, eqnX, eqnY);}
    }
    //---------------------------------------------------------------------------
    public void plotForceLabel (String fN, int tx, int ty, Graphics g) {
        g.drawString("F(" + fN + ")",tx,ty);
        addVectorSymbol(g,tx,ty);
    }
    //---------------------------------------------------------------------------
    // add a vector symbol over an upper-case letter
    public void addVectorSymbol(Graphics g, int x, int y){
        g.drawLine(x + 1, y -13, x + 8, y -13);
        g.drawLine(x + 4, y -15, x + 8, y -13);
        g.drawLine(x + 4, y -11, x + 8, y -13);
    }
    //---------------------------------------------------------------------------
    // set the FontMetrics and fixed widths once
    public void setFonts(Graphics g){
       fm=g.getFontMetrics(g.getFont());
    }
    //---------------------------------------------------------------------------
    // add a carot over i and j to make unit vector symbols
    public void addIjCarot(Graphics g, int x, int y){
        g.drawLine(x - 1, y -8, x + 1, y -10);
        g.drawLine(x + 3, y -8, x + 1, y -10);
    }
    //---------------------------------------------------------------------------
    // add a carot over x and y to make unit vector symbols
    public void addXyCarot(Graphics g, int x, int y){
        g.drawLine(x + 1, y -9, x + 3, y -11);
        g.drawLine(x + 5, y -9, x + 3, y -11);
    }
    //---------------------------------------------------------------------------
}
/*
=================================================================================
*/
