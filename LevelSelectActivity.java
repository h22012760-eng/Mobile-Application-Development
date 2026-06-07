package com.puzzleverse.game;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.view.animation.Animation;
import android.view.animation.TranslateAnimation;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

public class LevelSelectActivity extends AppCompatActivity {

    private GamePreferences gamePrefs;
    private RecyclerView    recyclerLevels;
    private TextView        tvCoinCount;
    private TextView        tvSummary;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_level_select);

        gamePrefs      = new GamePreferences(this);
        tvCoinCount    = findViewById(R.id.tv_coin_count);
        tvSummary      = findViewById(R.id.tv_levels_summary);
        recyclerLevels = findViewById(R.id.recycler_levels);

        updateCoinDisplay();
        setupLevelGrid();

        findViewById(R.id.btn_back).setOnClickListener(v -> {
            finish();
            overridePendingTransition(
                    android.R.anim.fade_in,
                    android.R.anim.fade_out);
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        updateCoinDisplay();
        setupLevelGrid();
    }

    private void updateCoinDisplay() {
        tvCoinCount.setText(String.valueOf(gamePrefs.getCoins()));
    }

    private void setupLevelGrid() {
        // Update subtitle
        int done = gamePrefs.getTotalLevelsCompleted();
        tvSummary.setText(done + " / "
                + GamePreferences.TOTAL_HOME_LEVELS + " completed");

        LevelAdapter adapter = new LevelAdapter();
        recyclerLevels.setLayoutManager(
                new GridLayoutManager(this, 3));
        recyclerLevels.setAdapter(adapter);

        // Fade in grid
        recyclerLevels.setAlpha(0f);
        recyclerLevels.animate().alpha(1f).setDuration(400).start();
    }

    private void onLevelClicked(int levelNumber) {
        int status = gamePrefs.getLevelStatus(levelNumber);
        if (status == GamePreferences.STATUS_LOCKED) return;
        launchLevel(levelNumber);
    }

    private void launchLevel(int levelNumber) {
        Intent intent = new Intent(this, PuzzleActivity.class);
        intent.putExtra(PuzzleActivity.EXTRA_LEVEL_NUMBER, levelNumber);
        intent.putExtra(PuzzleActivity.EXTRA_IS_DAILY, false);
        startActivity(intent);
        overridePendingTransition(
                android.R.anim.fade_in,
                android.R.anim.fade_out);
    }

    private Bitmap loadThumbnail(int levelNumber) {
        String name = "level_" + levelNumber;
        int resId = getResources().getIdentifier(
                name, "drawable", getPackageName());
        if (resId == 0) return null;
        try {
            BitmapFactory.Options opts = new BitmapFactory.Options();
            opts.inSampleSize    = 8;
            opts.inJustDecodeBounds = false;
            return BitmapFactory.decodeResource(
                    getResources(), resId, opts);
        } catch (Exception e) {
            return null;
        }
    }

    private String starsString(int stars) {
        switch (stars) {
            case 3:  return "⭐⭐⭐";
            case 2:  return "⭐⭐";
            case 1:  return "⭐";
            default: return "";
        }
    }

    private void shakeView(View view) {
        TranslateAnimation anim = new TranslateAnimation(
                0, 18, 0, 0);
        anim.setDuration(70);
        anim.setRepeatMode(Animation.REVERSE);
        anim.setRepeatCount(4);
        anim.setInterpolator(new AccelerateDecelerateInterpolator());
        view.startAnimation(anim);
    }

    // ── Adapter ───────────────────────────────────────────────────────
    private class LevelAdapter
            extends RecyclerView.Adapter<LevelAdapter.LevelVH> {

        @Override
        public LevelVH onCreateViewHolder(ViewGroup parent, int t) {
            View v = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_level_card, parent, false);
            return new LevelVH(v);
        }

        @Override
        public void onBindViewHolder(LevelVH h, int pos) {
            int num    = pos + 1;
            int status = gamePrefs.getLevelStatus(num);
            boolean isDiff = GamePreferences.isDifficultLevel(num);
            h.bind(num, status, isDiff);
        }

        @Override
        public int getItemCount() {
            return GamePreferences.TOTAL_HOME_LEVELS;
        }

        class LevelVH extends RecyclerView.ViewHolder {

            View      cardRoot;
            ImageView ivThumb;
            View      lockOverlay;
            TextView  tvLockIcon, tvBadge, tvNumber,
                    tvStars, tvStatus, tvBest;

            LevelVH(View v) {
                super(v);
                cardRoot    = v.findViewById(R.id.card_level_root);
                ivThumb     = v.findViewById(R.id.iv_level_thumb);
                lockOverlay = v.findViewById(R.id.view_lock_overlay);
                tvLockIcon  = v.findViewById(R.id.tv_lock_icon);
                tvBadge     = v.findViewById(R.id.tv_level_badge);
                tvNumber    = v.findViewById(R.id.tv_level_number);
                tvStars     = v.findViewById(R.id.tv_level_stars);
                tvStatus    = v.findViewById(R.id.tv_level_status);
                tvBest      = v.findViewById(R.id.tv_level_best);
            }

            void bind(int num, int status, boolean isDiff) {
                tvNumber.setText(String.valueOf(num));

                // Load thumbnail
                ivThumb.setImageBitmap(null);
                ivThumb.post(() -> {
                    Bitmap bmp = loadThumbnail(num);
                    if (bmp != null) ivThumb.setImageBitmap(bmp);
                    else ivThumb.setBackgroundColor(
                            getColor(R.color.aurora_panel_dark));
                });

                if (status == GamePreferences.STATUS_LOCKED) {
                    cardRoot.setBackgroundResource(
                            R.drawable.bg_aurora_level_locked);
                    lockOverlay.setVisibility(View.VISIBLE);
                    tvLockIcon.setVisibility(View.VISIBLE);
                    tvBadge.setText("");
                    tvNumber.setTextColor(getColor(R.color.aurora_text_hint));
                    tvStars.setText("");
                    // Show "Locked" — no grid size
                    tvStatus.setText(getString(R.string.locked));
                    tvStatus.setTextColor(getColor(R.color.aurora_text_hint));
                    tvBest.setVisibility(View.GONE);
                    itemView.setAlpha(0.5f);
                    itemView.setOnClickListener(v -> shakeView(itemView));

                } else if (status == GamePreferences.STATUS_COMPLETED) {
                    cardRoot.setBackgroundResource(
                            R.drawable.bg_aurora_level_completed);
                    lockOverlay.setVisibility(View.GONE);
                    tvLockIcon.setVisibility(View.GONE);
                    tvBadge.setText(isDiff ? getString(R.string.emoji_fire) : "");
                    tvNumber.setTextColor(getColor(R.color.aurora_cyan));
                    itemView.setAlpha(1f);

                    int stars = gamePrefs.getBestStars(num, false);
                    tvStars.setText(starsString(stars));

                    // Easy / Difficult — NO grid numbers
                    tvStatus.setText(isDiff
                            ? getString(R.string.label_difficult)
                            : getString(R.string.label_easy));
                    tvStatus.setTextColor(isDiff
                            ? getColor(R.color.aurora_violet)
                            : getColor(R.color.aurora_cyan_dim));

                    int best = gamePrefs.getBestMoves(num, false);
                    if (best < Integer.MAX_VALUE) {
                        tvBest.setVisibility(View.VISIBLE);
                        tvBest.setText(getString(
                                R.string.best_moves_format, best));
                    } else {
                        tvBest.setVisibility(View.GONE);
                    }
                    itemView.setOnClickListener(v -> onLevelClicked(num));

                } else {
                    // Unlocked, not completed
                    cardRoot.setBackgroundResource(isDiff
                            ? R.drawable.bg_aurora_level_difficult
                            : R.drawable.bg_aurora_level_normal);
                    lockOverlay.setVisibility(View.GONE);
                    tvLockIcon.setVisibility(View.GONE);
                    tvBadge.setText(isDiff
                            ? getString(R.string.emoji_fire) : "");
                    tvNumber.setTextColor(getColor(R.color.aurora_text_primary));
                    tvStars.setText("");

                    // Easy / Difficult — NO grid numbers
                    tvStatus.setText(isDiff
                            ? getString(R.string.label_difficult)
                            : getString(R.string.label_easy));
                    tvStatus.setTextColor(isDiff
                            ? getColor(R.color.aurora_violet)
                            : getColor(R.color.level_border_normal));
                    tvBest.setVisibility(View.GONE);
                    itemView.setAlpha(1f);
                    itemView.setOnClickListener(v -> onLevelClicked(num));
                }
            }
        }
    }
}