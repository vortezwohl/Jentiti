# Jentiti
Jentiti is an IoC container for entities or pojos.

# Annotations
```
@Entity(value = [Name for entity], scope = [“singleton” or "prototype"]) // Could be used on public Classes or Annotations.

@Singleton(value = [Name for singleton entity]) // Could be used on public Classes or Annotations.

@Prototype(value = [Name for prototype entity]) // Could be used on public Classes or Annotations.

@Jsonify // Enhance toString() method for an entity to return JSON string.

```
```
(Example)

@Entity(scope = "singleton")
@Jsonify
public class User{
    ...fields...
    ...methods...
    etc.
}
```

# IoC container
When you instantiated an EntityContext object. Entities are loaded to the container or you would call it a factory. You can use method get([Entity name or class]) to get the entity you need.
```
(Example)

public static void main(String[] args){
    EntityContext context = new EntityContext();
    User user = (User) context.get("user");
    System.out.println(user);
}
```

# Expansion
You can do some extra enhancement to an entity or some entities by implementing EntityPostProcessor. Once your class implements this interface, the method postInstantiation would be invoked during instantiation of entities.
```
package org.jentiti.xtend.intf;

public interface EntityPostProcessor {

    Object postInstantiation(Object entity);
}

```
