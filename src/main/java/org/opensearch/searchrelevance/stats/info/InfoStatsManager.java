/*
 * SPDX-License-Identifier: Apache-2.0
 *
 * The OpenSearch Contributors require contributions made to
 * this file be licensed under the Apache-2.0 license or a
 * compatible open source license.
 */
package org.opensearch.searchrelevance.stats.info;

import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.opensearch.Version;
import org.opensearch.searchrelevance.settings.SearchRelevanceSettingsAccessor;
import org.opensearch.searchrelevance.stats.common.StatSnapshot;

import lombok.AllArgsConstructor;

/**
 * Manager to generate stat snapshots for cluster level info stats
 */
@AllArgsConstructor
public class InfoStatsManager {
    private SearchRelevanceSettingsAccessor settingsAccessor;

    /**
     * Calculates and gets info stats
     * @param statsToRetrieve a set of the enums to retrieve
     * @return map of stat name to stat snapshot
     */
    public Map<InfoStatName, StatSnapshot<?>> getStats(EnumSet<InfoStatName> statsToRetrieve) {
        // info stats are calculated all at once regardless of filters
        Map<InfoStatName, CountableInfoStatSnapshot> countableInfoStats = getCountableStats();
        Map<InfoStatName, SettableInfoStatSnapshot<?>> settableInfoStats = getSettableStats();

        Map<InfoStatName, StatSnapshot<?>> prefilteredStats = new HashMap<>();
        prefilteredStats.putAll(countableInfoStats);
        prefilteredStats.putAll(settableInfoStats);

        // Filter based on specified stats
        Map<InfoStatName, StatSnapshot<?>> filteredStats = prefilteredStats.entrySet()
            .stream()
            .filter(entry -> statsToRetrieve.contains(entry.getKey()))
            .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));

        return filteredStats;
    }

    /**
     * Calculates and gets info stats
     * @return map of stat name to stat snapshot
     */
    private Map<InfoStatName, CountableInfoStatSnapshot> getCountableStats() {
        // Initialize empty map with keys so stat names are visible in JSON even if the value is not counted
        Map<InfoStatName, CountableInfoStatSnapshot> countableInfoStats = new HashMap<>();
        for (InfoStatName stat : EnumSet.allOf(InfoStatName.class)) {
            if (stat.getStatType() == InfoStatType.INFO_COUNTER) {
                countableInfoStats.put(stat, new CountableInfoStatSnapshot(stat));
            }
        }

        // Helpers to parse search pipeline processor configs for processor info would go here
        return countableInfoStats;
    }

    /**
     * Calculates and gets settable info stats
     * @return map of stat name to stat snapshot
     */
    private Map<InfoStatName, SettableInfoStatSnapshot<?>> getSettableStats() {
        Map<InfoStatName, SettableInfoStatSnapshot<?>> settableInfoStats = new HashMap<>();
        for (InfoStatName statName : EnumSet.allOf(InfoStatName.class)) {
            switch (statName.getStatType()) {
                case InfoStatType.INFO_BOOLEAN -> settableInfoStats.put(statName, new SettableInfoStatSnapshot<Boolean>(statName));
                case InfoStatType.INFO_STRING -> settableInfoStats.put(statName, new SettableInfoStatSnapshot<String>(statName));
            }
        }

        addClusterVersionStat(settableInfoStats);
        return settableInfoStats;
    }

    /**
     * Adds cluster version to settable stats, mutating the input
     * @param stats mutable map of info stats that the result will be added to
     */
    private void addClusterVersionStat(Map<InfoStatName, SettableInfoStatSnapshot<?>> stats) {
        InfoStatName infoStatName = InfoStatName.CLUSTER_VERSION;
        stats.put(infoStatName, new SettableInfoStatSnapshot<>(infoStatName, Version.CURRENT));
    }

    /**
     * Helper to cast generic object into a specific type
     * Used to parse pipeline processor configs
     * @param map the map
     * @param key the key
     * @param clazz the class to cast to
     * @return the map
     */
    @SuppressWarnings("unchecked")
    private <T> T getValue(Map<String, Object> map, String key, Class<T> clazz) {
        if (map == null || key == null) return null;
        Object value = map.get(key);
        return clazz.isInstance(value) ? clazz.cast(value) : null;
    }

    /**
     * Helper to cast generic object into Map<String, Object>
     * Used to parse pipeline processor configs
     * @param value the object
     * @return the map
     */
    @SuppressWarnings("unchecked")
    private Map<String, Object> asMap(Object value) {
        return value instanceof Map ? (Map<String, Object>) value : null;
    }

    /**
     * Helper to cast generic object into a list of Map<String, Object>
     * Used to parse pipeline processor configs
     * @param value the object
     * @return the list of maps
     */
    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> asListOfMaps(Object value) {
        if (value instanceof List) {
            List<?> list = (List<?>) value;
            for (Object item : list) {
                if (!(item instanceof Map)) return null;
            }
            return (List<Map<String, Object>>) value;
        }
        return null;
    }
}
