package sdis.Callables;

import sdis.Exceptions.ProtocolException;

import java.util.concurrent.Callable;

/**
 * Protocol callable.
 *
 * Can (and should) throw a ProtocolException when it fails.
 */
public abstract class ProtocolCallable<T> implements Callable<T> {
    public abstract T call() throws ProtocolException;
}
