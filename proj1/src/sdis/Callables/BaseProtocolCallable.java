package sdis.Callables;

/**
 * Base protocol callable.
 *
 * Should, under no circumstance, throw any exceptions.
 */
public abstract class BaseProtocolCallable extends ProtocolCallable<Void> {
    public abstract Void call();
}
