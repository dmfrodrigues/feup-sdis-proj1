package sdis.Callables;

import java.util.function.Supplier;

/**
 * Protocol supplier.
 *
 * Can (and should) throw a ProtocolException when it fails.
 */
public abstract class ProtocolSupplier<T> implements Supplier<T> {
}
