package com.puzzleverse.game;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import androidx.annotation.Nullable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

public class PuzzleEngine {

    private final Context context;
    private final int     gridCols;
    private final int     gridRows;
    private final int     levelNumber;
    private final boolean isDaily;

    private Bitmap            originalBitmap;
    private PuzzlePiece[][]   grid;
    private List<PuzzlePiece> pieces;

    private float boardLeft, boardTop;
    private float boardW,    boardH;
    private float pieceW,    pieceH;

    private int nextGroupId = 0;

    private OnPuzzleEventListener listener;

    public interface OnPuzzleEventListener {
        void onSnap(int groupSize);
        void onPuzzleComplete();
    }

    public PuzzleEngine(Context ctx, int cols, int rows,
                        int level, boolean daily) {
        this.context     = ctx;
        this.gridCols    = cols;
        this.gridRows    = rows;
        this.levelNumber = level;
        this.isDaily     = daily;
    }

    public void setEventListener(OnPuzzleEventListener l) {
        this.listener = l;
    }

    // ── Called by PuzzleView.onSizeChanged ────────────────────────────
    public void init(float canvasW, float canvasH) {
        pieceW    = canvasW / gridCols;
        pieceH    = canvasH / gridRows;
        boardW    = pieceW * gridCols;
        boardH    = pieceH * gridRows;
        boardLeft = (canvasW - boardW) / 2f;
        boardTop  = (canvasH - boardH) / 2f;

        loadAndSlice();
        shuffle();
    }

    private void loadAndSlice() {
        int resId = getResId();

        BitmapFactory.Options o = new BitmapFactory.Options();
        o.inJustDecodeBounds = true;
        BitmapFactory.decodeResource(context.getResources(), resId, o);
        o.inSampleSize       = calcSample(o, (int) boardW, (int) boardH);
        o.inJustDecodeBounds = false;

        Bitmap raw = BitmapFactory.decodeResource(
                context.getResources(), resId, o);
        originalBitmap = Bitmap.createScaledBitmap(
                raw, (int) boardW, (int) boardH, true);
        if (raw != originalBitmap) raw.recycle();

        pieces = new ArrayList<>();
        grid   = new PuzzlePiece[gridRows][gridCols];
        int idx = 0;

        for (int row = 0; row < gridRows; row++) {
            for (int col = 0; col < gridCols; col++) {
                int sx = (int)(col * pieceW);
                int sy = (int)(row * pieceH);
                int sw = Math.min((int) pieceW,
                        originalBitmap.getWidth()  - sx);
                int sh = Math.min((int) pieceH,
                        originalBitmap.getHeight() - sy);
                if (sw <= 0 || sh <= 0) continue;

                Bitmap slice = Bitmap.createBitmap(
                        originalBitmap, sx, sy, sw, sh);
                PuzzlePiece p = new PuzzlePiece(
                        slice, row, col, pieceW, pieceH, idx++);
                p.currentRow   = row;
                p.currentCol   = col;
                grid[row][col] = p;
                pieces.add(p);
            }
        }
    }

    private void shuffle() {
        List<PuzzlePiece> shuffled = new ArrayList<>(pieces);
        Random rng    = new Random();
        boolean solved;
        do {
            Collections.shuffle(shuffled, rng);
            solved = true;
            for (int i = 0; i < shuffled.size(); i++) {
                int r = i / gridCols, c = i % gridCols;
                if (shuffled.get(i).correctRow != r
                        || shuffled.get(i).correctCol != c) {
                    solved = false;
                    break;
                }
            }
        } while (solved);
        for (int i = 0; i < shuffled.size(); i++) {
            int r = i / gridCols, c = i % gridCols;
            PuzzlePiece p  = shuffled.get(i);
            p.currentRow   = r;
            p.currentCol   = c;
            grid[r][c]     = p;
        }
    }

    // ─────────────────────────────────────────────────────────────────
    // SWAP — the core action
    //
    // When player drops piece A onto cell (toRow, toCol):
    //   1. If A is in a group → move the whole group
    //   2. Otherwise, → A and the piece at destination swap positions
    //   3. After swap → scan ALL pairs and merge any that are
    //      image-adjacent AND currently adjacent on the board
    //
    // Returns true so caller decrements move counter.
    // ─────────────────────────────────────────────────────────────────
    public boolean swapPieces(PuzzlePiece dragged, int toRow, int toCol) {
        if (dragged == null || dragged.locked) return false;
        if (toRow < 0 || toRow >= gridRows
                || toCol < 0 || toCol >= gridCols) return false;
        if (dragged.currentRow == toRow
                && dragged.currentCol == toCol) return false;

        // Collect pieces involved BEFORE the move
        List<PuzzlePiece> toScan = new ArrayList<>();
        if (dragged.groupId >= 0)
            toScan.addAll(getGroupPieces(dragged.groupId));
        else
            toScan.add(dragged);

        PuzzlePiece target = grid[toRow][toCol];
        if (target != null) {
            if (target.groupId >= 0)
                toScan.addAll(getGroupPieces(target.groupId));
            else
                toScan.add(target);
        }

        if (dragged.groupId >= 0) {
            // Dragged is a group — move whole group
            moveGroup(dragged, toRow, toCol);
        } else if (target != null && target.groupId >= 0) {
            // Target is a group, dragged is single —
            // move target's group to where dragged is, then place dragged
            int fromRow = dragged.currentRow;
            int fromCol = dragged.currentCol;
            grid[fromRow][fromCol] = null;
            moveGroup(target, fromRow, fromCol);
            // Place dragged at destination (now freed by group moving away)
            dragged.currentRow = toRow;
            dragged.currentCol = toCol;
            grid[toRow][toCol] = dragged;
        } else {
            // Both are single pieces
            doSwap(dragged, toRow, toCol);
        }

        // Only scan involved pieces
        scanAndMergeFor(toScan);

        int maxGroup = largestGroupSize();
        if (maxGroup >= 2 && listener != null)
            listener.onSnap(maxGroup);

        if (isPuzzleComplete()) {
            lockAll();
            if (listener != null) listener.onPuzzleComplete();
        }

        assertGridIntegrity();
        return true;
    }

    // ── Swap dragged with piece at (toRow, toCol) ──────────────────────
    // Their grid positions are exchanged — simple and clean
    private void doSwap(PuzzlePiece dragged, int toRow, int toCol) {
        int fromRow = dragged.currentRow;
        int fromCol = dragged.currentCol;
        PuzzlePiece target = grid[toRow][toCol];

        // PHASE 1 — Clear both cells
        grid[fromRow][fromCol] = null;
        grid[toRow][toCol]     = null;

        // PHASE 2 — Place both pieces
        dragged.currentRow = toRow;
        dragged.currentCol = toCol;
        grid[toRow][toCol] = dragged;

        if (target != null && target != dragged) {
            target.currentRow      = fromRow;
            target.currentCol      = fromCol;
            grid[fromRow][fromCol] = target;
        }
    }

    // ── Move entire group, swap displaced pieces into vacated cells ────
    private void moveGroup(PuzzlePiece leader, int toRow, int toCol) {
        int dRow = toRow - leader.currentRow;
        int dCol = toCol - leader.currentCol;
        if (dRow == 0 && dCol == 0) return;

        List<PuzzlePiece> movingGroup = getGroupPieces(leader.groupId);

        // ── Bounds check for moving group ────────────────────────────
        for (PuzzlePiece p : movingGroup) {
            int nr = p.currentRow + dRow;
            int nc = p.currentCol + dCol;
            if (nr < 0 || nr >= gridRows || nc < 0 || nc >= gridCols)
                return;
        }

        // ── Collect ALL pieces that will be disturbed ─────────────────
        // (pieces sitting in cells the moving group wants to occupy)
        List<PuzzlePiece> displaced = new ArrayList<>();
        for (PuzzlePiece p : movingGroup) {
            int nr = p.currentRow + dRow;
            int nc = p.currentCol + dCol;
            PuzzlePiece occ = grid[nr][nc];
            if (occ != null && !movingGroup.contains(occ)
                    && !displaced.contains(occ)) {
                // If displaced piece is in a group, collect WHOLE group
                if (occ.groupId >= 0) {
                    for (PuzzlePiece gp : getGroupPieces(occ.groupId))
                        if (!displaced.contains(gp)) displaced.add(gp);
                } else {
                    displaced.add(occ);
                }
            }
        }

        // ── Bounds check: can displaced pieces move in reverse? ───────
        for (PuzzlePiece d : displaced) {
            int nr = d.currentRow - dRow;
            int nc = d.currentCol - dCol;
            if (nr < 0 || nr >= gridRows || nc < 0 || nc >= gridCols) {
                // Cannot move displaced piece — cancel the whole move
                return;
            }
        }

        // ════════════════════════════════════════════════════════════
        // PHASE 1 — Clear ALL involved pieces from the grid at once
        //           Do NOT place anything yet
        // ════════════════════════════════════════════════════════════
        for (PuzzlePiece p : movingGroup)
            grid[p.currentRow][p.currentCol] = null;

        for (PuzzlePiece d : displaced)
            grid[d.currentRow][d.currentCol] = null;

        // ════════════════════════════════════════════════════════════
        // PHASE 2 — Place ALL pieces into their new positions
        //           Grid is clean so no overwrites possible
        // ════════════════════════════════════════════════════════════

        // Move main group forward
        for (PuzzlePiece p : movingGroup) {
            p.currentRow += dRow;
            p.currentCol += dCol;
            grid[p.currentRow][p.currentCol] = p;
        }

        // Move displaced pieces in reverse
        for (PuzzlePiece d : displaced) {
            d.currentRow -= dRow;
            d.currentCol -= dCol;
            grid[d.currentRow][d.currentCol] = d;
        }

        // ════════════════════════════════════════════════════════════
        // PHASE 3 — Safety net: verify EVERY piece is in the grid.
        //           If any piece is missing, find the empty cell
        //           and place it there. This guarantees zero empty slots.
        // ════════════════════════════════════════════════════════════
        for (PuzzlePiece p : pieces) {
            // Check if this piece's recorded position matches grid
            if (grid[p.currentRow][p.currentCol] != p) {
                // Piece is orphaned — find an empty cell for it
                boolean placed = false;
                for (int r = 0; r < gridRows && !placed; r++) {
                    for (int c = 0; c < gridCols && !placed; c++) {
                        if (grid[r][c] == null) {
                            p.currentRow = r;
                            p.currentCol = c;
                            grid[r][c]   = p;
                            placed       = true;
                        }
                    }
                }
            }
        }
    }

// ─────────────────────────────────────────────────────────────────────
// GROUP DETECTION — called after every swap
//
// Rule (exact as specified):
// Two pieces A and B merge IF AND ONLY IF:
//   1. Their correctId positions are adjacent in the original image
//      in a specific direction (left-right OR top-bottom)
//   2. AND they are currently placed next to each other on the board
//      in that EXACT same direction
//
// We check all 4 neighbors of every piece after every swap.
// Physical adjacency alone is NOT enough — correct-position
// adjacency in the same direction is ALSO required.
// ─────────────────────────────────────────────────────────────────────
    private void scanAndMergeFor(List<PuzzlePiece> targets) {
        int[][] dirs = {{-1,0},{1,0},{0,-1},{0,1}};
        for (PuzzlePiece a : targets) {
            for (int[] d : dirs) {
                int nRow = a.currentRow + d[0];
                int nCol = a.currentCol + d[1];
                if (nRow < 0 || nRow >= gridRows
                        || nCol < 0 || nCol >= gridCols) continue;
                PuzzlePiece b = grid[nRow][nCol];
                if (b == null) continue;
                if (a.groupId >= 0 && a.groupId == b.groupId) continue;

                int boardDR = b.currentRow - a.currentRow;
                int boardDC = b.currentCol - a.currentCol;
                int imageDR = b.correctRow - a.correctRow;
                int imageDC = b.correctCol - a.correctCol;

                boolean imageAdjacent = (Math.abs(imageDR) + Math.abs(imageDC)) == 1;
                boolean directionMatches = boardDR == imageDR && boardDC == imageDC;

                if (imageAdjacent && directionMatches)
                    merge(a, b);
            }
        }
    }

private void merge(PuzzlePiece a, PuzzlePiece b) {
    if (a.groupId >= 0 && a.groupId == b.groupId) return;

    if (a.groupId < 0 && b.groupId < 0) {
        int id    = nextGroupId++;
        a.groupId = id;
        b.groupId = id;
    } else if (a.groupId >= 0 && b.groupId < 0) {
        b.groupId = a.groupId;
    } else if (a.groupId < 0) {
        a.groupId = b.groupId;
    } else {
        // Both have different groups — absorb b's group into a's group
        int keep = a.groupId;
        int rem  = b.groupId;
        for (PuzzlePiece p : pieces)
            if (p.groupId == rem) p.groupId = keep;
    }
}

    // ── Scramble ungrouped pieces ──────────────────────────────────────
    public void scramble() {
        List<PuzzlePiece> free = new ArrayList<>();
        for (PuzzlePiece p : pieces)
            if (p.groupId < 0) free.add(p);
        if (free.size() < 2) return;

        List<int[]> pos = new ArrayList<>();
        for (PuzzlePiece p : free)
            pos.add(new int[]{p.currentRow, p.currentCol});

        Random rng     = new Random();
        boolean ok     = false;
        while (!ok) {
            Collections.shuffle(pos, rng);
            for (int i = 0; i < free.size(); i++) {
                if (free.get(i).currentRow != pos.get(i)[0]
                        || free.get(i).currentCol != pos.get(i)[1]) {
                    ok = true; break;
                }
            }
        }

        for (int i = 0; i < free.size(); i++) {
            PuzzlePiece p  = free.get(i);
            p.currentRow   = pos.get(i)[0];
            p.currentCol   = pos.get(i)[1];
            grid[p.currentRow][p.currentCol] = p;
        }

        scanAndMergeFor(pieces);
    }

    // ── Completion ─────────────────────────────────────────────────────
    public boolean isPuzzleComplete() {
        if (pieces == null || pieces.isEmpty()) return false;
        int firstGroup = pieces.get(0).groupId;
        if (firstGroup < 0) return false; // not even grouped
        for (PuzzlePiece p : pieces)
            if (p.groupId != firstGroup) return false;
        return true;
    }

    private void lockAll() {
        for (PuzzlePiece p : pieces) p.locked = true;
    }

    // ── Lookups ────────────────────────────────────────────────────────
    public PuzzlePiece findAt(float tx, float ty) {
        if (pieces == null) return null;
        // Check last (top-drawn) first
        for (int i = pieces.size() - 1; i >= 0; i--) {
            PuzzlePiece p = pieces.get(i);
            float px = boardLeft + p.currentCol * pieceW;
            float py = boardTop  + p.currentRow * pieceH;
            if (tx >= px && tx <= px + pieceW
                    && ty >= py && ty <= py + pieceH) return p;
        }
        return null;
    }

    public int[] screenToCell(float tx, float ty) {
        if (tx < boardLeft || tx > boardLeft + boardW)  return null;
        if (ty < boardTop  || ty > boardTop  + boardH)  return null;
        int col = Math.max(0, Math.min(gridCols - 1,
                (int)((tx - boardLeft) / pieceW)));
        int row = Math.max(0, Math.min(gridRows - 1,
                (int)((ty - boardTop)  / pieceH)));
        return new int[]{row, col};
    }

    public List<PuzzlePiece> getGroupPieces(int groupId) {
        List<PuzzlePiece> g = new ArrayList<>();
        if (groupId < 0 || pieces == null) return g;
        for (PuzzlePiece p : pieces)
            if (p.groupId == groupId) g.add(p);
        return g;
    }

    // For PuzzleView group border rendering
    public List<PuzzlePiece> getGroup(PuzzlePiece p) {
        if (p == null) return new ArrayList<>();
        if (p.groupId < 0) {
            List<PuzzlePiece> s = new ArrayList<>();
            s.add(p); return s;
        }
        return getGroupPieces(p.groupId);
    }

    @Nullable
    public PuzzlePiece getPieceAt(int row, int col) {
        if (row < 0 || row >= gridRows
                || col < 0 || col >= gridCols) return null;
        return grid[row][col];
    }

    private int largestGroupSize() {
        int max = 0;
        if (pieces == null) return 0;
        for (PuzzlePiece p : pieces) {
            if (p.groupId < 0) continue;
            int sz = getGroupPieces(p.groupId).size();
            if (sz > max) max = sz;
        }
        return max;
    }

    public int getPlacedCount() {
        int n = 0;
        if (pieces == null) return 0;
        for (PuzzlePiece p : pieces)
            if (p.currentRow == p.correctRow
                    && p.currentCol == p.correctCol) n++;
        return n;
    }

    public List<PuzzlePiece> getPieces()  { return pieces;      }
    public Bitmap getFullImage()          { return originalBitmap; }
    public float getBoardLeft()           { return boardLeft;   }
    public float getBoardTop()            { return boardTop;    }
    public float getBoardW()              { return boardW;      }
    public float getBoardH()              { return boardH;      }
    public float getPieceW()              { return pieceW;      }
    public float getPieceH()              { return pieceH;      }
    public int   getGridCols()            { return gridCols;    }
    public int   getGridRows()            { return gridRows;    }
    public int   getTotalPieces()         {
        return pieces != null ? pieces.size() : 0;
    }

    private int getResId() {
        if (isDaily) {
            switch (levelNumber) {
                case 1: return R.drawable.daily_1;
                case 2: return R.drawable.daily_2;
                case 3: return R.drawable.daily_3;
                case 4: return R.drawable.daily_4;
                case 5: return R.drawable.daily_5;
            }
        } else {
            switch (levelNumber) {
                case 1: return R.drawable.level_1;
                case 2: return R.drawable.level_2;
                case 3: return R.drawable.level_3;
                case 4: return R.drawable.level_4;
                case 5: return R.drawable.level_5;
            }
        }
        return R.drawable.level_1;
    }

    private int calcSample(BitmapFactory.Options o, int rw, int rh) {
        int h = o.outHeight, w = o.outWidth, s = 1;
        while ((h/(s*2)) >= rh && (w/(s*2)) >= rw) s *= 2;
        return s;
    }

    public void recycle() {
        if (originalBitmap != null && !originalBitmap.isRecycled())
            originalBitmap.recycle();
        if (pieces != null) {
            for (PuzzlePiece p : pieces)
                if (p.bitmap != null && !p.bitmap.isRecycled())
                    p.bitmap.recycle();
            pieces.clear();
        }
    }

    private void assertGridIntegrity() {
        // Check no cell is null
        for (int r = 0; r < gridRows; r++)
            for (int c = 0; c < gridCols; c++)
                if (grid[r][c] == null)
                    android.util.Log.e("PUZZLE", "NULL at " + r + "," + c);

        // Check no piece appears twice
        for (int i = 0; i < pieces.size(); i++)
            for (int j = i + 1; j < pieces.size(); j++)
                if (pieces.get(i).currentRow == pieces.get(j).currentRow
                        && pieces.get(i).currentCol == pieces.get(j).currentCol)
                    android.util.Log.e("PUZZLE",
                            "OVERLAP at " + pieces.get(i).currentRow
                                    + "," + pieces.get(i).currentCol);
    }
}