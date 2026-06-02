package com.aiethicsquest.presentation.activity;

import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;
import androidx.navigation.NavController;
import androidx.navigation.NavDestination;
import androidx.navigation.fragment.NavHostFragment;
import androidx.navigation.ui.AppBarConfiguration;
import androidx.navigation.ui.NavigationUI;

import com.aiethicsquest.R;
import com.aiethicsquest.databinding.ActivityMainBinding;

import java.util.HashSet;
import java.util.Set;

/**
 * 应用主界面 Activity.
 *
 * <p>导航行为说明：</p>
 * <ul>
 *     <li>顶层页面（homeFragment / profileFragment）不显示返回箭头</li>
 *     <li>子页面（imageChallengeFragment / wrongBookFragment）显示返回箭头，点击可返回</li>
 *     <li>底部导航栏点击 Tab 时，始终弹出回对应顶层页面（不重新创建），保留子页面进度</li>
 *     <li>底部导航栏始终高亮"当前所在顶层页面"对应的 item</li>
 * </ul>
 */
public class MainActivity extends AppCompatActivity {

    private ActivityMainBinding binding;
    private NavController navController;

    /** 顶层页面 ID 集合，这些页面不显示返回箭头. */
    private Set<Integer> topLevelDestinations;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        setSupportActionBar(binding.toolbar);
        binding.navHostFragment.post(this::setupNavigation);
    }

    private void setupNavigation() {
        NavHostFragment navHostFragment = (NavHostFragment) getSupportFragmentManager()
                .findFragmentById(binding.navHostFragment.getId());
        if (navHostFragment == null) {
            throw new IllegalStateException("NavHostFragment 未找到，请检查布局文件中的 id 是否正确");
        }
        navController = navHostFragment.getNavController();

        // 顶层页面：这些页面不显示返回箭头
        topLevelDestinations = new HashSet<>();
        topLevelDestinations.add(R.id.homeFragment);
        topLevelDestinations.add(R.id.profileFragment);

        AppBarConfiguration appBarConfiguration = new AppBarConfiguration.Builder(
                topLevelDestinations
        ).build();

        NavigationUI.setupActionBarWithNavController(this, navController, appBarConfiguration);

        // 不使用 NavigationUI.setupWithNavController，手动接管底部导航点击，
        // 以便实现"点 Tab 弹出回顶层，保留子页面进度"的行为
        binding.bottomNav.setOnItemSelectedListener(item -> {
            int itemId = item.getItemId();
            NavDestination currentDest = navController.getCurrentDestination();
            if (currentDest == null) return false;

            int currentDestId = currentDest.getId();

            // 若已在目标顶层页面，不做任何导航（防止重建）
            if (currentDestId == itemId) {
                return true;
            }

            // 若在目标顶层页面的子页面（如 imageChallengeFragment 的父 Tab 是 homeFragment），
            // 则弹出回该顶层页面，不重新导航（保留返回栈中顶层页面的状态）
            if (itemId == R.id.homeFragment) {
                // 弹出回 homeFragment（inclusive=false 表示保留 homeFragment 不弹出）
                boolean popped = navController.popBackStack(R.id.homeFragment, false);
                if (!popped) {
                    // 返回栈中没有 homeFragment，说明当前在 profileFragment 侧，
                    // 正常导航过去
                    navController.navigate(R.id.homeFragment);
                }
                return true;
            }

            if (itemId == R.id.profileFragment) {
                boolean popped = navController.popBackStack(R.id.profileFragment, false);
                if (!popped) {
                    navController.navigate(R.id.profileFragment);
                }
                return true;
            }

            return false;
        });

        // 监听目的地变化，同步底部导航栏高亮
        navController.addOnDestinationChangedListener(
                (controller, destination, arguments) -> syncBottomNavHighlight(destination));
    }

    /**
     * 根据当前 destination 同步底部导航栏高亮状态.
     *
     * <p>当进入子页面时，底部导航栏高亮对应父顶层页面的 item。</p>
     *
     * @param destination 当前目的地
     */
    private void syncBottomNavHighlight(NavDestination destination) {
        if (destination == null) return;
        int destId = destination.getId();

        if (destId == R.id.homeFragment || destId == R.id.imageChallengeFragment) {
            binding.bottomNav.getMenu().findItem(R.id.homeFragment).setChecked(true);
        } else if (destId == R.id.profileFragment
                || destId == R.id.wrongBookFragment
                || destId == R.id.sessionHistoryFragment
                || destId == R.id.sessionDetailFragment) {
            binding.bottomNav.getMenu().findItem(R.id.profileFragment).setChecked(true);
        }
    }

    /**
     * 支持 Toolbar 返回箭头点击返回上一页.
     */
    @Override
    public boolean onSupportNavigateUp() {
        return navController != null && navController.navigateUp()
                || super.onSupportNavigateUp();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        binding = null;
    }
}

