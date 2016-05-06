/*
 * Copyright 2016-present Open Networking Laboratory
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.onosproject.store.resource.impl;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import org.onlab.util.Tools;
import org.onosproject.net.resource.DiscreteResource;
import org.onosproject.net.resource.DiscreteResourceId;
import org.onosproject.net.resource.Resource;
import org.onosproject.net.resource.ResourceAllocation;
import org.onosproject.net.resource.ResourceConsumer;
import org.onosproject.net.resource.Resources;
import org.onosproject.store.service.ConsistentMap;
import org.onosproject.store.service.ConsistentMapException;
import org.onosproject.store.service.StorageService;
import org.onosproject.store.service.TransactionContext;
import org.onosproject.store.service.Versioned;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;

import static org.onosproject.store.resource.impl.ConsistentResourceStore.MAX_RETRIES;
import static org.onosproject.store.resource.impl.ConsistentResourceStore.RETRY_DELAY;
import static org.onosproject.store.resource.impl.ConsistentResourceStore.SERIALIZER;

class ConsistentDiscreteResourceStore {
    private ConsistentMap<DiscreteResourceId, ResourceConsumer> consumers;
    private ConsistentMap<DiscreteResourceId, Set<DiscreteResource>> childMap;

    ConsistentDiscreteResourceStore(StorageService service) {
        this.consumers = service.<DiscreteResourceId, ResourceConsumer>consistentMapBuilder()
                .withName(MapNames.DISCRETE_CONSUMER_MAP)
                .withSerializer(SERIALIZER)
                .build();
        this.childMap = service.<DiscreteResourceId, Set<DiscreteResource>>consistentMapBuilder()
                .withName(MapNames.DISCRETE_CHILD_MAP)
                .withSerializer(SERIALIZER)
                .build();

        Tools.retryable(() -> childMap.put(Resource.ROOT.id(), new LinkedHashSet<>()),
                ConsistentMapException.class, MAX_RETRIES, RETRY_DELAY);
    }

    TransactionalDiscreteResourceStore transactional(TransactionContext tx) {
        return new TransactionalDiscreteResourceStore(tx);
    }

    // computational complexity: O(1)
    List<ResourceAllocation> getResourceAllocations(DiscreteResourceId resource) {
        Versioned<ResourceConsumer> consumer = consumers.get(resource);
        if (consumer == null) {
            return ImmutableList.of();
        }

        return ImmutableList.of(new ResourceAllocation(Resources.discrete(resource).resource(), consumer.value()));
    }

    Set<DiscreteResource> getChildResources(DiscreteResourceId parent) {
        Versioned<Set<DiscreteResource>> children = childMap.get(parent);

        if (children == null) {
            return ImmutableSet.of();
        }

        return children.value();
    }

    boolean isAvailable(DiscreteResource resource) {
        return getResourceAllocations(resource.id()).isEmpty();
    }

    <T> Stream<DiscreteResource> getAllocatedResources(DiscreteResourceId parent, Class<T> cls) {
        Set<DiscreteResource> children = getChildResources(parent);
        if (children.isEmpty()) {
            return Stream.of();
        }

        return children.stream()
                .filter(x -> x.isTypeOf(cls))
                .filter(x -> consumers.containsKey(x.id()));
    }

    Stream<DiscreteResource> getResources(ResourceConsumer consumer) {
        return consumers.entrySet().stream()
                .filter(x -> x.getValue().value().equals(consumer))
                .map(Map.Entry::getKey)
                .map(x -> Resources.discrete(x).resource());
    }
}