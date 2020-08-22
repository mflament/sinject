package org.yah.sinject;

import org.yah.sinject.exceptions.ConflictingServicesException;
import org.yah.sinject.exceptions.ServiceResolutionException;
import org.yah.sinject.exceptions.NoSuchServiceException;

import java.lang.reflect.Type;

public interface ServiceResolver {

    /**
     * Find a service assignable to <code>type</code> and with the given name if not null.
     * <ul>
     * <li>if no definitions are matched throws a {@link NoSuchServiceException}</li>
     * <li>if multiple definitions with same priority matches, throws a {@link ConflictingServicesException}</li>
     * <li>otherwise, return the definition with top most priority</li>
     * </ul>
     *
     * @param name the name of the service to resolve, if null, all services assignable to type will be matched
     * @param type the type of the service to resolve, must not be null
     * @return the definition matching name and type, or type of name is null
     * @throws NoSuchServiceException       If no definition matches
     * @throws ConflictingServicesException If more than one definition matches
     */
    Service<?> service(String name, Type type) throws ServiceResolutionException;

    default Service<?> service(ServiceDefinition definition) throws NoSuchServiceException, ConflictingServicesException {
        return service(definition.name(), definition.type());
    }

}
