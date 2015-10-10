package fiftyone.mobile.detection;

import fiftyone.mobile.detection.common.Results;
import fiftyone.properties.MatchMethods;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.rules.TestName;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.Map;

import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.Assert.assertTrue;

/**
 * Supporting methods for detection tests, and common formatting e.g.
 * top and tail each test with a header to allow easier identification
 * when running tests
 */
public class DetectionTestSupport {

    protected Logger logger = LoggerFactory.getLogger(this.getClass());

    @Rule
    public TestName testName = new TestName();

    private long startTime;

    @Before
    public void beforeHeader() {
        logger.info("*******************************");
        logger.info("* Starting test {} {}", this.getClass().getSimpleName(), getMethodName());
        logger.info("*******************************");
        startTime = System.currentTimeMillis();
    }

    @After
    public void afterHeader() {
        logger.info("*******************************");
        logger.info("* Finished test {} in {} millis", getMethodName(), System.currentTimeMillis() - startTime);
        logger.info("*******************************");
    }

    /**
     * Asserts that the file exists. Used at the beginning of each test.
     * @param dataFile
     */
    public static void assertFileExists(String dataFile) {
        assertTrue(String.format(
                        "Data file '%s' could not be found. " +
                                "See https://51degrees.com/compare-data-options to complete this test.",
                        dataFile),
                new File(dataFile).exists());
    }

    public static void assertPool(Provider provider) {
        if (provider.dataSet instanceof fiftyone.mobile.detection.entities.stream.Dataset) {
            fiftyone.mobile.detection.entities.stream.Dataset dataSet =
                    (fiftyone.mobile.detection.entities.stream.Dataset)provider.dataSet;

            // Check the size of the reader queues for equality now time has
            // passed and readers should have been returned to it.
            int queued = dataSet.getReadersQueued();
            int created = dataSet.getReadersCreated();
            assertTrue(
                    String.format(
                            "DataSet pooled readers mismatched. '%d' created and '%d' queued.",
                            created,
                            queued),
                    created == queued);
        }
    }

    public static void AssertCacheMissesGoodAll(Dataset dataSet) {
        assertTrue(String.format("Signature Cache Misses @ %.0f%%", dataSet.getPercentageSignatureCacheMisses() * 100), dataSet.getPercentageSignatureCacheMisses() < 0.4);
        assertTrue(String.format("Strings Cache Misses @ %.0f%%", dataSet.getPercentageStringsCacheMisses() * 100), dataSet.getPercentageStringsCacheMisses() < 0.6);
        assertTrue(String.format("Ranked Signatures Cache Misses @ %.0f%%", dataSet.getPercentageRankedSignatureCacheMisses() * 100), dataSet.getPercentageRankedSignatureCacheMisses() < 0.5);
        assertTrue(String.format("Node Cache Misses @ %.0f%%", dataSet.getPercentageNodeCacheMisses() * 100), dataSet.getPercentageNodeCacheMisses() < 0.3);
        assertTrue(String.format("Value Cache Misses @ %.0f%%", dataSet.getPercentageValuesCacheMisses() * 100), dataSet.getPercentageValuesCacheMisses() < 0.3);
        assertTrue(String.format("Profile Cache Misses @ %.0f%%", dataSet.getPercentageProfilesCacheMisses() * 100), dataSet.getPercentageProfilesCacheMisses() < 0.3);
    }

    public static void AssertCacheMissesGood(Dataset dataSet) {
        assertTrue(String.format("Signature Cache Misses @ %.0f%%", dataSet.getPercentageSignatureCacheMisses() * 100), dataSet.getPercentageSignatureCacheMisses() < 0.4);
        assertTrue(String.format("Strings Cache Misses @ %.0f%%", dataSet.getPercentageStringsCacheMisses() * 100), dataSet.getPercentageStringsCacheMisses() < 0.5);
        assertTrue(String.format("Ranked Signatures Cache Misses @ %.0f%%", dataSet.getPercentageRankedSignatureCacheMisses() * 100), dataSet.getPercentageRankedSignatureCacheMisses() < 0.5);
        assertTrue(String.format("Node Cache Misses @ %.0f%%", dataSet.getPercentageNodeCacheMisses() * 100), dataSet.getPercentageNodeCacheMisses() < 0.3);
    }

    public static void AssertCacheMissesBadAll(Dataset dataSet) {
        assertTrue(String.format("Signature Cache Misses @ %.0f%%", dataSet.getPercentageSignatureCacheMisses() * 100), dataSet.getPercentageSignatureCacheMisses() < 0.4);
        assertTrue(String.format("Strings Cache Misses @ %.0f%%", dataSet.getPercentageStringsCacheMisses() * 100), dataSet.getPercentageStringsCacheMisses() < 0.5);
        assertTrue(String.format("Ranked Signatures Cache Misses @ %.0f%%", dataSet.getPercentageRankedSignatureCacheMisses() * 100), dataSet.getPercentageRankedSignatureCacheMisses() < 0.5);
        assertTrue(String.format("Node Cache Misses @ %.0f%%", dataSet.getPercentageNodeCacheMisses() * 100), dataSet.getPercentageNodeCacheMisses() < 0.5);
        assertTrue(String.format("Value Cache Misses @ %.0f%%", dataSet.getPercentageValuesCacheMisses() * 100), dataSet.getPercentageValuesCacheMisses() < 0.3);
        assertTrue(String.format("Profile Cache Misses @ %.0f%%", dataSet.getPercentageProfilesCacheMisses() * 100), dataSet.getPercentageProfilesCacheMisses() < 0.3);
    }

    public static void AssertCacheMissesBad(Dataset dataSet) {
        assertTrue(String.format("Signature Cache Misses @ %.0f%%", dataSet.getPercentageSignatureCacheMisses() * 100), dataSet.getPercentageSignatureCacheMisses() < 0.4);
        assertTrue(String.format("Strings Cache Misses @ %.0f%%", dataSet.getPercentageStringsCacheMisses() * 100), dataSet.getPercentageStringsCacheMisses() < 0.8);
        assertTrue(String.format("Ranked Signatures Cache Misses @ %.0f%%", dataSet.getPercentageRankedSignatureCacheMisses() * 100), dataSet.getPercentageRankedSignatureCacheMisses() < 0.5);
        assertTrue(String.format("Node Cache Misses @ %.0f%%", dataSet.getPercentageNodeCacheMisses() * 100), dataSet.getPercentageNodeCacheMisses() < 0.5);
    }

    /**
     * Reads the contents of the file provided and returns a byte array.
     * @param file to be read
     * @return a byte array of the file content
     * @throws IOException if the file size is more than 2GB
     */
    public static byte[] readAllBytes(String file) throws IOException {
        return readAllBytes(new File(file));
    }

    /**
     * Reads the contents of the file provided and returns a byte array.
     * @param file to be read
     * @return a byte array of the file content
     * @throws IOException if the file size is more than 2GB
     */
    public static byte[] readAllBytes(File file) throws IOException {
        RandomAccessFile fileHandle = new RandomAccessFile(file, "r");
        try {
            long longlength = fileHandle.length();
            int length = (int)longlength;
            if (length != longlength)
                throw new IOException("File size cannot be greater than 2GB");
            byte[] array = new byte[length];
            fileHandle.readFully(array);
            return array;
        } finally {
            fileHandle.close();
        }
    }

    public static void reportMethods(Map<MatchMethods, AtomicInteger> methods) {
        int total = 0;
        for(AtomicInteger value : methods.values()){
            total += value.intValue();
        }
        for(Map.Entry<MatchMethods, AtomicInteger> method : methods.entrySet()) {
            System.out.printf("Method '%s' used '%.0f%%'\r\n",
                    method.getKey(),
                    (double)method.getValue().intValue() / (double)total * (double)100);
        }
    }

    public static void reportPool(fiftyone.mobile.detection.entities.stream.Dataset dataSet) {
        System.out.printf("Readers in queue '%d'\r\n", dataSet.getReadersQueued());
        System.out.printf("Readers created '%d'\r\n", dataSet.getReadersCreated());
    }

    public static void reportCache(Dataset dataSet) {
        System.out.printf("Node cache switches '%d' with '%.0f%%' misses\r\n",
                dataSet.getNodeCacheSwitches(),
                dataSet.getPercentageNodeCacheMisses() * (double)100);
        System.out.printf("Profiles cache switches '%d' with '%.0f%%' misses\r\n",
                dataSet.getProfilesCacheSwitches(),
                dataSet.getPercentageProfilesCacheMisses() * (double)100);
        System.out.printf("Ranked Signatures cache switches '%d' with '%.0f%%' misses\r\n",
                dataSet.getRankedSignatureCacheSwitches(),
                dataSet.getPercentageRankedSignatureCacheMisses() * (double)100);
        System.out.printf("Signatures cache switches '%d' with '%.0f%%' misses\r\n",
                dataSet.getSignatureCacheSwitches(),
                dataSet.getPercentageSignatureCacheMisses() * (double)100);
        System.out.printf("Strings cache switches '%d' with '%.0f%%' misses\r\n",
                dataSet.getStringsCacheSwitches(),
                dataSet.getPercentageStringsCacheMisses() * (double)100);
        System.out.printf("Values cache switches '%d' with '%.0f%%' misses\r\n",
                dataSet.getValuesCacheSwitches(),
                dataSet.getPercentageValuesCacheMisses() * (double)100);
    }

    public static void reportTime(Results results) {
        System.out.printf("Total of '%.2f's for '%d' tests.\r\n",
                (double)results.getElapsedTime() / (double)1000,
                results.count.intValue());
        System.out.printf("Average '%.2f'ms per test.\r\n",
                (double)results.getElapsedTime() / results.count.doubleValue());
    }

    public static void reportProvider(Provider provider) {
        System.out.printf("User-Agent cache switches '%d' with '%.0f%%' misses\r\n",
                provider.getCacheSwitches(),
                provider.getPercentageCacheMisses());
        if (provider.dataSet instanceof fiftyone.mobile.detection.entities.stream.Dataset)
        {
            reportCache(provider.dataSet);
            reportPool((fiftyone.mobile.detection.entities.stream.Dataset)provider.dataSet);
        }
    }

    public TestName getTestName() {
        return testName;
    }

    public String getMethodName() {
        return getTestName().getMethodName();
    }
}
