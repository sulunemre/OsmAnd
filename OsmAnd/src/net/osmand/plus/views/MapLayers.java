package net.osmand.plus.views;


import android.content.Context;
import android.content.DialogInterface;
import android.graphics.drawable.Drawable;
import android.widget.Button;
import android.widget.ListView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.StyleRes;
import androidx.appcompat.app.AlertDialog;

import net.osmand.CallbackWithObject;
import net.osmand.GPXUtilities.GPXFile;
import net.osmand.GPXUtilities.WptPt;
import net.osmand.IndexConstants;
import net.osmand.ResultMatcher;
import net.osmand.StateChangedListener;
import net.osmand.map.ITileSource;
import net.osmand.map.TileSourceManager.TileSourceTemplate;
import net.osmand.plus.DialogListItemAdapter;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.activities.MapActivity.ShowQuickSearchMode;
import net.osmand.plus.dashboard.DashboardOnMap;
import net.osmand.plus.helpers.GpxUiHelper;
import net.osmand.plus.measurementtool.MeasurementToolLayer;
import net.osmand.plus.plugins.OsmandPlugin;
import net.osmand.plus.plugins.rastermaps.OsmandRasterMapsPlugin;
import net.osmand.plus.poi.PoiFiltersHelper;
import net.osmand.plus.poi.PoiUIFilter;
import net.osmand.plus.render.RenderingIcons;
import net.osmand.plus.resources.SQLiteTileSource;
import net.osmand.plus.routing.RoutingHelper;
import net.osmand.plus.settings.backend.ApplicationMode;
import net.osmand.plus.settings.backend.OsmandSettings;
import net.osmand.plus.settings.backend.preferences.CommonPreference;
import net.osmand.plus.utils.UiUtilities;
import net.osmand.plus.views.layers.ContextMenuLayer;
import net.osmand.plus.views.layers.DistanceRulerControlLayer;
import net.osmand.plus.views.layers.DownloadedRegionsLayer;
import net.osmand.plus.views.layers.FavouritesLayer;
import net.osmand.plus.views.layers.GPXLayer;
import net.osmand.plus.views.layers.ImpassableRoadsLayer;
import net.osmand.plus.views.layers.MapControlsLayer;
import net.osmand.plus.views.layers.MapInfoLayer;
import net.osmand.plus.views.layers.MapMarkersLayer;
import net.osmand.plus.views.layers.MapQuickActionLayer;
import net.osmand.plus.views.layers.MapTextLayer;
import net.osmand.plus.views.layers.MapTileLayer;
import net.osmand.plus.views.layers.MapVectorLayer;
import net.osmand.plus.views.layers.POIMapLayer;
import net.osmand.plus.views.layers.PointLocationLayer;
import net.osmand.plus.views.layers.PointNavigationLayer;
import net.osmand.plus.views.layers.PreviewRouteLineLayer;
import net.osmand.plus.views.layers.RadiusRulerControlLayer;
import net.osmand.plus.views.layers.RouteLayer;
import net.osmand.plus.views.layers.TransportStopsLayer;
import net.osmand.plus.views.layers.base.OsmandMapLayer;
import net.osmand.plus.views.mapwidgets.MapWidgetRegistry;
import net.osmand.plus.widgets.ctxmenu.ContextMenuAdapter;
import net.osmand.plus.widgets.ctxmenu.ContextMenuListAdapter;
import net.osmand.plus.widgets.ctxmenu.ViewCreator;
import net.osmand.plus.widgets.ctxmenu.callback.ItemClickListener;
import net.osmand.plus.widgets.ctxmenu.callback.OnDataChangeUiAdapter;
import net.osmand.plus.widgets.ctxmenu.data.ContextMenuItem;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

/**
 * Object is responsible to maintain layers using by map activity
 */
public class MapLayers {

	private final OsmandApplication app;

	// the order of layer should be preserved ! when you are inserting new layer
	private MapTileLayer mapTileLayer;
	private MapVectorLayer mapVectorLayer;
	private GPXLayer gpxLayer;
	private RouteLayer routeLayer;
	private PreviewRouteLineLayer previewRouteLineLayer;
	private POIMapLayer poiMapLayer;
	private FavouritesLayer mFavouritesLayer;
	private TransportStopsLayer transportStopsLayer;
	private PointLocationLayer locationLayer;
	private RadiusRulerControlLayer radiusRulerControlLayer;
	private DistanceRulerControlLayer distanceRulerControlLayer;
	private PointNavigationLayer navigationLayer;
	private MapMarkersLayer mapMarkersLayer;
	private ImpassableRoadsLayer impassableRoadsLayer;
	private MapInfoLayer mapInfoLayer;
	private MapTextLayer mapTextLayer;
	private ContextMenuLayer contextMenuLayer;
	private MapControlsLayer mapControlsLayer;
	private MapQuickActionLayer mapQuickActionLayer;
	private DownloadedRegionsLayer downloadedRegionsLayer;
	private MapWidgetRegistry mapWidgetRegistry;
	private MeasurementToolLayer measurementToolLayer;

	private StateChangedListener<Integer> transparencyListener;

	public MapLayers(@NonNull OsmandApplication app) {
		this.app = app;
		this.mapWidgetRegistry = new MapWidgetRegistry(app);
	}

	public MapWidgetRegistry getMapWidgetRegistry() {
		return mapWidgetRegistry;
	}

	public void createLayers(@NonNull OsmandMapTileView mapView) {
		// first create to make accessible
		mapTextLayer = new MapTextLayer(app);
		// 5.95 all labels
		mapView.addLayer(mapTextLayer, 5.95f);
		// 8. context menu layer 
		contextMenuLayer = new ContextMenuLayer(app);
		mapView.addLayer(contextMenuLayer, 8);
		// mapView.addLayer(underlayLayer, -0.5f);
		mapTileLayer = new MapTileLayer(app, true);
		mapView.addLayer(mapTileLayer, 0.05f);
		mapView.setMainLayer(mapTileLayer);

		// 0.5 layer
		mapVectorLayer = new MapVectorLayer(app);
		mapView.addLayer(mapVectorLayer, 0.0f);

		downloadedRegionsLayer = new DownloadedRegionsLayer(app);
		mapView.addLayer(downloadedRegionsLayer, 0.5f);

		// 0.9 gpx layer
		gpxLayer = new GPXLayer(app);
		mapView.addLayer(gpxLayer, 0.9f);

		// 1. route layer
		routeLayer = new RouteLayer(app, -150000);
		mapView.addLayer(routeLayer, 1);

		// 1.5 preview route line layer
		previewRouteLineLayer = new PreviewRouteLineLayer(app);
		mapView.addLayer(previewRouteLineLayer, 1.5f);

		// 2. osm bugs layer
		// 3. poi layer
		poiMapLayer = new POIMapLayer(app);
		mapView.addLayer(poiMapLayer, 3);
		// 4. favorites layer
		mFavouritesLayer = new FavouritesLayer(app);
		mapView.addLayer(mFavouritesLayer, 4);
		// 4.6 measurement tool layer
		measurementToolLayer = new MeasurementToolLayer(app);
		mapView.addLayer(measurementToolLayer, 4.6f);
		// 5. transport layer
		transportStopsLayer = new TransportStopsLayer(app);
		mapView.addLayer(transportStopsLayer, 5);
		// 5.95 all text labels
		// 6. point location layer 
		locationLayer = new PointLocationLayer(app);
		mapView.addLayer(locationLayer, 6);
		// 7. point navigation layer
		navigationLayer = new PointNavigationLayer(app, -207000);
		mapView.addLayer(navigationLayer, 7);
		// 7.3 map markers layer
		mapMarkersLayer = new MapMarkersLayer(app);
		mapView.addLayer(mapMarkersLayer, 7.3f);
		// 7.5 Impassible roads
		impassableRoadsLayer = new ImpassableRoadsLayer(app);
		mapView.addLayer(impassableRoadsLayer, 7.5f);
		// 7.8 radius ruler control layer
		radiusRulerControlLayer = new RadiusRulerControlLayer(app);
		mapView.addLayer(radiusRulerControlLayer, 7.8f);
		// 7.9 ruler by tap control layer
		distanceRulerControlLayer = new DistanceRulerControlLayer(app);
		mapView.addLayer(distanceRulerControlLayer, 7.9f);
		// 8. context menu layer 
		// 9. map info layer
		mapInfoLayer = new MapInfoLayer(app, routeLayer);
		mapView.addLayer(mapInfoLayer, 9);
		// 11. route info layer
		mapControlsLayer = new MapControlsLayer(app);
		mapView.addLayer(mapControlsLayer, 11);
		// 12. quick actions layer
		mapQuickActionLayer = new MapQuickActionLayer(app);
		mapView.addLayer(mapQuickActionLayer, 12);
		contextMenuLayer.setMapQuickActionLayer(mapQuickActionLayer);

		transparencyListener = change -> app.runInUIThread(() -> {
			mapTileLayer.setAlpha(change);
			mapVectorLayer.setAlpha(change);
			mapView.refreshMap();
		});
		app.getSettings().MAP_TRANSPARENCY.addListener(transparencyListener);

		createAdditionalLayers(null);
	}

	public void createAdditionalLayers(@Nullable MapActivity mapActivity) {
		OsmandPlugin.createLayers(app, mapActivity);
		app.getAppCustomization().createLayers(app, mapActivity);
		app.getAidlApi().registerMapLayers(app);
	}

	public void setMapActivity(@Nullable MapActivity mapActivity) {
		OsmandMapTileView mapView = app.getOsmandMap().getMapView();
		for (OsmandMapLayer layer : mapView.getLayers()) {
			MapActivity layerMapActivity = layer.getMapActivity();
			if (mapActivity != null && layerMapActivity != null) {
				layer.setMapActivity(null);
			}
			layer.setMapActivity(mapActivity);
		}
	}

	public boolean hasMapActivity() {
		OsmandMapTileView mapView = app.getOsmandMap().getMapView();
		for (OsmandMapLayer layer : mapView.getLayers()) {
			if (layer.getMapActivity() != null) {
				return true;
			}
		}
		return false;
	}

	public void updateLayers(@Nullable MapActivity mapActivity) {
		OsmandSettings settings = app.getSettings();
		OsmandMapTileView mapView = app.getOsmandMap().getMapView();
		updateMapSource(mapView, settings.MAP_TILE_SOURCES);
		OsmandPlugin.refreshLayers(app, mapActivity);
	}

	public void updateMapSource(@NonNull OsmandMapTileView mapView, CommonPreference<String> settingsToWarnAboutMap) {
		OsmandSettings settings = app.getSettings();

		// update transparency
		int mapTransparency = settings.MAP_UNDERLAY.get() == null ? 255 : settings.MAP_TRANSPARENCY.get();
		mapTileLayer.setAlpha(mapTransparency);
		mapVectorLayer.setAlpha(mapTransparency);

		ITileSource newSource = settings.getMapTileSource(settings.MAP_TILE_SOURCES == settingsToWarnAboutMap);
		ITileSource oldMap = mapTileLayer.getMap();
		if (newSource != oldMap) {
			if (oldMap instanceof SQLiteTileSource) {
				((SQLiteTileSource) oldMap).closeDB();
			}
			mapTileLayer.setMap(newSource);
		}

		boolean vectorData = !settings.MAP_ONLINE_DATA.get();
		mapTileLayer.setVisible(!vectorData);
		mapVectorLayer.setVisible(vectorData);
		if (vectorData) {
			mapView.setMainLayer(mapVectorLayer);
		} else {
			mapView.setMainLayer(mapTileLayer);
		}
	}


	public AlertDialog showGPXFileLayer(@NonNull List<String> files, final MapActivity mapActivity) {
		final OsmandSettings settings = app.getSettings();
		OsmandMapTileView mapView = mapActivity.getMapView();
		DashboardOnMap dashboard = mapActivity.getDashboard();
		CallbackWithObject<GPXFile[]> callbackWithObject = result -> {
			WptPt locToShow = null;
			for (GPXFile g : result) {
				if (g.showCurrentTrack) {
					if (!settings.SAVE_TRACK_TO_GPX.get() && !
							settings.SAVE_GLOBAL_TRACK_TO_GPX.get()) {
						app.showToastMessage(R.string.gpx_monitoring_disabled_warn);
					}
					break;
				} else {
					locToShow = g.findPointToShow();
				}
			}
			app.getSelectedGpxHelper().setGpxFileToDisplay(result);
			if (locToShow != null) {
				mapView.getAnimatedDraggingThread().startMoving(locToShow.lat, locToShow.lon,
						mapView.getZoom(), true);
			}
			mapView.refreshMap();
			dashboard.refreshContent(true);
			return true;
		};
		return GpxUiHelper.selectGPXFiles(files, mapActivity, callbackWithObject, getThemeRes(), isNightMode());
	}

	public void showMultiChoicePoiFilterDialog(final MapActivity mapActivity, final DismissListener listener) {
		final PoiFiltersHelper poiFilters = app.getPoiFilters();
		final ContextMenuAdapter adapter = new ContextMenuAdapter(app);
		final List<PoiUIFilter> list = new ArrayList<>();
		for (PoiUIFilter f : poiFilters.getSortedPoiFilters(true)) {
			if (!f.isTopWikiFilter()
					&& !f.isRoutesFilter()
					&& !f.isRouteArticleFilter()
					&& !f.isRouteArticlePointFilter()
					&& !f.isCustomPoiFilter()) {
				addFilterToList(adapter, list, f, true);
			}
		}

		ApplicationMode appMode = app.getSettings().getApplicationMode();
		ViewCreator viewCreator = new ViewCreator(mapActivity, isNightMode());
		viewCreator.setCustomControlsColor(appMode.getProfileColor(isNightMode()));
		ContextMenuListAdapter listAdapter = adapter.toListAdapter(mapActivity, viewCreator);

		Context themedContext = UiUtilities.getThemedContext(mapActivity, isNightMode());
		AlertDialog.Builder builder = new AlertDialog.Builder(themedContext);
		final ListView listView = new ListView(themedContext);
		listView.setDivider(null);
		listView.setClickable(true);
		listView.setAdapter(listAdapter);
		listView.setOnItemClickListener((parent, view, position, id) -> {
			ContextMenuItem item = listAdapter.getItem(position);
			if (item != null) {
				item.setSelected(!item.getSelected());
				ItemClickListener clickListener = item.getItemClickListener();
				if (clickListener != null) {
					clickListener.onContextMenuClick(listAdapter, view, item, item.getSelected());
				}
				listAdapter.notifyDataSetChanged();
			}
		});
		builder.setView(listView)
				.setTitle(R.string.show_poi_over_map)
				.setPositiveButton(R.string.shared_string_ok, (dialog, which) -> {
					for (int i = 0; i < listAdapter.getCount(); i++) {
						ContextMenuItem item = listAdapter.getItem(i);
						PoiUIFilter filter = list.get(i);
						if (item != null && item.getSelected()) {
							if (filter.isStandardFilter()) {
								filter.removeUnsavedFilterByName();
							}
							poiFilters.addSelectedPoiFilter(filter);
						} else {
							poiFilters.removeSelectedPoiFilter(filter);
						}
					}
					mapActivity.getMapView().refreshMap();
				})
				.setNegativeButton(R.string.shared_string_cancel, null)
				.setNeutralButton(" ", (dialog, which) -> showSingleChoicePoiFilterDialog(mapActivity, listener));
		final AlertDialog alertDialog = builder.create();
		alertDialog.setOnShowListener(dialog -> {
			Button neutralButton = alertDialog.getButton(DialogInterface.BUTTON_NEUTRAL);
			Drawable drawable = app.getUIUtilities().getThemedIcon(R.drawable.ic_action_singleselect);
			neutralButton.setCompoundDrawablesWithIntrinsicBounds(drawable, null, null, null);
			neutralButton.setContentDescription(app.getString(R.string.shared_string_filters));
		});
		alertDialog.setOnDismissListener(dialog -> listener.dismiss());
		alertDialog.show();
	}

	public void showSingleChoicePoiFilterDialog(final MapActivity mapActivity, final DismissListener listener) {
		final PoiFiltersHelper poiFilters = app.getPoiFilters();
		final ContextMenuAdapter adapter = new ContextMenuAdapter(app);
		adapter.addItem(new ContextMenuItem(null)
				.setTitleId(R.string.shared_string_search, app)
				.setIcon(R.drawable.ic_action_search_dark));
		final List<PoiUIFilter> list = new ArrayList<>();
		list.add(null);
		for (PoiUIFilter f : poiFilters.getSortedPoiFilters(true)) {
			if (!f.isTopWikiFilter()
					&& !f.isRoutesFilter()
					&& !f.isRouteArticleFilter()
					&& !f.isRouteArticlePointFilter()
					&& !f.isCustomPoiFilter()) {
				addFilterToList(adapter, list, f, false);
			}
		}

		ApplicationMode appMode = app.getSettings().getApplicationMode();
		ViewCreator viewCreator = new ViewCreator(mapActivity, isNightMode());
		viewCreator.setCustomControlsColor(appMode.getProfileColor(isNightMode()));
		ContextMenuListAdapter listAdapter = adapter.toListAdapter(mapActivity, viewCreator);

		Context themedContext = UiUtilities.getThemedContext(mapActivity, isNightMode());
		AlertDialog.Builder builder = new AlertDialog.Builder(themedContext);
		builder.setAdapter(listAdapter, (dialog, which) -> {
			PoiUIFilter filter = list.get(which);
			if (filter == null) {
				if (mapActivity.getDashboard().isVisible()) {
					mapActivity.getDashboard().hideDashboard();
				}
				mapActivity.showQuickSearch(ShowQuickSearchMode.NEW, true);
			} else {
				if (filter.isStandardFilter()) {
					filter.removeUnsavedFilterByName();
				}
				PoiUIFilter wiki = poiFilters.getTopWikiPoiFilter();
				poiFilters.clearSelectedPoiFilters(wiki);
				poiFilters.addSelectedPoiFilter(filter);
				updateRoutingPoiFiltersIfNeeded();
				mapActivity.getMapView().refreshMap();
			}
		});
		builder.setTitle(R.string.show_poi_over_map);
		builder.setNegativeButton(R.string.shared_string_dismiss, null);
		builder.setNeutralButton(" ", (dialog, which) -> showMultiChoicePoiFilterDialog(mapActivity, listener));
		final AlertDialog alertDialog = builder.create();
		alertDialog.setOnShowListener(dialog -> {
			Button neutralButton = alertDialog.getButton(DialogInterface.BUTTON_NEUTRAL);
			Drawable drawable = app.getUIUtilities().getThemedIcon(R.drawable.ic_action_multiselect);
			neutralButton.setCompoundDrawablesWithIntrinsicBounds(drawable, null, null, null);
			neutralButton.setContentDescription(app.getString(R.string.apply_filters));
		});
		alertDialog.setOnDismissListener(dialog -> listener.dismiss());
		alertDialog.show();
	}

	private void addFilterToList(final ContextMenuAdapter adapter,
								 final List<PoiUIFilter> list,
								 final PoiUIFilter f,
								 boolean multiChoice) {
		list.add(f);
		ContextMenuItem item = new ContextMenuItem(null);
		if (multiChoice) {
			item.setSelected(app.getPoiFilters().isPoiFilterSelected(f));
			item.setListener((uiAdapter, view, it, isChecked) -> {
				it.setSelected(isChecked);
				return false;
			});
		}
		item.setTitle(f.getName());
		if (RenderingIcons.containsBigIcon(f.getIconId())) {
			item.setIcon(RenderingIcons.getBigIconResourceId(f.getIconId()));
		} else {
			item.setIcon(R.drawable.mx_special_custom_category);
		}
		item.setColor(app, ContextMenuItem.INVALID_ID);
		item.setUseNaturalIconColor(true);
		adapter.addItem(item);
	}

	public void selectMapLayer(@NonNull MapActivity mapActivity,
	                           @NonNull ContextMenuItem item,
	                           @NonNull OnDataChangeUiAdapter uiAdapter) {
		selectMapLayer(mapActivity, true, mapSourceName -> {
			item.setDescription(mapSourceName);
			uiAdapter.onDataSetChanged();
			return true;
		});
	}

	public void selectMapLayer(@NonNull MapActivity mapActivity,
	                           boolean includeOfflineMaps,
	                           @Nullable CallbackWithObject<String> callback) {
		if (!OsmandPlugin.isActive(OsmandRasterMapsPlugin.class)) {
			app.showToastMessage(R.string.map_online_plugin_is_not_installed);
			return;
		}

		OsmandSettings settings = app.getSettings();

		Map<String, String> entriesMap = new LinkedHashMap<>();

		final String layerOsmVector = "LAYER_OSM_VECTOR";
		final String layerInstallMore = "LAYER_INSTALL_MORE";
		final String layerAdd = "LAYER_ADD";

		if (includeOfflineMaps) {
			entriesMap.put(layerOsmVector, getString(R.string.vector_data));
		}
		entriesMap.putAll(settings.getTileSourceEntries());
		entriesMap.put(layerInstallMore, getString(R.string.install_more));
		entriesMap.put(layerAdd, getString(R.string.shared_string_add));
		List<Entry<String, String>> entriesMapList = new ArrayList<>(entriesMap.entrySet());


		String selectedTileSourceKey = settings.MAP_TILE_SOURCES.get();

		int selectedItem = -1;
		if (!settings.MAP_ONLINE_DATA.get()) {
			selectedItem = 0;
		} else {

			Entry<String, String> selectedEntry = null;
			for (Entry<String, String> entry : entriesMap.entrySet()) {
				if (entry.getKey().equals(selectedTileSourceKey)) {
					selectedEntry = entry;
					break;
				}
			}
			if (selectedEntry != null) {
				selectedItem = 0;
				entriesMapList.remove(selectedEntry);
				entriesMapList.add(0, selectedEntry);
			}
		}

		final String[] items = new String[entriesMapList.size()];
		int i = 0;
		for (Entry<String, String> entry : entriesMapList) {
			items[i++] = entry.getValue();
		}

		boolean nightMode = isNightMode();
		int themeRes = getThemeRes();
		int selectedModeColor = settings.getApplicationMode().getProfileColor(nightMode);
		DialogListItemAdapter dialogAdapter = DialogListItemAdapter.createSingleChoiceAdapter(
				items, nightMode, selectedItem, app, selectedModeColor, themeRes, v -> {
					int which = (int) v.getTag();
					String layerKey = entriesMapList.get(which).getKey();
					switch (layerKey) {
						case layerOsmVector:
							settings.MAP_ONLINE_DATA.set(false);
							updateMapSource(mapActivity.getMapView(), null);
							if (callback != null) {
								callback.processResult(null);
							}
							break;
						case layerAdd:
							OsmandRasterMapsPlugin.defineNewEditLayer(mapActivity.getSupportFragmentManager(), null, null);
							break;
						case layerInstallMore:
							OsmandRasterMapsPlugin.installMapLayers(mapActivity, new ResultMatcher<TileSourceTemplate>() {
								TileSourceTemplate template = null;
								int count = 0;

								@Override
								public boolean publish(TileSourceTemplate object) {
									if (object == null) {
										if (count == 1) {
											settings.MAP_TILE_SOURCES.set(template.getName());
											settings.MAP_ONLINE_DATA.set(true);
											updateMapSource(mapActivity.getMapView(), settings.MAP_TILE_SOURCES);
											if (callback != null) {
												callback.processResult(template.getName());
											}
										} else {
											selectMapLayer(mapActivity, includeOfflineMaps, callback);
										}
									} else {
										count++;
										template = object;
									}
									return false;
								}

								@Override
								public boolean isCancelled() {
									return false;
								}
							});
							break;
						default:
							settings.MAP_TILE_SOURCES.set(layerKey);
							settings.MAP_ONLINE_DATA.set(true);
							updateMapSource(mapActivity.getMapView(), settings.MAP_TILE_SOURCES);
							if (callback != null) {
								callback.processResult(layerKey.replace(IndexConstants.SQLITE_EXT, ""));
							}
							break;
					}
				}
		);

		Context themedContext = UiUtilities.getThemedContext(mapActivity, isNightMode());
		AlertDialog.Builder builder = new AlertDialog.Builder(themedContext);
		builder.setAdapter(dialogAdapter, null);
		builder.setNegativeButton(R.string.shared_string_dismiss, null);
		dialogAdapter.setDialog(builder.show());
	}

	private void updateRoutingPoiFiltersIfNeeded() {
		OsmandSettings settings = app.getSettings();
		RoutingHelper routingHelper = app.getRoutingHelper();
		boolean usingRouting = routingHelper.isFollowingMode() || routingHelper.isRoutePlanningMode()
				|| routingHelper.isRouteBeingCalculated() || routingHelper.isRouteCalculated();
		ApplicationMode routingMode = routingHelper.getAppMode();
		if (usingRouting && routingMode != settings.getApplicationMode()) {
			settings.setSelectedPoiFilters(routingMode, settings.getSelectedPoiFilters());
		}
	}

	private boolean isNightMode() {
		return app.getDaynightHelper().isNightModeForMapControls();
	}

	@StyleRes
	private int getThemeRes() {
		return isNightMode() ? R.style.OsmandDarkTheme : R.style.OsmandLightTheme;
	}

	private String getString(int resId) {
		return app.getString(resId);
	}

	public RouteLayer getRouteLayer() {
		return routeLayer;
	}

	public PreviewRouteLineLayer getPreviewRouteLineLayer() {
		return previewRouteLineLayer;
	}

	public PointNavigationLayer getNavigationLayer() {
		return navigationLayer;
	}

	public ImpassableRoadsLayer getImpassableRoadsLayer() {
		return impassableRoadsLayer;
	}

	public GPXLayer getGpxLayer() {
		return gpxLayer;
	}

	public ContextMenuLayer getContextMenuLayer() {
		return contextMenuLayer;
	}

	public FavouritesLayer getFavouritesLayer() {
		return mFavouritesLayer;
	}

	public MeasurementToolLayer getMeasurementToolLayer() {
		return measurementToolLayer;
	}

	public MapTextLayer getMapTextLayer() {
		return mapTextLayer;
	}

	public PointLocationLayer getLocationLayer() {
		return locationLayer;
	}

	public RadiusRulerControlLayer getRadiusRulerControlLayer() {
		return radiusRulerControlLayer;
	}

	public DistanceRulerControlLayer getDistanceRulerControlLayer() {
		return distanceRulerControlLayer;
	}

	public MapInfoLayer getMapInfoLayer() {
		return mapInfoLayer;
	}

	public MapControlsLayer getMapControlsLayer() {
		return mapControlsLayer;
	}

	public MapQuickActionLayer getMapQuickActionLayer() {
		return mapQuickActionLayer;
	}

	public MapMarkersLayer getMapMarkersLayer() {
		return mapMarkersLayer;
	}

	public MapTileLayer getMapTileLayer() {
		return mapTileLayer;
	}

	public MapVectorLayer getMapVectorLayer() {
		return mapVectorLayer;
	}

	public POIMapLayer getPoiMapLayer() {
		return poiMapLayer;
	}

	public TransportStopsLayer getTransportStopsLayer() {
		return transportStopsLayer;
	}

	public DownloadedRegionsLayer getDownloadedRegionsLayer() {
		return downloadedRegionsLayer;
	}

	public interface DismissListener {
		void dismiss();
	}
}
