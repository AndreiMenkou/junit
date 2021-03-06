package org.junit.tests.experimental.rules;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestRule;
import org.junit.rules.Timeout;
import org.junit.runner.JUnitCore;
import org.junit.runner.Result;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;

import static org.hamcrest.CoreMatchers.containsString;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;

public class TimeoutRuleTest {
    private static final ReentrantLock run1Lock = new ReentrantLock();

    private static volatile boolean run4done = false;

    public abstract static class AbstractTimeoutTest {
        public static final StringBuffer logger = new StringBuffer();

        @Test
        public void run1() throws InterruptedException {
            logger.append("run1");
            TimeoutRuleTest.run1Lock.lockInterruptibly();
            TimeoutRuleTest.run1Lock.unlock();
        }

        @Test
        public void run2() throws InterruptedException {
            logger.append("run2");
            Thread.currentThread().join();
        }

        @Test
        public synchronized void run3() throws InterruptedException {
            logger.append("run3");
            wait();
        }

        @Test
        public void run4() {
            logger.append("run4");
            while (!run4done) {
            }
        }
    }

    public static class HasGlobalLongTimeout extends AbstractTimeoutTest {

        @Rule
        public final TestRule globalTimeout = Timeout.millis(50L);
    }

    public static class HasGlobalTimeUnitTimeout extends AbstractTimeoutTest {

        @Rule
        public final TestRule globalTimeout = new Timeout(50, TimeUnit.MILLISECONDS);
    }

    @Before
    public void before() {
        run4done = false;
        run1Lock.lock();
    }

    @After
    public void after() {
        run4done = true;//to make sure that the thread won't continue at run4()
        run1Lock.unlock();
    }

    @Test
    public void timeUnitTimeout() throws InterruptedException {
        HasGlobalTimeUnitTimeout.logger.setLength(0);
        Result result = JUnitCore.runClasses(HasGlobalTimeUnitTimeout.class);
        assertEquals(4, result.getFailureCount());
        assertThat(HasGlobalTimeUnitTimeout.logger.toString(), containsString("run1"));
        assertThat(HasGlobalTimeUnitTimeout.logger.toString(), containsString("run2"));
        assertThat(HasGlobalTimeUnitTimeout.logger.toString(), containsString("run3"));
        assertThat(HasGlobalTimeUnitTimeout.logger.toString(), containsString("run4"));
    }

    @Test
    public void longTimeout() throws InterruptedException {
        HasGlobalLongTimeout.logger.setLength(0);
        Result result = JUnitCore.runClasses(HasGlobalLongTimeout.class);
        assertEquals(4, result.getFailureCount());
        assertThat(HasGlobalLongTimeout.logger.toString(), containsString("run1"));
        assertThat(HasGlobalLongTimeout.logger.toString(), containsString("run2"));
        assertThat(HasGlobalLongTimeout.logger.toString(), containsString("run3"));
        assertThat(HasGlobalLongTimeout.logger.toString(), containsString("run4"));
    }
}
