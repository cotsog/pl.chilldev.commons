/**
 * This file is part of the ChillDev-Commons.
 *
 * @license http://mit-license.org/ The MIT license
 * @copyright 2015 © by Rafał Wrzeszcz - Wrzasq.pl.
 */

package test.pl.chilldev.commons.db;

import java.util.UUID;

import org.junit.Test;
import org.junit.Assert;

import pl.chilldev.commons.db.AbstractEntity;

public class AbstractEntityTest
{
    @Test
    public void getId()
    {
        AbstractEntity entity = new Entity();

        Assert.assertNull(
            "AbstractEntity.getId() should return NULL if entity is not persisted.",
            entity.getId()
        );
    }

    @Test
    public void setId()
    {
        UUID id = UUID.randomUUID();
        AbstractEntity entity = new Entity();
        entity.setId(id);

        Assert.assertEquals(
            "AbstractEntity.getId() should return previously set ID if assigned.",
            id,
            entity.getId()
        );
    }

    @Test
    public void equalsSame()
    {
        AbstractEntity entity = new Entity();
        Assert.assertTrue(
            "AbstractEntity.equals() should return TRUE if the object is the very same instance.",
            entity.equals(entity)
        );
    }

    @Test
    public void equalsNull()
    {
        AbstractEntity entity = new Entity();
        Assert.assertFalse(
            "AbstractEntity.equals() should return FALSE when compared to NULL.",
            entity.equals(null)
        );
    }

    @Test
    public void equalsNotEntity()
    {
        UUID id = UUID.randomUUID();
        AbstractEntity entity = new Entity();
        Assert.assertFalse(
            "AbstractEntity.equals() should return FALSE when compared to a non-entity object.",
            entity.equals(id)
        );
    }

    @Test
    public void equalsNullId()
    {
        AbstractEntity object = new Entity();
        AbstractEntity entity = new Entity();
        Assert.assertFalse(
            "AbstractEntity.equals() should return FALSE when it is not persisted.",
            entity.equals(object)
        );
    }

    @Test
    public void equals()
    {
        UUID id = UUID.randomUUID();

        AbstractEntity same = new Entity();
        same.setId(id);

        AbstractEntity other = new Entity();
        other.setId(UUID.randomUUID());

        AbstractEntity entity = new Entity();
        entity.setId(id);

        Assert.assertTrue(
            "AbstractEntity.equals() should return TRUE when compared entity has same ID.",
            entity.equals(same)
        );

        Assert.assertFalse(
            "AbstractEntity.equals() should return FALSE when compared entity has different ID.",
            entity.equals(other)
        );
    }

    @Test
    public void testHashCode()
    {
        UUID id = UUID.randomUUID();
        AbstractEntity entity = new Entity();
        entity.setId(id);

        Assert.assertEquals(
            "AbstractEntity.hashCode() should return hash code that is equal to it's ID hash code.",
            id.hashCode(),
            entity.hashCode()
        );
    }

    @Test
    public void testHashCodeNull()
    {
        AbstractEntity entity = new Entity();

        Assert.assertEquals(
            "AbstractEntity.hashCode() should return 0 if entity is not persisted.",
            0,
            entity.hashCode()
        );
    }
}

class Entity extends AbstractEntity
{
}
