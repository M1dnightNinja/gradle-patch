package org.wallentines.mdcfg.serializer;

import org.wallentines.mdcfg.Functions;

import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * A context-aware serializer. A serializer which expects an object of a specific type to be available while (de)serializing
 * @param <T> The type of objects to serialize
 * @param <C> The context which should be present
 */
public interface ContextSerializer<T,C> {

    <O> SerializeResult<O> serialize(SerializeContext<O> serializeContext, T value, C context);

    <O> SerializeResult<T> deserialize(SerializeContext<O> serializeContext, O value, C context);


    default Serializer<T> forContext(C context) {
        return new Serializer<>() {
            @Override
            public <O> SerializeResult<O> serialize(SerializeContext<O> ctx, T value) {
                return ContextSerializer.this.serialize(ctx, value, context);
            }

            @Override
            public <O> SerializeResult<T> deserialize(SerializeContext<O> ctx, O value) {
                return ContextSerializer.this.deserialize(ctx, value, context);
            }
        };
    }

    default Serializer<T> forContext(Supplier<C> context) {
        return new Serializer<>() {
            @Override
            public <O> SerializeResult<O> serialize(SerializeContext<O> ctx, T value) {
                return ContextSerializer.this.serialize(ctx, value, context.get());
            }

            @Override
            public <O> SerializeResult<T> deserialize(SerializeContext<O> ctx, O value) {
                return ContextSerializer.this.deserialize(ctx, value, context.get());
            }
        };
    }

    static <T,C> ContextSerializer<T, C> fromStatic(Serializer<T> serializer) {
        return new ContextSerializer<>() {
            @Override
            public <O> SerializeResult<O> serialize(SerializeContext<O> serializeContext, T value, C context) {
                return serializer.serialize(serializeContext, value);
            }

            @Override
            public <O> SerializeResult<T> deserialize(SerializeContext<O> serializeContext, O value, C context) {
                return serializer.deserialize(serializeContext, value);
            }
        };
    }


    /**
     * Creates an entry for use in an ObjectSerializer using the given key and getter
     * @param key The key to use for lookups and saving
     * @param getter A function to retrieve an instance of T from an instance of O when serializing
     * @return An entry for use in ObjectSerializer.create()
     * @param <O> The type of object to serialize
     */
    default <O> ObjectSerializer.ContextEntry<T, O, C> entry(String key, Functions.F2<O, C, T> getter) {
        return ObjectSerializer.entry(key, this, getter);
    }

    /**
     * Creates a list serializer from this serializer
     * @return A serializer for a list of objects with type T
     */
    default ListContextSerializer<T, C> listOf() {
        return new ListContextSerializer<>(this);
    }

    /**
     * Creates a list serializer from this serializer which does not require all contained objects to (de-)serialize successfully
     * @return A serializer for a list of objects with type T
     */
    default ListContextSerializer<T, C> filteredListOf() {
        return new ListContextSerializer<>(this, (err) -> false);
    }

    /**
     * Creates a list serializer from this serializer which does not require all contained objects to (de-)serialize successfully
     * @param onError A callback to send error text whenever an object fails to serialize
     * @return A serializer for a list of objects with type T
     */
    default ListContextSerializer<T, C> filteredListOf(Consumer<String> onError) {
        return new ListContextSerializer<>(this, str -> {
            onError.accept(str);
            return false;
        });
    }


    /**
     * Creates a map serializer from this serializer
     * @return A serializer for a map with string keys and T values
     */
    default MapContextSerializer<String, T, C> mapOf() {
        return mapOf(InlineContextSerializer.raw());
    }

    /**
     * Creates a map serializer from this serializer using the given key serializer
     * @param keySerializer The serializer to use to serialize the keys in the map to Strings
     * @return A serializer for map with K keys and T values
     * @param <K> The type of values for the keys in the map
     */
    default <K> MapContextSerializer<K, T, C> mapOf(InlineContextSerializer<K, C> keySerializer) {
        return new MapContextSerializer<>(keySerializer, this);
    }

    /**
     * Creates a map serializer from this serializer which does not require all contained objects to (de-)serialize successfully
     * @return A serializer for a map with string keys and T values
     */
    default MapContextSerializer<String, T, C> filteredMapOf() {
        return filteredMapOf(InlineContextSerializer.raw());
    }

    /**
     * Creates a map serializer from this serializer using the given key serializer which does not require all contained objects to (de-)serialize successfully
     * @param keySerializer The serializer to use to serialize the keys in the map to Strings
     * @return A serializer for map with K keys and T values
     * @param <K> The type of values for the keys in the map
     */
    default <K> MapContextSerializer<K, T, C> filteredMapOf(InlineContextSerializer<K, C> keySerializer) {
        return new MapContextSerializer<>(keySerializer, this, (key, str) -> false);
    }

    /**
     * Creates a map serializer from this serializer using the given key serializer which does not require all contained objects to (de-)serialize successfully
     * @param onError A callback to send error text whenever an object fails to serialize
     * @return A serializer for map with K keys and T values
     */
    default MapContextSerializer<String, T, C> filteredMapOf(BiConsumer<String, String> onError) {
        return new MapContextSerializer<>(InlineContextSerializer.raw(), this, (key, str) -> {
            onError.accept(key, str);
            return false;
        });
    }

    /**
     * Creates a map serializer from this serializer using the given key serializer which does not require all contained objects to (de-)serialize successfully
     * @param keySerializer The serializer to use to serialize the keys in the map to Strings
     * @param onError A callback to send error text whenever an object fails to serialize
     * @return A serializer for map with K keys and T values
     * @param <K> The type of values for the keys in the map
     */
    default <K> MapContextSerializer<K, T, C> filteredMapOf(InlineContextSerializer<K, C> keySerializer, BiConsumer<K, String> onError) {
        return new MapContextSerializer<>(keySerializer, this, (key, str) -> {
            onError.accept(key, str);
            return false;
        });
    }

}
