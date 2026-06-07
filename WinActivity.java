package com.puzzleverse.game;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.animation.BounceInterpolator;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import com.google.android.material.button.MaterialButton;

public class WinActivity extends AppCompatActivity {

    private static final String TAG = "WinActivity";

    public static final String EXTRA_LEVEL_NUMBER = "level_number";
    public static final String EXTRA_IS_DAILY     = "is_daily";
    public static final String EXTRA_MOVES         = "moves";
    public static final String EXTRA_ELAPSED_MS    = "elapsed_ms";
    public static final String EXTRA_COINS_EARNED  = "coins_earned";
    public static final String EXTRA_IS_NEW_BEST   = "is_new_best";

    private AuroraBackgroundView auroraBg;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_win);

        auroraBg = findViewById(R.id.aurora_bg_win);

        int     levelNumber = getIntent().getIntExtra(EXTRA_LEVEL_NUMBER, 1);
        boolean isDaily     = getIntent().getBooleanExtra(EXTRA_IS_DAILY, false);
        int     moves       = getIntent().getIntExtra(EXTRA_MOVES, 0);
        int     coinsEarned = getIntent().getIntExtra(EXTRA_COINS_EARNED, 0);
        boolean isNewBest   = getIntent().getBooleanExtra(EXTRA_IS_NEW_BEST, false);

        // Views
        TextView       tvTitle   = findViewById(R.id.tv_win_title);
        TextView       tvTrophy  = findViewById(R.id.tv_win_trophy);
        TextView       tvMoves   = findViewById(R.id.tv_win_moves);
        TextView       tvCoins   = findViewById(R.id.tv_win_coins);
        TextView       tvNewBest = findViewById(R.id.tv_win_new_best);
        MaterialButton btnNext   = findViewById(R.id.btn_win_next);
        MaterialButton btnAgain  = findViewById(R.id.btn_win_again);
        MaterialButton btnHome   = findViewById(R.id.btn_win_home);
        MaterialButton btnShare  = findViewById(R.id.btn_win_share);

        // Set content
        tvTitle.setText(isDaily
                ? getString(R.string.challenge_complete_emoji)
                : getString(R.string.puzzle_complete_emoji));
        tvMoves.setText(getString(R.string.stat_moves, moves));
        tvCoins.setText(getString(R.string.coins_earned_value_format, coinsEarned));
        tvNewBest.setVisibility(isNewBest ? View.VISIBLE : View.GONE);

        // Animate trophy — drop in with bounce
        animateTrophy(tvTrophy);

        // Animate coins count
        animateCoins(tvCoins, coinsEarned);

        // Launch confetti
        launchConfetti();

        // Buttons
        boolean hasNext = !isDaily
                && levelNumber < GamePreferences.TOTAL_HOME_LEVELS;
        if (btnNext != null) {
            btnNext.setVisibility(hasNext ? View.VISIBLE : View.GONE);
            btnNext.setOnClickListener(v -> {
                Intent i = new Intent(this, PuzzleActivity.class);
                i.putExtra(PuzzleActivity.EXTRA_LEVEL_NUMBER, levelNumber + 1);
                i.putExtra(PuzzleActivity.EXTRA_IS_DAILY, false);
                startActivity(i);
                TransitionHelper.forward(this);
                finish();
            });
        }

        if (btnAgain != null) btnAgain.setOnClickListener(v -> {
            Intent i = new Intent(this, PuzzleActivity.class);
            i.putExtra(PuzzleActivity.EXTRA_LEVEL_NUMBER, levelNumber);
            i.putExtra(PuzzleActivity.EXTRA_IS_DAILY, isDaily);
            startActivity(i);
            TransitionHelper.fade(this);
            finish();
        });

        if (btnHome != null) btnHome.setOnClickListener(v -> {
            Intent i = new Intent(this, MainActivity.class);
            i.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
            startActivity(i);
            TransitionHelper.back(this);
            finish();
        });

        if (btnShare != null) btnShare.setOnClickListener(v ->
                shareResult(moves, coinsEarned, isNewBest));
    }

    // ── Trophy animation — drops from above with bounce ────────────────
    private void animateTrophy(TextView tv) {
        if (tv == null) return;
        tv.setTranslationY(-300f);
        tv.setAlpha(0f);
        tv.animate()
                .translationY(0f)
                .alpha(1f)
                .setDuration(900)
                .setInterpolator(new BounceInterpolator())
                .setStartDelay(200)
                .start();
    }

    // ── Coin count animates from 0 to final value ──────────────────────
    private void animateCoins(TextView tv, int finalValue) {
        if (tv == null) return;
        android.animation.ValueAnimator va =
                android.animation.ValueAnimator.ofInt(0, finalValue);
        va.setDuration(1200);
        va.setStartDelay(600);
        va.setInterpolator(new DecelerateInterpolator2());
        va.addUpdateListener(a -> {
            int value = (int) a.getAnimatedValue();
            tv.setText(getString(R.string.coins_earned_value_format, value));
        });
        va.start();
    }

    static class DecelerateInterpolator2
            implements android.animation.TimeInterpolator {
        @Override public float getInterpolation(float t) {
            return 1f - (1f - t) * (1f - t);
        }
    }

    private void launchConfetti() {
        android.view.ViewGroup root = findViewById(android.R.id.content);
        ConfettiView cv = new ConfettiView(this);
        cv.setLayoutParams(new android.view.ViewGroup.LayoutParams(
                android.view.ViewGroup.LayoutParams.MATCH_PARENT,
                android.view.ViewGroup.LayoutParams.MATCH_PARENT));
        root.addView(cv);
        cv.post(cv::launch);
    }

    private void shareResult(int moves, int coins, boolean newBest) {
        try {
            // Create a simple text-based share
            String text = "🏆 I just solved a PuzzleVerse puzzle!\n"
                    + "Moves: " + moves + "\n"
                    + "Coins earned: +" + coins + " 🪙"
                    + (newBest ? "\n🏅 New Personal Best!" : "");

            Intent share = new Intent(Intent.ACTION_SEND);
            share.setType("text/plain");
            share.putExtra(Intent.EXTRA_TEXT, text);
            startActivity(Intent.createChooser(share, getString(R.string.share)));
        } catch (Exception e) {
            Log.e(TAG, "Error sharing result", e);
        }
    }

    @Override protected void onResume() {
        super.onResume();
        if (auroraBg != null) auroraBg.resumeAnimations();
    }

    @Override protected void onPause() {
        super.onPause();
        if (auroraBg != null) auroraBg.pauseAnimations();
    }
}
