package com.puzzleverse.game;

import android.graphics.Bitmap;
import android.graphics.RectF;

public class PuzzlePiece {

    public Bitmap bitmap;

    // Correct grid position — identity, never changes
    public final int correctRow;
    public final int correctCol;

    // Current grid position
    public int currentRow;
    public int currentCol;

    // Current pixel position (top-left corner of this piece)
    // These are FREE — piece can be anywhere on the canvas
    public float x;
    public float y;

    // Piece dimensions in pixels
    public final float pieceW;
    public final float pieceH;

    // Group ID — all pieces in same group move together
    // -1 means not in any group yet
    public int groupId = -1;

    // True once puzzle fully solved
    public boolean locked = false;

    // Index for ordering
    public final int index;

    public PuzzlePiece(Bitmap bitmap,
                       int correctRow, int correctCol,
                       float pieceW, float pieceH,
                       int index) {
        this.bitmap      = bitmap;
        this.correctRow  = correctRow;
        this.correctCol  = correctCol;
        this.pieceW      = pieceW;
        this.pieceH      = pieceH;
        this.index       = index;
    }

    // Where this piece SHOULD be when puzzle is solved
    // boardLeft/boardTop are the board origin
    public float correctX(float boardLeft) {
        return boardLeft + correctCol * pieceW;
    }

    public float correctY(float boardTop) {
        return boardTop + correctRow * pieceH;
    }

    // Touch hit test
    public boolean contains(float tx, float ty) {
        return tx >= x && tx <= x + pieceW
                && ty >= y && ty <= y + pieceH;
    }

    // Screen rect at current position
    public RectF rect() {
        return new RectF(x, y, x + pieceW, y + pieceH);
    }
}