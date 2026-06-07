package com.puzzleverse.game;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import java.util.List;

public class AchievementsActivity extends AppCompatActivity {

    private GamePreferences   gamePrefs;
    private AchievementManager manager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_achievements);

        gamePrefs = new GamePreferences(this);
        manager   = new AchievementManager(gamePrefs);

        TextView tvCoins    = findViewById(R.id.tv_coin_count);
        TextView tvUnlocked = findViewById(R.id.tv_achievements_unlocked);
        TextView tvCoinsSum = findViewById(R.id.tv_total_coins_earned);

        tvCoins.setText(String.valueOf(gamePrefs.getCoins()));

        List<Achievement> all      = manager.buildAll();
        int unlocked = 0;
        int totalReward = 0;
        for (Achievement a : all) {
            if (a.unlocked) { unlocked++; totalReward += a.coinReward; }
        }
        tvUnlocked.setText(unlocked + " / " + all.size() + " unlocked");
        tvCoinsSum.setText("Earned from achievements: "
                + totalReward + " 🪙");

        RecyclerView rv = findViewById(R.id.recycler_achievements);
        rv.setLayoutManager(new LinearLayoutManager(this));
        rv.setAdapter(new AchAdapter(all));

        findViewById(R.id.btn_back).setOnClickListener(v -> {
            finish();
            overridePendingTransition(
                    android.R.anim.fade_in, android.R.anim.fade_out);
        });
    }

    // ── Adapter ───────────────────────────────────────────────────────
    static class AchAdapter
            extends RecyclerView.Adapter<AchAdapter.VH> {

        private final List<Achievement> items;

        AchAdapter(List<Achievement> items) { this.items = items; }

        @Override
        public VH onCreateViewHolder(ViewGroup p, int t) {
            View v = LayoutInflater.from(p.getContext())
                    .inflate(R.layout.item_achievement, p, false);
            return new VH(v);
        }

        @Override
        public void onBindViewHolder(VH h, int pos) {
            h.bind(items.get(pos));
        }

        @Override public int getItemCount() { return items.size(); }

        static class VH extends RecyclerView.ViewHolder {
            View        root;
            TextView    tvEmoji, tvTitle, tvDesc, tvStatus, tvTier, tvReward;
            ProgressBar bar;

            VH(View v) {
                super(v);
                root     = v.findViewById(R.id.achievement_root);
                tvEmoji  = v.findViewById(R.id.tv_ach_emoji);
                tvTitle  = v.findViewById(R.id.tv_ach_title);
                tvDesc   = v.findViewById(R.id.tv_ach_desc);
                tvStatus = v.findViewById(R.id.tv_ach_status);
                tvTier   = v.findViewById(R.id.tv_ach_tier);
                tvReward = v.findViewById(R.id.tv_ach_reward);
                bar      = v.findViewById(R.id.ach_progress_bar);
            }

            void bind(Achievement a) {
                tvEmoji.setText(a.emoji);
                tvTitle.setText(a.title);
                tvDesc.setText(a.description);
                tvTier.setText(a.getTierLabel());
                tvReward.setText("+" + a.coinReward + " 🪙");

                bar.setMax(a.maxProgress);
                bar.setProgress(a.progress);

                if (a.unlocked) {
                    root.setBackgroundResource(
                            R.drawable.bg_achievement_unlocked);
                    tvTitle.setTextColor(
                            itemView.getContext().getColor(
                                    R.color.accent_secondary));
                    tvStatus.setText("✅ Unlocked");
                    tvStatus.setTextColor(
                            itemView.getContext().getColor(
                                    R.color.accent_secondary));
                    // Tier badge colour based on tier
                    setTierColor(a.tier);
                } else {
                    root.setBackgroundResource(
                            R.drawable.bg_achievement_locked);
                    tvTitle.setTextColor(
                            itemView.getContext().getColor(
                                    R.color.text_primary));
                    tvStatus.setText(a.progress + " / " + a.maxProgress);
                    tvStatus.setTextColor(
                            itemView.getContext().getColor(
                                    R.color.text_secondary));
                    setTierColor(a.tier);
                }
            }

            void setTierColor(int tier) {
                int color;
                switch (tier) {
                    case Achievement.TIER_BRONZE:
                        color = itemView.getContext().getColor(
                                R.color.tier_bronze); break;
                    case Achievement.TIER_SILVER:
                        color = itemView.getContext().getColor(
                                R.color.tier_silver); break;
                    case Achievement.TIER_GOLD:
                        color = itemView.getContext().getColor(
                                R.color.tier_gold); break;
                    default:
                        color = itemView.getContext().getColor(
                                R.color.tier_diamond); break;
                }
                tvTier.setTextColor(color);
                tvReward.setTextColor(color);
            }
        }
    }
}