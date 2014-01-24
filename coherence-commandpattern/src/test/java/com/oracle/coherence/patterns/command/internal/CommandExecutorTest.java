package com.oracle.coherence.patterns.command.internal;

import com.oracle.coherence.common.finitestatemachines.AnnotationDrivenModel;
import org.junit.Test;
import sun.reflect.ReflectionFactory;

import java.lang.reflect.Constructor;

import static org.junit.Assert.fail;

/**
 * @author Jonathan Knight
 */
public class CommandExecutorTest
{
    /**
     * This test was added for JIRA COHINC-80 to assert that the CommandExecutor
     * class has valid annotated method signatures to be compatible with
     * the AnnotationDrivenModel.
     *
     * @throws Exception
     */
    @Test
    public void shouldBeValidForAnnotationDrivenModel() throws Exception
    {
        ReflectionFactory reflectionFactory = ReflectionFactory.getReflectionFactory();
        Constructor       objectConstructor = Object.class.getConstructor((Class[]) null);
        Constructor       mungedConstructor = reflectionFactory.newConstructorForSerialization(CommandExecutor.class,
                                                                                               objectConstructor);

        mungedConstructor.setAccessible(true);

        //Creates new CommandExecutor instance without calling its constructor
        Object commandExecutor = mungedConstructor.newInstance((Object[]) null);

        try
        {
            new AnnotationDrivenModel<CommandExecutor.State>(CommandExecutor.State.class, commandExecutor);
        }
        catch (Exception e)
        {
            e.printStackTrace();
            fail(String.format("Did not expect any exceptions but caught %s with message:\n%s",
                               e.getClass().getSimpleName(), e.getMessage()));
        }
    }

}
