// GeneralData.java
// copyright, Peter Signell, 9/1/97

import java.awt.*;
//-------------------------------------------------------------------------------
// classes using this database: user -> user, results, problem, canvasEquation
//-------------------------------------------------------------------------------
interface GeneralData {

    static final Color  backgroundColor = Color.lightGray;

    static final int    problemFrameX = 300;
    static final int    problemFrameY = 210;
    static final int    problemEqnX = 50;
    static final int    problemEqnY = 140;

    static final int    userFrameX = 370;
    static final int    userFrameY = 400;
    static final String userTrailer = "Done!";
    static final int    userTrailerPosX = 10;
    static final int    userTrailerPosY = 250;
    static final int    userTolerance = 5;
    static final Color  userTrailerColor  = Color.red.darker();
    static final Color  userVectorColor   = Color.blue;
    static final Color  userTruVecColor   = Color.black;
    static final Color  userEquationColor = Color.blue;
    static final Color  userTrueEqnColor  = Color.black;
    static final int    userEqnPosX = 10;
    static final int[]  userEqnPosY = {250,270};

    static final int     resultsFrameX = 300;
    // was 195
    static final int     resultsFrameY = 235;
    static final int[]   resultsEqnPosX = {10,10,10,10};
    static final int[]   resultsEqnPosY = {52,72,92,112};
    static final int     resultsHeaderPosX = 10;
    static final int     resultsHeaderPosY = 30;
    static final int     resultsTrailerPosX = 8;
    static final int     resultsTrailerPosY = 118;
    static final String  resultsHeader = "Your list of correctly drawn force vectors:";
    static final String  resultsTrailer = "ALL OF YOUR FORCES ARE CORRECT!";
    static final Color   resultsEquationColor = Color.black;
    static final Color   resultsHeaderColor = Color.blue;
    static final Color   resultsTrailerColor = Color.red.darker();

    static final int    messageFrameX = 370;
    static final int    messageFrameY = 35;
    static final int    messageNumStrings = 2;
    static final Color  messageColor        = Color.black;
    static final Color  messageBlinkerColor = Color.blue;
    static final int    messagePosX = 7;
    static final int    messagePosY = 30;
    static final Color  messageDoneColor = Color.red.darker();
    static final String messageDoneBlinker = "CLICK BELOW TO GO TO MENU";
    static final String messageDoneString = "PROBLEM DONE!   ";
    static final String messageDataBlinker = "Then Click Below.";
    static final String messageDataString = "Look left at data and unit vectors. ";

}
