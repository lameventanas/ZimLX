/*
 * Copyright (C) 2008 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.android.launcher3;

import android.appwidget.AppWidgetHostView;
import android.content.ComponentName;
import android.content.Context;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.graphics.Point;
import android.graphics.PointF;
import android.graphics.Rect;
import android.util.DisplayMetrics;
import android.view.Surface;
import android.view.WindowManager;

import com.android.launcher3.CellLayout.ContainerType;
import com.android.launcher3.badge.BadgeRenderer;
import com.android.launcher3.graphics.IconNormalizer;

import org.zimmob.zimlx.ZimPreferences;

import java.util.ArrayList;

public class DeviceProfile implements ZimPreferences.OnPreferenceChangeListener {

    public final InvariantDeviceProfile inv;

    // Device properties
    public final boolean isTablet;
    public final boolean isLargeTablet;
    public final boolean isPhone;
    public final boolean transposeLayoutWithOrientation;
    public boolean isTallDevice;

    // Device properties in current orientation
    public final boolean isLandscape;
    public final boolean isMultiWindowMode;

    public final int widthPx;
    public final int heightPx;
    public final int availableWidthPx;
    public final int availableHeightPx;
    /**
     * The maximum amount of left/right workspace padding as a percentage of the screen width.
     * To be clear, this means that up to 7% of the screen width can be used as left padding, and
     * 7% of the screen width can be used as right padding.
     */
    private static final float MAX_HORIZONTAL_PADDING_PERCENT = 0.14f;

    private static final float TALL_DEVICE_ASPECT_RATIO_THRESHOLD = 2.0f;

    // To evenly space the icons, increase the left/right margins for tablets in portrait mode.
    private static final int PORTRAIT_TABLET_LEFT_RIGHT_PADDING_MULTIPLIER = 4;

    // Workspace
    public final int desiredWorkspaceLeftRightMarginPx;
    public int cellLayoutPaddingLeftRightPx;
    public final int cellLayoutBottomPaddingPx;
    public final int edgeMarginPx;
    public final Rect defaultWidgetPadding;
    public final int defaultPageSpacingPx;
    private final int topWorkspacePadding;
    public float workspaceSpringLoadShrinkFactor;
    public final int workspaceSpringLoadedBottomSpace;

    // Drag handle
    public int verticalDragHandleSizePx;
    private final int verticalDragHandleOverlapWorkspace;

    // Workspace icons
    public int iconSizePx;
    public int iconSizeOriginalPx;
    public int iconTextSizePx;
    public int iconDrawablePaddingPx;
    public int iconDrawablePaddingOriginalPx;

    public int cellWidthPx;
    public int cellHeightPx;
    public int workspaceCellPaddingXPx;
    private final int mBottomMarginHw;

    // Folder
    public int folderIconSizePx;
    public int folderIconOffsetYPx;

    // Folder cell
    public int folderCellWidthPx;
    public int folderCellHeightPx;

    // Folder child
    public int folderChildIconSizePx;
    public int folderChildTextSizePx;
    public int folderChildDrawablePaddingPx;

    // Hotseat
    public int hotseatCellHeightPx;
    // In portrait: size = height, in landscape: size = width
    public int hotseatBarSizePx;
    // Start is the side next to the nav bar, end is the side next to the workspace
    public int hotseatBarSidePaddingStartPx;
    public int hotseatBarSidePaddingEndPx;
    public int hotseatBarTopPaddingPx;
    public int hotseatBarBottomPaddingPx;

    // All apps
    public int allAppsCellHeightPx;
    public int allAppsIconSizePx;
    public int allAppsButtonVisualSize;
    public int allAppsIconDrawablePaddingPx;
    public float allAppsIconTextSizePx;
    public int allAppsNumCols;
    public int allAppsNumPredictiveCols;

    // Widgets
    public final PointF appWidgetScale = new PointF(1.0f, 1.0f);

    // Drop Target
    public int dropTargetBarSizePx;

    // Insets
    private final Rect mInsets = new Rect();
    public final Rect workspacePadding = new Rect();
    private final Rect mHotseatPadding = new Rect();
    private boolean mIsSeascape;

    // Icon badges
    public BadgeRenderer mBadgeRenderer;
    private Context mContext;

    // Listeners
    private ArrayList<LauncherLayoutChangeListener> mListeners = new ArrayList<>();


    public DeviceProfile(Context context, InvariantDeviceProfile inv,
                         Point minSize, Point maxSize,
                         int width, int height, boolean isLandscape, boolean isMultiWindowMode) {

        mContext = context;

        this.inv = inv;
        this.isLandscape = isLandscape;
        this.isMultiWindowMode = isMultiWindowMode;

        // Determine sizes.
        widthPx = width;
        heightPx = height;
        if (isLandscape) {
            availableWidthPx = maxSize.x;
            availableHeightPx = minSize.y;
        } else {
            availableWidthPx = minSize.x;
            availableHeightPx = maxSize.y;
        }

        Resources res = context.getResources();
        DisplayMetrics dm = res.getDisplayMetrics();

        // Constants from resources
        isTablet = res.getBoolean(R.bool.is_tablet);
        isLargeTablet = res.getBoolean(R.bool.is_large_tablet);
        isPhone = !isTablet && !isLargeTablet;
        float aspectRatio = ((float) Math.max(widthPx, heightPx)) / Math.min(widthPx, heightPx);
        boolean isTallDevice = Float.compare(aspectRatio, TALL_DEVICE_ASPECT_RATIO_THRESHOLD) >= 0;

        // Some more constants
        transposeLayoutWithOrientation =
                res.getBoolean(R.bool.hotseat_transpose_layout_with_orientation);

        context = getContext(context, isVerticalBarLayout()
                ? Configuration.ORIENTATION_LANDSCAPE
                : Configuration.ORIENTATION_PORTRAIT);
        res = context.getResources();


        ComponentName cn = new ComponentName(context.getPackageName(),
                this.getClass().getName());
        defaultWidgetPadding = AppWidgetHostView.getDefaultPaddingForWidget(context, cn, null);
        edgeMarginPx = res.getDimensionPixelSize(R.dimen.dynamic_grid_edge_margin);
        desiredWorkspaceLeftRightMarginPx = isVerticalBarLayout() ? 0 : edgeMarginPx;

        int cellLayoutPaddingLeftRightMultiplier = !isVerticalBarLayout() && isTablet
                ? PORTRAIT_TABLET_LEFT_RIGHT_PADDING_MULTIPLIER : 1;
        cellLayoutPaddingLeftRightPx = cellLayoutPaddingLeftRightMultiplier *
                res.getDimensionPixelSize(R.dimen.dynamic_grid_cell_layout_padding);
        cellLayoutBottomPaddingPx =
                res.getDimensionPixelSize(R.dimen.dynamic_grid_cell_layout_bottom_padding);

        verticalDragHandleSizePx = res.getDimensionPixelSize(
                R.dimen.vertical_drag_handle_size);
        verticalDragHandleOverlapWorkspace =
                res.getDimensionPixelSize(R.dimen.vertical_drag_handle_overlap_workspace);
        defaultPageSpacingPx =
                res.getDimensionPixelSize(R.dimen.dynamic_grid_workspace_page_spacing);
        topWorkspacePadding =
                res.getDimensionPixelSize(R.dimen.dynamic_grid_workspace_top_padding);
        iconDrawablePaddingOriginalPx =
                res.getDimensionPixelSize(R.dimen.dynamic_grid_icon_drawable_padding);
        dropTargetBarSizePx = res.getDimensionPixelSize(R.dimen.dynamic_grid_drop_target_size);
        workspaceSpringLoadedBottomSpace =
                res.getDimensionPixelSize(R.dimen.dynamic_grid_min_spring_loaded_space);

        workspaceCellPaddingXPx = res.getDimensionPixelSize(R.dimen.dynamic_grid_cell_padding_x);

        hotseatBarTopPaddingPx =
                res.getDimensionPixelSize(R.dimen.dynamic_grid_hotseat_top_padding);
        hotseatBarBottomPaddingPx = (isTallDevice ? 0
                : res.getDimensionPixelSize(R.dimen.dynamic_grid_hotseat_bottom_non_tall_padding))
                + res.getDimensionPixelSize(R.dimen.dynamic_grid_hotseat_bottom_padding);
        hotseatBarSidePaddingEndPx =
                res.getDimensionPixelSize(R.dimen.dynamic_grid_hotseat_side_padding);
        // Add a bit of space between nav bar and hotseat in multi-window vertical bar layout.
        hotseatBarSidePaddingStartPx = isMultiWindowMode && isVerticalBarLayout()
                ? edgeMarginPx : 0;
        hotseatBarSizePx = isVerticalBarLayout()
                ? Utilities.pxFromDp(inv.iconSize, dm) + hotseatBarSidePaddingStartPx
                + hotseatBarSidePaddingEndPx
                : res.getDimensionPixelSize(R.dimen.dynamic_grid_hotseat_size)
                + hotseatBarTopPaddingPx + hotseatBarBottomPaddingPx;

        mBottomMarginHw = 0; // res.getDimensionPixelSize(R.dimen.qsb_hotseat_bottom_margin_hw); (For now)
        if (!isVerticalBarLayout()) {
            hotseatBarSizePx += mBottomMarginHw;
            hotseatBarBottomPaddingPx += mBottomMarginHw;
        }

        // Calculate all of the remaining variables.
        updateAvailableDimensions(dm, res);

        // Now that we have all of the variables calculated, we can tune certain sizes.
        if (!isVerticalBarLayout() && isPhone && isTallDevice) {
            // We increase the hotseat size when there is extra space.
            // ie. For a display with a large aspect ratio, we can keep the icons on the workspace
            // in portrait mode closer together by adding more height to the hotseat.
            // Note: This calculation was created after noticing a pattern in the design spec.
            int extraSpace = getCellSize().y - iconSizePx - iconDrawablePaddingPx * 2
                    - verticalDragHandleSizePx;
            hotseatBarSizePx += extraSpace;
            hotseatBarBottomPaddingPx += extraSpace;

            // Recalculate the available dimensions using the new hotseat size.
            updateAvailableDimensions(dm, res);
        }
        updateWorkspacePadding();

        // This is done last, after iconSizePx is calculated above.
        mBadgeRenderer = new BadgeRenderer(mContext, iconSizePx);

        Utilities.getZimPrefs(context)
                .addOnPreferenceChangeListener(this, "pref_fullWidthWidgets", "pref_dockSearchBar",
                        "pref_twoRowDock", "pref_compactDock", "pref_allAppsPaddingScale", "pref_dockScale");
    }

    /**
     * All values which (even indirectly) depend on preferences have to be calculated here. This
     * removes the need to completely restart the launcher to apply changes. A simple recreate
     * should be enough to apply this.
     */
    @Override
    public void onValueChanged(String key, ZimPreferences prefs, boolean force) {
        Resources res = mContext.getResources();
        DisplayMetrics dm = res.getDisplayMetrics();

        boolean fullWidthWidgets = Utilities.getZimPrefs(mContext).getAllowFullWidthWidgets();
        boolean dockHidden = prefs.getDockHide();
        int dockRows = prefs.getDockRowsCount();
        float dockScale = prefs.getDockScale();

        cellLayoutPaddingLeftRightPx = (!isVerticalBarLayout() && fullWidthWidgets) ? 0
                : res.getDimensionPixelSize(R.dimen.dynamic_grid_cell_layout_padding);

        float aspectRatio = ((float) Math.max(widthPx, heightPx)) / Math.min(widthPx, heightPx);
        isTallDevice = Float.compare(aspectRatio, TALL_DEVICE_ASPECT_RATIO_THRESHOLD) >= 0;

        hotseatBarTopPaddingPx =
                res.getDimensionPixelSize(
                        R.dimen.v1_dynamic_grid_hotseat_top_padding);
        hotseatBarBottomPaddingPx = (isTallDevice ? 0
                : res.getDimensionPixelSize(R.dimen.dynamic_grid_hotseat_bottom_non_tall_padding))
                + res.getDimensionPixelSize(
                R.dimen.v1_dynamic_grid_hotseat_bottom_padding);
        hotseatBarSidePaddingEndPx =
                res.getDimensionPixelSize(R.dimen.dynamic_grid_hotseat_side_padding);
        // Add a bit of space between nav bar and hotseat in multi-window vertical bar layout.
        hotseatBarSidePaddingStartPx = isMultiWindowMode && isVerticalBarLayout()
                ? edgeMarginPx : 0;
        hotseatBarSizePx = isVerticalBarLayout()
                ? iconSizePx + hotseatBarSidePaddingStartPx
                + hotseatBarSidePaddingEndPx
                : res.getDimensionPixelSize(
                R.dimen.v1_dynamic_grid_hotseat_size) * dockRows
                + hotseatBarTopPaddingPx + hotseatBarBottomPaddingPx;
        verticalDragHandleSizePx = res.getDimensionPixelSize(
                R.dimen.vertical_drag_handle_size);

        // Calculate all of the remaining variables.
        updateAvailableDimensions(dm, res);

        // Now that we have all of the variables calculated, we can tune certain sizes.
        /*if (!isVerticalBarLayout() && isPhone && isTallDevice) {
            // We increase the hotseat size when there is extra space.
            // ie. For a display with a large aspect ratio, we can keep the icons on the workspace
            // in portrait mode closer together by adding more height to the hotseat.
            // Note: This calculation was created after noticing a pattern in the design spec.
            int extraSpace = (int) ((getCellSizeOriginal().y - iconSizeOriginalPx
                    - iconDrawablePaddingOriginalPx * 2 - verticalDragHandleSizePx) * dockScale);
            hotseatBarSizePx += extraSpace;
            hotseatBarBottomPaddingPx += extraSpace / 2;
            hotseatBarTopPaddingPx += extraSpace / 2;


            // Recalculate the available dimensions using the new hotseat size.
            updateAvailableDimensions(dm, res);
        }*/

        if (dockHidden) {
            hotseatBarSizePx = 0;
            verticalDragHandleSizePx = 0;

            updateAvailableDimensions(dm, res);
        } else if (!isVerticalBarLayout()) {
            verticalDragHandleSizePx *= dockScale;
            int bottomPaddingNew = Math.max((int) (hotseatBarBottomPaddingPx * dockScale), 0);
            if (prefs.getDockShouldUseCustomOpacity() || prefs.getDockShouldUseExtractedColors()) {
                hotseatBarTopPaddingPx *= dockScale;
                hotseatBarBottomPaddingPx = bottomPaddingNew;
            } else {
                int difference = hotseatBarBottomPaddingPx - bottomPaddingNew;
                hotseatBarTopPaddingPx -= difference;
                hotseatBarBottomPaddingPx = bottomPaddingNew;
            }

            // TODO: Fix icon cut off on smaller devices with lower values
            int minHeight = hotseatCellHeightPx * dockRows + hotseatBarBottomPaddingPx + hotseatBarTopPaddingPx;
            hotseatBarSizePx = Math.max(minHeight, (int) (hotseatBarSizePx * dockScale));
        }

        updateWorkspacePadding();

        // This is done last, after iconSizePx is calculated above.
        mBadgeRenderer = new BadgeRenderer(mContext, iconSizePx);
    }

    public DeviceProfile copy(Context context) {
        Point size = new Point(availableWidthPx, availableHeightPx);
        return new DeviceProfile(context, inv, size, size, widthPx, heightPx, isLandscape,
                isMultiWindowMode);
    }

    public DeviceProfile getMultiWindowProfile(Context context, Point mwSize) {
        // We take the minimum sizes of this profile and it's multi-window variant to ensure that
        // the system decor is always excluded.
        mwSize.set(Math.min(availableWidthPx, mwSize.x), Math.min(availableHeightPx, mwSize.y));

        // In multi-window mode, we can have widthPx = availableWidthPx
        // and heightPx = availableHeightPx because Launcher uses the InvariantDeviceProfiles'
        // widthPx and heightPx values where it's needed.
        DeviceProfile profile = new DeviceProfile(context, inv, mwSize, mwSize, mwSize.x, mwSize.y,
                isLandscape, true);

        // If there isn't enough vertical cell padding with the labels displayed, hide the labels.
        float workspaceCellPaddingY = profile.getCellSize().y - profile.iconSizePx
                - iconDrawablePaddingPx - profile.iconTextSizePx;
        if (workspaceCellPaddingY < profile.iconDrawablePaddingPx * 2) {
            profile.adjustToHideWorkspaceLabels();
        }

        // We use these scales to measure and layout the widgets using their full invariant profile
        // sizes and then draw them scaled and centered to fit in their multi-window mode cellspans.
        float appWidgetScaleX = (float) profile.getCellSize().x / getCellSize().x;
        float appWidgetScaleY = (float) profile.getCellSize().y / getCellSize().y;
        profile.appWidgetScale.set(appWidgetScaleX, appWidgetScaleY);
        profile.updateWorkspacePadding();

        return profile;
    }

    /**
     * Inverse of {@link #getMultiWindowProfile(Context, Point)}
     *
     * @return device profile corresponding to the current orientation in non multi-window mode.
     */
    public DeviceProfile getFullScreenProfile() {
        return isLandscape ? inv.landscapeProfile : inv.portraitProfile;
    }

    public void addLauncherLayoutChangedListener(LauncherLayoutChangeListener listener) {
        if (!mListeners.contains(listener)) {
            mListeners.add(listener);
        }
    }

    public void removeLauncherLayoutChangedListener(LauncherLayoutChangeListener listener) {
        mListeners.remove(listener);
    }

    /**
     * Adjusts the profile so that the labels on the Workspace are hidden.
     * It is important to call this method after the All Apps variables have been set.
     */
    private void adjustToHideWorkspaceLabels() {
        iconTextSizePx = 0;
        iconDrawablePaddingPx = 0;
        cellHeightPx = iconSizePx;

        // In normal cases, All Apps cell height should equal the Workspace cell height.
        // Since we are removing labels from the Workspace, we need to manually compute the
        // All Apps cell height.
        int topBottomPadding = allAppsIconDrawablePaddingPx * (isVerticalBarLayout() ? 2 : 1);
        allAppsCellHeightPx = allAppsIconSizePx + allAppsIconDrawablePaddingPx
                + Utilities.calculateTextHeight(allAppsIconTextSizePx)
                + topBottomPadding * 2;
    }

    /**
     * Determine the exact visual footprint of the all apps button, taking into account scaling
     * and internal padding of the drawable.
     */
    private void computeAllAppsButtonSize(Context context) {
        Resources res = context.getResources();
        float padding = res.getInteger(R.integer.config_allAppsButtonPaddingPercent) / 100f;
        allAppsButtonVisualSize = (int) (iconSizePx * (1 - padding)) - context.getResources()
                .getDimensionPixelSize(R.dimen.all_apps_button_scale_down);
    }


    private void updateAvailableDimensions(DisplayMetrics dm, Resources res) {
        updateIconSize(1f, 1f, res, dm);
        float workspaceScale = 1f;
        float allAppsScale = 1f;
        // Check to see if the icons fit within the available height.  If not, then scale down.
        float usedHeight = (cellHeightPx * inv.numRows);
        int maxHeight = (availableHeightPx - getTotalWorkspacePadding().y);
        if (usedHeight > maxHeight) {
            updateIconSize(workspaceScale, allAppsScale, res, dm);
        }
        updateAvailableFolderCellDimensions(dm, res);
    }

    private void updateIconSize(float workspaceScale, float allAppsScale, Resources res, DisplayMetrics dm) {
        ZimPreferences prefs = Utilities.getZimPrefs(mContext);
        boolean dockVisible = !prefs.getDockHide();
        int labelRowCount = prefs.getHomeLabelRows();
        int drawerLabelRowCount = prefs.getDrawerLabelRows();

        // Workspace
        final boolean isVerticalLayout = isVerticalBarLayout();
        float invIconSizePx = isVerticalLayout ? inv.landscapeIconSize : inv.iconSize;
        iconSizeOriginalPx = Utilities.pxFromDp(invIconSizePx, dm);
        iconSizePx = (int) (Utilities.pxFromDp(invIconSizePx, dm) * workspaceScale);
        iconSizePx = (int) (iconSizeOriginalPx * workspaceScale);
        iconTextSizePx = (int) (Utilities.pxFromSp(inv.iconTextSize, dm) * workspaceScale);
        iconDrawablePaddingPx = (int) (iconDrawablePaddingOriginalPx * workspaceScale);
        int textHeight = Utilities.calculateTextHeight(iconTextSizePx) * labelRowCount;
        cellHeightPx = iconSizePx + iconDrawablePaddingPx + textHeight;
        int cellYPadding = (getCellSize().y - cellHeightPx) / 2;
        if (iconDrawablePaddingPx > cellYPadding && !isVerticalLayout
                && !isMultiWindowMode) {
            // Ensures that the label is closer to its corresponding icon. This is not an issue
            // with vertical bar layout or multi-window mode since the issue is handled separately
            // with their calls to {@link #adjustToHideWorkspaceLabels}.
            cellHeightPx -= (iconDrawablePaddingPx - cellYPadding);
            iconDrawablePaddingPx = cellYPadding;
        }
        cellWidthPx = iconSizePx + iconDrawablePaddingPx;

        // All apps
        allAppsIconTextSizePx = iconTextSizePx;
        textHeight = Utilities.calculateTextHeight(iconTextSizePx) * drawerLabelRowCount;
        allAppsIconSizePx = (int) (Utilities.pxFromDp(inv.allAppsIconSize, dm) * allAppsScale);
        allAppsIconDrawablePaddingPx = (int) (iconDrawablePaddingOriginalPx * workspaceScale);
        int additionalPadding = (int) (
                res.getDimensionPixelSize(R.dimen.dynamic_grid_drawer_additional_padding) * prefs
                        .getDrawerPaddingScale());
        allAppsCellHeightPx = allAppsIconSizePx + allAppsIconDrawablePaddingPx + textHeight
                + additionalPadding;

        allAppsCellHeightPx = getCellSize().y;

        if (isVerticalLayout) {
            // Always hide the Workspace text with vertical bar layout.
            adjustToHideWorkspaceLabels();
        }

        // Hotseat
        if (isVerticalLayout) {
            hotseatBarSizePx =
                    iconSizePx * prefs.getDockRowsCount()
                            + hotseatBarSidePaddingStartPx + hotseatBarSidePaddingEndPx;
        }
        hotseatCellHeightPx = iconSizePx;

        if (!isVerticalLayout) {
            int expectedWorkspaceHeight = availableHeightPx - (dockVisible ? hotseatBarSizePx : 0)
                    - verticalDragHandleSizePx - topWorkspacePadding;
            float minRequiredHeight = dropTargetBarSizePx + workspaceSpringLoadedBottomSpace;
            workspaceSpringLoadShrinkFactor = Math.min(
                    res.getInteger(R.integer.config_workspaceSpringLoadShrinkPercentage) / 100.0f,
                    1 - (minRequiredHeight / expectedWorkspaceHeight));
        } else {
            workspaceSpringLoadShrinkFactor =
                    res.getInteger(R.integer.config_workspaceSpringLoadShrinkPercentage) / 100.0f;
        }

        // Folder icon
        folderIconSizePx = IconNormalizer.getNormalizedCircleSize(iconSizePx);
        folderIconOffsetYPx = (iconSizePx - folderIconSizePx) / 2;
    }

    private void updateAvailableFolderCellDimensions(DisplayMetrics dm, Resources res) {
        int folderBottomPanelSize = res.getDimensionPixelSize(R.dimen.folder_label_padding_top)
                + res.getDimensionPixelSize(R.dimen.folder_label_padding_bottom)
                + Utilities.calculateTextHeight(res.getDimension(R.dimen.folder_label_text_size));

        updateFolderCellSize(1f, dm, res);

        // Don't let the folder get too close to the edges of the screen.
        int folderMargin = edgeMarginPx;
        Point totalWorkspacePadding = getTotalWorkspacePadding();

        // Check if the icons fit within the available height.
        float usedHeight = folderCellHeightPx * inv.numFolderRows + folderBottomPanelSize;
        int maxHeight = availableHeightPx - totalWorkspacePadding.y - folderMargin;
        float scaleY = maxHeight / usedHeight;

        // Check if the icons fit within the available width.
        float usedWidth = folderCellWidthPx * inv.numFolderColumns;
        int maxWidth = availableWidthPx - totalWorkspacePadding.x - folderMargin;
        float scaleX = maxWidth / usedWidth;

        float scale = Math.min(scaleX, scaleY);
        if (scale < 1f) {
            updateFolderCellSize(scale, dm, res);
        }
    }

    private void updateFolderCellSize(float scale, DisplayMetrics dm, Resources res) {
        folderChildIconSizePx = (int) (Utilities.pxFromDp(inv.iconSize, dm) * scale);
        folderChildTextSizePx =
                (int) (res.getDimensionPixelSize(R.dimen.folder_child_text_size) * scale);

        int textHeight = Utilities.calculateTextHeight(folderChildTextSizePx);
        int cellPaddingX = (int) (res.getDimensionPixelSize(R.dimen.folder_cell_x_padding) * scale);
        int cellPaddingY = (int) (res.getDimensionPixelSize(R.dimen.folder_cell_y_padding) * scale);

        folderCellWidthPx = folderChildIconSizePx + 2 * cellPaddingX;
        folderCellHeightPx = folderChildIconSizePx + 2 * cellPaddingY + textHeight;
        folderChildDrawablePaddingPx = Math.max(0,
                (folderCellHeightPx - folderChildIconSizePx - textHeight) / 3);
    }

    public void updateInsets(Rect insets) {
        mInsets.set(insets);
        updateWorkspacePadding();
    }

    public Rect getInsets() {
        return mInsets;
    }

    public void updateAppsViewNumCols() {
        allAppsNumCols = allAppsNumPredictiveCols = inv.numColumns;
    }

    private int getCurrentWidth() {
        return isLandscape
                ? Math.max(widthPx, heightPx)
                : Math.min(widthPx, heightPx);
    }

    private int getCurrentHeight() {
        return isLandscape
                ? Math.min(widthPx, heightPx)
                : Math.max(widthPx, heightPx);
    }

    public Point getCellSize() {
        Point result = new Point();
        // Since we are only concerned with the overall padding, layout direction does
        // not matter.
        Point padding = getTotalWorkspacePadding();
        result.x = calculateCellWidth(availableWidthPx - padding.x
                - cellLayoutPaddingLeftRightPx * 2, inv.numColumns);
        result.y = calculateCellHeight(availableHeightPx - padding.y
                - cellLayoutBottomPaddingPx, inv.numRows);
        return result;
    }

    public Point getCellSizeOriginal() {
        Point result = new Point();
        // Since we are only concerned with the overall padding, layout direction does
        // not matter.
        Point padding = getTotalWorkspacePadding();
        result.x = calculateCellWidth(availableWidthPx - padding.x
                - cellLayoutPaddingLeftRightPx * 2, inv.numColumnsOriginal);
        result.y = calculateCellHeight(availableHeightPx - padding.y
                - cellLayoutBottomPaddingPx, inv.numRowsOriginal);
        return result;
    }
    public Point getTotalWorkspacePadding() {
        updateWorkspacePadding();
        return new Point(workspacePadding.left + workspacePadding.right,
                workspacePadding.top + workspacePadding.bottom);
    }

    /**
     * Updates {@link #workspacePadding} as a result of any internal value change to reflect the
     * new workspace padding
     */
    private void updateWorkspacePadding() {
        boolean dockVisible = !Utilities.getZimPrefs(mContext).getDockHide();
        Rect padding = workspacePadding;
        if (isVerticalBarLayout()) {
            padding.top = 0;
            padding.bottom = edgeMarginPx;
            if (isSeascape()) {
                padding.left = dockVisible ? hotseatBarSizePx : 0;
                padding.right = verticalDragHandleSizePx;
            } else {
                padding.left = verticalDragHandleSizePx;
                padding.right = dockVisible ? hotseatBarSizePx : 0;
            }
        } else {
            int paddingBottom = (dockVisible ? hotseatBarSizePx : 0) + verticalDragHandleSizePx
                    - verticalDragHandleOverlapWorkspace;
            if (isTablet) {
                // Pad the left and right of the workspace to ensure consistent spacing
                // between all icons
                // The amount of screen space available for left/right padding.
                int availablePaddingX = Math.max(0, widthPx - ((inv.numColumns * cellWidthPx) +
                        ((inv.numColumns - 1) * cellWidthPx)));
                availablePaddingX = (int) Math.min(availablePaddingX,
                        widthPx * MAX_HORIZONTAL_PADDING_PERCENT);
                int availablePaddingY = Math.max(0, heightPx - topWorkspacePadding - paddingBottom
                        - (2 * inv.numRows * cellHeightPx) - hotseatBarTopPaddingPx
                        - hotseatBarBottomPaddingPx);
                padding.set(availablePaddingX / 2, topWorkspacePadding + availablePaddingY / 2,
                        availablePaddingX / 2, paddingBottom + availablePaddingY / 2);
            } else {
                // Pad the top and bottom of the workspace with search/hotseat bar sizes
                padding.set(desiredWorkspaceLeftRightMarginPx,
                        topWorkspacePadding,
                        desiredWorkspaceLeftRightMarginPx,
                        paddingBottom);
            }
        }
    }

    public Rect getHotseatLayoutPadding() {
        if (isVerticalBarLayout()) {
            if (isSeascape()) {
                mHotseatPadding.set(mInsets.left + hotseatBarSidePaddingStartPx,
                        mInsets.top, hotseatBarSidePaddingEndPx, mInsets.bottom);
            } else {
                mHotseatPadding.set(hotseatBarSidePaddingEndPx, mInsets.top,
                        mInsets.right + hotseatBarSidePaddingStartPx, mInsets.bottom);
            }
        } else {

            // We want the edges of the hotseat to line up with the edges of the workspace, but the
            // icons in the hotseat are a different size, and so don't line up perfectly. To account
            // for this, we pad the left and right of the hotseat with half of the difference of a
            // workspace cell vs a hotseat cell.
            float workspaceCellWidth = (float) widthPx / inv.numColumns;
            float hotseatCellWidth = (float) widthPx / inv.numHotseatIcons;
            int hotseatAdjustment = Math.round((workspaceCellWidth - hotseatCellWidth) / 2);
            mHotseatPadding.set(
                    hotseatAdjustment + workspacePadding.left + cellLayoutPaddingLeftRightPx,
                    hotseatBarTopPaddingPx,
                    hotseatAdjustment + workspacePadding.right + cellLayoutPaddingLeftRightPx,
                    hotseatBarBottomPaddingPx + mInsets.bottom + cellLayoutBottomPaddingPx);
        }
        return mHotseatPadding;
    }

    /**
     * @return the bounds for which the open folders should be contained within
     */
    public Rect getAbsoluteOpenFolderBounds() {
        if (isVerticalBarLayout()) {
            // Folders should only appear right of the drop target bar and left of the hotseat
            return new Rect(mInsets.left + dropTargetBarSizePx + edgeMarginPx,
                    mInsets.top,
                    mInsets.left + availableWidthPx - hotseatBarSizePx - edgeMarginPx,
                    mInsets.top + availableHeightPx);
        } else {
            // Folders should only appear below the drop target bar and above the hotseat
            return new Rect(mInsets.left + edgeMarginPx,
                    mInsets.top + dropTargetBarSizePx + edgeMarginPx,
                    mInsets.left + availableWidthPx - edgeMarginPx,
                    mInsets.top + availableHeightPx - hotseatBarSizePx
                            - verticalDragHandleSizePx - edgeMarginPx);
        }
    }

    public static int calculateCellWidth(int width, int countX) {
        return width / countX;
    }

    public static int calculateCellHeight(int height, int countY) {
        return height / countY;
    }

    /**
     * When {@code true}, the device is in landscape mode and the hotseat is on the right column.
     * When {@code false}, either device is in portrait mode or the device is in landscape mode and
     * the hotseat is on the bottom row.
     */
    public boolean isVerticalBarLayout() {
        return isLandscape && transposeLayoutWithOrientation;
    }

    public void layout(Launcher launcher, boolean notifyListeners) {
        if (notifyListeners) {
            for (int i = mListeners.size() - 1; i >= 0; i--) {
                mListeners.get(i).onLauncherLayoutChanged();
            }
        }
    }

    /**
     * Updates orientation information and returns true if it has changed from the previous value.
     */
    public boolean updateIsSeascape(WindowManager wm) {
        if (isVerticalBarLayout()) {
            boolean isSeascape = wm.getDefaultDisplay().getRotation() == Surface.ROTATION_270;
            if (mIsSeascape != isSeascape) {
                mIsSeascape = isSeascape;
                return true;
            }
        }
        return false;
    }

    public boolean isSeascape() {
        return isVerticalBarLayout() && mIsSeascape;
    }

    public boolean shouldFadeAdjacentWorkspaceScreens() {
        return isVerticalBarLayout() || isLargeTablet;
    }

    public int getCellHeight(@ContainerType int containerType) {
        switch (containerType) {
            case CellLayout.WORKSPACE:
                return cellHeightPx;
            case CellLayout.FOLDER:
                return folderCellHeightPx;
            case CellLayout.HOTSEAT:
                return hotseatCellHeightPx;
            default:
                // ??
                return 0;
        }
    }

    private static Context getContext(Context c, int orientation) {
        Configuration context = new Configuration(c.getResources().getConfiguration());
        context.orientation = orientation;
        return c.createConfigurationContext(context);
    }

    /**
     * Callback when a component changes the DeviceProfile associated with it, as a result of
     * configuration change
     */
    public interface OnDeviceProfileChangeListener {

        /**
         * Called when the device profile is reassigned. Note that for layout and measurements, it
         * is sufficient to listen for inset changes. Use this callback when you need to perform
         * a one time operation.
         */
        void onDeviceProfileChanged(DeviceProfile dp);
    }

    public interface LauncherLayoutChangeListener {
        void onLauncherLayoutChanged();
    }
}