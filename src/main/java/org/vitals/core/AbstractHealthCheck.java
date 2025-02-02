package org.vitals.core;

import static org.vitals.core.util.Util.validateName;
import static org.vitals.core.util.Util.validateTags;

import java.util.Collections;
import java.util.Set;

public abstract class AbstractHealthCheck implements HealthCheck {
    private final String name;
    private final Set<String> tags;

    protected AbstractHealthCheck(String name, Set<String> tags) {
        validateName(name);
        validateTags(tags);

        this.name = name;
        this.tags = tags;
    }

    protected AbstractHealthCheck(String name, String... tags) {
        this(name, Set.of(tags));
    }

    protected AbstractHealthCheck(String name) {
        this(name, Collections.emptySet());
    }

    @Override
    public String getName() {
        return this.name;
    }

    @Override
    public Set<String> getTags() {
        return this.tags;
    }

    public AbstractHealthCheck addTag(String tag) {
        this.tags.add(tag);
        return this;
    }

    public AbstractHealthCheck removeTag(String tag) {
        this.tags.remove(tag);
        return this;
    }
}
