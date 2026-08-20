package org.kingdoms.specialties.data;

import org.kingdoms.constants.base.KeyedKingdomsObject;
import org.kingdoms.constants.land.abstraction.data.DeserializationContext;
import org.kingdoms.constants.land.abstraction.data.SerializationContext;
import org.kingdoms.constants.metadata.KingdomMetadata;
import org.kingdoms.constants.metadata.KingdomMetadataHandler;
import org.kingdoms.constants.namespace.Namespace;
import org.kingdoms.data.database.dataprovider.SectionCreatableDataSetter;
import org.kingdoms.data.database.dataprovider.SectionableDataGetter;

/**
 * Persists the specialty a kingdom has committed to, as {@code Specialties:SPECIALTY}.
 */
public final class SpecialtyMetaHandler extends KingdomMetadataHandler {
    public static final SpecialtyMetaHandler INSTANCE = new SpecialtyMetaHandler();

    private SpecialtyMetaHandler() {
        super(new Namespace("Specialties", "SPECIALTY"));
    }

    @Override
    public KingdomMetadata deserialize(KeyedKingdomsObject<?> container,
                                       DeserializationContext<SectionableDataGetter> context) {
        return new SpecialtyMeta(Specialty.fromString(context.getDataProvider().asString()));
    }

    public static final class SpecialtyMeta implements KingdomMetadata {
        private Specialty specialty;

        public SpecialtyMeta(Specialty specialty) {
            this.specialty = specialty;
        }

        public Specialty getSpecialty() {
            return specialty;
        }

        @Override
        public Object getValue() {
            return specialty;
        }

        @Override
        public void setValue(Object value) {
            this.specialty = value instanceof Specialty ? (Specialty) value : Specialty.fromString(String.valueOf(value));
        }

        @Override
        public void serialize(KeyedKingdomsObject<?> container,
                              SerializationContext<SectionCreatableDataSetter> context) {
            context.getDataProvider().setString(specialty == null ? "" : specialty.name());
        }

        @Override
        public boolean shouldSave(KeyedKingdomsObject<?> container) {
            return specialty != null;
        }

        @Override
        public String toString() {
            return "SpecialtyMeta[" + specialty + ']';
        }
    }
}
