package com.mara.zoic.utils.numeric;

/**
 * 提供对Bit的操作方法（位运算）。
 * @author Mara.X.Ma
 * @since 1.0.0
 */
public class Bitx {

    /**
     * 判断一个数是否是奇数。
     * @param value 任意数
     * @return 奇数true，偶数false
     */
    public static boolean isOdd(long value) {
        return (value & 1) == 1;
    }

    /**
     * 判断一个数是否是偶数。
     * @param value 任意数
     * @return 偶数true，奇数false
     */
    public static boolean isEven(long value) {
        return (value & 1) == 0;
    }

    /**
     * 取得一个数的指定位区间上的数。
     * @param value 数字
     * @param lowIndex 低位下标（含，从0开始）
     * @param highIndex 高位下标（不含，必须大于lowIndex，最大64）
     * @return 取得后的数，不足的位使用0填充
     */
    public static long seg(long value, int lowIndex, int highIndex) {
        checkRange(lowIndex, "lowIndex");
        checkRange(lowIndex, "highIndex");
        checkLowHigh(lowIndex, highIndex);

        // 高位置0
        value &= repeatOne(highIndex - lowIndex + 1);

        // 去除低位
        value >>= lowIndex;

        return value;
    }

    /**
     * 取得一个数的位片段并以字符串二进制形式输出。
     * @param value 数字
     * @param lowIndex 低位下标（含，从0开始）
     * @param highIndex 高位下标（不含，必须大于lowIndex，最大64）
     * @return 片段的二进制字符串形式
     */
    public static String segStr(long value, int lowIndex, int highIndex) {
        return Long.toHexString(seg(value, lowIndex, highIndex));
    }

    /**
     * 将指数字二进制中指定index上的位设定为0。
     * @param value 数字
     * @param index 下标，从0开始，最大63
     * @return 设定后的数字
     */
    public static long bitToZero(long value, int index) {
        checkIndex(index);
        return value & (~(1L << index));
    }

    private static void checkIndex(int index) {
        if (index < 0 || index > 63) {
            throw new IllegalArgumentException("index must LTE 63 and GTE 0");
        }
    }

    /**
     * 将指定数字二进制区间上的位设为0.
     * @param value 数字
     * @param lowIndex 低位下标（含，从0开始）
     * @param highIndex 高位下标（不含，必须大于lowIndex，最大64）
     * @return 设定后的数字
     */
    public static long bitToZero(long value, int lowIndex, int highIndex) {
        checkRange(lowIndex, "lowIndex");
        checkRange(lowIndex, "highIndex");
        checkLowHigh(lowIndex, highIndex);

        long mask = repeatOne(highIndex - lowIndex);
        mask <<= lowIndex;

        return value & (~mask);
    }

    /**
     * 将指数字二进制中指定index上的位设定为1。
     * @param value 数字
     * @return 设定后的数字
     */
    public static long bitToOne(long value, int index) {
        return value | (1L << index);
    }

    /**
     * 将指定数字二进制区间上的位设为1.
     * @param value 数字
     * @param lowIndex 低位下标（含，从0开始）
     * @param highIndex 高位下标（不含，必须大于lowIndex，最大64）
     * @return 设定后的数字
     */
    public static long bitToOne(long value, int lowIndex, int highIndex) {
        checkRange(lowIndex, "lowIndex");
        checkRange(lowIndex, "highIndex");
        checkLowHigh(lowIndex, highIndex);

        long mask = repeatOne(highIndex - lowIndex);
        mask <<= lowIndex;

        return value | mask;
    }

    /**
     * 将指定数字的二进制区间上的位设定为指定的数字中的位。
     * @param value 数字
     * @param lowIndex 低位下标（含，从0开始）
     * @param highIndex 高位下标（不含，必须大于lowIndex，最大64）
     * @param expected 期望设定的数字，优先使用这个数字的低位填充到目标数字指定的区间
     * @return 设定后的数字
     */
    public static long bitToExpected(long value, int lowIndex, int highIndex, long expected) {
        checkRange(lowIndex, "lowIndex");
        checkRange(lowIndex, "highIndex");
        checkLowHigh(lowIndex, highIndex);

        long removed = bitToZero(value, lowIndex, highIndex);

        return removed | (expected << lowIndex);
    }

    /**
     * 将指定数字的二进制区间上的位设定为指定的数字中的位（较慢）。
     * @param value 数字
     * @param lowIndex 低位下标（含，从0开始）
     * @param highIndex 高位下标（不含，必须大于lowIndex，最大64）
     * @param expected 字符串形式的期望设定的数字，必须是二进制形式
     * @return 设定后的数字
     */
    public static long bitToExpected(long value, int lowIndex, int highIndex, String expected) {
        return bitToExpected(value, lowIndex, highIndex, Long.parseLong(expected, 2));
    }

    /**
     * 生成二进制全是1的数字。
     * @param times 重复多少次1
     * @return 生成的数字
     */
    public static long repeatOne(int times) {
        if (times == 0) {
            return 0;
        }
        checkRange(times, "times");
        long digital = 1;
        for (int i = 1; i < times; i++) {
            digital = (digital << 1) | 1;
        }
        return digital;
    }

    public static long repeatOneZero(int times) {
        if (times == 0) {
            return 0;
        }
        checkRange(times, "times");
        long digital = 2; // 10b
        for (int i = 1; i < times; i++) {
            digital = (digital << 2) | 2;
        }
        return digital;
    }

    public static long repeatZeroOne(int times) {
        if (times == 0) {
            return 0;
        }
        checkRange(times, "times");
        long digital = 1;
        for (int i = 1; i < times; i++) {
            digital = (digital << 2) | 1;
        }
        return digital;
    }

    public static long lshift(long value, long shifts) {
        int validBits = validBits(shifts);
        return bitToExpected(value, Long.SIZE - validBits, Long.SIZE, shifts);
    }

    public static long rshift(long value, long shifts) {
        int validBits = validBits(shifts);
        return bitToExpected(value, 0, validBits, shifts);
    }

    /**
     * 统计一个数的二进制中有多少有效位。
     * @param flag 数
     * @return 有效位，如00010100中有5个有效位
     */
    public static int validBits(long flag) {
        // if (flag == 0) {
        //     return 0;
        // }
        // byte validBitCount = 0;
        // byte shift = 0;
        // long mask = -1;
        // while ((flag & mask) != 0) {
        //     validBitCount++;
        //     mask &= ~(1L << shift++);
        // }
        // return validBitCount;
        return (byte) (Long.SIZE - Long.numberOfLeadingZeros(flag));
    }

    /**
     * 将一个数字的最低位的1变为0。
     * @param value 在哪个数字上操作
     * @return 数字
     */
    public static long lowestBitOneToZero(long value) {
        return value & (value - 1);
    }

    /**
     * 获得一个数字的低len位。
     * @param value 在哪个数上操作
     * @param len 长度
     * @return 提取出来的数字
     */
    public static long lowBits(long value, int len) {
        checkRange(len, "len");
        return value & (repeatOne(len));
    }

    /**
     * 获得一个数字的高len位。
     * @param value 在哪个数上操作
     * @param len 长度
     * @return 提取出来的数字
     */
    public static long highBits(long value, byte len) {
        checkRange(len, "len");
        return value >> (validBits(value) - len);
    }

    /**
     * 获得指定位置上的bit。
     * @param value 在哪个数字上操作
     * @param index 从哪一位开始（从最低位0开始计数）
     * @return 1或0
     */
    public static byte bit(long value, byte index) {
        return (byte) ((value >> index) & 1);
    }

    /**
     * 获得指定数字上的以最后一个1作为最高位组成的数字。
     * @param value 在哪个数字上操作
     * @return 数字
     */
    public static long lowestOneBitNumber(long value) {
        // 等同于 flag & ((~flag) + 1)
        return value & -value;
    }

    /**
     * 计算要表示目标数至少需要多少位二进制。
     * @param value 目标数
     * @return 多少位二进制
     */
    public static byte bitCountAtLeast(long value) {
        byte i  = 1;
        long n = 1;
        while (n < value) {
            n = (n << 1) | n;
            i++;
        }
        return i;
    }

    private static void checkRange(long range, String name) {
        if (range < 0 || range > 64) {
            throw new IllegalArgumentException("Invalid `" + name + "` (Must 0 <= X <= 64)");
        }
    }

    private static void checkLowHigh(int lowIndex, int highIndex) {
        if (lowIndex > highIndex) {
            throw new IllegalArgumentException("lowIndex must lower than or equals to highIndex");
        }
    }

    public static void main(String[] args) {
        System.out.println(Long.toBinaryString(repeatOne(3)));
        System.out.println(Long.toBinaryString(repeatZeroOne(3)));
        System.out.println(Long.toBinaryString(repeatOneZero(3)));

        System.out.println(Long.toBinaryString(seg(107, 1, 6)));

        System.out.println(Long.toBinaryString(bitToZero(repeatOne(5), 1, 4)));

        System.out.println(Long.toBinaryString(bitToOne(0, 3)));
        System.out.println(Long.toBinaryString(bitToOne(0, 1, 4)));

        System.out.println(Long.toBinaryString(bitToExpected(repeatOne(7), 1, 6, "10010")));
    }

}
