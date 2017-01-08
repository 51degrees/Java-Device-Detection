package fiftyone.mobile.detection;

import fiftyone.mobile.detection.cache.ICache;
import fiftyone.mobile.detection.cache.IPutCache;
import fiftyone.mobile.detection.cache.IValueLoader;
import fiftyone.mobile.detection.cache.LruCache;
import fiftyone.mobile.detection.entities.*;
import fiftyone.mobile.detection.entities.Map;
import fiftyone.mobile.detection.entities.headers.Header;
import fiftyone.mobile.detection.entities.memory.MemoryFixedList;
import fiftyone.mobile.detection.entities.memory.PropertiesList;
import fiftyone.mobile.detection.entities.stream.IntegerList;
import fiftyone.mobile.detection.factories.*;
import fiftyone.mobile.detection.factories.stream.NodeStreamFactoryV31;
import fiftyone.mobile.detection.factories.stream.NodeStreamFactoryV32;
import fiftyone.mobile.detection.factories.stream.ProfileStreamFactory;
import fiftyone.mobile.detection.readers.BinaryReader;


import java.io.File;
import java.io.IOException;
import java.util.*;

import static fiftyone.mobile.detection.DatasetBuilder.CacheType.*;

/**
 * Allows construction of a {@link Dataset}.
 * <p>
 * Use as follows:<br>
 *<pre><code>
 *      // for stream dataset read from byte array buffer
 *      Dataset dataset = DatasetBuilder.buffer()
 *          // to use caching
 *          .addDefaultCaches()
 *          .build(array);
 *
 *      // for stream dataset read from file
 *      Dataset dataset = DatasetBuilder.stream()
 *          // to use caching (recommended)
 *          .addDefaultCaches()
 *          // if a temporary file (deleted on dataset close)
 *          .setTempFile()
 *          // to set the date explicitly
 *          .lastModified(date)
 *          // or to build from a file
 *          .build(filename);
 *          
 * </code></pre>
 */

public class DatasetBuilder {

    /* Default Cache sizes */
    public static final int STRINGS_CACHE_SIZE = 5000;
    public static final int NODES_CACHE_SIZE = 15000;
    public static final int VALUES_CACHE_SIZE = 5000;
    public static final int PROFILES_CACHE_SIZE = 600;
    public static final int SIGNATURES_CACHE_SIZE = 500;

    /**
     * Cache types for Stream Dataset
     */
    public enum CacheType {
        StringsCache, NodesCache, ValuesCache, ProfilesCache, SignaturesCache
    }

    private final java.util.Map<CacheType, ICache> defaultCacheMap = new HashMap<CacheType, ICache>(5);
    // initialise the default caches
    {
        defaultCacheMap.put(StringsCache, new LruCache(STRINGS_CACHE_SIZE));
        defaultCacheMap.put(NodesCache, new LruCache(NODES_CACHE_SIZE));
        defaultCacheMap.put(ValuesCache, new LruCache(VALUES_CACHE_SIZE));
        defaultCacheMap.put(ProfilesCache, new LruCache(PROFILES_CACHE_SIZE));
        defaultCacheMap.put(SignaturesCache, new LruCache(SIGNATURES_CACHE_SIZE));
    }

    private java.util.Map<CacheType, ICache> cacheMap = new HashMap<CacheType, ICache>(5);

    // prevent direct construction
    private DatasetBuilder() {

    }

    /**
     * Create a stream file dataset
     */
    public static Stream stream() {
        return new DatasetBuilder().new Stream();
    }

    /**
     * Create a stream buffer dataset
     */
    public static Buffer buffer() {
        return new DatasetBuilder().new Buffer();
    }


    /**
     * Holds cache methods for buffer and file stream mode
     */
    @SuppressWarnings("WeakerAccess")
    public class Cachable<T extends Cachable<T>> {

        // no direct instantiation
        private Cachable () {

        }

        /**
         * Add a cache to this (Stream) Dataset
         * @param cacheType the type
         * @param cache the cache
         */
        public T addCache(CacheType cacheType, ICache cache) {
            cacheMap.put(cacheType, cache);
            //noinspection unchecked
            return (T) this;
        }

        /**
         * Add all the caches in the map
         * @param map a map of instantiated caches with their type
         */
        public T addCaches(java.util.Map<CacheType, ICache> map) {
            cacheMap.putAll(map);
            //noinspection unchecked
            return (T) this;
        }

        /**
         * Add the internal default caches
         */
        public T addDefaultCaches() {
            addCaches(defaultCacheMap);
            //noinspection unchecked
            return (T) this;
        }
    }

    /**
     * Buffer dataset builder
     */
    public class Buffer extends Cachable<Buffer>{

        // cannot be instantiated directly
        private Buffer() {

        }

        /**
         * build the dataset from a buffer
         * @param buffer the buffer
         */
        public StreamDataset build(byte[] buffer) throws IOException {
            StreamDataset dataSet = new StreamDataset(buffer, Modes.MEMORY_MAPPED);
            loadForStreaming(dataSet, cacheMap);
            return dataSet;
        }
    }

    /**
     * File dataset builder
     */
    @SuppressWarnings("WeakerAccess")
    public class Stream extends Cachable<Stream> {

        private boolean isTempFile = false;
        private Date lastModified = null;

        // cannot be instantiated directly
        private Stream() {

        }

        /**
         * If this dataset is built from a file, delete the file after close
         */
        public Stream setTempFile() {
            isTempFile = true;
            return this;
        }

        /**
         * If this dataset is built from a file
         * @param isTemp if true, delete the file after close
         */
        public Stream setTempFile(boolean isTemp) {
            isTempFile = isTemp;
            return this;
        }

        /**
         * If this dataset is built from a file, override the creation date
         * @param date the date
         */
        public Stream lastModified(Date date) {
            lastModified = date;
            return this;
        }

        /**
         * build the dataset from a file
         * @param filename the filename to build from
         */
        public StreamDataset build(String filename) throws IOException {
            Date modDate = lastModified;
            if (modDate == null) {
                modDate = new Date(new File(filename).lastModified());
            }
            StreamDataset dataSet = new StreamDataset(filename, modDate, Modes.FILE, isTempFile);
            loadForStreaming(dataSet, cacheMap);
            return dataSet;
        }
    }

    /*
    public Memory memory() {
        return new DatasetBuilder().new Memory();
    }

    public class Memory extends DatasetBuilder {

        public Memory init() {
            init = true;
            return this;
        }

        public Memory init(boolean isInit) {
            init = isInit;
            return this;
        }

        public Dataset build(String filename) throws IOException {
            Date modDate = lastModified;
            if (modDate.equals(DATE_NONE)) {
                modDate = new Date(new File(filename).lastModified());
            }
            Dataset dataSet = new Dataset(modDate, Modes.MEMORY);
            loadForMemory(dataSet, init);
            return dataSet;
        }

        public Dataset build(byte[] buffer) throws IOException {
            Date modDate = lastModified;
            if (modDate.equals(DATE_NONE)) {
                modDate = new Date();
            }
            Dataset dataSet = new Dataset(modDate, Modes.MEMORY);
            loadForMemory(dataSet, init);
            return dataSet;
        }
    }
*/

    /**
     * Class adapts an EntityFactory to a Loader
     *
     * @param <V> type of the entity
     */
    private static class EntityLoader<V> implements IValueLoader<Integer, V> {

        final StreamDataset dataset;
        final BaseEntityFactory<V> entityFactory;
        final Header header;
        boolean fixedLength = false;

        EntityLoader(Header header, StreamDataset dataset, BaseEntityFactory<V> entityFactory) {
            this.dataset = dataset;
            this.entityFactory = entityFactory;
            this.header = header;
            try {
                getEntityFactory().getLength();
                fixedLength = true;
            } catch (UnsupportedOperationException ignored) {
                // expected for variable length entities
            } catch (IOException e) {
                throw new IllegalStateException(e);
            }
        }

        @Override
        public V load(Integer key) throws IOException {
            BinaryReader reader = dataset.pool.getReader();
            try {
                if (fixedLength) {
                    reader.setPos(header.getStartPosition()
                            + (getEntityFactory().getLength() * key));
                } else {
                    reader.setPos(header.getStartPosition() + key);
                }
                return entityFactory.create(dataset, key, reader);
            } finally {
                dataset.pool.release(reader);
            }
        }

        @SuppressWarnings("WeakerAccess")
        public int nextPosition(int position, V result) throws IOException {
            if (fixedLength) {
                return ++position;
            } else {
                // this method supported only for variable length entities
                return position + getEntityFactory().getLength(result);
            }
        }

        @SuppressWarnings("WeakerAccess")
        public BaseEntityFactory<V> getEntityFactory() {
            return entityFactory;
        }

        public Header getHeader() {
            return header;
        }

    }

    /**
     * A cacheing entity loader that uses an {@link LruCache}
     *
     * @param <V> type of entity
     */
    private static class LruEntityLoader<V> extends EntityLoader<V> {

        private LruCache<Integer, V> cache;

        LruEntityLoader(final Header header, final StreamDataset dataset, final BaseEntityFactory<V> entityFactory, LruCache<Integer, V> cache) {
            super(header, dataset, entityFactory);
            this.cache = cache;
            this.cache.setCacheLoader(new EntityLoader<V>(header, dataset, entityFactory));
        }

        @Override
        public V load(Integer key) throws IOException {
            return cache.get(key);
        }
    }

    /**
     * A caching entity loader that uses a {@link IPutCache}
     *
     * @param <V> type of entity
     */

    private static class CachedEntityLoader<V> extends EntityLoader<V> {

        private IPutCache<Integer, V> cache;

        CachedEntityLoader(Header header, StreamDataset dataset, BaseEntityFactory<V> entityFactory, IPutCache<Integer, V> cache) {
            super(header, dataset, entityFactory);
            this.cache = cache;
        }

        @Override
        public V load(Integer key) throws IOException {
            V value;
            value = cache.get(key);
            if (value == null) {
                value = super.load(key);
                if (value != null) {
                    cache.put(key, value);
                }
            }
            return value;
        }
    }

    /**
     * Implementation of IReadOnlyList for Streams
     *
     * @param <T> type of entity
     */
    private static class StreamList<T extends BaseEntity> implements IReadonlyList<T> {

        private EntityLoader<T> loader;

        StreamList(EntityLoader<T> loader) {
            this.loader = loader;
        }

        @Override
        public T get(int i) throws IOException {
            return loader.load(i);
        }

        @Override
        public int size() {
            return this.loader.getHeader().getCount();
        }

        @Override
        public void close() throws IOException {

        }

        @Override
        public Iterator<T> iterator() {
            return new Iterator<T>() {
                // the item number
                int count = 0;
                // the position in the file or the item number (as above)
                // depending on whether the entity is fixed or variable size
                int position = 0;
                // number of elements
                int total = loader.getHeader().getCount();

                @Override
                public boolean hasNext() {
                    return count < total;
                }

                @Override
                public T next() {
                    try {
                        if (count >= total) {
                            throw new NoSuchElementException();
                        }
                        T result = get(position);
                        count++;
                        position = loader.nextPosition(position, result);
                        return result;
                    } catch (IOException e) {
                        throw new IllegalStateException(e);
                    }
                }

                @Override
                public void remove() {
                    throw new UnsupportedOperationException("remove not supported");

                }
            };
        }
    }

    /**
     * helper to create an appropriate loader for a cached list given the cache type
     *
     * @param header  the header defining the list this will create the loader for
     * @param cache   the cache, or null
     * @param dataset the dataset
     * @param factory the factory for the type
     * @param <T>     the type
     * @return an entity loader
     */
    @SuppressWarnings("unchecked")
    private static <T> EntityLoader<T> getLoaderFor(Header header, ICache cache, StreamDataset dataset, BaseEntityFactory factory) {
        EntityLoader loader;
        if (cache == null) {
            loader = new EntityLoader(header, dataset, factory);
        } else if (cache instanceof LruCache) {
            loader = new LruEntityLoader(header, dataset, factory, (LruCache) cache);
        } else if (cache instanceof IPutCache) {
            loader = new CachedEntityLoader(header, dataset, factory, (IPutCache) cache);
        } else {
            throw new IllegalStateException("Cache must be null, LruCache or IPutCache");
        }
        return loader;

    }

    /**
     * Load the necessary values from the data
     * file in to the Dataset. Stream mode only loads the essential information
     * such as file headers.
     *
     * @param dataSet The dataset object to load in to.
     * @throws IOException if there was a problem accessing data file.
     */
    @SuppressWarnings("null")
    private static void loadForStreaming(StreamDataset dataSet, java.util.Map<CacheType, ICache> cacheMap) throws IOException {
        BinaryReader reader = dataSet.pool.getReader();
        try {
            dataSet.setCacheMap(cacheMap);
            reader.setPos(0);
            //Load headers that are common for both V31 and V32.
            CommonFactory.loadHeader(dataSet, reader);

            EntityLoader<AsciiString> loader = getLoaderFor(new Header(reader), cacheMap.get(StringsCache), dataSet, new AsciiStringFactory());
            dataSet.strings = new StreamList<AsciiString>(loader);

            MemoryFixedList<Component> components;
            switch (dataSet.versionEnum) {
                case PatternV31:
                    components = new MemoryFixedList<Component>(
                            dataSet, reader, new ComponentFactoryV31());
                    break;
                case PatternV32:
                    components = new MemoryFixedList<Component>(
                            dataSet, reader, new ComponentFactoryV32());
                    break;

                default:
                    throw new IllegalStateException("Unknown data version number");
            }
            dataSet.components = components;

            MemoryFixedList<Map> maps = new MemoryFixedList<Map>(
                    dataSet, reader, new MapFactory());
            dataSet.maps = maps;

            PropertiesList properties = new PropertiesList(
                    dataSet, reader, new PropertyFactory());
            dataSet.properties = properties;

            EntityLoader<Value> valueLoader = getLoaderFor(new Header(reader), cacheMap.get(ValuesCache),
                    dataSet, new ValueFactory());
            dataSet.values = new StreamList<Value>(valueLoader);

            EntityLoader<Profile> profileLoader = getLoaderFor(new Header(reader), cacheMap.get(ProfilesCache),
                    dataSet, new ProfileStreamFactory());
            dataSet.profiles = new StreamList<Profile>(profileLoader);

            switch (dataSet.versionEnum) {
                case PatternV31:
                    EntityLoader<Signature> signature31Loader = getLoaderFor(new Header(reader), cacheMap.get(SignaturesCache),
                            dataSet, new SignatureFactoryV31(dataSet));
                    dataSet.signatures = new StreamList<Signature>(signature31Loader);
                    break;
                case PatternV32:
                    EntityLoader<Signature> signature32Loader = getLoaderFor(new Header(reader), cacheMap.get(SignaturesCache),
                            dataSet, new SignatureFactoryV32(dataSet));
                    dataSet.signatures = new StreamList<Signature>(signature32Loader);
                    dataSet.signatureNodeOffsets =
                            new IntegerList(dataSet, reader);
                    dataSet.nodeRankedSignatureIndexes =
                            new IntegerList(dataSet, reader);
                    break;
            }
            dataSet.rankedSignatureIndexes = new IntegerList(dataSet, reader);

            switch (dataSet.versionEnum) {
                case PatternV31:
                    EntityLoader<Node> node31Loader = getLoaderFor(new Header(reader), cacheMap.get(NodesCache),
                            dataSet, new NodeStreamFactoryV31());
                    dataSet.nodes = new StreamList<Node>(node31Loader);
                    break;
                case PatternV32:
                    EntityLoader<Node> node32Loader = getLoaderFor(new Header(reader), cacheMap.get(NodesCache),
                            dataSet, new NodeStreamFactoryV32());
                    dataSet.nodes = new StreamList<Node>(node32Loader);
                    break;
            }

            MemoryFixedList<Node> rootNodes = new MemoryFixedList<Node>(
                    dataSet, reader, new RootNodeFactory());
            dataSet.rootNodes = rootNodes;

            MemoryFixedList<ProfileOffset> profileOffsets =
                    new MemoryFixedList<ProfileOffset>(dataSet, reader,
                            new ProfileOffsetFactory());
            dataSet.profileOffsets = profileOffsets;

            //Read into memory all small lists which are frequently accessed.
            reader.setPos(components.header.getStartPosition());
            components.read(reader);
            reader.setPos(maps.header.getStartPosition());
            maps.read(reader);
            reader.setPos(properties.header.getStartPosition());
            properties.read(reader);
            reader.setPos(rootNodes.header.getStartPosition());
            rootNodes.read(reader);
            reader.setPos(profileOffsets.header.getStartPosition());
            profileOffsets.read(reader);

        } finally {
            if (reader != null) {
                dataSet.pool.release(reader);
            }
        }
    }
}
