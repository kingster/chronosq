package flipkart.cp.convert.chronosQ.redis;

import flipkart.cp.convert.chronosQ.core.DefaultSchedulerEntry;
import flipkart.cp.convert.chronosQ.core.SchedulerEntry;
import flipkart.cp.convert.chronosQ.core.SchedulerStore;
import flipkart.cp.convert.chronosQ.exceptions.SchedulerException;
import flipkart.cp.convert.chronosQ.impl.redis.RedisParitioner;
import flipkart.cp.convert.chronosQ.impl.redis.RedisSchedulerStore;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import redis.clients.jedis.JedisPool;

import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

/**
 * Created with IntelliJ IDEA.
 * User: barani.subramanian
 * Date: 23/02/15
 * Time: 1:53 PM
 * To change this template use File | Settings | File Templates.
 */
public class RedisSchedulerStoreTest {
    @Mock
    private ArrayList<JedisPool> poolArrayList;
    private SchedulerStore redisSchedulerStore;
    @Mock
    private JedisPool jedisPool;
    @Mock
    private RedisParitioner redisParitioner;

    private final long dataStoreReturnValue = 1;
    private final long dataStoreCheckValue = 0;
    private final String value = "testSourceValue";
    private final int firstKey = 500;
    private final int secondKey = 520;
    private final int thirdKey = 1200;
    private final int partitionNum = 1;
    private final int newKey = 720;
    private final int removalKey = 800;
    private final int n = 2;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        redisSchedulerStore = new RedisSchedulerStore(redisParitioner, "testCase");
        jedisPool = new JedisPool("localhost", 6379);
        when(redisParitioner.getJedis(partitionNum)).thenReturn(jedisPool);
    }

    @Test
    public void addToDataStore() throws SchedulerException {
        redisSchedulerStore.add(new DefaultSchedulerEntry(value), firstKey, partitionNum);
        checkRedisPartitionerFlow(1);
    }

    @Test(expected = SchedulerException.class)
    public void notAddToDataStore() throws SchedulerException {
        returnNullRedis();
        redisSchedulerStore.add(new DefaultSchedulerEntry(value), firstKey, partitionNum);
        checkRedisPartitionerFlow(1);
    }

    @Test
    public void updateDataStore() throws SchedulerException {
        Long result = redisSchedulerStore.update(new DefaultSchedulerEntry(value), firstKey, secondKey, partitionNum);
        assertTrue(result == dataStoreReturnValue);
        checkRedisPartitionerFlow(1);
    }

    @Test(expected = SchedulerException.class)
    public void notUpdateDataStore() throws SchedulerException {
        returnNullRedis();
        Long result = redisSchedulerStore.update(new DefaultSchedulerEntry(value), secondKey, firstKey, 1);
        assertFalse(result == dataStoreReturnValue);
        checkRedisPartitionerFlow(1);
    }

    @Test
    public void getOldKeyFromDataStore() throws SchedulerException {
        List<SchedulerEntry> value = redisSchedulerStore.get(firstKey, partitionNum);
        assertEquals(0, value.size());
        checkRedisPartitionerFlow(1);
    }


    @Test(expected = SchedulerException.class)
    public void notRemoveDataStore() throws SchedulerException {
        returnNullRedis();
        Long result = redisSchedulerStore.remove(value, firstKey, partitionNum);
        assertTrue(result == dataStoreCheckValue);
        checkRedisPartitionerFlow(1);
    }

    @Test
    public void keyNotFound() throws SchedulerException {
        redisSchedulerStore.remove(value, newKey, partitionNum);
        checkRedisPartitionerFlow(1);
    }

    @Test
    public void addTestKey() throws SchedulerException {
        redisSchedulerStore.add(new DefaultSchedulerEntry(value), removalKey, partitionNum);
        redisSchedulerStore.add(new DefaultSchedulerEntry(value + "x"), secondKey, partitionNum);
        redisSchedulerStore.add(new DefaultSchedulerEntry(value + "y"), secondKey, partitionNum);
        redisSchedulerStore.add(new DefaultSchedulerEntry(value + "z"), secondKey, partitionNum);
    }

    @Test
    public void removeFromDataStore() throws SchedulerException {
        Long result = redisSchedulerStore.remove(value, removalKey, partitionNum);
        assertTrue(result == dataStoreReturnValue);
        checkRedisPartitionerFlow(1);
    }

    @Test
    public void getNFromSetVerifyData() throws SchedulerException {
        redisSchedulerStore.add(new DefaultSchedulerEntry("key1"), thirdKey, partitionNum);
        redisSchedulerStore.add(new DefaultSchedulerEntry("key2", "value2"), thirdKey, partitionNum);
        List<SchedulerEntry> result = redisSchedulerStore.getNextN(thirdKey, partitionNum, 2);
        assertTrue(result.size() == 2);

        assertEquals("key1", result.get(0).getKey());
        assertEquals("key2", result.get(1).getKey());
        assertEquals("value2", result.get(1).getPayload());
        checkRedisPartitionerFlow(4);
    }

    @Test
    public void getNFromSet() throws SchedulerException {
        int result = redisSchedulerStore.getNextN(secondKey, partitionNum, n).size();
        assertTrue(result == n);
        checkRedisPartitionerFlow(2);
    }


    @Test
    public void removeNFromSet() throws SchedulerException {
        List<String> values = new ArrayList<String>();
        values.add(value + "x");
        values.add(value + "y");
        redisSchedulerStore.removeBulk(secondKey, partitionNum, values);
        checkRedisPartitionerFlow(1);

    }

    private void checkRedisPartitionerFlow(int wantedinvocations) {
        verify(redisParitioner, times(wantedinvocations)).getJedis(partitionNum);
        verifyZeroInteractions(redisParitioner);
    }

    private void returnNullRedis() {
        when(redisParitioner.getJedis(partitionNum)).thenReturn(null);
    }


}

