package dev.loki.lovisual.events;

import dev.loki.lovisual.Lifecycle;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import dev.loki.lovisual.events.meta.EventHandler;

public final class EventBusTest {

    static class BaseEvent extends Event {
    }

    static class DerivedEvent extends BaseEvent {
    }

    @BeforeAll
    static void activateLifecycle() {
        // EventBus.post() доставляет события только при активном lifecycle.
        Lifecycle.activate();
    }

    @Test
    void postNullReturnsNull() {
        EventBus bus = new EventBus();
        assertNull(bus.post(null));
    }

    @Test
    void deliversToHandlerAndReturnsSameInstance() {
        EventBus bus = new EventBus();
        SingleListener listener = new SingleListener();
        bus.register(listener);

        BaseEvent event = new BaseEvent();
        assertSame(event, bus.post(event));
        assertTrue(listener.called);
    }

    @Test
    void higherPriorityRunsFirst() {
        EventBus bus = new EventBus();
        PriorityListener listener = new PriorityListener();
        bus.register(listener);

        bus.post(new BaseEvent());
        assertEquals(List.of("high", "low"), listener.calls);
    }

    @Test
    void subclassDispatchFiresDerivedThenBase() {
        EventBus bus = new EventBus();
        HierarchyListener listener = new HierarchyListener();
        bus.register(listener);

        bus.post(new DerivedEvent());
        assertEquals(List.of("derived", "base"), listener.calls);

        listener.calls.clear();
        bus.post(new BaseEvent());
        assertEquals(List.of("base"), listener.calls);
    }

    @Test
    void unregisterStopsDelivery() {
        EventBus bus = new EventBus();
        SingleListener listener = new SingleListener();
        bus.register(listener);
        bus.unregister(listener);

        bus.post(new BaseEvent());
        assertFalse(listener.called);
        assertFalse(bus.hasListeners(BaseEvent.class));
    }

    @Test
    void doubleRegistrationIgnored() {
        EventBus bus = new EventBus();
        SingleListener listener = new SingleListener();
        bus.register(listener);
        bus.register(listener);

        assertEquals(1, bus.listenerCount());
        bus.post(new BaseEvent());
        assertTrue(listener.called);
    }

    @Test
    void listenerWithoutValidHandlersIsIgnored() {
        EventBus bus = new EventBus();
        bus.register(new Object());
        bus.register(new InvalidSignaturesListener());

        assertEquals(0, bus.listenerCount());
        assertFalse(bus.hasListeners(BaseEvent.class));
    }

    @Test
    void handlerExceptionDoesNotStopOtherHandlers() {
        EventBus bus = new EventBus();
        ThrowingListener throwing = new ThrowingListener();
        SingleListener healthy = new SingleListener();
        bus.register(throwing);
        bus.register(healthy);

        assertDoesNotThrow(() -> bus.post(new BaseEvent()));
        assertTrue(healthy.called);
    }

    @Test
    void privateHandlerInvoked() {
        EventBus bus = new EventBus();
        PrivateListener listener = new PrivateListener();
        bus.register(listener);

        bus.post(new BaseEvent());
        assertTrue(listener.called);
    }

    @Test
    void hasListenersReflectsRegistration() {
        EventBus bus = new EventBus();
        assertFalse(bus.hasListeners(BaseEvent.class));

        SingleListener listener = new SingleListener();
        bus.register(listener);
        assertTrue(bus.hasListeners(BaseEvent.class));
        assertTrue(bus.hasListeners(DerivedEvent.class));
        assertFalse(bus.hasListeners(Event.class));
    }

    @Test
    void clearRemovesAllListeners() {
        EventBus bus = new EventBus();
        SingleListener listener = new SingleListener();
        bus.register(listener);
        bus.clear();

        assertEquals(0, bus.listenerCount());
        assertFalse(bus.hasListeners(BaseEvent.class));
        bus.post(new BaseEvent());
        assertFalse(listener.called);
    }

    @Test
    void nullAndUnknownListenersAreNoOps() {
        EventBus bus = new EventBus();
        assertDoesNotThrow(() -> {
            bus.register(null);
            bus.unregister(null);
            bus.unregister(new SingleListener());
        });
        assertEquals(0, bus.listenerCount());
    }

    static class SingleListener {
        boolean called;

        @EventHandler
        public void onBase(BaseEvent event) {
            called = true;
        }
    }

    static class PriorityListener {
        final List<String> calls = new ArrayList<>();

        @EventHandler(priority = 5)
        public void onHigh(BaseEvent event) {
            calls.add("high");
        }

        @EventHandler(priority = 1)
        public void onLow(BaseEvent event) {
            calls.add("low");
        }
    }

    static class HierarchyListener {
        final List<String> calls = new ArrayList<>();

        @EventHandler
        public void onBase(BaseEvent event) {
            calls.add("base");
        }

        @EventHandler
        public void onDerived(DerivedEvent event) {
            calls.add("derived");
        }
    }

    static class ThrowingListener {
        @EventHandler
        public void onBase(BaseEvent event) {
            throw new IllegalStateException("boom");
        }
    }

    static class PrivateListener {
        boolean called;

        @EventHandler
        private void onBase(BaseEvent event) {
            called = true;
        }
    }

    static class InvalidSignaturesListener {
        @EventHandler
        public void twoParams(BaseEvent event, int extra) {
        }

        @EventHandler
        public void notAnEvent(String value) {
        }
    }
}
