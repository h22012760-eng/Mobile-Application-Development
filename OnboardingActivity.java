package com.puzzleverse.game;

import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.animation.DecelerateInterpolator;
import android.view.animation.OvershootInterpolator;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import androidx.viewpager2.widget.ViewPager2;
import androidx.recyclerview.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.ViewGroup;
import java.util.ArrayList;
import java.util.List;

public class OnboardingActivity extends AppCompatActivity {

    private ViewPager2    viewPager;
    private LinearLayout  dotsContainer;
    private TextView      btnNext;
    private TextView      btnSkip;

    private List<OnboardPage> pages;
    private int currentPage = 0;

    static class OnboardPage {
        String emoji;
        String title;
        String description;
        String tip;
        int    bgColor;

        OnboardPage(String emoji, String title,
                    String description, String tip, int bgColor) {
            this.emoji       = emoji;
            this.title       = title;
            this.description = description;
            this.tip         = tip;
            this.bgColor     = bgColor;
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_onboarding);

        viewPager     = findViewById(R.id.onboard_pager);
        dotsContainer = findViewById(R.id.onboard_dots);
        btnNext       = findViewById(R.id.btn_onboard_next);
        btnSkip       = findViewById(R.id.btn_onboard_skip);

        buildPages();
        setupPager();
        setupDots();
        setupButtons();
    }

    private void buildPages() {
        pages = new ArrayList<>();

        pages.add(new OnboardPage(
                "🧩",
                "Welcome to PuzzleVerse!",
                "Arrange scrambled image pieces to reveal a beautiful picture. Every piece starts inside the grid — shuffled and waiting for you.",
                "💡 Tip: Look for obvious corners and edges first!",
                0xFF1A1A2E));

        pages.add(new OnboardPage(
                "👆",
                "Swap Pieces",
                "Drag any piece and drop it onto another piece to swap their positions. Pieces that are correctly placed next to each other automatically join into a group.",
                "💡 Tip: Swapping a group moves all connected pieces together!",
                0xFF16213E));

        pages.add(new OnboardPage(
                "⭐",
                "Earn Stars",
                "You start with 3 stars. Stars are lost as you use more moves. Complete the puzzle quickly to keep all 3 stars and show off your skill!",
                "💡 Tip: The fewer moves you use, the more stars you keep!",
                0xFF1A1A2E));

        pages.add(new OnboardPage(
                "👁",
                "Peek Feature",
                "Spend 15 coins to peek at the complete image for 3 seconds. Use it wisely when you are stuck and need a quick reference.",
                "💡 Tip: Save coins by solving easy levels without peeking!",
                0xFF16213E));

        pages.add(new OnboardPage(
                "👻",
                "Ghost Image",
                "Spend 30 coins to activate a faint ghost of the complete image permanently visible behind the pieces. Great for tricky puzzles!",
                "💡 Tip: Ghost stays on until you tap Hide Ghost!",
                0xFF1A1A2E));

        pages.add(new OnboardPage(
                "🔀",
                "Scramble Button",
                "Feeling stuck? Tap Scramble to reshuffle all unplaced pieces for free. Sometimes a fresh arrangement reveals new patterns.",
                "💡 Tip: Scramble does not affect pieces already in correct position!",
                0xFF16213E));

        pages.add(new OnboardPage(
                "📅",
                "Daily Challenges",
                "A new daily challenge unlocks every day with a tough 7×7 grid of 49 pieces. Complete it to earn 40 coins — the best reward in the game!",
                "💡 Tip: Each daily challenge can only be played once per day!",
                0xFF1A1A2E));

        pages.add(new OnboardPage(
                "🏆",
                "Ready to Play!",
                "You know everything you need. Complete levels, earn coins, unlock achievements, and become the ultimate PuzzleVerse master!",
                "🚀 Let\'s go — your first puzzle is waiting!",
                0xFF16213E));
    }

    private void setupPager() {
        viewPager.setAdapter(new OnboardAdapter());
        viewPager.registerOnPageChangeCallback(
                new ViewPager2.OnPageChangeCallback() {
                    @Override
                    public void onPageSelected(int position) {
                        currentPage = position;
                        updateDots(position);
                        updateButtons(position);
                    }
                });

        // Page transition animation
        viewPager.setPageTransformer((page, position) -> {
            float absPos = Math.abs(position);
            page.setAlpha(1f - absPos * 0.4f);
            page.setScaleX(1f - absPos * 0.1f);
            page.setScaleY(1f - absPos * 0.1f);
            page.setTranslationX(page.getWidth() * position * 0.05f);
        });
    }

    private void setupDots() {
        dotsContainer.removeAllViews();
        for (int i = 0; i < pages.size(); i++) {
            View dot = new View(this);
            int size = i == 0 ? 28 : 16;
            LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(
                    dp(size), dp(8));
            p.setMargins(dp(4), 0, dp(4), 0);
            dot.setLayoutParams(p);
            dot.setBackgroundResource(i == 0
                    ? R.drawable.dot_active
                    : R.drawable.dot_inactive);
            dotsContainer.addView(dot);
        }
    }

    private void updateDots(int selected) {
        for (int i = 0; i < dotsContainer.getChildCount(); i++) {
            View dot = dotsContainer.getChildAt(i);
            boolean isSelected = i == selected;
            int size = isSelected ? 28 : 16;
            LinearLayout.LayoutParams p =
                    (LinearLayout.LayoutParams) dot.getLayoutParams();
            p.width = dp(size);
            dot.setLayoutParams(p);
            dot.setBackgroundResource(isSelected
                    ? R.drawable.dot_active
                    : R.drawable.dot_inactive);
        }
    }

    private void updateButtons(int position) {
        boolean isLast = position == pages.size() - 1;
        btnNext.setText(isLast ? "Start Playing! 🚀" : "Next →");
        btnSkip.setVisibility(isLast ? View.INVISIBLE : View.VISIBLE);
    }

    private void setupButtons() {
        btnNext.setOnClickListener(v -> {
            if (currentPage < pages.size() - 1) {
                viewPager.setCurrentItem(currentPage + 1, true);
            } else {
                finishOnboarding();
            }
        });

        btnSkip.setOnClickListener(v -> finishOnboarding());
    }

    private void finishOnboarding() {
        // Mark tutorial as shown
        new GamePreferences(this);
        android.content.SharedPreferences prefs =
                getSharedPreferences("PuzzleVersePrefs",
                        MODE_PRIVATE);
        prefs.edit().putBoolean("tutorial_shown", true).apply();

        // Slide out animation
        View root = findViewById(R.id.onboard_root);
        root.animate().translationY(-root.getHeight()).alpha(0f)
                .setDuration(400)
                .setInterpolator(new DecelerateInterpolator(2f))
                .withEndAction(() -> {
                    startActivity(new Intent(
                            OnboardingActivity.this,
                            MainActivity.class));
                    finish();
                    overridePendingTransition(
                            android.R.anim.fade_in,
                            android.R.anim.fade_out);
                }).start();
    }

    private int dp(int dp) {
        return (int)(dp * getResources().getDisplayMetrics().density);
    }

    // ── Adapter ───────────────────────────────────────────────────────
    class OnboardAdapter extends RecyclerView.Adapter<OnboardAdapter.VH> {

        @Override
        public VH onCreateViewHolder(ViewGroup parent, int viewType) {
            View v = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_onboard_page, parent, false);
            return new VH(v);
        }

        @Override
        public void onBindViewHolder(VH h, int pos) {
            OnboardPage page = pages.get(pos);
            h.tvEmoji.setText(page.emoji);
            h.tvTitle.setText(page.title);
            h.tvDesc.setText(page.description);
            h.tvTip.setText(page.tip);
            h.root.setBackgroundColor(page.bgColor);

            // Animate content in
            h.tvEmoji.setAlpha(0f);
            h.tvEmoji.setTranslationY(30f);
            h.tvTitle.setAlpha(0f);
            h.tvTitle.setTranslationY(30f);
            h.tvDesc.setAlpha(0f);
            h.root.postDelayed(() -> {
                h.tvEmoji.animate().alpha(1f).translationY(0f)
                        .setDuration(400)
                        .setInterpolator(new OvershootInterpolator(1.2f))
                        .start();
                h.tvTitle.animate().alpha(1f).translationY(0f)
                        .setDuration(400).setStartDelay(100)
                        .setInterpolator(new DecelerateInterpolator())
                        .start();
                h.tvDesc.animate().alpha(1f)
                        .setDuration(400).setStartDelay(200)
                        .start();
            }, 50);
        }

        @Override public int getItemCount() { return pages.size(); }

        class VH extends RecyclerView.ViewHolder {
            View     root;
            TextView tvEmoji, tvTitle, tvDesc, tvTip;
            VH(View v) {
                super(v);
                root    = v.findViewById(R.id.onboard_page_root);
                tvEmoji = v.findViewById(R.id.tv_onboard_emoji);
                tvTitle = v.findViewById(R.id.tv_onboard_title);
                tvDesc  = v.findViewById(R.id.tv_onboard_desc);
                tvTip   = v.findViewById(R.id.tv_onboard_tip);
            }
        }
    }
}