package org.hl7.tinkar.provider.spinedarray.internal;

import org.hl7.tinkar.common.service.PrimitiveDataService;
import org.hl7.tinkar.entity.EntityFactory;
import org.hl7.tinkar.component.Chronology;
import org.hl7.tinkar.entity.Entity;
import org.hl7.tinkar.entity.SemanticEntity;
import org.hl7.tinkar.provider.spinedarray.SpinedArrayProvider;

public class Put {
    public static SpinedArrayProvider singleton;
    public static void put(Chronology chronology) {
        Entity entity = EntityFactory.make(chronology);
        if (entity instanceof SemanticEntity semanticEntity) {
            singleton.merge(entity.nid(),
                    entity.definitionNid(),
                    semanticEntity.referencedComponentNid(),
                    entity.getBytes());
        } else {
            singleton.merge(entity.nid(), entity.definitionNid(), Integer.MAX_VALUE, entity.getBytes());
        }
    }

}
