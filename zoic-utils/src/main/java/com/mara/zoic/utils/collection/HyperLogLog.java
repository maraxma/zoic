package com.mara.zoic.utils.collection;

import com.mara.zoic.utils.numeric.Bitx;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

/**
 * HyperLogLog的Java实现。
 *
 * @author Mara.X.Ma
 * @since 1.0.0 2021-12-15
 */
public final class HyperLogLog {

    private final int bucketCount;
    private final byte bucketsCountPowerOfTwo;

    private final double hhlTooSmallThreshold;

    /**
     * 注册器，保存已加入的信息。
     * <p>声明为byte，尽量节省空间。</p>
     */
    private final byte[] register;

    private static final int MIN_BUCKETS_POWER_OF_TWO = 4;
    private static final int MAX_BUCKETS_POWER_OF_TWO = 32;

    private static final byte MAX_BITS = 64;

    /**
     * 构造一个HyperLogLog对象。
     *
     * @param bucketsCountPowerOfTwo 桶的个数，这里需要传入指数，底数是2，比如16，那么此参数应该设定为4
     *                               注意，这个参数至少应该是4，最大值不超过32
     */
    public HyperLogLog(byte bucketsCountPowerOfTwo) {
        if (bucketsCountPowerOfTwo < MIN_BUCKETS_POWER_OF_TWO || bucketsCountPowerOfTwo > MAX_BUCKETS_POWER_OF_TWO) {
            throw new IllegalArgumentException("bucketsCountPowerOfTwo limit: >=4 && <=32");
        }
        this.bucketsCountPowerOfTwo = bucketsCountPowerOfTwo;
        bucketCount = 1 << bucketsCountPowerOfTwo;
        register = new byte[bucketCount];
        hhlTooSmallThreshold = 5 * bucketCount / 2D;
    }

    /**
     * 构造一个HyperLogLog对象。
     * <p>桶的个数为2^14=16384个。是Redis中HyperLogLog的桶的个数。</p>
     */
    public HyperLogLog() {
        this((byte) 14);
    }

    public void add(String key) {
        // 计算hash
        long hash = hash(key);

        // 定位桶
        int idx = (int) Bitx.lowBits(hash, bucketsCountPowerOfTwo);

        // 计算值，值为剩余部分二进制从右至左第一次出现1的地方
        int firstOneIdx = -1;
        for (int i = 0; i < MAX_BITS - bucketsCountPowerOfTwo; i++) {
            if ((hash & (1L << (bucketsCountPowerOfTwo + i))) != 0) {
                firstOneIdx = i + 1;
                break;
            }
        }
        if (firstOneIdx != -1 && firstOneIdx > register[idx]) {
            register[idx] = (byte) firstOneIdx;
        }
    }

    public long count() {
        double alpha = getAlpha();
        int zeroCount = 0;
        double invSum = 0;
        for (byte num : register) {
            if (num == 0) {
                zeroCount++;
            }
            invSum += 1D / (1 << num);
        }
        double est = alpha * bucketCount * bucketCount / invSum;
        if (zeroCount != 0 && est < hhlTooSmallThreshold) {
            return (long) (bucketCount * Math.log(bucketCount * 1D / zeroCount));
        } else {
            return (long) est;
        }
    }

    /**
     * 根据桶的个数获得一个特征值。
     * @return
     */
    private double getAlpha() {
        switch (bucketCount) {
            case 1/*2^0*/:
            case 2/*2^1*/:
            case 4/*2^2*/:
            case 8/*2^3*/:
                throw new IllegalArgumentException("'m' cannot be less than 16 (" + bucketCount + " < 16).");

            case 16/*2^4*/:
                return 0.673;

            case 32/*2^5*/:
                return 0.697;

            case 64/*2^6*/:
                return 0.709;

            default/*>2^6*/:
                return 0.7213 / (1.0 + 1.079 / bucketCount);
        }
    }

    public long hash(String key) {
        ByteBuffer buf = ByteBuffer.wrap(key.getBytes());
        int seed = 0x1234ABCD;

        ByteOrder byteOrder = buf.order();
        buf.order(ByteOrder.LITTLE_ENDIAN);

        long m = 0xc6a4a7935bd1e995L;
        int r = 47;

        long h = seed ^ (buf.remaining() * m);

        long k;
        while (buf.remaining() >= 8) {
            k = buf.getLong();

            k *= m;
            k ^= k >>> r;
            k *= m;

            h ^= k;
            h *= m;
        }

        if (buf.remaining() > 0) {
            ByteBuffer finish = ByteBuffer.allocate(8).order(ByteOrder.LITTLE_ENDIAN);
            finish.put(buf).rewind();
            h ^= finish.getLong();
            h *= m;
        }

        h ^= h >>> r;
        h *= m;
        h ^= h >>> r;

        buf.order(byteOrder);
        return h;
    }

    /**
     * 计算需要多少的位来存储特征数据。
     * <p>这是为极值节省内存而存在的，计算出来后可以以较少的变量来存储特HyperLogLog特征信息。</p>
     *
     * @return 需要多少的位来存储特征数据
     */
    private int computeSpace() {
        // 特征最大即为最高位是1
        byte featureBitCount = (byte) (MAX_BITS - bucketsCountPowerOfTwo);
        // 计算需要至少需要多少个bit来存储
        byte bitCountAtLeast = Bitx.bitCountAtLeast(featureBitCount);
        // 计算需要多少个long来存储
        return new BigDecimal(bitCountAtLeast * bucketCount).divide(new BigDecimal(MAX_BITS), RoundingMode.UP).setScale(0, RoundingMode.UP).intValue();
    }

    public static void main(String[] args) {
         HyperLogLog hyperLogLog = new HyperLogLog((byte) 17);
         int exact = 10000000;
         for (int i = 1; i <= exact; i++) {
             hyperLogLog.add(i + "");
             long count = hyperLogLog.count();
             System.out.println("实际: " + i + ", HLL: " + count + ", 误差: " + (1D - Math.abs(i * 1D/ count)) * 100 +
                     "%");
         }

        // System.out.println(bitSizeFor(2));

        System.out.println(new BigDecimal("1.213456").setScale(1, RoundingMode.UP));
    }
}
