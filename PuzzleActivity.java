package com.puzzleverse.game;

import android.animation.ObjectAnimator;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.OvershootInterpolator;
import android.widget.ProgressBar;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

public class PuzzleActivity extends AppCompatActivity implements PuzzleView.Listener {

    public static final String EXTRA_LEVEL_NUMBER = "level_number";
    public static final String EXTRA_IS_DAILY     = "is_daily";

    // Dependencies
    private GamePreferences      gamePrefs;
    private SoundManager         soundManager;
    private AchievementManager   achManager;
    private HapticManager        hapticManager;
    private PuzzleEngine         engine;
    private PuzzleView           puzzleView;
    private AuroraBackgroundView auroraBg;

    // HUD views — ONLY what is actually in the layout
    private TextView       tvCoinCount;
    private TextView       tvMovesCount;
    private ProgressBar    pbMoves;
    private TextView       tvMovesRemaining;

    // Bottom bar buttons
    private MaterialButton btnPeek;
    private MaterialButton btnGhost;

    // State
    private int     levelNumber;
    private boolean isDaily;
    private int     moveLimit;
    private boolean puzzleFinished    = false;
    private boolean peekUsedThisLevel = false;
    private boolean ghostActive       = false;
    private int     moveCount         = 0;
    private long    startTimeMs       = 0; // for achievement time check only

    private CountDownTimer peekTimer;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_puzzle);

        gamePrefs     = new GamePreferences(this);
        soundManager  = SoundManager.getInstance(this);
        hapticManager = new HapticManager(this, gamePrefs);
        soundManager.startMusic();

        achManager = new AchievementManager(gamePrefs);
        achManager.setListener(this::showAchievementUnlocked);

        levelNumber = getIntent().getIntExtra(EXTRA_LEVEL_NUMBER, 1);
        isDaily     = getIntent().getBooleanExtra(EXTRA_IS_DAILY, false);
        boolean isDifficult = !isDaily && GamePreferences.isDifficultLevel(levelNumber);

        // Build engine
        int cols = isDaily ? 7 : isDifficult ? 6 : 5;
        int rows = isDaily ? 7 : isDifficult ? 6 : 5;

        moveLimit = GamePreferences.getMoveLimit(isDifficult, isDaily);

        // Bind views
        auroraBg         = findViewById(R.id.aurora_bg);
        tvCoinCount      = findViewById(R.id.tv_coin_count);
        TextView tvLevelTitle = findViewById(R.id.tv_level_title);
        tvMovesCount     = findViewById(R.id.tv_moves_count);
        MaterialButton btnBack = findViewById(R.id.btn_back);
        btnPeek          = findViewById(R.id.btn_peek);
        btnGhost         = findViewById(R.id.btn_ghost);
        MaterialButton btnScramble = findViewById(R.id.btn_scramble);
        puzzleView       = findViewById(R.id.puzzle_view);

        // Handle window insets for edge-to-edge support
        View hudRow = findViewById(R.id.hud_row);
        View bottomBar = findViewById(R.id.bottom_bar);
        View uiContainer = findViewById(R.id.puzzle_ui_container);
        if (uiContainer != null) {
            ViewCompat.setOnApplyWindowInsetsListener(uiContainer, (v, windowInsets) -> {
                Insets insets = windowInsets.getInsets(WindowInsetsCompat.Type.systemBars()
                        | WindowInsetsCompat.Type.displayCutout());
                if (hudRow != null) {
                    hudRow.setPadding(hudRow.getPaddingLeft(), insets.top,
                            hudRow.getPaddingRight(), hudRow.getPaddingBottom());
                }
                if (bottomBar != null) {
                    bottomBar.setPadding(bottomBar.getPaddingLeft(), bottomBar.getPaddingTop(),
                            bottomBar.getPaddingRight(), insets.bottom);
                }
                return windowInsets;
            });
        }

        View movesBarView = findViewById(R.id.moves_bar);
        if (movesBarView != null) {
            pbMoves          = movesBarView.findViewById(R.id.pb_moves);
            tvMovesRemaining = movesBarView.findViewById(R.id.tv_moves_remaining);
        }

        tvLevelTitle.setText(isDaily
                ? getString(R.string.daily_challenge_format, levelNumber)
                : getString(R.string.level_format, levelNumber));

        updateCoinDisplay();
        updateMovesBar();

        engine = new PuzzleEngine(this, cols, rows, levelNumber, isDaily);
        puzzleView.setEngine(engine);
        puzzleView.setListener(this);
        puzzleView.setMysteryMode(gamePrefs.isMysteryModeEnabled());

        startTimeMs = System.currentTimeMillis();

        // Buttons
        if (btnBack    != null) btnBack.setOnClickListener(v -> showExitConfirm());
        if (btnPeek    != null) btnPeek.setOnClickListener(v -> handlePeek());
        if (btnGhost   != null) btnGhost.setOnClickListener(v -> handleGhost());
        if (btnScramble!= null) btnScramble.setOnClickListener(v -> handleScramble());

        // Show mode toast after board renders (no grid numbers shown)
        if (isDifficult || isDaily) {
            puzzleView.postDelayed(() -> {
                String emoji = isDaily ? "📅" : "🔥";
                String title = isDaily
                        ? getString(R.string.label_mode_daily)
                        : getString(R.string.label_mode_difficult);
                String msg = isDaily
                        ? getString(R.string.mode_daily_desc)
                        : getString(R.string.mode_difficult_desc);
                AuroraToast.showTop(this, emoji, title, msg);
            }, 900);
        }

        // Level start dialog
        puzzleView.postDelayed(this::showLevelStartDialog, 250);
    }

    // ── Level start dialog ─────────────────────────────────────────────
    @android.annotation.SuppressLint("DiscouragedApi")
    private void showLevelStartDialog() {
        View dv = LayoutInflater.from(this)
                .inflate(R.layout.dialog_level_start, null);

        TextView  tvLabel  = dv.findViewById(R.id.tv_start_level_label);
        TextView  tvReward = dv.findViewById(R.id.tv_start_reward);
        android.widget.ImageView ivThumb =
                dv.findViewById(R.id.iv_start_thumb);

        tvLabel.setText(isDaily
                ? getString(R.string.daily_challenge_format, levelNumber)
                : getString(R.string.level_format, levelNumber));

        int coins = isDaily ? GamePreferences.COINS_DAILY_CHALLENGE
                : GamePreferences.getCoinReward(levelNumber);
        tvReward.setText(getString(R.string.start_level_reward_format, coins));

        String imgName = isDaily
                ? "daily_" + levelNumber : "level_" + levelNumber;
        int resId = getResources().getIdentifier(
                imgName, "drawable", getPackageName());
        if (resId != 0) {
            android.graphics.BitmapFactory.Options opts =
                    new android.graphics.BitmapFactory.Options();
            opts.inSampleSize = 4;
            android.graphics.Bitmap bmp =
                    android.graphics.BitmapFactory.decodeResource(
                            getResources(), resId, opts);
            if (bmp != null) ivThumb.setImageBitmap(bmp);
        }

        dv.setScaleX(0.75f); dv.setScaleY(0.75f); dv.setAlpha(0f);
        dv.animate().scaleX(1f).scaleY(1f).alpha(1f).setDuration(380)
                .setInterpolator(new OvershootInterpolator(1.3f)).start();

        androidx.appcompat.app.AlertDialog dialog =
                new MaterialAlertDialogBuilder(this, R.style.PuzzleVerseDialog)
                        .setView(dv).setCancelable(false).create();

        MaterialButton btnPlay = dv.findViewById(R.id.btn_start_play);
        if (btnPlay != null) btnPlay.setOnClickListener(v -> dialog.dismiss());
        dialog.show();
    }

    // ── Moves bar ──────────────────────────────────────────────────────
    private void updateMovesBar() {
        int remaining = Math.max(0, moveLimit - moveCount);
        int progress  = (int)(100f * remaining / moveLimit);

        if (tvMovesCount     != null) tvMovesCount.setText(String.valueOf(remaining));
        if (tvMovesRemaining != null) tvMovesRemaining.setText(String.valueOf(remaining));

        if (pbMoves != null) {
            ObjectAnimator.ofInt(pbMoves, "progress",
                            pbMoves.getProgress(), progress)
                    .setDuration(200).start();

            // Color shifts from cyan → dimmer cyan as moves run out
            int col;
            if      (remaining <= moveLimit * 0.25f) col = Color.parseColor("#3A8A88");
            else if (remaining <= moveLimit * 0.50f) col = Color.parseColor("#4ABCB5");
            else                                     col = Color.parseColor("#66FCF1");

            pbMoves.setProgressTintList(
                    android.content.res.ColorStateList.valueOf(col));
            if (tvMovesCount     != null) tvMovesCount.setTextColor(col);
            if (tvMovesRemaining != null) tvMovesRemaining.setTextColor(col);
        }
    }

    // ── Peek ───────────────────────────────────────────────────────────
    private void handlePeek() {
        if (puzzleFinished) return;
        if (!gamePrefs.spendCoins(GamePreferences.COINS_PEEK_COST)) {
            soundManager.playTap();
            showInfoDialog(getString(R.string.not_enough_coins),
                    getString(R.string.peek_not_enough_coins_msg,
                            GamePreferences.COINS_PEEK_COST));
            return;
        }
        // Mark peek used — AchievementManager handles streak logic
        if (!peekUsedThisLevel) {
            peekUsedThisLevel = true;
            achManager.checkPeekUsed();
        }
        soundManager.playPeek();
        updateCoinDisplay();
        puzzleView.startPeek(engine.getFullImage());
        if (btnPeek != null) {
            btnPeek.setEnabled(false);
            btnPeek.setText(R.string.peek_peeking);
        }

        peekTimer = new CountDownTimer(3000, 1000) {
            int s = 3;
            @Override public void onTick(long ms) {
                if (btnPeek != null)
                    btnPeek.setText(getString(R.string.peek_timer_format, s--));
            }
            @Override public void onFinish() {
                puzzleView.stopPeek();
                if (btnPeek != null) {
                    btnPeek.setEnabled(true);
                    btnPeek.setText(R.string.peek_button_text);
                }
            }
        }.start();
    }

    // ── Ghost ──────────────────────────────────────────────────────────
    private void handleGhost() {
        if (puzzleFinished) return;
        if (ghostActive) {
            puzzleView.setGhostVisible(false);
            ghostActive = false;
            if (btnGhost != null) btnGhost.setText(R.string.ghost_btn);
            return;
        }
        if (!gamePrefs.spendCoins(GamePreferences.COINS_GHOST_COST)) {
            showInfoDialog(getString(R.string.not_enough_coins),
                    getString(R.string.ghost_not_enough));
            return;
        }
        updateCoinDisplay();
        puzzleView.setGhostVisible(true);
        ghostActive = true;
        if (btnGhost != null) btnGhost.setText(R.string.ghost_hide);
    }

    // ── Scramble ───────────────────────────────────────────────────────
    private void handleScramble() {
        if (puzzleFinished) return;
        new MaterialAlertDialogBuilder(this, R.style.PuzzleVerseDialog)
                .setTitle(R.string.scramble_title)
                .setMessage(R.string.scramble_msg)
                .setPositiveButton(R.string.scramble_btn, (d, w) -> {
                    engine.scramble();
                    puzzleView.invalidate();
                    soundManager.playTap();
                })
                .setNegativeButton(R.string.cancel, null)
                .show();
    }

    // ── Engine callbacks ───────────────────────────────────────────────
    @Override
    public void onMoveUsed() {
        moveCount++;
        updateMovesBar();
        soundManager.playSnap();
        hapticManager.vibrateSnap();

        if (moveCount >= moveLimit && !engine.isPuzzleComplete()) {
            puzzleView.postDelayed(this::showMovesFailedDialog, 350);
        }
    }

    @Override
    public void onSnap(int groupSize) {
        achManager.checkChainReaction(groupSize);
        hapticManager.vibrateForGroup(groupSize);
    }

    @Override
    public void onPuzzleComplete() {
        if (puzzleFinished) return;
        puzzleFinished = true;

        long elapsed = System.currentTimeMillis() - startTimeMs;

        soundManager.playWin();
        hapticManager.vibrateComplete();
        launchConfetti();

        boolean isNewBest = gamePrefs.updateBestMoves(levelNumber, isDaily, moveCount);
        gamePrefs.updateBestTime(levelNumber, isDaily, elapsed);

        int coinsEarned;
        if (isDaily) {
            gamePrefs.completeDaily(levelNumber);
            coinsEarned = GamePreferences.COINS_DAILY_CHALLENGE;
        } else {
            gamePrefs.completeLevel(levelNumber);
            coinsEarned = GamePreferences.getCoinReward(levelNumber);
        }
        gamePrefs.addCoins(coinsEarned);
        updateCoinDisplay();

        achManager.checkAfterLevel(
                levelNumber, isDaily, moveCount, elapsed, peekUsedThisLevel);

        boolean firstWin = gamePrefs.getTotalLevelsCompleted() == 1
                && !gamePrefs.isFirstWinShown();

        puzzleView.postDelayed(() -> {
            if (firstWin) {
                showFirstWinOverlay(() ->
                        launchWin(coinsEarned, elapsed, isNewBest));
            } else {
                launchWin(coinsEarned, elapsed, isNewBest);
            }
        }, 900);
    }

    // ── First win overlay ──────────────────────────────────────────────
    private void showFirstWinOverlay(Runnable after) {
        gamePrefs.markFirstWinShown();
        ViewGroup root = findViewById(android.R.id.content);
        View ov = LayoutInflater.from(this)
                .inflate(R.layout.layout_first_win, root, false);
        root.addView(ov);
        ov.setAlpha(0f);
        ov.animate().alpha(1f).setDuration(300).start();
        puzzleView.postDelayed(() ->
                ov.animate().alpha(0f).setDuration(300)
                        .withEndAction(() -> {
                            root.removeView(ov);
                            after.run();
                        }).start(), 2000);
    }

    private void launchConfetti() {
        ViewGroup root = findViewById(android.R.id.content);
        ConfettiView cv = new ConfettiView(this);
        cv.setLayoutParams(new ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT));
        root.addView(cv);
        cv.post(cv::launch);
    }

    private void launchWin(int coins, long elapsed, boolean newBest) {
        Intent i = new Intent(this, WinActivity.class);
        i.putExtra(WinActivity.EXTRA_LEVEL_NUMBER, levelNumber);
        i.putExtra(WinActivity.EXTRA_IS_DAILY, isDaily);
        i.putExtra(WinActivity.EXTRA_MOVES, moveCount);
        i.putExtra(WinActivity.EXTRA_ELAPSED_MS, elapsed);
        i.putExtra(WinActivity.EXTRA_COINS_EARNED, coins);
        i.putExtra(WinActivity.EXTRA_IS_NEW_BEST, newBest);
        startActivity(i);
        TransitionHelper.fade(this);
        finish();
    }

    // ── Moves failed ───────────────────────────────────────────────────
    private void showMovesFailedDialog() {
        if (puzzleFinished) return;
        puzzleFinished = true;
        View dv = LayoutInflater.from(this)
                .inflate(R.layout.dialog_moves_failed, null);
        ((TextView) dv.findViewById(R.id.tv_fail_moves_used))
                .setText(getString(R.string.moves_used_format, moveCount));
        ((TextView) dv.findViewById(R.id.tv_fail_pieces_placed))
                .setText(getString(R.string.pieces_placed_format,
                        engine.getPlacedCount(), engine.getTotalPieces()));
        new MaterialAlertDialogBuilder(this, R.style.PuzzleVerseDialog)
                .setView(dv).setCancelable(false)
                .setPositiveButton(R.string.try_again,
                        (d, w) -> { launchLevel(levelNumber); finish(); })
                .setNegativeButton(R.string.back_home, (d, w) -> finish())
                .show();
    }

    // ── Achievement popup ──────────────────────────────────────────────
    private void showAchievementUnlocked(Achievement a) {
        runOnUiThread(() -> {
            View dv = LayoutInflater.from(this)
                    .inflate(R.layout.dialog_achievement_unlock, null);
            ((TextView) dv.findViewById(R.id.tv_ach_unlock_emoji)).setText(a.emoji);
            ((TextView) dv.findViewById(R.id.tv_ach_unlock_title)).setText(a.title);
            ((TextView) dv.findViewById(R.id.tv_ach_unlock_desc)).setText(a.description);
            ((TextView) dv.findViewById(R.id.tv_ach_unlock_tier)).setText(a.getTierLabel());
            ((TextView) dv.findViewById(R.id.tv_ach_unlock_reward))
                    .setText(getString(R.string.coins_earned_value_format, a.coinReward));
            new MaterialAlertDialogBuilder(this, R.style.PuzzleVerseDialog)
                    .setView(dv).setCancelable(true)
                    .setPositiveButton(R.string.awesome, null).show();
            updateCoinDisplay();
        });
    }

    private void showExitConfirm() {
        new com.google.android.material.dialog.MaterialAlertDialogBuilder(
                this, R.style.PuzzleVerseDialog)
                .setTitle(R.string.exit_confirm_title)
                .setMessage(R.string.exit_confirm_msg)
                .setPositiveButton(R.string.leave, (d, w) -> finish())
                .setNegativeButton(R.string.stay, (d, w) -> d.dismiss())
                .show();
    }

    private void showInfoDialog(String title, String msg) {
        // Replace dialog with aurora toast — much less intrusive
        AuroraToast.showCenter(this, "ℹ️", title, msg);
    }

    private void updateCoinDisplay() {
        if (tvCoinCount != null)
            tvCoinCount.setText(String.valueOf(gamePrefs.getCoins()));
    }

    private void launchLevel(int num) {
        Intent i = new Intent(this, PuzzleActivity.class);
        i.putExtra(EXTRA_LEVEL_NUMBER, num);
        i.putExtra(EXTRA_IS_DAILY, isDaily);
        startActivity(i);
        TransitionHelper.fade(this);
        finish();
    }

    @Override protected void onPause() {
        super.onPause();
        soundManager.pauseMusic();
        if (auroraBg != null) auroraBg.pauseAnimations();
    }

    @Override protected void onResume() {
        super.onResume();
        soundManager.resumeMusic();
        if (auroraBg != null) auroraBg.resumeAnimations();
    }

    @Override protected void onDestroy() {
        super.onDestroy();
        if (peekTimer != null) peekTimer.cancel();
        if (engine    != null) engine.recycle();
    }
}