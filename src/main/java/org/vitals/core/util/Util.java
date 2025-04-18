package org.vitals.core.util;

import com.google.common.base.Preconditions;
import org.slf4j.Logger;

import java.util.Set;

public class Util {
    private Util() {
        // only static util methods
    }

    public static void logTrace(Logger logger, String message) {
        if (logger.isTraceEnabled()) {
            logger.trace(message);
        }
    }

    public static void validateName(String name) {
        Preconditions.checkNotNull(name, "Health check healthCheckName must not be null");
        Preconditions.checkArgument(!name.trim().isEmpty(), "Health check healthCheckName must not be empty");
    }

    public static void validateTag(String tag) {
        Preconditions.checkNotNull(tag, "Tag must not be null");
        Preconditions.checkArgument(!tag.trim().isEmpty(), "Tag must not be empty");
    }

    public static void validateTags(Set<String> tags) {
        Preconditions.checkNotNull(tags, "Tags must not be null");
        tags.forEach(Util::validateTag);
    }
}
