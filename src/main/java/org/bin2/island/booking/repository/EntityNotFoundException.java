package org.bin2.island.booking.repository;

public class EntityNotFoundException extends RuntimeException {
    private String entityName;
    private String id;

    public EntityNotFoundException(String entityName, String id) {
        super(String.format("cannot found '%s' with id '%s'", entityName, id));
        this.entityName = entityName;
        this.id = id;
    }
}
