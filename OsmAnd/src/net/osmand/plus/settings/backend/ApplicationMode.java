package net.osmand.plus.settings.backend;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.annotations.Expose;

import net.osmand.StateChangedListener;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.profiles.LocationIcon;
import net.osmand.plus.profiles.NavigationIcon;
import net.osmand.plus.profiles.ProfileIconColors;
import net.osmand.plus.routing.RouteService;
import net.osmand.plus.settings.backend.OsmAndAppCustomization.OsmAndAppCustomizationListener;
import net.osmand.util.Algorithms;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import androidx.annotation.ColorInt;
import androidx.annotation.DrawableRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;

import static net.osmand.plus.views.mapwidgets.WidgetParams.ALTITUDE;
import static net.osmand.plus.views.mapwidgets.WidgetParams.BATTERY;
import static net.osmand.plus.views.mapwidgets.WidgetParams.BEARING;
import static net.osmand.plus.views.mapwidgets.WidgetParams.CURRENT_SPEED;
import static net.osmand.plus.views.mapwidgets.WidgetParams.DISTANCE_TO_DESTINATION;
import static net.osmand.plus.views.mapwidgets.WidgetParams.GPS_INFO;
import static net.osmand.plus.views.mapwidgets.WidgetParams.INTERMEDIATE_DISTANCE;
import static net.osmand.plus.views.mapwidgets.WidgetParams.INTERMEDIATE_TIME;
import static net.osmand.plus.views.mapwidgets.WidgetParams.MAX_SPEED;
import static net.osmand.plus.views.mapwidgets.WidgetParams.NAVIGATION_TIME;
import static net.osmand.plus.views.mapwidgets.WidgetParams.NEXT_TURN;
import static net.osmand.plus.views.mapwidgets.WidgetParams.SMALL_NEXT_TURN;
import static net.osmand.plus.views.mapwidgets.WidgetParams.PLAIN_TIME;
import static net.osmand.plus.views.mapwidgets.WidgetParams.RADIUS_RULER;
import static net.osmand.plus.views.mapwidgets.WidgetParams.SECOND_NEXT_TURN;
import static net.osmand.plus.views.mapwidgets.WidgetParams.SIDE_MARKER_1;

public class ApplicationMode {

	private static final Map<String, Set<ApplicationMode>> widgetsVisibilityMap = new LinkedHashMap<>();
	private static final Map<String, Set<ApplicationMode>> widgetsAvailabilityMap = new LinkedHashMap<>();

	private static List<ApplicationMode> defaultValues = new ArrayList<>();
	private static List<ApplicationMode> values = new ArrayList<>();
	private static List<ApplicationMode> cachedFilteredValues = new ArrayList<>();

	private static StateChangedListener<String> listener;
	private static StateChangedListener<String> iconNameListener;
	private static OsmAndAppCustomization.OsmAndAppCustomizationListener customizationListener;

	private OsmandApplication app;

	private final int keyName;
	private final String stringKey;
	private int descriptionId;

	private ApplicationMode parentAppMode;
	private int iconRes = R.drawable.ic_world_globe_dark;

	private ApplicationMode(int key, String stringKey) {
		this.keyName = key;
		this.stringKey = stringKey;
	}

	/*
	 * DEFAULT("Browse map"), CAR("Car"), BICYCLE("Bicycle"), PEDESTRIAN("Pedestrian"); NAUTICAL("boat"); PUBLIC_TRANSPORT("Public transport"); AIRCRAFT("Aircraft")
	 */
	public static final ApplicationMode DEFAULT = createBase(R.string.app_mode_default, "default")
			.icon(R.drawable.ic_world_globe_dark).reg();

	public static final ApplicationMode CAR = createBase(R.string.app_mode_car, "car")
			.icon(R.drawable.ic_action_car_dark)
			.description(R.string.base_profile_descr_car).reg();

	public static final ApplicationMode BICYCLE = createBase(R.string.app_mode_bicycle, "bicycle")
			.icon(R.drawable.ic_action_bicycle_dark)
			.description(R.string.base_profile_descr_bicycle).reg();

	public static final ApplicationMode PEDESTRIAN = createBase(R.string.app_mode_pedestrian, "pedestrian")
			.icon(R.drawable.ic_action_pedestrian_dark)
			.description(R.string.base_profile_descr_pedestrian).reg();

	public static final ApplicationMode TRUCK = create(ApplicationMode.CAR, R.string.app_mode_truck, "truck")
			.icon(R.drawable.ic_action_truck_dark)
			.description(R.string.app_mode_truck).reg();

	public static final ApplicationMode MOTORCYCLE = create(ApplicationMode.CAR, R.string.app_mode_motorcycle, "motorcycle")
			.icon(R.drawable.ic_action_motorcycle_dark)
			.description(R.string.app_mode_motorcycle).reg();

	public static final ApplicationMode PUBLIC_TRANSPORT = createBase(R.string.app_mode_public_transport, "public_transport")
			.icon(R.drawable.ic_action_bus_dark)
			.description(R.string.base_profile_descr_public_transport).reg();

	public static final ApplicationMode BOAT = createBase(R.string.app_mode_boat, "boat")
			.icon(R.drawable.ic_action_sail_boat_dark)
			.description(R.string.base_profile_descr_boat).reg();

	public static final ApplicationMode AIRCRAFT = createBase(R.string.app_mode_aircraft, "aircraft")
			.icon(R.drawable.ic_action_aircraft)
			.description(R.string.base_profile_descr_aircraft).reg();

	public static final ApplicationMode SKI = createBase(R.string.app_mode_skiing, "ski")
			.icon(R.drawable.ic_action_skiing)
			.description(R.string.base_profile_descr_ski).reg();

	public static final ApplicationMode HORSE = createBase(R.string.horseback_riding, "horse")
			.icon(R.drawable.ic_action_horse)
			.description(R.string.horseback_riding).reg();

	public static List<ApplicationMode> values(@NonNull OsmandApplication app) {
		if (customizationListener == null) {
			customizationListener = new OsmAndAppCustomizationListener() {
				@Override
				public void onOsmAndSettingsCustomized() {
					cachedFilteredValues = new ArrayList<>();
				}
			};
			app.getAppCustomization().addListener(customizationListener);
		}
		if (cachedFilteredValues.isEmpty()) {

			OsmandSettings settings = app.getSettings();
			if (listener == null) {
				listener = new StateChangedListener<String>() {
					@Override
					public void stateChanged(String change) {
						cachedFilteredValues = new ArrayList<>();
					}
				};
				settings.AVAILABLE_APP_MODES.addListener(listener);
			}
			String available = settings.AVAILABLE_APP_MODES.get();
			cachedFilteredValues = new ArrayList<>();
			for (ApplicationMode v : values) {
				if (available.contains(v.getStringKey() + ",") || v == DEFAULT) {
					cachedFilteredValues.add(v);
				}
			}
		}
		return cachedFilteredValues;
	}

	public static List<ApplicationMode> allPossibleValues() {
		return values;
	}

	public static List<ApplicationMode> getDefaultValues() {
		return defaultValues;
	}

	public static List<ApplicationMode> getCustomValues() {
		List<ApplicationMode> customModes = new ArrayList<>();
		for (ApplicationMode mode : values) {
			if (mode.isCustomProfile()) {
				customModes.add(mode);
			}
		}
		return customModes;
	}

	public static ApplicationMode valueOfStringKey(String key, ApplicationMode def) {
		for (ApplicationMode p : values) {
			if (p.getStringKey().equals(key)) {
				return p;
			}
		}
		return def;
	}

	public static List<ApplicationMode> getModesDerivedFrom(ApplicationMode am) {
		List<ApplicationMode> list = new ArrayList<ApplicationMode>();
		for (ApplicationMode a : values) {
			if (a == am || a.getParent() == am) {
				list.add(a);
			}
		}
		return list;
	}

	private static void initRegVisibility() {
		// CAR, BICYCLE, PEDESTRIAN, PUBLIC_TRANSPORT, BOAT, AIRCRAFT, SKI, TRUCK, HORSE
		ApplicationMode[] exceptDefault = new ApplicationMode[] {CAR, BICYCLE, PEDESTRIAN,
				PUBLIC_TRANSPORT, BOAT, AIRCRAFT, SKI, TRUCK, MOTORCYCLE, HORSE};
		ApplicationMode[] all = null;
		ApplicationMode[] none = new ApplicationMode[] {};

		// left
		ApplicationMode[] navigationSet1 = new ApplicationMode[] {CAR, BICYCLE, BOAT, SKI, TRUCK, MOTORCYCLE, HORSE};
		ApplicationMode[] navigationSet2 = new ApplicationMode[] {PEDESTRIAN, PUBLIC_TRANSPORT, AIRCRAFT};

		regWidgetVisibility(NEXT_TURN.id, navigationSet1);
		regWidgetVisibility(SMALL_NEXT_TURN.id, navigationSet2);
		regWidgetVisibility(SECOND_NEXT_TURN.id, navigationSet1);
		regWidgetAvailability(NEXT_TURN.id, exceptDefault);
		regWidgetAvailability(SMALL_NEXT_TURN.id, exceptDefault);
		regWidgetAvailability(SECOND_NEXT_TURN.id, exceptDefault);

		// right
		regWidgetVisibility(INTERMEDIATE_DISTANCE.id, all);
		regWidgetVisibility(DISTANCE_TO_DESTINATION.id, all);
		regWidgetVisibility(NAVIGATION_TIME.id, all);
		regWidgetVisibility(INTERMEDIATE_TIME.id, all);
		regWidgetVisibility(CURRENT_SPEED.id, CAR, BICYCLE, BOAT, SKI, PUBLIC_TRANSPORT, AIRCRAFT, TRUCK,
				MOTORCYCLE, HORSE);
		regWidgetVisibility(MAX_SPEED.id, CAR, TRUCK, MOTORCYCLE);
		regWidgetVisibility(ALTITUDE.id, PEDESTRIAN, BICYCLE);
		regWidgetAvailability(INTERMEDIATE_DISTANCE.id, all);
		regWidgetAvailability(DISTANCE_TO_DESTINATION.id, all);
		regWidgetAvailability(NAVIGATION_TIME.id, all);
		regWidgetAvailability(INTERMEDIATE_TIME.id, all);
		regWidgetAvailability(CURRENT_SPEED.id, all);
		regWidgetAvailability(MAX_SPEED.id, all);
		regWidgetAvailability(ALTITUDE.id, all);

		// all = null everything
		regWidgetAvailability(SIDE_MARKER_1.id, none);
		regWidgetAvailability(SIDE_MARKER_1.id, none);
		regWidgetAvailability(GPS_INFO.id, all);
		regWidgetAvailability(BATTERY.id, all);
		regWidgetAvailability(BEARING.id, all);
		regWidgetAvailability(RADIUS_RULER.id, all);
		regWidgetAvailability(PLAIN_TIME.id, all);
	}

	// returns modifiable ! Set<ApplicationMode> to exclude non-wanted derived
	public static Set<ApplicationMode> regWidgetVisibility(@NonNull String widgetId,
	                                                       @Nullable ApplicationMode... appModes) {
		HashSet<ApplicationMode> set = new HashSet<>();
		if (appModes == null) {
			set.addAll(values);
		} else {
			Collections.addAll(set, appModes);
		}
		for (ApplicationMode m : values) {
			// add derived modes
			if (set.contains(m.getParent())) {
				set.add(m);
			}
		}
		widgetsVisibilityMap.put(widgetId, set);
		return set;
	}

	public boolean isWidgetVisibleByDefault(@NonNull String widgetId) {
		if (app.getAppCustomization().areWidgetsCustomized()) {
			return app.getAppCustomization().isWidgetVisible(widgetId, this);
		}
		Set<ApplicationMode> widgetsVisibility = widgetsVisibilityMap.get(widgetId);
		return widgetsVisibility != null && widgetsVisibility.contains(this);
	}

	public static Set<ApplicationMode> regWidgetAvailability(@NonNull String widgetId,
	                                                         @Nullable ApplicationMode... appModes) {
		HashSet<ApplicationMode> set = new HashSet<>();
		if (appModes == null) {
			set.addAll(values);
		} else {
			Collections.addAll(set, appModes);
		}
		for (ApplicationMode m : values) {
			// add derived modes
			if (set.contains(m.getParent())) {
				set.add(m);
			}
		}
		widgetsAvailabilityMap.put(widgetId, set);
		return set;
	}

	public boolean isWidgetAvailable(String key) {
		if (app.getAppCustomization().areWidgetsCustomized()) {
			return app.getAppCustomization().isWidgetAvailable(key, this);
		}
		Set<ApplicationMode> set = widgetsAvailabilityMap.get(key);
		if (set == null) {
			return true;
		}
		return set.contains(this);
	}

	public String getStringKey() {
		return stringKey;
	}

	public boolean isCustomProfile() {
		for (ApplicationMode mode : defaultValues) {
			if (Algorithms.stringsEqual(mode.getStringKey(), getStringKey())) {
				return false;
			}
		}
		return true;
	}

	public boolean isDerivedRoutingFrom(ApplicationMode mode) {
		return this == mode || getParent() == mode;
	}

	public ApplicationMode getParent() {
		return parentAppMode;
	}

	public void setParentAppMode(ApplicationMode parentAppMode) {
		if (isCustomProfile()) {
			this.parentAppMode = parentAppMode;
			app.getSettings().PARENT_APP_MODE.setModeValue(this, parentAppMode.getStringKey());
		}
	}

	public int getNameKeyResource() {
		return keyName;
	}

	public String toHumanString() {
		String userProfileName = getUserProfileName();
		if (Algorithms.isEmpty(userProfileName)) {
			if (keyName != -1) {
				return app.getString(keyName);
			} else {
				return Algorithms.capitalizeFirstLetter(getStringKey());
			}
		} else {
			return userProfileName;
		}
	}

	public String getDescription() {
		if (descriptionId != 0) {
			return app.getString(descriptionId);
		}
		return "";
	}

	@DrawableRes
	public int getIconRes() {
		return iconRes;
	}

	public void setIconResName(String iconResName) {
		if (!Algorithms.isEmpty(iconResName)) {
			app.getSettings().ICON_RES_NAME.setModeValue(this, iconResName);
		}
	}

	private void updateAppModeIcon() {
		String iconResName = app.getSettings().ICON_RES_NAME.getModeValue(this);
		try {
			int iconRes = app.getResources().getIdentifier(iconResName, "drawable", app.getPackageName());
			if (iconRes != 0) {
				this.iconRes = iconRes;
			}
		} catch (Exception e) {
//				return R.drawable.map_world_globe_dark;
		}
	}

	public String getIconName() {
		return app.getSettings().ICON_RES_NAME.getModeValue(this);
	}

	public int getMinDistanceForTurn() {
		// used to be: 50 kmh - 35 m, 10 kmh - 15 m, 4 kmh - 5 m, 10 kmh - 20 m, 400 kmh - 100 m,
		float speed = Math.max(getDefaultSpeed(), 0.3f);
		// 2 sec + 7 m: 50 kmh - 35 m, 10 kmh - 12 m, 4 kmh - 9 m, 400 kmh - 230 m
		return (int) (7 + speed * 2);
	}


	public boolean hasFastSpeed() {
		return getDefaultSpeed() > 10;
	}

	public float getDefaultSpeed() {
		return app.getSettings().DEFAULT_SPEED.getModeValue(this);
	}

	public void setDefaultSpeed(float defaultSpeed) {
		app.getSettings().DEFAULT_SPEED.setModeValue(this, defaultSpeed);
	}

	public void resetDefaultSpeed() {
		app.getSettings().DEFAULT_SPEED.resetModeToDefault(this);
	}

	public float getMinSpeed() {
		return app.getSettings().MIN_SPEED.getModeValue(this);
	}

	public void setMinSpeed(float defaultSpeed) {
		app.getSettings().MIN_SPEED.setModeValue(this, defaultSpeed);
	}

	public float getMaxSpeed() {
		return app.getSettings().MAX_SPEED.getModeValue(this);
	}

	public void setMaxSpeed(float defaultSpeed) {
		app.getSettings().MAX_SPEED.setModeValue(this, defaultSpeed);
	}

	public float getStrAngle() {
		return app.getSettings().ROUTE_STRAIGHT_ANGLE.getModeValue(this);
	}

	public void setStrAngle(float angle) {
		app.getSettings().ROUTE_STRAIGHT_ANGLE.setModeValue(this, angle);
	}

	public String getUserProfileName() {
		return app.getSettings().USER_PROFILE_NAME.getModeValue(this);
	}

	public void setUserProfileName(String userProfileName) {
		if (!Algorithms.isEmpty(userProfileName)) {
			app.getSettings().USER_PROFILE_NAME.setModeValue(this, userProfileName);
		}
	}

	public String getRoutingProfile() {
		return app.getSettings().ROUTING_PROFILE.getModeValue(this);
	}

	public String getDefaultRoutingProfile() {
		return app.getSettings().ROUTING_PROFILE.getProfileDefaultValue(this);
	}

	public void setRoutingProfile(String routingProfile) {
		if (!Algorithms.isEmpty(routingProfile)) {
			app.getSettings().ROUTING_PROFILE.setModeValue(this, routingProfile);
		}
	}

	public RouteService getRouteService() {
		return app.getSettings().ROUTE_SERVICE.getModeValue(this);
	}

	public void setRouteService(RouteService routeService) {
		if (routeService != null) {
			app.getSettings().ROUTE_SERVICE.setModeValue(this, routeService);
		}
	}

	public NavigationIcon getNavigationIcon() {
		return app.getSettings().NAVIGATION_ICON.getModeValue(this);
	}

	public void setNavigationIcon(NavigationIcon navigationIcon) {
		if (navigationIcon != null) {
			app.getSettings().NAVIGATION_ICON.setModeValue(this, navigationIcon);
		}
	}

	public LocationIcon getLocationIcon() {
		return app.getSettings().LOCATION_ICON.getModeValue(this);
	}

	@ColorInt
	public int getProfileColor(boolean nightMode) {
		Integer customProfileColor = getCustomIconColor();
		if (customProfileColor != null) {
			return customProfileColor;
		}
		return ContextCompat.getColor(app, getIconColorInfo().getColor(nightMode));
	}

	public void setLocationIcon(LocationIcon locationIcon) {
		if (locationIcon != null) {
			app.getSettings().LOCATION_ICON.setModeValue(this, locationIcon);
		}
	}

	public ProfileIconColors getIconColorInfo() {
		return app.getSettings().ICON_COLOR.getModeValue(this);
	}

	public void setIconColor(ProfileIconColors iconColor) {
		if (iconColor != null) {
			app.getSettings().ICON_COLOR.setModeValue(this, iconColor);
		}
	}

	public List<String> getCustomIconColors() {
		return app.getSettings().CUSTOM_ICON_COLORS.getStringsListForProfile(this);
	}

	public void setCustomIconColors(List<String> customColors) {
		app.getSettings().CUSTOM_ICON_COLORS.setModeValues(this, customColors);
	}

	public Integer getCustomIconColor() {
		try {
			String customColor = app.getSettings().CUSTOM_ICON_COLOR.getModeValue(this);
			return Algorithms.isEmpty(customColor) ? null : Algorithms.parseColor(customColor);
		} catch (IllegalArgumentException e) {
			return null;
		}
	}

	public void setCustomIconColor(Integer customIconColor) {
		String valueToSave = customIconColor == null ? null : Algorithms.colorToString(customIconColor);
		app.getSettings().CUSTOM_ICON_COLOR.setModeValue(this, valueToSave);
	}

	public int getOrder() {
		return app.getSettings().APP_MODE_ORDER.getModeValue(this);
	}

	public void setOrder(int order) {
		app.getSettings().APP_MODE_ORDER.setModeValue(this, order);
	}

	public int getVersion() {
		return app.getSettings().APP_MODE_VERSION.getModeValue(this);
	}

	public void setVersion(int version) {
		app.getSettings().APP_MODE_VERSION.setModeValue(this, version);
	}

	public static void onApplicationStart(OsmandApplication app) {
		initCustomModes(app);
		initModesParams(app);
		initRegVisibility();
		reorderAppModes();
	}

	private static void initModesParams(final OsmandApplication app) {
		OsmandSettings settings = app.getSettings();
		if (iconNameListener == null) {
			iconNameListener = new StateChangedListener<String>() {
				@Override
				public void stateChanged(String change) {
					app.runInUIThread(() -> {
						List<ApplicationMode> modes = new ArrayList<>(allPossibleValues());
						for (ApplicationMode mode : modes) {
							mode.updateAppModeIcon();
						}
					});
				}
			};
			settings.ICON_RES_NAME.addListener(iconNameListener);
		}
		for (ApplicationMode mode : allPossibleValues()) {
			mode.app = app;
			mode.updateAppModeIcon();
		}
		if (settings.APP_MODE_ORDER.isSetForMode(PEDESTRIAN)) {
			if (!settings.APP_MODE_ORDER.isSetForMode(TRUCK)) {
				TRUCK.setOrder(PEDESTRIAN.getOrder() + 1);
			}
			if (!settings.APP_MODE_ORDER.isSetForMode(MOTORCYCLE)) {
				MOTORCYCLE.setOrder(PEDESTRIAN.getOrder() + 1);
			}
		}
		if (settings.APP_MODE_ORDER.isSetForMode(SKI) && !settings.APP_MODE_ORDER.isSetForMode(HORSE)) {
			HORSE.setOrder(SKI.getOrder() + 1);
		}
	}

	private static void initCustomModes(OsmandApplication app) {
		OsmandSettings settings = app.getSettings();
		for (String appModeKey : settings.getCustomAppModesKeys()) {
			Object profilePreferences = settings.getProfilePreferences(appModeKey);
			String parent = settings.PARENT_APP_MODE.getValue(profilePreferences, null);
			int order = settings.APP_MODE_ORDER.getValue(profilePreferences, values.size());

			ApplicationModeBuilder builder = createCustomMode(valueOfStringKey(parent, CAR), appModeKey, app);
			builder.setOrder(order);
			builder.customReg();
		}
	}

	public static void reorderAppModes() {
		Comparator<ApplicationMode> comparator = new Comparator<ApplicationMode>() {
			@Override
			public int compare(ApplicationMode mode1, ApplicationMode mode2) {
				return (mode1.getOrder() < mode2.getOrder()) ? -1 : ((mode1.getOrder() == mode2.getOrder()) ? 0 : 1);
			}
		};
		Collections.sort(values, comparator);
		Collections.sort(defaultValues, comparator);
		Collections.sort(cachedFilteredValues, comparator);
		updateAppModesOrder();
	}

	private static void updateAppModesOrder() {
		for (int i = 0; i < values.size(); i++) {
			values.get(i).setOrder(i);
		}
	}

	private static void saveCustomAppModesToSettings(OsmandApplication app) {
		OsmandSettings settings = app.getSettings();
		StringBuilder stringBuilder = new StringBuilder();
		Iterator<ApplicationMode> it = ApplicationMode.getCustomValues().iterator();
		while (it.hasNext()) {
			stringBuilder.append(it.next().getStringKey());
			if (it.hasNext()) {
				stringBuilder.append(",");
			}
		}
		if (!stringBuilder.toString().equals(settings.CUSTOM_APP_MODES_KEYS.get())) {
			settings.CUSTOM_APP_MODES_KEYS.set(stringBuilder.toString());
		}
	}

	public static ApplicationMode saveProfile(ApplicationModeBuilder builder, OsmandApplication app) {
		ApplicationMode mode = ApplicationMode.valueOfStringKey(builder.applicationMode.stringKey, null);
		if (mode != null) {
			mode.setParentAppMode(builder.applicationMode.parentAppMode);
			mode.setIconResName(builder.iconResName);
			mode.setUserProfileName(builder.userProfileName);
			mode.setRoutingProfile(builder.routingProfile);
			mode.setRouteService(builder.routeService);
			mode.setIconColor(builder.iconColor);
			mode.setCustomIconColor(builder.customIconColor);
			mode.setLocationIcon(builder.locationIcon);
			mode.setNavigationIcon(builder.navigationIcon);
			mode.setOrder(builder.order);
			mode.setVersion(builder.version);
		} else {
			mode = builder.customReg();
			initRegVisibility();
		}
		reorderAppModes();
		saveCustomAppModesToSettings(app);
		return mode;
	}

	public static ApplicationModeBean fromJson(String json) {
		Gson gson = new GsonBuilder().excludeFieldsWithoutExposeAnnotation().create();
		return gson.fromJson(json, ApplicationModeBean.class);
	}

	public static ApplicationModeBuilder fromModeBean(OsmandApplication app, ApplicationModeBean modeBean) {
		ApplicationModeBuilder builder = createCustomMode(valueOfStringKey(modeBean.parent, null), modeBean.stringKey, app);
		builder.setUserProfileName(modeBean.userProfileName);
		builder.setIconResName(modeBean.iconName);
		builder.setIconColor(modeBean.iconColor);
		builder.setCustomIconColor(modeBean.customIconColor);
		builder.setRoutingProfile(modeBean.routingProfile);
		builder.setRouteService(modeBean.routeService);
		builder.setLocationIcon(modeBean.locIcon);
		builder.setNavigationIcon(modeBean.navIcon);
		builder.setOrder(modeBean.order);
		builder.setVersion(modeBean.version);

		return builder;
	}

	public String toJson() {
		Gson gson = new GsonBuilder().excludeFieldsWithoutExposeAnnotation().create();
		return gson.toJson(toModeBean());
	}

	public ApplicationModeBean toModeBean() {
		ApplicationModeBean mb = new ApplicationModeBean();
		mb.stringKey = stringKey;
		mb.userProfileName = getUserProfileName();
		mb.iconColor = getIconColorInfo();
		mb.customIconColor = getCustomIconColor();
		mb.iconName = getIconName();
		mb.parent = parentAppMode != null ? parentAppMode.getStringKey() : null;
		mb.routeService = getRouteService();
		mb.routingProfile = getRoutingProfile();
		mb.locIcon = getLocationIcon();
		mb.navIcon = getNavigationIcon();
		mb.order = getOrder();
		mb.version = getVersion();
		return mb;
	}

	public static void deleteCustomModes(List<ApplicationMode> modes, OsmandApplication app) {
		Iterator<ApplicationMode> it = values.iterator();
		while (it.hasNext()) {
			ApplicationMode m = it.next();
			if (modes.contains(m)) {
				it.remove();
			}
		}
		OsmandSettings settings = app.getSettings();
		if (modes.contains(settings.APPLICATION_MODE.get())) {
			settings.APPLICATION_MODE.resetToDefault();
		}
		cachedFilteredValues.removeAll(modes);
		saveCustomAppModesToSettings(app);
	}

	public static boolean changeProfileAvailability(ApplicationMode mode, boolean isSelected, OsmandApplication app) {
		Set<ApplicationMode> selectedModes = new LinkedHashSet<>(ApplicationMode.values(app));
		StringBuilder vls = new StringBuilder(ApplicationMode.DEFAULT.getStringKey() + ",");
		if (allPossibleValues().contains(mode)) {
			OsmandSettings settings = app.getSettings();
			if (isSelected) {
				selectedModes.add(mode);
			} else {
				selectedModes.remove(mode);
				if (settings.APPLICATION_MODE.get() == mode) {
					settings.APPLICATION_MODE.resetToDefault();
				}
			}
			for (ApplicationMode m : selectedModes) {
				vls.append(m.getStringKey()).append(",");
			}
			settings.AVAILABLE_APP_MODES.set(vls.toString());
			return true;
		}
		return false;
	}

	private static ApplicationModeBuilder create(ApplicationMode parent, int key, String stringKey) {
		ApplicationModeBuilder builder = new ApplicationModeBuilder();
		builder.applicationMode = new ApplicationMode(key, stringKey);
		builder.parent(parent);
		return builder;
	}

	private static ApplicationModeBuilder createBase(int key, String stringKey) {
		ApplicationModeBuilder builder = new ApplicationModeBuilder();
		builder.applicationMode = new ApplicationMode(key, stringKey);
		return builder;
	}

	public static ApplicationModeBuilder createCustomMode(ApplicationMode parent, String stringKey, OsmandApplication app) {
		ApplicationModeBuilder builder = create(parent, -1, stringKey);
		builder.getApplicationMode().app = app;
		return builder;
	}

	public static class ApplicationModeBuilder {

		private ApplicationMode applicationMode;

		private String userProfileName;
		private RouteService routeService;
		private String routingProfile;
		private String iconResName;
		private ProfileIconColors iconColor;
		private Integer customIconColor;
		private LocationIcon locationIcon;
		private NavigationIcon navigationIcon;
		private int order = -1;
		private int version = -1;

		public ApplicationMode getApplicationMode() {
			return applicationMode;
		}

		private ApplicationMode reg() {
			values.add(applicationMode);
			defaultValues.add(applicationMode);
			return applicationMode;
		}

		private ApplicationMode customReg() {
			values.add(applicationMode);

			ApplicationMode parent = applicationMode.parentAppMode;
			applicationMode.setParentAppMode(parent);
			applicationMode.setUserProfileName(userProfileName);
			applicationMode.setRouteService(routeService);
			applicationMode.setRoutingProfile(routingProfile);
			applicationMode.setIconResName(iconResName);
			applicationMode.setCustomIconColor(customIconColor);
			applicationMode.setIconColor(iconColor);
			applicationMode.setLocationIcon(locationIcon);
			applicationMode.setNavigationIcon(navigationIcon);
			applicationMode.setOrder(order != -1 ? order : values.size());
			applicationMode.setVersion(version);

			return applicationMode;
		}

		public ApplicationModeBuilder icon(int iconRes) {
			applicationMode.iconRes = iconRes;
			return this;
		}

		public ApplicationModeBuilder description(int strId) {
			applicationMode.descriptionId = strId;
			return this;
		}

		public ApplicationModeBuilder parent(ApplicationMode parent) {
			applicationMode.parentAppMode = parent;
			return this;
		}

		public ApplicationModeBuilder setUserProfileName(String userProfileName) {
			this.userProfileName = userProfileName;
			return this;
		}

		public ApplicationModeBuilder setRouteService(RouteService routeService) {
			this.routeService = routeService;
			return this;
		}

		public ApplicationModeBuilder setRoutingProfile(String routingProfile) {
			this.routingProfile = routingProfile;
			return this;
		}

		public ApplicationModeBuilder setIconResName(String iconResName) {
			this.iconResName = iconResName;
			return this;
		}

		public ApplicationModeBuilder setIconColor(ProfileIconColors iconColor) {
			this.iconColor = iconColor;
			return this;
		}

		public ApplicationModeBuilder setCustomIconColor(Integer customIconColor) {
			this.customIconColor = customIconColor;
			return this;
		}

		public ApplicationModeBuilder setOrder(int order) {
			this.order = order;
			return this;
		}

		public ApplicationModeBuilder setVersion(int version) {
			this.version = version;
			return this;
		}

		public ApplicationModeBuilder setLocationIcon(LocationIcon locIcon) {
			this.locationIcon = locIcon;
			return this;
		}

		public ApplicationModeBuilder setNavigationIcon(NavigationIcon navIcon) {
			this.navigationIcon = navIcon;
			return this;
		}
	}

	public static class ApplicationModeBean {
		@Expose
		public String stringKey;
		@Expose
		public String userProfileName;
		@Expose
		public String parent;
		@Expose
		public String iconName = "map_world_globe_dark";
		@Expose
		public ProfileIconColors iconColor = ProfileIconColors.DEFAULT;
		@Expose
		public Integer customIconColor = null;
		@Expose
		public String routingProfile = null;
		@Expose
		public RouteService routeService = RouteService.OSMAND;
		@Expose
		public LocationIcon locIcon = null;
		@Expose
		public NavigationIcon navIcon = null;
		@Expose
		public int order = -1;
		@Expose
		public int version = -1;
	}
}