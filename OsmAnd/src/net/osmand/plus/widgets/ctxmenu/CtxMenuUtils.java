package net.osmand.plus.widgets.ctxmenu;

import android.text.TextUtils;

import androidx.annotation.NonNull;

import net.osmand.plus.OsmandApplication;
import net.osmand.plus.settings.backend.OsmAndAppCustomization;
import net.osmand.plus.settings.backend.OsmandSettings;
import net.osmand.plus.settings.backend.preferences.ContextMenuItemsPreference;
import net.osmand.plus.widgets.ctxmenu.data.ContextMenuItem;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static net.osmand.aidlapi.OsmAndCustomizationConstants.APP_PROFILES_ID;
import static net.osmand.aidlapi.OsmAndCustomizationConstants.DRAWER_CONFIGURE_PROFILE_ID;
import static net.osmand.aidlapi.OsmAndCustomizationConstants.DRAWER_SWITCH_PROFILE_ID;

public class CtxMenuUtils {

	public static List<String> getNames(@NonNull List<ContextMenuItem> items) {
		List<String> itemNames = new ArrayList<>();
		for (ContextMenuItem item : items) {
			itemNames.add(item.getTitle());
		}
		return itemNames;
	}

	public static void removeHiddenItems(@NonNull ContextMenuAdapter adapter) {
		OsmandApplication app = adapter.getApplication();
		List<ContextMenuItem> items = adapter.getItems();
		OsmAndAppCustomization custom = app.getAppCustomization();
		Set<ContextMenuItem> hidden = new HashSet<>();
		for (ContextMenuItem item : items) {
			String id = item.getId();
			boolean hiddenInCustomization = !TextUtils.isEmpty(id) && !custom.isFeatureEnabled(id);
			if (item.isHidden() || hiddenInCustomization) {
				hidden.add(item);
			}
		}
		items.removeAll(hidden);
	}

	public static void hideExtraDividers(@NonNull ContextMenuAdapter adapter) {
		List<ContextMenuItem> items = adapter.getItems();
		for (int i = 0; i < items.size() - 1; i++) {
			ContextMenuItem item = items.get(i);
			if (!item.shouldHideDivider()) {
				// Hide divider before next category
				ContextMenuItem next = items.get(i + 1);
				item.setHideDivider(next.isCategory());
			}
		}
		items.get(items.size() - 1).setHideDivider(true);
	}

	public static Map<ContextMenuItem, List<ContextMenuItem>> collectItemsByCategories(@NonNull List<ContextMenuItem> items) {
		ContextMenuItem c = null;
		Map<ContextMenuItem, List<ContextMenuItem>> result = new LinkedHashMap<>();
		for (ContextMenuItem item : items) {
			if (item.isCategory()) {
				c = item;
				result.put(c, new ArrayList<>());
			} else if (c != null) {
				List<ContextMenuItem> list = result.get(c);
				if (list != null) {
					list.add(item);
				}
			} else {
				result.put(item, null);
			}
		}
		return result;
	}

	public static List<ContextMenuItem> getCustomizableItems(@NonNull ContextMenuAdapter adapter) {
		List<ContextMenuItem> result = new ArrayList<>();
		for (ContextMenuItem item : getDefaultItems(adapter)) {
			if (!APP_PROFILES_ID.equals(item.getId())
					&& !DRAWER_CONFIGURE_PROFILE_ID.equals(item.getId())
					&& !DRAWER_SWITCH_PROFILE_ID.equals(item.getId())) {
				result.add(item);
			}
		}
		return result;
	}

	private static List<ContextMenuItem> getDefaultItems(@NonNull ContextMenuAdapter adapter) {
		String idScheme = getIdScheme(adapter);
		List<ContextMenuItem> result = new ArrayList<>();
		for (ContextMenuItem item : adapter.getItems()) {
			String id = item.getId();
			if (id != null && (id.startsWith(idScheme))) {
				result.add(item);
			}
		}
		return result;
	}

	private static String getIdScheme(@NonNull ContextMenuAdapter adapter) {
		String idScheme = "";
		for (ContextMenuItem item : adapter.getItems()) {
			String id = item.getId();
			if (id != null) {
				OsmandApplication app = adapter.getApplication();
				OsmandSettings settings = app.getSettings();
				ContextMenuItemsPreference pref = settings.getContextMenuItemsPreference(id);
				if (pref != null) {
					return pref.getIdScheme();
				}
			}
		}
		return idScheme;
	}

}
