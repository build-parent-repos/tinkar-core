package org.hl7.tinkar.lombok.dto.graph;


import lombok.NonNull;
import lombok.Value;
import lombok.experimental.Accessors;
import org.eclipse.collections.api.factory.Lists;
import org.eclipse.collections.api.list.ImmutableList;
import org.eclipse.collections.api.list.MutableList;
import org.eclipse.collections.api.list.primitive.ImmutableIntList;
import org.eclipse.collections.api.list.primitive.MutableIntList;
import org.eclipse.collections.api.map.primitive.ImmutableIntObjectMap;
import org.eclipse.collections.api.map.primitive.MutableIntObjectMap;
import org.eclipse.collections.impl.factory.primitive.IntLists;
import org.eclipse.collections.impl.factory.primitive.IntObjectMaps;
import org.hl7.tinkar.lombok.dto.binary.*;
import org.hl7.tinkar.component.graph.DiGraph;

import java.util.Objects;


@Value
@Accessors(fluent = true)
public final class DiGraphDTO extends GraphDTO implements DiGraph<VertexDTO>, Marshalable {

    private static final int LOCAL_MARSHAL_VERSION = 3;

    @NonNull
    private final ImmutableIntList rootSequences;

    @NonNull
    private final ImmutableIntObjectMap<ImmutableIntList> predecessorMap;

    public DiGraphDTO(@NonNull ImmutableIntList rootSequences,
                      @NonNull ImmutableIntObjectMap<ImmutableIntList> predecessorMap,
                      @NonNull ImmutableList<VertexDTO> vertexMap,
                      @NonNull ImmutableIntObjectMap<ImmutableIntList> successorMap) {
        super(vertexMap, successorMap);
        this.rootSequences = rootSequences;
        this.predecessorMap = predecessorMap;
    }

    @Override
    public ImmutableList<VertexDTO> roots() {
        MutableList<VertexDTO> roots = Lists.mutable.ofInitialCapacity(rootSequences.size());
        rootSequences.forEach(rootSequence -> roots.add(vertex(rootSequence)));
        return roots.toImmutable();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof DiGraphDTO that)) return false;
        if (!super.equals(o)) return false;
        return rootSequences.equals(that.rootSequences) && predecessorMap.equals(that.predecessorMap);
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), rootSequences, predecessorMap);
    }

    @Override
    public ImmutableList<VertexDTO> predecessors(VertexDTO vertex) {
        ImmutableIntList predecessorIntList = predecessorMap.getIfAbsent(vertex.vertexIndex(), () -> IntLists.immutable.empty());
        if (predecessorIntList.isEmpty()) {
            return Lists.immutable.empty();
        }
        MutableList<VertexDTO> predecessorList = Lists.mutable.ofInitialCapacity(predecessorIntList.size());
        predecessorIntList.forEach(vertexSequence -> predecessorList.add(vertexMap().get(vertexSequence)));
        return predecessorList.toImmutable();
    }

    @Unmarshaler
    public static DiGraphDTO make(TinkarInput in) {
        if (LOCAL_MARSHAL_VERSION == in.getTinkerFormatVersion()) {
            ImmutableList<VertexDTO> vertexMap = GraphDTO.unmarshalVertexMap(in);
            ImmutableIntObjectMap<ImmutableIntList> successorMap = GraphDTO.unmarshalSuccessorMap(in, vertexMap);

            int rootCount = in.getInt();
            MutableIntList roots = IntLists.mutable.empty();
            for (int i = 0; i < rootCount; i++) {
                roots.add(in.getInt());
            }
            int predecessorMapSize = in.getInt();
            MutableIntObjectMap<ImmutableIntList> predecessorMap = IntObjectMaps.mutable.ofInitialCapacity(predecessorMapSize);
            for (int i = 0; i < predecessorMapSize; i++) {
                int vertexSequence = in.getInt();
                int predecessorCount = in.getInt();
                MutableIntList predecessorList = IntLists.mutable.empty();
                for (int j = 0; j < predecessorCount; j++) {
                    predecessorList.add(in.getInt());
                }
                predecessorMap.put(vertexSequence, predecessorList.toImmutable());
            }
            return new DiGraphDTO(roots.toImmutable(),
                    predecessorMap.toImmutable(),
                    vertexMap,
                    successorMap);
        } else {
            throw new UnsupportedOperationException("Unsupported version: " + marshalVersion);
        }
    }

    @Override
    @Marshaler
    public void marshal(TinkarOutput out) {
        marshalVertexMap(out);
        marshalSuccessorMap(out);
        out.putInt(rootSequences.size());
        rootSequences.forEach(root -> out.putInt(root));

        out.putInt(predecessorMap.size());
        predecessorMap.forEachKeyValue((vertex, predecessorList) -> {
            out.putInt(vertex);
            out.putInt(predecessorList.size());
            predecessorList.forEach(predecessorSequence -> out.putInt(predecessorSequence));
        });
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private final MutableList<VertexDTO> vertexMap = Lists.mutable.empty();
        private final MutableIntObjectMap<MutableIntList> successorMap = IntObjectMaps.mutable.empty();
        private final MutableIntObjectMap<MutableIntList> predecessorMap = IntObjectMaps.mutable.empty();;
        private final MutableIntList roots = IntLists.mutable.empty();

        protected Builder() {
        }

        public Builder addRoot(VertexDTO root) {
            vertexMap.add(root.vertexIndex(), root);
            roots.add(root.vertexIndex());
            return this;
        }

        public Builder add(VertexDTO child, VertexDTO parent) {
            vertexMap.add(child.vertexIndex(), child);
            successorMap.getIfAbsentPut(parent.vertexIndex(), IntLists.mutable.empty()).add(child.vertexIndex());
            predecessorMap.getIfAbsentPut(child.vertexIndex(), IntLists.mutable.empty()).add(parent.vertexIndex());
            return this;
        }

        public DiGraphDTO build() {

            MutableIntObjectMap<ImmutableIntList> intermediateSuccessorMap = IntObjectMaps.mutable.ofInitialCapacity(successorMap.size());
            successorMap.forEachKeyValue((vertex, successorList) -> intermediateSuccessorMap.put(vertex, successorList.toImmutable()));

            MutableIntObjectMap<ImmutableIntList> intermediatePredecessorMap = IntObjectMaps.mutable.ofInitialCapacity(predecessorMap.size());
            predecessorMap.forEachKeyValue((vertex, predecessorList) -> intermediatePredecessorMap.put(vertex, predecessorList.toImmutable()));

            return new DiGraphDTO(roots.toImmutable(),
                    intermediatePredecessorMap.toImmutable(),
                    vertexMap.toImmutable(),
                    intermediateSuccessorMap.toImmutable());
        }
    }

}
