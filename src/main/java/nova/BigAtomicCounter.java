package nova;

import java.math.BigInteger;
import java.util.concurrent.atomic.AtomicReference;

public class BigAtomicCounter {
    private final AtomicReference<BigInteger> value;

    public BigAtomicCounter() {
        this(BigInteger.ZERO);
    }

    public BigAtomicCounter(BigInteger initialValue) {
        value = new AtomicReference<>(initialValue);
    }

    public BigInteger incrementAndGet() {
        while (true) {
            BigInteger current = value.get();
            BigInteger next = current.add(BigInteger.ONE);
            if (value.compareAndSet(current, next)) {
                return next;
            }
        }
    }

    public BigInteger getAndIncrement() {
        while (true) {
            BigInteger current = value.get();
            BigInteger next = current.add(BigInteger.ONE);
            if (value.compareAndSet(current, next)) {
                return current;
            }
        }
    }

    public BigInteger get() {
        return value.get();
    }

    public void set(BigInteger newValue) {
        value.set(newValue);
    }
}