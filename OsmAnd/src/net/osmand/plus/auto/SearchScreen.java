package net.osmand.plus.auto;

import static androidx.car.app.constraints.ConstraintManager.CONTENT_LIMIT_TYPE_LIST;

import android.graphics.drawable.Drawable;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.car.app.CarContext;
import androidx.car.app.Screen;
import androidx.car.app.constraints.ConstraintManager;
import androidx.car.app.model.Action;
import androidx.car.app.model.ItemList;
import androidx.car.app.model.Row;
import androidx.car.app.model.SearchTemplate;
import androidx.car.app.model.SearchTemplate.SearchCallback;
import androidx.car.app.model.Template;
import androidx.lifecycle.DefaultLifecycleObserver;
import androidx.lifecycle.LifecycleOwner;

import net.osmand.PlatformUtil;
import net.osmand.data.LatLon;
import net.osmand.data.PointDescription;
import net.osmand.osm.AbstractPoiType;
import net.osmand.plus.AppInitializer;
import net.osmand.plus.AppInitializer.AppInitializeListener;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.helpers.TargetPointsHelper;
import net.osmand.plus.helpers.TargetPointsHelper.TargetPoint;
import net.osmand.plus.auto.SearchHelper.SearchHelperListener;
import net.osmand.plus.mapmarkers.MapMarker;
import net.osmand.plus.poi.PoiUIFilter;
import net.osmand.plus.routing.RoutingHelper;
import net.osmand.plus.search.QuickSearchHelper.SearchHistoryAPI;
import net.osmand.plus.search.listitems.QuickSearchListItem;
import net.osmand.plus.settings.backend.ApplicationMode;
import net.osmand.plus.settings.backend.preferences.OsmandPreference;
import net.osmand.search.SearchUICore;
import net.osmand.search.SearchUICore.SearchResultCollection;
import net.osmand.search.core.ObjectType;
import net.osmand.search.core.SearchPhrase;
import net.osmand.search.core.SearchResult;
import net.osmand.search.core.SearchWord;
import net.osmand.util.Algorithms;

import org.apache.commons.logging.Log;

import java.util.ArrayList;
import java.util.List;

public final class SearchScreen extends Screen implements DefaultLifecycleObserver,
		AppInitializeListener, SearchHelperListener {

	private static final Log LOG = PlatformUtil.getLog(SearchScreen.class);
	private static final int MAP_MARKERS_LIMIT = 3;

	private final SearchHelper searchHelper;

	@NonNull
	private final Action settingsAction;

	@NonNull
	private final SurfaceRenderer surfaceRenderer;

	private ItemList itemList = withNoResults(new ItemList.Builder()).build();

	private String searchText;
	private boolean loading;
	private boolean destroyed;
	private List<SearchResult> recentResults;
	private boolean showResult;

	public SearchScreen(@NonNull CarContext carContext, @NonNull Action settingsAction,
						@NonNull SurfaceRenderer surfaceRenderer) {
		super(carContext);
		ConstraintManager manager = carContext.getCarService(ConstraintManager.class);
		this.searchHelper = new SearchHelper(getApp(), true, manager.getContentLimit(CONTENT_LIMIT_TYPE_LIST));
		this.settingsAction = settingsAction;
		this.surfaceRenderer = surfaceRenderer;

		getLifecycle().addObserver(this);
		getApp().getAppInitializer().addListener(this);
		searchHelper.setListener(this);
		searchHelper.setupSearchSettings(true);
		reloadHistory();
	}

	@NonNull
	public OsmandApplication getApp() {
		return (OsmandApplication) getCarContext().getApplicationContext();
	}

	@NonNull
	public SearchUICore getSearchUICore() {
		return getApp().getSearchUICore().getCore();
	}

	@Override
	public void onDestroy(@NonNull LifecycleOwner owner) {
		getApp().getAppInitializer().removeListener(this);
		getLifecycle().removeObserver(this);
		destroyed = true;
	}

	@NonNull
	@Override
	public Template onGetTemplate() {
		String searchQuery = searchHelper.getSearchQuery();
		String searchHint = searchHelper.getSearchHint();
		SearchTemplate.Builder builder = new SearchTemplate.Builder(
				new SearchCallback() {
					@Override
					public void onSearchTextChanged(@NonNull String searchText) {
						SearchScreen.this.searchText = searchText;
						searchHelper.resetSearchRadius();
						doSearch(searchText);
					}

					@Override
					public void onSearchSubmitted(@NonNull String searchTerm) {
						// When the user presses the search key use the top item in the list
						// as the result and simulate as if the user had pressed that.
						List<SearchResult> searchResults = searchHelper.getSearchResults();
						if (!Algorithms.isEmpty(searchResults)) {
							onClickSearchResult(searchResults.get(0));
						}
					}
				});

		builder.setHeaderAction(Action.BACK)
				.setShowKeyboardByDefault(false)
				.setInitialSearchText(searchQuery == null ? "" : searchQuery);
		if (!Algorithms.isEmpty(searchHint)) {
			builder.setSearchHint(searchHint);
		}
		if (loading || searchHelper.isSearching()) {
			builder.setLoading(true);
		} else {
			builder.setLoading(false);
			if (itemList != null) {
				builder.setItemList(itemList);
			}
		}

		return builder.build();
	}

	private void doSearch(String searchText) {
		if (!getApp().isApplicationInitializing()) {
			if (searchText.isEmpty()) {
				showRecents();
			} else {
				searchHelper.runSearch(searchText);
			}
		}
		invalidate();
	}

	public void onClickSearchResult(@NonNull SearchResult sr) {
		if (sr.objectType == ObjectType.POI
				|| sr.objectType == ObjectType.LOCATION
				|| sr.objectType == ObjectType.HOUSE
				|| sr.objectType == ObjectType.FAVORITE
				|| sr.objectType == ObjectType.MAP_MARKER
				|| sr.objectType == ObjectType.ROUTE
				|| sr.objectType == ObjectType.RECENT_OBJ
				|| sr.objectType == ObjectType.WPT
				|| sr.objectType == ObjectType.STREET_INTERSECTION
				|| sr.objectType == ObjectType.GPX_TRACK) {

			showResult(sr);
		} else {
			if (sr.objectType == ObjectType.CITY || sr.objectType == ObjectType.VILLAGE || sr.objectType == ObjectType.STREET) {
				showResult = true;
			}
			searchHelper.completeQueryWithObject(sr);
			if (sr.object instanceof AbstractPoiType || sr.object instanceof PoiUIFilter) {
				reloadHistory();
			}
			invalidate();
		}
	}

	private void showResult(SearchResult sr) {
		showResult = false;
		getScreenManager().pushForResult(new RoutePreviewScreen(getCarContext(), settingsAction, surfaceRenderer, sr),
				obj -> {
					if (obj != null) {
						SearchScreen.this.onRouteSelected(sr);
					}
				});
	}

	@Override
	public void onClickSearchMore() {
		invalidate();
	}

	@Override
	public void onSearchDone(@NonNull SearchPhrase phrase, @Nullable List<SearchResult> searchResults,
							 @Nullable ItemList itemList, int resultsCount) {
		SearchWord lastSelectedWord = phrase.getLastSelectedWord();
		if (showResult && resultsCount == 0 && lastSelectedWord != null) {
			showResult(lastSelectedWord.getResult());
		} else {
			if (resultsCount > 0) {
				showResult = false;
			}
			this.itemList = itemList;
			invalidate();
		}
	}

	private ItemList.Builder withNoResults(ItemList.Builder builder) {
		return builder.setNoItemsMessage(getCarContext().getString(R.string.search_nothing_found));
	}

	public void reloadHistory() {
		if (getApp().isApplicationInitializing()) {
			loading = true;
		} else {
			reloadHistoryInternal();
		}
	}

	private void reloadHistoryInternal() {
		if (!destroyed) {
			OsmandApplication app = getApp();
			try {
				List<SearchResult> recentResults = new ArrayList<>();

				// Home / work
				/* Disable since points exists at favorites screen
				FavouritesDbHelper favorites = app.getFavorites();
				FavouritePoint homePoint = favorites.getSpecialPoint(FavouritePoint.SpecialPointType.HOME);
				FavouritePoint workPoint = favorites.getSpecialPoint(FavouritePoint.SpecialPointType.WORK);
				if (homePoint != null) {
					SearchResult result = new SearchResult();
					result.location = new LatLon(homePoint.getLatitude(), homePoint.getLongitude());
					result.objectType = ObjectType.FAVORITE;
					result.object = homePoint;
					result.localeName = homePoint.getAddress();
					recentResults.add(result);
				}
				if (workPoint != null) {
					SearchResult result = new SearchResult();
					result.location = new LatLon(workPoint.getLatitude(), workPoint.getLongitude());
					result.objectType = ObjectType.FAVORITE;
					result.object = workPoint;
					result.localeName = workPoint.getAddress();
					recentResults.add(result);
				}
				*/
				// Previous route card
				TargetPointsHelper targetPointsHelper = app.getTargetPointsHelper();
				TargetPoint startPoint = targetPointsHelper.getPointToStartBackup();
				boolean myLocation = false;
				if (startPoint == null) {
					myLocation = true;
					startPoint = targetPointsHelper.getMyLocationToStart();
				}
				TargetPoint destinationPoint = targetPointsHelper.getPointToNavigateBackup();
				if (startPoint != null && destinationPoint != null) {
					StringBuilder startText = new StringBuilder(myLocation ? app.getText(R.string.my_location) : "");
					String startDescr = getPointName(startPoint);
					if (!Algorithms.isEmpty(startDescr)) {
						if (startText.length() > 0) {
							startText.append(" — ");
						}
						startText.append(startDescr);
					}
					String destDescr = getPointName(destinationPoint);
					SearchResult result = new SearchResult();
					result.location = new LatLon(destinationPoint.getLatitude(), destinationPoint.getLongitude());
					result.objectType = ObjectType.ROUTE;
					result.object = destinationPoint;
					result.localeName = destDescr;
					result.relatedObject = startPoint;
					result.localeRelatedObjectName = startText.toString();
					recentResults.add(result);
				}

				// Map markers
				List<MapMarker> mapMarkers = app.getMapMarkersHelper().getMapMarkers();
				int mapMarkersCount = 0;
				for (MapMarker marker : mapMarkers) {
					SearchResult result = new SearchResult();
					result.location = new LatLon(marker.getLatitude(), marker.getLongitude());
					result.objectType = ObjectType.MAP_MARKER;
					result.object = marker;
					result.localeName = marker.getName(app);
					recentResults.add(result);
					mapMarkersCount++;
					if (mapMarkersCount >= MAP_MARKERS_LIMIT) {
						break;
					}
				}

				// History
				SearchResultCollection res = getSearchUICore().shallowSearch(SearchHistoryAPI.class, "", null, false, false);
				if (res != null) {
					recentResults.addAll(res.getCurrentSearchResults());
				}
				this.recentResults = recentResults;
				if (!searchHelper.isSearching() && Algorithms.isEmpty(searchHelper.getSearchQuery())) {
					showRecents();
					invalidate();
				}
			} catch (Exception e) {
				LOG.error(e.getMessage(), e);
				app.showToastMessage(e.getMessage());
			}
		}
	}

	private String getPointName(TargetPoint targetPoint) {
		OsmandApplication app = getApp();
		String name = "";
		if (targetPoint != null) {
			PointDescription description = targetPoint.getOriginalPointDescription();
			if (description != null && !Algorithms.isEmpty(description.getName()) &&
					!description.getName().equals(app.getString(R.string.no_address_found))) {
				name = description.getName();
			} else {
				name = PointDescription.getLocationName(app, targetPoint.point.getLatitude(),
						targetPoint.point.getLongitude(), true).replace('\n', ' ');
			}
		}
		return name;
	}

	private void showRecents() {
		OsmandApplication app = getApp();
		ItemList.Builder itemList = new ItemList.Builder();
		itemList.setNoItemsMessage(getCarContext().getString(R.string.search_nothing_found));
		if (Algorithms.isEmpty(recentResults)) {
			this.itemList = itemList.build();
			return;
		}
		int count = 0;
		for (SearchResult r : recentResults) {
			String name = QuickSearchListItem.getName(app, r);
			if (Algorithms.isEmpty(name)) {
				continue;
			}
			Drawable icon = QuickSearchListItem.getIcon(app, r);
			String typeName = QuickSearchListItem.getTypeName(app, r);
			Row.Builder builder = searchHelper.buildSearchRow(searchHelper.getSearchLocation(),
					r.location, name, icon, typeName);
			if (builder != null) {
				builder.setOnClickListener(() -> onClickSearchResult(r));
				itemList.addItem(builder.build());
				count++;
				if (count >= searchHelper.getContentLimit()) {
					break;
				}
			}
		}
		this.itemList = itemList.build();
	}

	@Override
	public void onStart(AppInitializer init) {
	}

	@Override
	public void onProgress(AppInitializer init, AppInitializer.InitEvents event) {
	}

	@Override
	public void onFinish(AppInitializer init) {
		loading = false;
		if (!destroyed) {
			reloadHistoryInternal();
			if (!Algorithms.isEmpty(searchText)) {
				searchHelper.runSearch(searchText);
			}
			invalidate();
		}
	}

	private void updateApplicationMode(ApplicationMode mode, ApplicationMode next) {
		OsmandApplication app = getApp();
		RoutingHelper routingHelper = app.getRoutingHelper();
		OsmandPreference<ApplicationMode> appMode = app.getSettings().APPLICATION_MODE;
		if (routingHelper.isFollowingMode() && appMode.get() == mode) {
			appMode.set(next);
		}
		routingHelper.setAppMode(next);
		app.initVoiceCommandPlayer(app, next, null, true,
				false, false, true);
		routingHelper.onSettingsChanged(true);
	}

	private void onRouteSelected(@NonNull SearchResult sr) {
		OsmandApplication app = getApp();
		if (sr.objectType == ObjectType.ROUTE) {
			ApplicationMode lastAppMode = app.getSettings().LAST_ROUTE_APPLICATION_MODE.get();
			ApplicationMode currentAppMode = app.getRoutingHelper().getAppMode();
			if (lastAppMode == ApplicationMode.DEFAULT) {
				lastAppMode = currentAppMode;
			}
			updateApplicationMode(currentAppMode, lastAppMode);
			app.getTargetPointsHelper().restoreTargetPoints(true);
		}
		app.getOsmandMap().getMapLayers().getMapControlsLayer().startNavigation();
		finish();
	}
}
