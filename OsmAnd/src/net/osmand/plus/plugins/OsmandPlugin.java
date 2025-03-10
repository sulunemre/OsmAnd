package net.osmand.plus.plugins;


import android.annotation.TargetApi;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager.NameNotFoundException;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.view.View;

import androidx.annotation.DrawableRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.fragment.app.FragmentManager;

import net.osmand.IProgress;
import net.osmand.Location;
import net.osmand.PlatformUtil;
import net.osmand.data.Amenity;
import net.osmand.data.LatLon;
import net.osmand.data.MapObject;
import net.osmand.map.WorldRegion;
import net.osmand.plus.AppInitializer;
import net.osmand.plus.AppInitializer.AppInitializeListener;
import net.osmand.plus.AppInitializer.InitEvents;
import net.osmand.plus.widgets.ctxmenu.ContextMenuAdapter;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.Version;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.activities.TabActivity.TabItem;
import net.osmand.plus.api.SettingsAPI;
import net.osmand.plus.dashboard.tools.DashFragmentData;
import net.osmand.plus.download.CustomRegion;
import net.osmand.plus.download.DownloadActivityType;
import net.osmand.plus.download.DownloadResources;
import net.osmand.plus.download.IndexItem;
import net.osmand.plus.mapcontextmenu.MenuBuilder;
import net.osmand.plus.mapcontextmenu.MenuController;
import net.osmand.plus.mapcontextmenu.builders.cards.ImageCard.GetImageCardsTask.GetImageCardsListener;
import net.osmand.plus.mapcontextmenu.builders.cards.ImageCard.ImageCardsHolder;
import net.osmand.plus.myplaces.ui.FavoritesActivity;
import net.osmand.plus.plugins.accessibility.AccessibilityPlugin;
import net.osmand.plus.plugins.audionotes.AudioVideoNotesPlugin;
import net.osmand.plus.plugins.development.OsmandDevelopmentPlugin;
import net.osmand.plus.plugins.mapillary.MapillaryPlugin;
import net.osmand.plus.plugins.monitoring.OsmandMonitoringPlugin;
import net.osmand.plus.plugins.openplacereviews.OpenPlaceReviewsPlugin;
import net.osmand.plus.plugins.openseamaps.NauticalMapsPlugin;
import net.osmand.plus.plugins.osmedit.OsmEditingPlugin;
import net.osmand.plus.plugins.parking.ParkingPositionPlugin;
import net.osmand.plus.plugins.rastermaps.OsmandRasterMapsPlugin;
import net.osmand.plus.plugins.skimaps.SkiMapsPlugin;
import net.osmand.plus.plugins.srtm.SRTMPlugin;
import net.osmand.plus.poi.PoiUIFilter;
import net.osmand.plus.quickaction.QuickActionType;
import net.osmand.plus.search.QuickSearchDialogFragment;
import net.osmand.plus.settings.backend.ApplicationMode;
import net.osmand.plus.settings.backend.preferences.CommonPreference;
import net.osmand.plus.settings.backend.preferences.ListStringPreference;
import net.osmand.plus.settings.backend.preferences.OsmandPreference;
import net.osmand.plus.settings.fragments.BaseSettingsFragment.SettingsScreenType;
import net.osmand.plus.wikipedia.WikipediaPlugin;
import net.osmand.render.RenderingRuleProperty;
import net.osmand.render.RenderingRulesStorage;
import net.osmand.search.core.SearchPhrase;
import net.osmand.util.Algorithms;

import org.apache.commons.logging.Log;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

public abstract class OsmandPlugin {

	public static final String PLUGIN_ID_KEY = "plugin_id";

	private static final String PLUGINS_PREFERENCES_NAME = "net.osmand.plugins";
	private static final String CUSTOM_PLUGINS_KEY = "custom_plugins";

	private static final Log LOG = PlatformUtil.getLog(OsmandPlugin.class);

	private static final List<OsmandPlugin> allPlugins = new ArrayList<>();

	protected OsmandApplication app;

	protected List<OsmandPreference> pluginPreferences = new ArrayList<>();

	private boolean enabled;
	private String installURL = null;

	public OsmandPlugin(OsmandApplication app) {
		this.app = app;
	}

	public abstract String getId();

	public abstract String getName();

	public abstract CharSequence getDescription();

	@Nullable
	public Drawable getAssetResourceImage() {
		return null;
	}

	@DrawableRes
	public int getLogoResourceId() {
		return R.drawable.ic_extension_dark;
	}

	@NonNull
	public Drawable getLogoResource() {
		return app.getUIUtilities().getIcon(getLogoResourceId());
	}

	public SettingsScreenType getSettingsScreenType() {
		return null;
	}

	public List<OsmandPreference> getPreferences() {
		return pluginPreferences;
	}

	public String getPrefsDescription() {
		return null;
	}

	public int getVersion() {
		return -1;
	}

	/**
	 * Initialize plugin runs just after creation
	 */
	public boolean init(@NonNull OsmandApplication app, @Nullable Activity activity) {
		if (activity != null) {
			// called from UI
			for (ApplicationMode appMode : getAddedAppModes()) {
				ApplicationMode.changeProfileAvailability(appMode, true, app);
			}
		}
		return true;
	}

	public void setEnabled(boolean enabled) {
		this.enabled = enabled;
	}

	public boolean isEnabled() {
		return enabled;
	}

	public boolean isLocked() {
		return needsInstallation();
	}

	public boolean isActive() {
		return isEnabled() && !isLocked();
	}

	public boolean isEnableByDefault() {
		return false;
	}

	public boolean isMarketPlugin() {
		return false;
	}

	public boolean isPaid() {
		return false;
	}

	public boolean needsInstallation() {
		return installURL != null;
	}

	public void setInstallURL(String installURL) {
		this.installURL = installURL;
	}

	public String getInstallURL() {
		return installURL;
	}

	public String getComponentId1() {
		return null;
	}

	public String getComponentId2() {
		return null;
	}

	public List<ApplicationMode> getAddedAppModes() {
		return Collections.emptyList();
	}

	public List<IndexItem> getSuggestedMaps() {
		return Collections.emptyList();
	}

	public List<WorldRegion> getDownloadMaps() {
		return Collections.emptyList();
	}

	public List<String> getRendererNames() {
		return Collections.emptyList();
	}

	public List<String> getRouterNames() {
		return Collections.emptyList();
	}

	protected List<QuickActionType> getQuickActionTypes() {
		return Collections.emptyList();
	}

	protected List<PoiUIFilter> getCustomPoiFilters() {
		return Collections.emptyList();
	}

	protected void collectContextMenuImageCards(@NonNull ImageCardsHolder holder,
	                                            @NonNull Map<String, String> params,
	                                            @Nullable Map<String, String> additionalParams,
	                                            @Nullable GetImageCardsListener listener) {
	}

	protected boolean createContextMenuImageCard(@NonNull ImageCardsHolder holder,
	                                             @NonNull JSONObject imageObject) {
		return false;
	}

	/**
	 * Plugin was installed
	 */
	public void onInstall(@NonNull OsmandApplication app, @Nullable Activity activity) {
		for (ApplicationMode appMode : getAddedAppModes()) {
			ApplicationMode.changeProfileAvailability(appMode, true, app);
		}
		showInstallDialog(activity);
	}

	public void showInstallDialog(@Nullable Activity activity) {
		if (activity instanceof FragmentActivity) {
			FragmentManager fragmentManager = ((FragmentActivity) activity).getSupportFragmentManager();
			PluginInstalledBottomSheetDialog.showInstance(fragmentManager, getId(), activity instanceof MapActivity);
		}
	}

	public void showDisableDialog(@Nullable Activity activity) {
		if (activity instanceof FragmentActivity) {
			FragmentManager fragmentManager = ((FragmentActivity) activity).getSupportFragmentManager();
			PluginDisabledBottomSheet.showInstance(fragmentManager, getId(), activity instanceof MapActivity);
		}
	}

	public void disable(OsmandApplication app) {
		for (ApplicationMode appMode : getAddedAppModes()) {
			ApplicationMode.changeProfileAvailability(appMode, false, app);
		}
	}

	public String getHelpFileName() {
		return null;
	}

	/*
	 * Return true in case if plugin should fill the map context menu with buildContextMenuRows method.
	 */
	public boolean isMenuControllerSupported(Class<? extends MenuController> menuControllerClass) {
		return false;
	}

	/*
	 * Add menu rows to the map context menu.
	 */
	public void buildContextMenuRows(@NonNull MenuBuilder menuBuilder, @NonNull View view) {
	}

	/*
	 * Clear resources after menu was closed
	 */
	public void clearContextMenuRows() {
	}

	public static void initPlugins(@NonNull OsmandApplication app) {
		Set<String> enabledPlugins = app.getSettings().getEnabledPlugins();
		allPlugins.clear();

		allPlugins.add(new WikipediaPlugin(app));
		allPlugins.add(new OsmandRasterMapsPlugin(app));
		allPlugins.add(new OsmandMonitoringPlugin(app));
		checkMarketPlugin(app, new SRTMPlugin(app));
		checkMarketPlugin(app, new NauticalMapsPlugin(app));
		checkMarketPlugin(app, new SkiMapsPlugin(app));
		allPlugins.add(new AudioVideoNotesPlugin(app));
		checkMarketPlugin(app, new ParkingPositionPlugin(app));
		allPlugins.add(new OsmEditingPlugin(app));
		allPlugins.add(new OpenPlaceReviewsPlugin(app));
		allPlugins.add(new MapillaryPlugin(app));
		allPlugins.add(new AccessibilityPlugin(app));
		allPlugins.add(new OsmandDevelopmentPlugin(app));

		loadCustomPlugins(app);
		registerAppInitializingDependedProperties(app);
		enablePluginsByDefault(app, enabledPlugins);
		activatePlugins(app, enabledPlugins);
	}

	public static void addCustomPlugin(@NonNull OsmandApplication app, @NonNull CustomOsmandPlugin plugin) {
		OsmandPlugin oldPlugin = OsmandPlugin.getPlugin(plugin.getId());
		if (oldPlugin != null) {
			allPlugins.remove(oldPlugin);
		}
		allPlugins.add(plugin);
		enablePlugin(null, app, plugin, true);
		saveCustomPlugins(app);
	}

	public static void removeCustomPlugin(@NonNull OsmandApplication app, @NonNull final CustomOsmandPlugin plugin) {
		allPlugins.remove(plugin);
		if (plugin.isActive()) {
			plugin.removePluginItems(() -> Algorithms.removeAllFiles(plugin.getPluginDir()));
		} else {
			Algorithms.removeAllFiles(plugin.getPluginDir());
		}
		saveCustomPlugins(app);
	}

	private static void loadCustomPlugins(@NonNull OsmandApplication app) {
		SettingsAPI settingsAPI = app.getSettings().getSettingsAPI();
		Object pluginPrefs = settingsAPI.getPreferenceObject(PLUGINS_PREFERENCES_NAME);
		String customPluginsJson = settingsAPI.getString(pluginPrefs, CUSTOM_PLUGINS_KEY, "");
		if (!Algorithms.isEmpty(customPluginsJson)) {
			try {
				JSONArray jArray = new JSONArray(customPluginsJson);
				for (int i = 0; i < jArray.length(); i++) {
					JSONObject json = jArray.getJSONObject(i);
					CustomOsmandPlugin plugin = new CustomOsmandPlugin(app, json);
					allPlugins.add(plugin);
				}
			} catch (JSONException e) {
				e.printStackTrace();
			}
		}
	}

	private static void saveCustomPlugins(OsmandApplication app) {
		List<CustomOsmandPlugin> customOsmandPlugins = getCustomPlugins();
		SettingsAPI settingsAPI = app.getSettings().getSettingsAPI();
		Object pluginPrefs = settingsAPI.getPreferenceObject(PLUGINS_PREFERENCES_NAME);
		JSONArray itemsJson = new JSONArray();
		for (CustomOsmandPlugin plugin : customOsmandPlugins) {
			try {
				JSONObject json = new JSONObject();
				json.put("pluginId", plugin.getId());
				json.put("version", plugin.getVersion());
				plugin.writeAdditionalDataToJson(json);
				plugin.writeDependentFilesJson(json);
				itemsJson.put(json);
			} catch (JSONException e) {
				e.printStackTrace();
			}
		}
		String jsonStr = itemsJson.toString();
		if (!jsonStr.equals(settingsAPI.getString(pluginPrefs, CUSTOM_PLUGINS_KEY, ""))) {
			settingsAPI.edit(pluginPrefs).putString(CUSTOM_PLUGINS_KEY, jsonStr).commit();
		}
	}

	private static void enablePluginsByDefault(@NonNull OsmandApplication app, @NonNull Set<String> enabledPlugins) {
		for (OsmandPlugin plugin : allPlugins) {
			if (plugin.isEnableByDefault()
					&& !enabledPlugins.contains(plugin.getId())
					&& !isPluginDisabledManually(app, plugin)) {
				enabledPlugins.add(plugin.getId());
				app.getSettings().enablePlugin(plugin.getId(), true);
			}
		}
	}

	private static void activatePlugins(OsmandApplication app, Set<String> enabledPlugins) {
		for (OsmandPlugin plugin : allPlugins) {
			if (enabledPlugins.contains(plugin.getId()) || plugin.isEnabled()) {
				initPlugin(app, plugin);
			}
		}
		app.getQuickActionRegistry().updateActionTypes();
	}

	private static void initPlugin(OsmandApplication app, OsmandPlugin plugin) {
		try {
			if (plugin.init(app, null)) {
				plugin.setEnabled(true);
			}
		} catch (Exception e) {
			LOG.error("Plugin initialization failed " + plugin.getId(), e);
		}
	}

	private static void checkMarketPlugin(@NonNull OsmandApplication app,
	                                      @NonNull OsmandPlugin plugin) {
		if (updateMarketPlugin(app, plugin)) {
			allPlugins.add(plugin);
		}
	}

	private static boolean updateMarketPlugin(@NonNull OsmandApplication app,
	                                          @NonNull OsmandPlugin plugin) {
		boolean marketEnabled = Version.isMarketEnabled();
		boolean available = plugin.isAvailable(app);
		boolean paid = plugin.isPaid();
		boolean processed = false;
		// for test reasons
		//if ((Version.isDeveloperVersion(app) || !Version.isProductionVersion(app)) && !paid) {
		//	marketEnabled = false;
		//}
		if (available || (!marketEnabled && !paid)) {
			plugin.setInstallURL(null);
			processed = true;
		} else if (marketEnabled) {
			plugin.setInstallURL(Version.getUrlWithUtmRef(app, plugin.getComponentId1()));
			processed = true;
		}
		return processed;
	}

	public static void checkInstalledMarketPlugins(@NonNull OsmandApplication app, @Nullable Activity activity) {
		for (OsmandPlugin plugin : OsmandPlugin.getMarketPlugins()) {
			if (plugin.getInstallURL() != null && plugin.isAvailable(app)) {
				plugin.onInstall(app, activity);
				initPlugin(app, plugin);
			}
			updateMarketPlugin(app, plugin);
		}
		app.getQuickActionRegistry().updateActionTypes();
	}

	protected boolean isAvailable(OsmandApplication app) {
		return checkPluginPackage(app, this) || !isPaid();
	}

	public static boolean checkPluginPackage(@NonNull OsmandApplication app, @NonNull OsmandPlugin plugin) {
		return isPackageInstalled(plugin.getComponentId1(), app) || isPackageInstalled(plugin.getComponentId2(), app);
	}

	public static boolean enablePluginIfNeeded(@Nullable Activity activity,
	                                           @NonNull OsmandApplication app,
	                                           @Nullable OsmandPlugin plugin,
	                                           boolean enable) {
		if (plugin != null) {
			boolean stateChanged = enable != plugin.isEnabled();
			boolean canChangeState = !enable || !plugin.isLocked();
			if (stateChanged && canChangeState) {
				return enablePlugin(activity, app, plugin, enable);
			}
		}
		return false;
	}

	public static boolean enablePlugin(@Nullable Activity activity,
	                                   @NonNull OsmandApplication app,
	                                   @NonNull OsmandPlugin plugin,
	                                   boolean enable) {
		if (enable) {
			if (!plugin.init(app, activity)) {
				plugin.setEnabled(false);
				return false;
			} else {
				plugin.setEnabled(true);
			}
		} else {
			plugin.disable(app);
			plugin.setEnabled(false);
		}
		app.getSettings().enablePlugin(plugin.getId(), enable);
		app.getQuickActionRegistry().updateActionTypes();
		if (activity != null) {
			if (activity instanceof MapActivity) {
				final MapActivity mapActivity = (MapActivity) activity;
				plugin.updateLayers(mapActivity, mapActivity);
				mapActivity.getDashboard().refreshDashboardFragments();

				DashFragmentData fragmentData = plugin.getCardFragment();
				if (!enable && fragmentData != null) {
					FragmentManager fm = mapActivity.getSupportFragmentManager();
					Fragment fragment = fm.findFragmentByTag(fragmentData.tag);
					if (fragment != null) {
						fm.beginTransaction().remove(fragment).commitAllowingStateLoss();
					}
				}
			}

			if (plugin.isMarketPlugin() || plugin.isPaid()) {
				if (plugin.isActive()) {
					plugin.showInstallDialog(activity);
				} else if (OsmandPlugin.checkPluginPackage(app, plugin)) {
					plugin.showDisableDialog(activity);
				}
			}
		}
		return true;
	}

	private static void registerAppInitializingDependedProperties(@NonNull OsmandApplication app) {
		app.getAppInitializer().addListener(new AppInitializeListener() {

			@Override
			public void onStart(AppInitializer init) {
			}

			@Override
			public void onProgress(AppInitializer init, InitEvents event) {
			}

			@Override
			public void onFinish(AppInitializer init) {
				registerRenderingPreferences(app);
			}
		});
	}

	protected List<IndexItem> getMapsForType(@NonNull LatLon latLon, @NonNull DownloadActivityType type) {
		try {
			return DownloadResources.findIndexItemsAt(app, latLon, type);
		} catch (IOException e) {
			LOG.error(e);
		}
		return Collections.emptyList();
	}

	public void updateLayers(@NonNull Context context, @Nullable MapActivity mapActivity) {
	}

	public void registerLayers(@NonNull Context context, @Nullable MapActivity mapActivity) {
	}

	public void mapActivityCreate(MapActivity activity) {
	}

	public void mapActivityResume(MapActivity activity) {
	}

	public void mapActivityResumeOnTop(MapActivity activity) {
	}

	public void mapActivityPause(MapActivity activity) {
	}

	public void mapActivityDestroy(MapActivity activity) {
	}

	public void mapActivityScreenOff(MapActivity activity) {
	}

	@TargetApi(Build.VERSION_CODES.M)
	public void handleRequestPermissionsResult(int requestCode, String[] permissions,
	                                           int[] grantResults) {
	}

	public static void onRequestPermissionsResult(int requestCode, String[] permissions,
	                                              int[] grantResults) {
		for (OsmandPlugin plugin : getAvailablePlugins()) {
			plugin.handleRequestPermissionsResult(requestCode, permissions, grantResults);
		}
	}

	protected void registerLayerContextMenuActions(@NonNull ContextMenuAdapter adapter, @NonNull MapActivity mapActivity, List<RenderingRuleProperty> customRules) {
	}

	protected void registerConfigureMapCategoryActions(@NonNull ContextMenuAdapter adapter, @NonNull MapActivity mapActivity, @NonNull List<RenderingRuleProperty> customRules) {
	}

	@Nullable
	protected String getRenderPropertyPrefix() {
		return null;
	}

	protected void registerMapContextMenuActions(@NonNull MapActivity mapActivity, double latitude, double longitude,
	                                             ContextMenuAdapter adapter, Object selectedObj, boolean configureMenu) {
	}

	protected void registerOptionsMenuItems(MapActivity mapActivity, ContextMenuAdapter helper) {
	}

	public DashFragmentData getCardFragment() {
		return null;
	}

	public void updateLocation(Location location) {
	}

	protected void addMyPlacesTab(FavoritesActivity favoritesActivity, List<TabItem> mTabs, Intent intent) {
	}

	protected void optionsMenuFragment(FragmentActivity activity, Fragment fragment, ContextMenuAdapter optionsMenuAdapter) {
	}

	protected boolean searchFinished(QuickSearchDialogFragment searchFragment, SearchPhrase phrase, boolean isResultEmpty) {
		return false;
	}

	protected void newDownloadIndexes(Fragment fragment) {
	}

	protected void prepareExtraTopPoiFilters(Set<PoiUIFilter> poiUIFilter) {
	}

	protected String getMapObjectsLocale(Amenity amenity, String preferredLocale) {
		return null;
	}

	protected String getMapObjectPreferredLang(MapObject object, String defaultLanguage) {
		return null;
	}

	public List<String> indexingFiles(@Nullable IProgress progress) {
		return null;
	}

	public boolean mapActivityKeyUp(MapActivity mapActivity, int keyCode) {
		return false;
	}

	public void onMapActivityExternalResult(int requestCode, int resultCode, Intent data) {
	}

	public static void refreshLayers(@NonNull Context context, @Nullable MapActivity mapActivity) {
		for (OsmandPlugin plugin : getAvailablePlugins()) {
			plugin.updateLayers(context, mapActivity);
		}
	}

	@NonNull
	public static List<OsmandPlugin> getAvailablePlugins() {
		return allPlugins;
	}

	@NonNull
	public static List<OsmandPlugin> getEnabledPlugins() {
		ArrayList<OsmandPlugin> lst = new ArrayList<OsmandPlugin>(allPlugins.size());
		for (OsmandPlugin p : allPlugins) {
			if (p.isEnabled()) {
				lst.add(p);
			}
		}
		return lst;
	}

	@NonNull
	public static List<OsmandPlugin> getActivePlugins() {
		ArrayList<OsmandPlugin> lst = new ArrayList<OsmandPlugin>(allPlugins.size());
		for (OsmandPlugin p : allPlugins) {
			if (p.isActive()) {
				lst.add(p);
			}
		}
		return lst;
	}

	@NonNull
	public static List<OsmandPlugin> getNotActivePlugins() {
		ArrayList<OsmandPlugin> lst = new ArrayList<OsmandPlugin>(allPlugins.size());
		for (OsmandPlugin p : allPlugins) {
			if (!p.isActive()) {
				lst.add(p);
			}
		}
		return lst;
	}

	@NonNull
	public static List<OsmandPlugin> getMarketPlugins() {
		ArrayList<OsmandPlugin> lst = new ArrayList<OsmandPlugin>(allPlugins.size());
		for (OsmandPlugin p : allPlugins) {
			if (p.isMarketPlugin()) {
				lst.add(p);
			}
		}
		return lst;
	}

	@NonNull
	public static List<CustomOsmandPlugin> getCustomPlugins() {
		ArrayList<CustomOsmandPlugin> lst = new ArrayList<CustomOsmandPlugin>(allPlugins.size());
		for (OsmandPlugin plugin : allPlugins) {
			if (plugin instanceof CustomOsmandPlugin) {
				lst.add((CustomOsmandPlugin) plugin);
			}
		}
		return lst;
	}

	@NonNull
	public static List<OsmandPlugin> getEnabledSettingsScreenPlugins() {
		List<OsmandPlugin> plugins = new ArrayList<>();
		for (OsmandPlugin plugin : getEnabledPlugins()) {
			if (plugin.getSettingsScreenType() != null) {
				plugins.add(plugin);
			}
		}
		return plugins;
	}

	@SuppressWarnings("unchecked")
	public static <T extends OsmandPlugin> T getEnabledPlugin(Class<T> clz) {
		for (OsmandPlugin lr : getEnabledPlugins()) {
			if (clz.isInstance(lr)) {
				return (T) lr;
			}
		}
		return null;
	}

	@SuppressWarnings("unchecked")
	public static <T extends OsmandPlugin> T getActivePlugin(Class<T> clz) {
		for (OsmandPlugin lr : getActivePlugins()) {
			if (clz.isInstance(lr)) {
				return (T) lr;
			}
		}
		return null;
	}

	@SuppressWarnings("unchecked")
	public static <T extends OsmandPlugin> T getPlugin(Class<T> clz) {
		for (OsmandPlugin lr : getAvailablePlugins()) {
			if (clz.isInstance(lr)) {
				return (T) lr;
			}
		}
		return null;
	}

	public static OsmandPlugin getPlugin(String id) {
		for (OsmandPlugin plugin : getAvailablePlugins()) {
			if (plugin.getId().equals(id)) {
				return plugin;
			}
		}
		return null;
	}

	public static <T extends OsmandPlugin> boolean isEnabled(Class<T> clz) {
		return getEnabledPlugin(clz) != null;
	}

	public static <T extends OsmandPlugin> boolean isActive(Class<T> clz) {
		return getActivePlugin(clz) != null;
	}

	public static boolean isPluginDisabledManually(OsmandApplication app, OsmandPlugin plugin) {
		return app.getSettings().getPlugins().contains("-" + plugin.getId());
	}

	public static List<WorldRegion> getCustomDownloadRegions() {
		List<WorldRegion> l = new ArrayList<>();
		for (OsmandPlugin plugin : getEnabledPlugins()) {
			l.addAll(plugin.getDownloadMaps());
		}
		return l;
	}

	public static List<IndexItem> getCustomDownloadItems() {
		List<IndexItem> l = new ArrayList<>();
		for (WorldRegion region : getCustomDownloadRegions()) {
			collectIndexItemsFromSubregion(region, l);
		}
		return l;
	}

	public static void collectIndexItemsFromSubregion(WorldRegion region, List<IndexItem> items) {
		if (region instanceof CustomRegion) {
			items.addAll(((CustomRegion) region).loadIndexItems());
		}
		for (WorldRegion subregion : region.getSubregions()) {
			collectIndexItemsFromSubregion(subregion, items);
		}
	}

	public static List<String> getDisabledRendererNames() {
		List<String> l = new ArrayList<String>();
		for (OsmandPlugin plugin : getNotActivePlugins()) {
			l.addAll(plugin.getRendererNames());
		}
		return l;
	}

	public static List<String> getDisabledRouterNames() {
		List<String> l = new ArrayList<String>();
		for (OsmandPlugin plugin : getNotActivePlugins()) {
			l.addAll(plugin.getRouterNames());
		}
		return l;
	}

	public static List<String> onIndexingFiles(@Nullable IProgress progress) {
		List<String> l = new ArrayList<String>();
		for (OsmandPlugin plugin : getEnabledPlugins()) {
			List<String> ls = plugin.indexingFiles(progress);
			if (ls != null && ls.size() > 0) {
				l.addAll(ls);
			}
		}
		return l;
	}

	public static void onMapActivityCreate(MapActivity activity) {
		for (OsmandPlugin plugin : getEnabledPlugins()) {
			plugin.mapActivityCreate(activity);
		}
	}

	public static void onMapActivityResume(MapActivity activity) {
		for (OsmandPlugin plugin : getEnabledPlugins()) {
			plugin.mapActivityResume(activity);
		}
	}

	public static void onMapActivityResumeOnTop(MapActivity activity) {
		for (OsmandPlugin plugin : getEnabledPlugins()) {
			plugin.mapActivityResumeOnTop(activity);
		}
	}

	public static void onMapActivityPause(MapActivity activity) {
		for (OsmandPlugin plugin : getEnabledPlugins()) {
			plugin.mapActivityPause(activity);
		}
	}

	public static void onMapActivityDestroy(MapActivity activity) {
		for (OsmandPlugin plugin : getEnabledPlugins()) {
			plugin.mapActivityDestroy(activity);
		}
	}

	public static void onMapActivityResult(int requestCode, int resultCode, Intent data) {
		for (OsmandPlugin plugin : getEnabledPlugins()) {
			plugin.onMapActivityExternalResult(requestCode, resultCode, data);
		}
	}

	public static void onMapActivityScreenOff(MapActivity activity) {
		for (OsmandPlugin plugin : getEnabledPlugins()) {
			plugin.mapActivityScreenOff(activity);
		}
	}

	public static void createLayers(@NonNull Context context, @Nullable MapActivity mapActivity) {
		for (OsmandPlugin plugin : getEnabledPlugins()) {
			plugin.registerLayers(context, mapActivity);
		}
	}

	public static void registerMapContextMenu(@NonNull MapActivity mapActivity, double latitude, double longitude,
	                                          ContextMenuAdapter adapter, Object selectedObj, boolean configureMenu) {
		for (OsmandPlugin plugin : getEnabledPlugins()) {
			plugin.registerMapContextMenuActions(mapActivity, latitude, longitude, adapter, selectedObj, configureMenu);
		}
	}

	public static void registerLayerContextMenu(@NonNull ContextMenuAdapter adapter, @NonNull MapActivity mapActivity, @NonNull List<RenderingRuleProperty> customRules) {
		for (OsmandPlugin plugin : getEnabledPlugins()) {
			plugin.registerLayerContextMenuActions(adapter, mapActivity, customRules);
		}
	}

	public static void registerConfigureMapCategory(@NonNull ContextMenuAdapter adapter, @NonNull MapActivity mapActivity, @NonNull List<RenderingRuleProperty> customRules) {
		for (OsmandPlugin plugin : getEnabledPlugins()) {
			plugin.registerConfigureMapCategoryActions(adapter, mapActivity, customRules);
		}
	}

	public static void registerRenderingPreferences(@NonNull OsmandApplication app) {
		RenderingRulesStorage renderer = app.getRendererRegistry().getCurrentSelectedRenderer();
		if (renderer == null) return;

		List<RenderingRuleProperty> customRules = new ArrayList<>(renderer.PROPS.getCustomRules());
		for (OsmandPlugin plugin : getAvailablePlugins()) {
			String prefix = plugin.getRenderPropertyPrefix();
			if (prefix != null) {
				Iterator<RenderingRuleProperty> it = customRules.iterator();
				while (it.hasNext()) {
					RenderingRuleProperty rule = it.next();
					if (rule.getAttrName().startsWith(prefix)) {
						it.remove();
						if (rule.isBoolean()) {
							plugin.registerBooleanRenderingPreference(rule.getAttrName(), false);
						} else {
							plugin.registerRenderingPreference(rule.getAttrName(), "");
						}
					}
				}
			}
		}
	}

	public static void registerOptionsMenu(MapActivity map, ContextMenuAdapter helper) {
		for (OsmandPlugin plugin : getEnabledPlugins()) {
			plugin.registerOptionsMenuItems(map, helper);
		}
	}

	public static void onOptionsMenuActivity(FragmentActivity activity, Fragment fragment, ContextMenuAdapter optionsMenuAdapter) {
		for (OsmandPlugin plugin : getEnabledPlugins()) {
			plugin.optionsMenuFragment(activity, fragment, optionsMenuAdapter);
		}
	}

	public static boolean onSearchFinished(QuickSearchDialogFragment searchFragment, SearchPhrase phrase, boolean isResultEmpty) {
		boolean processed = false;
		for (OsmandPlugin plugin : getEnabledPlugins()) {
			processed = plugin.searchFinished(searchFragment, phrase, isResultEmpty) || processed;
		}
		return processed;
	}

	public static void onNewDownloadIndexes(Fragment fragment) {
		for (OsmandPlugin plugin : getEnabledPlugins()) {
			plugin.newDownloadIndexes(fragment);
		}
	}

	public static void onPrepareExtraTopPoiFilters(Set<PoiUIFilter> poiUIFilters) {
		for (OsmandPlugin plugin : getEnabledPlugins()) {
			plugin.prepareExtraTopPoiFilters(poiUIFilters);
		}
	}

	public static String onGetMapObjectPreferredLang(MapObject object, String preferredMapLang, String preferredMapAppLang) {
		for (OsmandPlugin plugin : getEnabledPlugins()) {
			String locale = plugin.getMapObjectPreferredLang(object, preferredMapLang);
			if (locale != null) {
				return locale;
			}
		}
		return preferredMapAppLang;
	}

	public static String onGetMapObjectsLocale(Amenity amenity, String preferredLocale) {
		for (OsmandPlugin plugin : getEnabledPlugins()) {
			String locale = plugin.getMapObjectsLocale(amenity, preferredLocale);
			if (locale != null) {
				return locale;
			}
		}
		return preferredLocale;
	}

	public static void registerCustomPoiFilters(List<PoiUIFilter> poiUIFilters) {
		for (OsmandPlugin p : getAvailablePlugins()) {
			poiUIFilters.addAll(p.getCustomPoiFilters());
		}
	}

	public static Collection<DashFragmentData> getPluginsCardsList() {
		HashSet<DashFragmentData> collection = new HashSet<>();
		for (OsmandPlugin plugin : getEnabledPlugins()) {
			final DashFragmentData fragmentData = plugin.getCardFragment();
			if (fragmentData != null) collection.add(fragmentData);
		}
		return collection;
	}

	public static void populateContextMenuImageCards(@NonNull ImageCardsHolder holder, @NonNull Map<String, String> params,
	                                                 @Nullable Map<String, String> additionalParams, @Nullable GetImageCardsListener listener) {
		for (OsmandPlugin plugin : getEnabledPlugins()) {
			plugin.collectContextMenuImageCards(holder, params, additionalParams, listener);
		}
	}

	/**
	 * @param holder      an object to collect results
	 * @param imageObject json object that contains data for create an image card
	 * @return 'true' if an image card was created
	 */
	public static boolean createImageCardForJson(@NonNull ImageCardsHolder holder,
	                                             @NonNull JSONObject imageObject) {
		for (OsmandPlugin plugin : getEnabledPlugins()) {
			if (plugin.createContextMenuImageCard(holder, imageObject)) {
				return true;
			}
		}
		return false;
	}

	public static boolean isPackageInstalled(@Nullable String packageInfo, @NonNull Context ctx) {
		if (packageInfo == null) {
			return false;
		}
		boolean installed = false;
		try {
			installed = ctx.getPackageManager().getPackageInfo(packageInfo, 0) != null;
		} catch (NameNotFoundException e) {
			LOG.error("Package not found: " + packageInfo, e);
		}
		return installed;
	}

	public static boolean onMapActivityKeyUp(MapActivity mapActivity, int keyCode) {
		for (OsmandPlugin p : getEnabledPlugins()) {
			if (p.mapActivityKeyUp(mapActivity, keyCode))
				return true;
		}
		return false;
	}

	public static void registerQuickActionTypesPlugins(List<QuickActionType> allTypes,
	                                                   List<QuickActionType> enabledTypes) {
		for (OsmandPlugin p : getAvailablePlugins()) {
			List<QuickActionType> types = p.getQuickActionTypes();
			allTypes.addAll(types);
			if (p.isEnabled()) {
				enabledTypes.addAll(types);
			}
		}
	}

	public static void updateLocationPlugins(net.osmand.Location location) {
		for (OsmandPlugin p : getEnabledPlugins()) {
			p.updateLocation(location);
		}
	}

	public static boolean isDevelopment() {
		return getEnabledPlugin(OsmandDevelopmentPlugin.class) != null;
	}

	public static void addMyPlacesTabPlugins(FavoritesActivity favoritesActivity, List<TabItem> mTabs, Intent intent) {
		for (OsmandPlugin p : getEnabledPlugins()) {
			p.addMyPlacesTab(favoritesActivity, mTabs, intent);
		}
	}

	protected CommonPreference<Boolean> registerBooleanPreference(@NonNull String prefId, boolean defValue) {
		CommonPreference<Boolean> preference = app.getSettings().registerBooleanPreference(prefId, defValue);
		preference.setRelatedPlugin(this);
		pluginPreferences.add(preference);
		return preference;
	}

	protected CommonPreference<Boolean> registerBooleanAccessibilityPreference(@NonNull String prefId, boolean defValue) {
		CommonPreference<Boolean> preference = app.getSettings().registerBooleanAccessibilityPreference(prefId, defValue);
		preference.setRelatedPlugin(this);
		pluginPreferences.add(preference);
		return preference;
	}

	protected CommonPreference<String> registerStringPreference(@NonNull String prefId, @Nullable String defValue) {
		CommonPreference<String> preference = app.getSettings().registerStringPreference(prefId, defValue);
		preference.setRelatedPlugin(this);
		pluginPreferences.add(preference);
		return preference;
	}

	protected CommonPreference<Integer> registerIntPreference(@NonNull String prefId, int defValue) {
		CommonPreference<Integer> preference = app.getSettings().registerIntPreference(prefId, defValue);
		preference.setRelatedPlugin(this);
		pluginPreferences.add(preference);
		return preference;
	}

	protected CommonPreference<Long> registerLongPreference(@NonNull String prefId, long defValue) {
		CommonPreference<Long> preference = app.getSettings().registerLongPreference(prefId, defValue);
		preference.setRelatedPlugin(this);
		pluginPreferences.add(preference);
		return preference;
	}

	protected CommonPreference<Float> registerFloatPreference(@NonNull String prefId, float defValue) {
		CommonPreference<Float> preference = app.getSettings().registerFloatPreference(prefId, defValue);
		preference.setRelatedPlugin(this);
		pluginPreferences.add(preference);
		return preference;
	}

	protected <T extends Enum<?>> CommonPreference<T> registerEnumIntPreference(@NonNull String prefId, @NonNull Enum<?> defaultValue,
	                                                                            @NonNull Enum<?>[] values, @NonNull Class<T> clz) {
		CommonPreference<T> preference = app.getSettings().registerEnumIntPreference(prefId, defaultValue, values, clz);
		preference.setRelatedPlugin(this);
		pluginPreferences.add(preference);
		return preference;
	}

	protected ListStringPreference registerListStringPreference(@NonNull String prefId, @Nullable String defValue, @NonNull String delimiter) {
		ListStringPreference preference = app.getSettings().registerStringListPreference(prefId, defValue, delimiter);
		preference.setRelatedPlugin(this);
		pluginPreferences.add(preference);
		return preference;
	}

	protected CommonPreference<Boolean> registerBooleanRenderingPreference(@NonNull String prefId, boolean defValue) {
		CommonPreference<Boolean> preference = app.getSettings().registerCustomRenderBooleanProperty(prefId, defValue);
		preference.setRelatedPlugin(this);
		pluginPreferences.add(preference);
		return preference;
	}

	protected CommonPreference<String> registerRenderingPreference(@NonNull String prefId, @Nullable String defValue) {
		CommonPreference<String> preference = app.getSettings().registerCustomRenderProperty(prefId, defValue);
		preference.setRelatedPlugin(this);
		pluginPreferences.add(preference);
		return preference;
	}
}
