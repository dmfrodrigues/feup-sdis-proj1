package sdis.Runnables;

/**
 * Base protocol callable.
 *
 * Should, under no circumstance, throw any exceptions.
 */
public abstract class BaseProtocolCallable extends ProtocolCallable {
    public abstract Void call();
}
