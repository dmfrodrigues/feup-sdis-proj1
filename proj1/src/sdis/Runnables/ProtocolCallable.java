package sdis.Runnables;

import sdis.Exceptions.ProtocolException;

import java.util.concurrent.Callable;

/**
 * Protocol callable.
 *
 * Can (and should) throw a ProtocolException when it fails.
 */
public abstract class ProtocolCallable implements Callable<Void> {
    public abstract Void call() throws ProtocolException;
}
