package au.com.glob.clodmc.command;

import org.jspecify.annotations.NullMarked;

/** base interface for command execution */
@NullMarked
public sealed interface Executor
    permits ExecutorE,
        ExecutorEP,
        ExecutorEPS,
        ExecutorES,
        ExecutorESP,
        ExecutorESS,
        ExecutorP,
        ExecutorPS {}
