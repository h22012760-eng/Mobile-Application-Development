package com.puzzleverse.game;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

public class DailyChallengeActivity extends AppCompatActivity {

    private GamePreferences gamePrefs;
    private RecyclerView    recyclerDaily;
    private TextView        tvCoinCount;
    private TextView        tvCompleted;
    private DailyAdapter    adapter;

    // Countdown tick every second
    private final Handler  countdownHandler  = new Handler(Looper.getMainLooper());
    private final Runnable countdownRunnable = new Runnable() {
        @Override public void run() {
            if (adapter != null) {
                // More efficient than notifyDataSetChanged()
                adapter.notifyItemRangeChanged(0, adapter.getItemCount());
            }
            countdownHandler.postDelayed(this, 1000);
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_daily_challenge);

        gamePrefs     = new GamePreferences(this);
        tvCoinCount   = findViewById(R.id.tv_coin_count);
        tvCompleted   = findViewById(R.id.tv_daily_completed);
        recyclerDaily = findViewById(R.id.recycler_daily);

        updateCoinDisplay();
        updateCompletedCount();
        setupGrid();

        // Start countdown ticker
        countdownHandler.post(countdownRunnable);

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
        updateCompletedCount();
        setupGrid();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        countdownHandler.removeCallbacks(countdownRunnable);
    }

    private void updateCoinDisplay() {
        tvCoinCount.setText("" + gamePrefs.getCoins());
    }

    private void updateCompletedCount() {
        int done  = gamePrefs.getTotalDailyCompleted();
        int total = GamePreferences.TOTAL_DAILY_LEVELS;
        tvCompleted.setText(getString(R.string.daily_completed_format, done, total));
    }

    private void setupGrid() {
        adapter = new DailyAdapter();
        recyclerDaily.setLayoutManager(
                new GridLayoutManager(this, 3));
        recyclerDaily.setAdapter(adapter);
    }

    private void onDailyClicked(int num) {
        int status = gamePrefs.getDailyStatus(num);
        if (status == GamePreferences.STATUS_LOCKED) return;

        // Played today — show locked countdown dialog
        if (status == GamePreferences.STATUS_COMPLETED
                && gamePrefs.isDailyPlayedToday(num)) {
            showLockedTodayDialog(num);
            return;
        }

        showDailyInfoDialog(num);
    }

    private void showLockedTodayDialog(int num) {
        long msLeft = gamePrefs.getMsUntilNextDay();
        String countdown = GamePreferences.formatCountdown(msLeft);

        new MaterialAlertDialogBuilder(this, R.style.PuzzleVerseDialog)
                .setTitle(R.string.daily_already_completed_title)
                .setMessage(getString(R.string.daily_already_completed_msg, num, countdown))
                .setPositiveButton(R.string.ok, null)
                .setNegativeButton(R.string.daily_replay_no_coins,
                        (d, w) -> launchDaily(num))
                .show();
    }

    private void showDailyInfoDialog(int num) {
        View dv = LayoutInflater.from(this)
                .inflate(R.layout.dialog_daily_info, null);
        ((TextView) dv.findViewById(R.id.tv_daily_num))
                .setText(getString(R.string.daily_challenge_format, num));

        new MaterialAlertDialogBuilder(this, R.style.PuzzleVerseDialog)
                .setView(dv)
                .setPositiveButton(getString(R.string.daily_start_puzzle),
                        (d, w) -> launchDaily(num))
                .setNegativeButton(getString(R.string.cancel), null)
                .show();
    }

    private void launchDaily(int num) {
        Intent intent = new Intent(this, PuzzleActivity.class);
        intent.putExtra(PuzzleActivity.EXTRA_LEVEL_NUMBER, num);
        intent.putExtra(PuzzleActivity.EXTRA_IS_DAILY, true);
        startActivity(intent);
        overridePendingTransition(
                android.R.anim.fade_in,
                android.R.anim.fade_out);
    }

    // ── Adapter ───────────────────────────────────────────────────────
    private class DailyAdapter
            extends RecyclerView.Adapter<DailyAdapter.DailyVH> {

        @NonNull
        @Override
        public DailyVH onCreateViewHolder(@NonNull ViewGroup parent, int t) {
            View v = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_daily_card, parent, false);
            return new DailyVH(v);
        }

        @Override
        public void onBindViewHolder(@NonNull DailyVH h, int pos) {
            int num    = pos + 1;
            int status = gamePrefs.getDailyStatus(num);
            boolean playedToday = gamePrefs.isDailyPlayedToday(num);
            h.bind(num, status, playedToday);
        }

        @Override public int getItemCount() {
            return GamePreferences.TOTAL_DAILY_LEVELS;
        }

        class DailyVH extends RecyclerView.ViewHolder {
            View     cardRoot;
            TextView tvBadge, tvNumber, tvStatus,
                    tvCountdown;

            DailyVH(View v) {
                super(v);
                cardRoot    = v.findViewById(R.id.card_daily_root);
                tvBadge     = v.findViewById(R.id.tv_daily_badge);
                tvNumber    = v.findViewById(R.id.tv_daily_number);
                tvStatus    = v.findViewById(R.id.tv_daily_status);
                tvCountdown = v.findViewById(R.id.tv_daily_countdown);
            }

            void bind(int num, int status, boolean playedToday) {
                tvNumber.setText(String.valueOf(num));

                int bg;
                int numColor;
                int badge;
                int statusText;
                int statusColor;
                float alpha;
                int countdownVisibility = View.GONE;

                if (status == GamePreferences.STATUS_LOCKED) {
                    bg = R.drawable.bg_level_locked;
                    numColor = R.color.text_hint;
                    badge = R.string.emoji_lock;
                    statusText = R.string.locked;
                    statusColor = R.color.text_hint;
                    alpha = 0.5f;
                    itemView.setOnClickListener(null);
                } else if (status == GamePreferences.STATUS_COMPLETED && playedToday) {
                    bg = R.drawable.bg_daily_completed;
                    numColor = R.color.text_secondary;
                    badge = R.string.emoji_check;
                    statusText = R.string.done_today;
                    statusColor = R.color.accent_secondary;
                    alpha = 0.75f;
                    countdownVisibility = View.VISIBLE;
                    tvCountdown.setText(GamePreferences.formatCountdown(gamePrefs.getMsUntilNextDay()));
                    itemView.setOnClickListener(v -> onDailyClicked(num));
                } else if (status == GamePreferences.STATUS_COMPLETED) {
                    bg = R.drawable.bg_daily_completed;
                    numColor = R.color.accent_gold;
                    badge = R.string.emoji_star;
                    statusText = R.string.completed;
                    statusColor = R.color.accent_gold;
                    alpha = 1.0f;
                    itemView.setOnClickListener(v -> onDailyClicked(num));
                } else {
                    bg = R.drawable.bg_daily_unlocked;
                    numColor = R.color.text_primary;
                    badge = R.string.emoji_calendar;
                    statusText = R.string.play;
                    statusColor = R.color.accent_primary;
                    alpha = 1.0f;
                    itemView.setOnClickListener(v -> onDailyClicked(num));
                }

                cardRoot.setBackgroundResource(bg);
                tvNumber.setTextColor(ContextCompat.getColor(DailyChallengeActivity.this, numColor));
                tvBadge.setText(badge);
                tvStatus.setText(statusText);
                tvStatus.setTextColor(ContextCompat.getColor(DailyChallengeActivity.this, statusColor));
                tvCountdown.setVisibility(countdownVisibility);
                itemView.setAlpha(alpha);
            }
        }
    }
}