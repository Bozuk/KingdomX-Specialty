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
 * Resource points that were collected but weren't worth a full unit of the specialty resource yet,
 * stored as {@code Specialties:PRODUCTION_REMAINDER}.
 * <p>
 * Without it, a kingdom collecting its extractors often - each time below the conversion
 * threshold - would never produce anything.
 */
public final class ProductionRemainderMetaHandler extends KingdomMetadataHandler {
    public static final ProductionRemainderMetaHandler INSTANCE = new ProductionRemainderMetaHandler();

    private ProductionRemainderMetaHandler() {
        super(new Namespace("Specialties", "PRODUCTION_REMAINDER"));
    }

    @Override
    public KingdomMetadata deserialize(KeyedKingdomsObject<?> container,
                                       DeserializationContext<SectionableDataGetter> context) {
        return new RemainderMeta(context.getDataProvider().asLong());
    }

    public static final class RemainderMeta implements KingdomMetadata {
        private long remainder;

        public RemainderMeta(long remainder) {
            this.remainder = remainder;
        }

        public long getRemainder() {
            return remainder;
        }

        public void setRemainder(long remainder) {
            this.remainder = Math.max(0, remainder);
        }

        @Override
        public Object getValue() {
            return remainder;
        }

        @Override
        public void setValue(Object value) {
            this.remainder = value instanceof Number ? ((Number) value).longValue() : 0;
        }

        @Override
        public void serialize(KeyedKingdomsObject<?> container,
                              SerializationContext<SectionCreatableDataSetter> context) {
            context.getDataProvider().setLong(remainder);
        }

        @Override
        public boolean shouldSave(KeyedKingdomsObject<?> container) {
            return remainder > 0;
        }

        @Override
        public String toString() {
            return "RemainderMeta[" + remainder + ']';
        }
    }
}
