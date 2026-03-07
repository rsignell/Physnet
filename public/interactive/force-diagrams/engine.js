"use strict";
// ============================================================
// Shared engine for One-Body Force Diagram problems
// Requires: problem, drawProblemPanel(), drawUserApparatus(ctx)
// to be defined before this script is loaded.
// ============================================================

// ---- Drawing helpers (Java-style coordinate transforms) ----

// Line: shift only (lPlot equivalent)
function lPlot(ctx, uOX, uOY, x1, y1, x2, y2) {
  ctx.beginPath();
  ctx.moveTo(x1 + uOX, -y1 + uOY);
  ctx.lineTo(x2 + uOX, -y2 + uOY);
  ctx.stroke();
}
// Circle (outline)
function cPlot(ctx, uOX, uOY, x1, y1, r) {
  ctx.beginPath();
  ctx.arc(x1 + uOX, -y1 + uOY, r, 0, 2 * Math.PI);
  ctx.stroke();
}
// Filled circle
function fcPlot(ctx, uOX, uOY, x1, y1, r) {
  ctx.beginPath();
  ctx.arc(x1 + uOX, -y1 + uOY, r, 0, 2 * Math.PI);
  ctx.fill();
}
// Outline rect
function rPlot(ctx, uOX, uOY, x1, y1, w, h) {
  ctx.strokeRect(x1 + uOX, -y1 + uOY, w, h);
}
// Filled polygon using xs/ys arrays
function polyPlot(ctx, uOX, uOY, xs, ys, n) {
  ctx.beginPath();
  ctx.moveTo(xs[0] + uOX, ys[0] + uOY);
  for (var k = 1; k < n; k++) {
    ctx.lineTo(xs[k] + uOX, ys[k] + uOY);
  }
  ctx.closePath();
  ctx.fill();
}

// Draw caret (^) above character at (x, y)
function drawCaret(ctx, x, y) {
  ctx.beginPath();
  ctx.moveTo(x,     y - 14);
  ctx.lineTo(x + 4, y - 18);
  ctx.lineTo(x + 8, y - 14);
  ctx.stroke();
}

// Draw ij unit vectors at position (uOx, uOy) in a panel
// uOx, uOy are the origin of the unit vector display in screen pixels
function drawIJArrows(ctx, uOx, uOy) {
  var lineL = 20, arroL = 7, arroS = 3;
  ctx.strokeStyle = "blue";
  ctx.fillStyle = "blue";
  ctx.lineWidth = 1;

  // j (up)
  ctx.beginPath();
  ctx.moveTo(uOx, uOy);
  ctx.lineTo(uOx, uOy - lineL);
  ctx.stroke();
  ctx.beginPath();
  ctx.moveTo(uOx - arroS, uOy - lineL + arroL);
  ctx.lineTo(uOx, uOy - lineL);
  ctx.lineTo(uOx + arroS, uOy - lineL + arroL);
  ctx.stroke();
  ctx.font = "italic 13px 'Times New Roman', serif";
  ctx.fillText("j", uOx - 12, uOy - 8);
  drawCaret(ctx, uOx - 12, uOy - 8);

  // i (right)
  ctx.beginPath();
  ctx.moveTo(uOx, uOy);
  ctx.lineTo(uOx + lineL, uOy);
  ctx.stroke();
  ctx.beginPath();
  ctx.moveTo(uOx + lineL - arroL, uOy - arroS);
  ctx.lineTo(uOx + lineL, uOy);
  ctx.lineTo(uOx + lineL - arroL, uOy + arroS);
  ctx.stroke();
  ctx.fillText("i", uOx + lineL + 5, uOy + 3);
  drawCaret(ctx, uOx + lineL + 5, uOy + 3);
}

// Draw rotated xy unit vectors (for 2-equation incline problems)
// Uses uPlot-style rotation by incline angle (cost, sint)
function drawXYArrows(ctx, uOx, uOy, cost, sint) {
  var lineL = 20, arroL = 7, arroS = 3;
  ctx.strokeStyle = "blue";
  ctx.fillStyle = "blue";
  ctx.lineWidth = 1;

  // Helper: uPlot rotation
  function scr(x, y) {
    return { sx: x * cost - y * sint + uOx,
             sy: -(x * sint + y * cost) + uOy };
  }

  // y unit vector (along incline normal)
  var p0 = scr(0, 0), p1 = scr(0, lineL);
  var pa = scr(arroS, lineL - arroL), pb = scr(-arroS, lineL - arroL);
  ctx.beginPath(); ctx.moveTo(p0.sx, p0.sy); ctx.lineTo(p1.sx, p1.sy); ctx.stroke();
  ctx.beginPath(); ctx.moveTo(pa.sx, pa.sy); ctx.lineTo(p1.sx, p1.sy); ctx.lineTo(pb.sx, pb.sy); ctx.stroke();
  // label: sRPlot "y" at (-lineL/2-5, lineL/2+5)
  var pl = scr(-lineL / 2 - 5, lineL / 2 + 5);
  ctx.font = "italic 13px 'Times New Roman', serif";
  ctx.fillText("y", pl.sx, pl.sy);
  drawCaret(ctx, pl.sx, pl.sy);

  // x unit vector (along incline)
  var px1 = scr(lineL, 0);
  var pxa = scr(lineL - arroL, arroS), pxb = scr(lineL - arroL, -arroS);
  ctx.beginPath(); ctx.moveTo(p0.sx, p0.sy); ctx.lineTo(px1.sx, px1.sy); ctx.stroke();
  ctx.beginPath(); ctx.moveTo(pxa.sx, pxa.sy); ctx.lineTo(px1.sx, px1.sy); ctx.lineTo(pxb.sx, pxb.sy); ctx.stroke();
  // label: sRPlot "x" at (lineL+3, -4)
  var plx = scr(lineL + 3, -4);
  ctx.fillText("x", plx.sx, plx.sy);
  drawCaret(ctx, plx.sx, plx.sy);
}

// ---- Vector drawing ----

function drawVector(ctx, tx, ty, hx, hy, color) {
  var headHalfWidth = 6, headLength = 20;
  var dx = hx - tx, dy = hy - ty;
  var len = Math.sqrt(dx * dx + dy * dy);
  if (len < 2) return;
  var shankReduction = headLength / len;
  var offsetFactor = headHalfWidth / len;
  if (len < headLength) { offsetFactor *= len / headLength; shankReduction = 1; }
  var shankX = hx - dx * shankReduction;
  var shankY = hy - dy * shankReduction;
  var offsetX = dy * offsetFactor, offsetY = dx * offsetFactor;
  ctx.strokeStyle = color; ctx.lineWidth = 2;
  ctx.beginPath(); ctx.moveTo(tx, ty); ctx.lineTo(hx, hy); ctx.stroke();
  ctx.fillStyle = color;
  ctx.beginPath();
  ctx.moveTo(Math.round(shankX + offsetX), Math.round(shankY - offsetY));
  ctx.lineTo(Math.round(shankX - offsetX), Math.round(shankY + offsetY));
  ctx.lineTo(Math.round(hx), Math.round(hy));
  ctx.closePath(); ctx.fill();
}

function drawLabeledVector(ctx, forceIndex) {
  var hx = truHdsX[forceIndex], hy = truHdsY[forceIndex];
  drawVector(ctx, tailPosX, tailPosY, hx, hy, "black");
  ctx.fillStyle = "black";
  ctx.font = "11px Helvetica, Arial, sans-serif";
  var name = problem.forceNames[forceIndex];
  var labelX = hx + 8, labelY = hy + 4;
  if (hx === tailPosX && hy === tailPosY) { labelX = tailPosX + 12; labelY = tailPosY - 4; }
  ctx.fillText(name, labelX, labelY);
}

// ---- Force equation formatting ----

function formatEquation(forceName, fx, fy, coordSys) {
  var dispFy = -fy;
  var sign = dispFy >= 0 ? " + " : " - ";
  var absDispFy = Math.abs(dispFy);
  var xhat = coordSys === 2 ? "<i>x&#x0302;</i>" : "<i>i&#x0302;</i>";
  var yhat = coordSys === 2 ? "<i>y&#x0302;</i>" : "<i>j&#x0302;</i>";
  return "<b>F</b>(" + forceName + ") = (" + fx + xhat +
         sign + absDispFy + yhat + ") " + problem.units;
}

// ---- State ----

var cost = problem.cost || 0;
var sint = problem.sint || 0;

// Compute tail position and true head positions
var theta = problem.thetaDeg * Math.PI / 180;
// Note: problem.tailPosX/Y may be set directly in problem object
var tailPosX = problem.tailPosX;
var tailPosY = problem.tailPosY;

var truHdsX = [], truHdsY = [];
for (var i = 0; i < problem.numForces; i++) {
  truHdsX[i] = Math.round(tailPosX + problem.truAnsX[i][0] / problem.scale);
  truHdsY[i] = Math.round(tailPosY + problem.truAnsY[i][0] / problem.scale);
}

var forceTolerance = Math.round(problem.scale * 5); // 5-pixel tolerance in force units

var forceNumber = -1;
var maxForceNumber = problem.numForces - 1;
var mouseIsDown = false;
var userPosX = tailPosX, userPosY = tailPosY;
var forceX = 0, forceY = 0;
var triesPerForce = [];
for (var k = 0; k < problem.numForces; k++) triesPerForce.push(0);
var gameOver = false;

// ---- Canvas references ----

var problemCanvas = document.getElementById("problemCanvas");
var ctxProblem = problemCanvas.getContext("2d");
var userBkg = document.getElementById("userBkg");
var ctxBkg = userBkg.getContext("2d");
var userDraw = document.getElementById("userDraw");
var ctxDraw = userDraw.getContext("2d");

// ---- Pixel to force conversion ----

function p2fX(px) { return Math.round(problem.scale * (px - tailPosX)); }
function p2fY(py) { return Math.round(problem.scale * (py - tailPosY)); }

// ---- UI update ----

function setMessage(html) {
  document.getElementById("messageText").innerHTML = html;
}

function showForceMessage(idx) {
  if (idx > maxForceNumber) {
    setMessage('<span style="color:#8b0000;font-weight:bold">PROBLEM DONE! &nbsp;&nbsp;</span>' +
               '<span class="done-blink">CLICK BELOW TO RESTART</span>');
    return;
  }
  var name = problem.blinkerStrings[idx];
  setMessage(problem.messageStrings[0] +
             '<span class="blink">' + name + '</span>' +
             problem.messageStrings[1]);
}

function addResult(forceIndex) {
  var name = problem.forceNames[forceIndex];
  var fx = problem.truAnsX[forceIndex][0];
  var fy = problem.truAnsY[forceIndex][0];
  var eq = formatEquation(name, fx, fy, 1);
  var div = document.createElement("div");
  div.className = "result-eq";
  div.innerHTML = eq;
  document.getElementById("resultsList").appendChild(div);
  if (forceIndex === maxForceNumber) {
    document.getElementById("resultsTrailer").style.display = "block";
  }
}

function updateEquation() {
  if (forceNumber < 0 || forceNumber > maxForceNumber) return;
  var name = problem.forceNames[forceNumber];
  var fx0 = p2fX(userPosX), fy0 = p2fY(userPosY);
  var eqArea = document.getElementById("equationArea");
  if (problem.numEquations === 2) {
    var fx1 = Math.round(cost * fx0 - sint * fy0);
    var fy1 = Math.round(sint * fx0 + cost * fy0);
    eqArea.innerHTML = formatEquation(name, fx0, fy0, 1) + "<br>" +
                       formatEquation(name, fx1, fy1, 2);
  } else {
    eqArea.innerHTML = formatEquation(name, fx0, fy0, 1);
  }
}

// ---- Redraw ----

function redrawUserDraw() {
  ctxDraw.clearRect(0, 0, 370, 400);
  drawUserApparatus(ctxDraw);
  for (var n = 0; n < forceNumber; n++) drawLabeledVector(ctxDraw, n);
  if (forceNumber >= 0 && forceNumber <= maxForceNumber) {
    drawVector(ctxDraw, tailPosX, tailPosY, userPosX, userPosY, "blue");
  }
}

// ---- Validation ----

function validateForce() {
  var matchIndex = problem.matchForceComps[forceNumber] - 1;
  var fx0 = p2fX(userPosX), fy0 = p2fY(userPosY);
  var fxComp = fx0, fyComp = fy0;
  if (matchIndex === 1) {
    // rotate into incline frame
    fxComp = Math.round(cost * fx0 - sint * fy0);
    fyComp = Math.round(sint * fx0 + cost * fy0);
  }
  var depX = Math.abs(fxComp - problem.truAnsX[forceNumber][matchIndex]);
  var depY = Math.abs(fyComp - problem.truAnsY[forceNumber][matchIndex]);

  if (depX <= forceTolerance && depY <= forceTolerance) {
    drawLabeledVector(ctxBkg, forceNumber);
    addResult(forceNumber);
    if (forceNumber < maxForceNumber) {
      forceNumber++;
      triesPerForce[forceNumber] = 0;
      userPosX = tailPosX; userPosY = tailPosY;
      forceX = 0; forceY = 0;
      showForceMessage(forceNumber);
      updateEquation();
      redrawUserDraw();
    } else {
      forceNumber++;
      document.getElementById("doneText").style.display = "block";
      document.getElementById("equationArea").innerHTML = "";
      showForceMessage(forceNumber);
      redrawUserDraw();
      gameOver = true;
    }
  } else {
    triesPerForce[forceNumber]++;
    if (triesPerForce[forceNumber] >= problem.maxTriesEachForce) {
      triesPerForce[forceNumber] = 0;
    }
  }
}

// ---- Mouse / Touch handlers ----

function getCanvasPos(e) {
  var rect = userDraw.getBoundingClientRect();
  return { x: Math.round(e.clientX - rect.left), y: Math.round(e.clientY - rect.top) };
}

function startExercise() {
  if (forceNumber >= 0) return;
  forceNumber = 0;
  userPosX = tailPosX; userPosY = tailPosY;
  forceX = 0; forceY = 0;
  showForceMessage(0);
  redrawUserDraw();
}

document.getElementById("messagePanel").addEventListener("click", startExercise);

userDraw.addEventListener("mousedown", function(e) {
  if (gameOver) { location.reload(); return; }
  if (forceNumber < 0) { startExercise(); return; }
  mouseIsDown = true;
  var pos = getCanvasPos(e);
  userPosX = pos.x; userPosY = pos.y;
  forceX = p2fX(userPosX); forceY = p2fY(userPosY);
  redrawUserDraw(); updateEquation();
});

userDraw.addEventListener("mousemove", function(e) {
  if (!mouseIsDown) return;
  var pos = getCanvasPos(e);
  userPosX = pos.x; userPosY = pos.y;
  forceX = p2fX(userPosX); forceY = p2fY(userPosY);
  redrawUserDraw(); updateEquation();
});

document.addEventListener("mouseup", function() {
  if (!mouseIsDown) return;
  mouseIsDown = false;
  if (forceNumber >= 0 && forceNumber <= maxForceNumber) validateForce();
});

userDraw.addEventListener("touchstart", function(e) {
  e.preventDefault();
  userDraw.dispatchEvent(new MouseEvent("mousedown", { clientX: e.touches[0].clientX, clientY: e.touches[0].clientY }));
}, { passive: false });

userDraw.addEventListener("touchmove", function(e) {
  e.preventDefault();
  userDraw.dispatchEvent(new MouseEvent("mousemove", { clientX: e.touches[0].clientX, clientY: e.touches[0].clientY }));
}, { passive: false });

userDraw.addEventListener("touchend", function(e) {
  e.preventDefault();
  document.dispatchEvent(new MouseEvent("mouseup", {}));
}, { passive: false });

// ---- Nav bar ----

function insertNavBar() {
  var nav = document.createElement("div");
  nav.style.cssText = "width:684px;margin:8px auto 4px;font-family:Helvetica,Arial,sans-serif;font-size:13px;display:flex;gap:1.5em;";
  nav.innerHTML = '<a href="/Physnet/interactive/force-diagrams/" style="color:#1e3a5f;text-decoration:none;">&#8592; Problem Index</a>' +
                  '<a href="/Physnet/" style="color:#1e3a5f;text-decoration:none;">&#8962; PHYSNET Home</a>';
  var app = document.getElementById("app");
  app.parentNode.insertBefore(nav, app);
}

// ---- Init ----

function init() {
  insertNavBar();
  drawProblemPanel();
  drawUserApparatus(ctxBkg);
  ctxBkg.strokeStyle = "black"; ctxBkg.lineWidth = 1;
  ctxBkg.strokeRect(0, 0, 370, 400);
  setMessage('Look left at data and unit vectors. <span class="blink">Then Click Here.</span>');
  drawUserApparatus(ctxDraw);
}

init();
