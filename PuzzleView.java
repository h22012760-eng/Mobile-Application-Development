package com.puzzleverse.game;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import androidx.annotation.NonNull;
import java.util.List;

public class PuzzleView extends View {

    private PuzzleEngine engine;
    private boolean      ready = false;

    // ── Drag state ────────────────────────────────────────────────────
    // dragPiece   = the piece the player touched
    // dragX/dragY = current finger position in pixels
    // dragOffX/Y  = offset from piece top-left to touch point
    //               so piece doesn't jump when you pick it up
    private PuzzlePiece dragPiece = null;
    private float       dragX     = 0f;
    private float       dragY     = 0f;
    private float       dragOffX  = 0f;
    private float       dragOffY  = 0f;

    // Drop target highlight — the cell the finger is hovering over
    private int hiRow = -1;
    private int hiCol = -1;

    // Features
    private boolean ghostVisible = false;
    private boolean isPeeking    = false;
    private Bitmap  peekBitmap   = null;
    private boolean mysteryMode  = false;

    // Particles on snap
    private final SnapParticleSystem particles = new SnapParticleSystem();

    // ── Listener ──────────────────────────────────────────────────────
    public interface Listener {
        void onMoveUsed();
        void onSnap(int groupSize);
        void onPuzzleComplete();
    }
    private Listener listener;

    // ── Paints ────────────────────────────────────────────────────────
    private final Paint bitmapPaint  = new Paint(
            Paint.ANTI_ALIAS_FLAG | Paint.FILTER_BITMAP_FLAG);
    private final Paint boardBgPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint borderPaint  = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint gridPaint    = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint pieceBorder  = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint groupPaint   = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint hiPaint      = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint shadowPaint  = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint dragRing     = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint ghostPaint   = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint mysteryPaint = new Paint(Paint.ANTI_ALIAS_FLAG);

    public PuzzleView(Context ctx) {
        super(ctx); init();
    }
    public PuzzleView(Context ctx, AttributeSet a) {
        super(ctx, a); init();
    }

    private void init() {
        boardBgPaint.setColor(Color.argb(210, 13, 13, 26));
        boardBgPaint.setStyle(Paint.Style.FILL);

        borderPaint.setColor(Color.parseColor("#66FCF1"));
        borderPaint.setStyle(Paint.Style.STROKE);
        borderPaint.setStrokeWidth(3f);
        borderPaint.setAntiAlias(true);

        gridPaint.setColor(Color.argb(18, 102, 252, 241));
        gridPaint.setStyle(Paint.Style.STROKE);
        gridPaint.setStrokeWidth(0.5f);

        pieceBorder.setColor(Color.argb(50, 102, 252, 241));
        pieceBorder.setStyle(Paint.Style.STROKE);
        pieceBorder.setStrokeWidth(0.8f);

        groupPaint.setColor(Color.parseColor("#66FCF1"));
        groupPaint.setStyle(Paint.Style.STROKE);
        groupPaint.setStrokeWidth(4f);
        groupPaint.setAntiAlias(true);

        hiPaint.setColor(Color.argb(60, 102, 252, 241));
        hiPaint.setStyle(Paint.Style.FILL);

        shadowPaint.setColor(Color.argb(140, 0, 0, 0));
        shadowPaint.setStyle(Paint.Style.FILL);

        dragRing.setColor(Color.parseColor("#66FCF1"));
        dragRing.setStyle(Paint.Style.STROKE);
        dragRing.setStrokeWidth(4f);
        dragRing.setAntiAlias(true);

        ghostPaint.setAlpha(38);
        mysteryPaint.setStyle(Paint.Style.FILL);
    }

    // ── Wiring ────────────────────────────────────────────────────────
    public void setEngine(PuzzleEngine eng) {
        this.engine = eng;
        eng.setEventListener(new PuzzleEngine.OnPuzzleEventListener() {
            @Override public void onSnap(int size) {
                particles.emit(
                        eng.getBoardLeft() + eng.getBoardW() / 2f,
                        eng.getBoardTop()  + eng.getBoardH() / 2f,
                        Math.min(6 + size * 2, 16));
                if (listener != null) listener.onSnap(size);
                invalidate();
            }
            @Override public void onPuzzleComplete() {
                if (listener != null) listener.onPuzzleComplete();
                invalidate();
            }
        });
        ready = true;
        invalidate();
    }

    public void setListener(Listener l)    { this.listener = l; }
    public void setGhostVisible(boolean v) { ghostVisible = v; invalidate(); }
    public void setMysteryMode(boolean v)  { mysteryMode  = v; invalidate(); }
    public void startPeek(Bitmap b)        { peekBitmap = b; isPeeking = true;  invalidate(); }
    public void stopPeek()                 { isPeeking = false; invalidate(); }

    // ── Draw ──────────────────────────────────────────────────────────
    @Override
    protected void onDraw(@NonNull Canvas canvas) {
        super.onDraw(canvas);
        if (!ready || engine == null) return;

        drawBoardBackground(canvas);
        drawGridLines(canvas);
        drawDropHighlight(canvas);
        drawAllPieces(canvas);
        drawGroupBorders(canvas);
        drawGhost(canvas);
        drawMystery(canvas);
        drawDraggedPiece(canvas);
        drawBoardBorder(canvas);

        if (isPeeking && peekBitmap != null) drawPeek(canvas);

        if (particles.isActive()) {
            particles.updateAndDraw(canvas);
            invalidate();
        }
    }

    private void drawBoardBackground(Canvas canvas) {
        canvas.drawRect(
                engine.getBoardLeft(), engine.getBoardTop(),
                engine.getBoardLeft() + engine.getBoardW(),
                engine.getBoardTop()  + engine.getBoardH(),
                boardBgPaint);
    }

    private void drawBoardBorder(Canvas canvas) {
        canvas.drawRect(
                engine.getBoardLeft()  + 1.5f,
                engine.getBoardTop()   + 1.5f,
                engine.getBoardLeft()  + engine.getBoardW() - 1.5f,
                engine.getBoardTop()   + engine.getBoardH() - 1.5f,
                borderPaint);
    }

    private void drawGridLines(Canvas canvas) {
        float bl = engine.getBoardLeft(), bt = engine.getBoardTop();
        float bw = engine.getBoardW(),    bh = engine.getBoardH();
        float pw = engine.getPieceW(),    ph = engine.getPieceH();
        int   gc = engine.getGridCols(),  gr = engine.getGridRows();
        for (int c = 0; c <= gc; c++)
            canvas.drawLine(bl + c*pw, bt, bl + c*pw, bt + bh, gridPaint);
        for (int r = 0; r <= gr; r++)
            canvas.drawLine(bl, bt + r*ph, bl + bw, bt + r*ph, gridPaint);
    }

    private void drawDropHighlight(Canvas canvas) {
        if (dragPiece == null || hiRow < 0) return;
        float x = engine.getBoardLeft() + hiCol * engine.getPieceW();
        float y = engine.getBoardTop()  + hiRow * engine.getPieceH();
        canvas.drawRect(x, y,
                x + engine.getPieceW(), y + engine.getPieceH(), hiPaint);
    }

    private void drawAllPieces(Canvas canvas) {
        if (engine.getPieces() == null) return;
        float bl = engine.getBoardLeft(), bt = engine.getBoardTop();
        float pw = engine.getPieceW(),    ph = engine.getPieceH();

        for (PuzzlePiece p : engine.getPieces()) {
            if (isBeingDragged(p)) continue;

            // Piece draws at its GRID position — always inside board
            float px = bl + p.currentCol * pw;
            float py = bt + p.currentRow * ph;

            canvas.drawBitmap(p.bitmap, px, py, bitmapPaint);
            canvas.drawRect(px, py, px + pw, py + ph, pieceBorder);
        }
    }

    // Draw bright cyan border on exposed edges of every group
    private void drawGroupBorders(Canvas canvas) {
        if (engine.getPieces() == null) return;
        float bl = engine.getBoardLeft(), bt = engine.getBoardTop();
        float pw = engine.getPieceW(),    ph = engine.getPieceH();

        for (PuzzlePiece p : engine.getPieces()) {
            if (isBeingDragged(p)) continue;
            if (p.groupId < 0) continue;

            float px = bl + p.currentCol * pw;
            float py = bt + p.currentRow * ph;

            // Top edge — draw if it's a boundary of the group
            if (isGroupEdge(p, -1, 0))
                canvas.drawLine(px, py, px + pw, py, groupPaint);
            // Bottom edge
            if (isGroupEdge(p, 1, 0))
                canvas.drawLine(px, py + ph, px + pw, py + ph, groupPaint);
            // Left edge
            if (isGroupEdge(p, 0, -1))
                canvas.drawLine(px, py, px, py + ph, groupPaint);
            // Right edge
            if (isGroupEdge(p, 0, 1))
                canvas.drawLine(px + pw, py, px + pw, py + ph, groupPaint);
        }
    }

    // Check if the neighbor at (p.currentRow+dR, p.currentCol+dC)
    // is NOT in the same group or doesn't exist
    private boolean isGroupEdge(PuzzlePiece p, int dR, int dC) {
        if (p.groupId < 0) return true;
        PuzzlePiece nb = engine.getPieceAt(
                p.currentRow + dR, p.currentCol + dC);
        if (nb == null || nb.groupId != p.groupId) return true;
        // Confirm they are truly image-adjacent in this direction
        return (p.correctRow - nb.correctRow) != dR
                || (p.correctCol - nb.correctCol) != dC;
    }

    private void drawGhost(Canvas canvas) {
        if (!ghostVisible || engine.getFullImage() == null) return;
        canvas.drawBitmap(engine.getFullImage(), null,
                new RectF(engine.getBoardLeft(), engine.getBoardTop(),
                        engine.getBoardLeft() + engine.getBoardW(),
                        engine.getBoardTop()  + engine.getBoardH()),
                ghostPaint);
    }

    private void drawMystery(Canvas canvas) {
        if (!mysteryMode) return;
        float ratio = engine.getTotalPieces() > 0
                ? (float) engine.getPlacedCount() / engine.getTotalPieces()
                : 0f;
        int alpha = (int)(220 * (1f - ratio));
        if (alpha <= 0) return;
        mysteryPaint.setColor(Color.argb(alpha, 0, 0, 0));
        canvas.drawRect(engine.getBoardLeft(), engine.getBoardTop(),
                engine.getBoardLeft() + engine.getBoardW(),
                engine.getBoardTop()  + engine.getBoardH(),
                mysteryPaint);
    }

    // Draw dragged piece(s) floating under the finger
    private void drawDraggedPiece(Canvas canvas) {
        if (dragPiece == null) return;
        float pw = engine.getPieceW(), ph = engine.getPieceH();
        List<PuzzlePiece> group = engine.getGroup(dragPiece);

        // Where the dragged piece draws under the finger
        float drawX = dragX - dragOffX;
        float drawY = dragY - dragOffY;

        // Delta from dragged piece's grid position to draw position
        float bl    = engine.getBoardLeft(), bt = engine.getBoardTop();
        float gridX = bl + dragPiece.currentCol * pw;
        float gridY = bt + dragPiece.currentRow * ph;
        float dx    = drawX - gridX;
        float dy    = drawY - gridY;

        // Shadow pass
        for (PuzzlePiece p : group) {
            float px = bl + p.currentCol * pw + dx;
            float py = bt + p.currentRow * ph + dy;
            canvas.drawRoundRect(
                    new RectF(px+7, py+7, px+pw+7, py+ph+7),
                    4, 4, shadowPaint);
        }

        // Bitmap + ring pass
        for (PuzzlePiece p : group) {
            float px = bl + p.currentCol * pw + dx;
            float py = bt + p.currentRow * ph + dy;
            canvas.drawBitmap(p.bitmap, px, py, bitmapPaint);
            canvas.drawRect(px, py, px+pw, py+ph, dragRing);
        }
    }

    private void drawPeek(Canvas canvas) {
        canvas.drawBitmap(peekBitmap, null,
                new RectF(engine.getBoardLeft(), engine.getBoardTop(),
                        engine.getBoardLeft() + engine.getBoardW(),
                        engine.getBoardTop()  + engine.getBoardH()),
                new Paint());
    }

    private boolean isBeingDragged(PuzzlePiece p) {
        if (dragPiece == null) return false;
        if (p == dragPiece) return true;
        return dragPiece.groupId >= 0
                && p.groupId == dragPiece.groupId;
    }

    // ── Touch handling ────────────────────────────────────────────────
    @Override
    public boolean performClick() {
        return super.performClick();
    }

    @Override
    public boolean onTouchEvent(@NonNull MotionEvent e) {
        if (!ready || engine == null || isPeeking) return false;

        float tx = e.getX(), ty = e.getY();

        switch (e.getActionMasked()) {

            case MotionEvent.ACTION_DOWN:
                performClick();
                dragPiece = engine.findAt(tx, ty);
                if (dragPiece != null) {
                    if (dragPiece.locked) {
                        dragPiece = null;
                        return true;
                    }
                    // Store offset so piece doesn't jump to fingertip
                    float bl = engine.getBoardLeft();
                    float bt = engine.getBoardTop();
                    float pw = engine.getPieceW();
                    float ph = engine.getPieceH();
                    float pieceScreenX = bl + dragPiece.currentCol * pw;
                    float pieceScreenY = bt + dragPiece.currentRow * ph;
                    dragOffX = tx - pieceScreenX;
                    dragOffY = ty - pieceScreenY;
                    dragX    = tx;
                    dragY    = ty;
                    updateHighlight(tx, ty);
                    invalidate();
                }
                return true;

            case MotionEvent.ACTION_MOVE:
                if (dragPiece != null) {
                    dragX = tx;
                    dragY = ty;
                    updateHighlight(tx, ty);
                    invalidate();
                }
                return true;

            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL:
                if (dragPiece != null) {
                    // Find the target cell from the finger position
                    int[] cell = engine.screenToCell(tx, ty);

                    if (cell != null
                            && !(cell[0] == dragPiece.currentRow
                            && cell[1] == dragPiece.currentCol)) {
                        // SWAP — positions interchange
                        boolean swapped = engine.swapPieces(
                                dragPiece, cell[0], cell[1]);
                        if (swapped && listener != null) {
                            listener.onMoveUsed(); // decrement counter
                        }
                    }

                    dragPiece = null;
                    hiRow = -1;
                    hiCol = -1;
                    invalidate();
                }
                return true;
        }
        return false;
    }

    private void updateHighlight(float tx, float ty) {
        int[] c = engine.screenToCell(tx, ty);
        if (c != null) { hiRow = c[0]; hiCol = c[1]; }
        else            { hiRow = -1;  hiCol = -1;   }
    }

    @Override
    protected void onSizeChanged(int w, int h, int ow, int oh) {
        super.onSizeChanged(w, h, ow, oh);
        if (engine != null && w > 0 && h > 0) {
            engine.init(w, h);
            ready = true;
            invalidate();
        }
    }
}