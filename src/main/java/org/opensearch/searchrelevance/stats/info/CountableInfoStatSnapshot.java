/*
 * SPDX-License-Identifier: Apache-2.0
 *
 * The OpenSearch Contributors require contributions made to
 * this file be licensed under the Apache-2.0 license or a
 * compatible open source license.
 */
package org.opensearch.searchrelevance.stats.info;

import java.io.IOException;
import java.util.concurrent.atomic.LongAdder;

import org.opensearch.core.xcontent.XContentBuilder;
import org.opensearch.searchrelevance.stats.common.StatSnapshot;

/**
 * A countable stat snapshot for info stats.
 * Can be updated in place
 */
public class CountableInfoStatSnapshot implements StatSnapshot<Long> {
    private LongAdder adder;
    private InfoStatName statName;

    /**
     * Creates a new stat snapshot
     * @param statName the name of the stat it corresponds to
     */
    public CountableInfoStatSnapshot(InfoStatName statName) {
        this.statName = statName;
        this.adder = new LongAdder();
    }

    /**
     * Gets the counter value
     * @return the counter value
     */
    public Long getValue() {
        return adder.longValue();
    }

    /**
     * Increment the counter by a given delta
     * @param delta the amount ot increment by
     */
    public void incrementBy(Long delta) {
        adder.add(delta);
    }

    /**
     * Converts to fields xContent, including stat metadata
     *
     * @param builder XContentBuilder
     * @param params Params
     * @return XContentBuilder
     * @throws IOException thrown by builder for invalid field
     */
    @Override
    public XContentBuilder toXContent(XContentBuilder builder, Params params) throws IOException {
        builder.startObject();
        builder.field(StatSnapshot.VALUE_FIELD, getValue());
        builder.field(StatSnapshot.STAT_TYPE_FIELD, statName.getStatType().getTypeString());
        builder.endObject();
        return builder;
    }
}
