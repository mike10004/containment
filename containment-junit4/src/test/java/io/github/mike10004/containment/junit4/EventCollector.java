package io.github.mike10004.containment.junit4;

import io.github.mike10004.containment.lifecycle.LifecycleEvent;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Consumer;
import java.util.stream.Collectors;

class EventCollector implements Consumer<LifecycleEvent> {

    public final List<LifecycleEvent> events = Collections.synchronizedList(new ArrayList<>());

    @Override
    public final void accept(LifecycleEvent lifecycleEvent) {
        System.out.println(lifecycleEvent);
        events.add(lifecycleEvent);
    }

    public List<LifecycleEvent.Category> categories() {
        return events.stream().map(LifecycleEvent::getCategory).collect(Collectors.toList());
    }
}
