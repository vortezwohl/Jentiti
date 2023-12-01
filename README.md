# Jentiti
Jentiti is an IoC container for entities or pojos.

# How to use
```
@Entity(value = [Name for entity], scope = [“singleton” or "prototype"]) // Could be used on public Classes or Annotations

@Singleton(value = [Name for singleton entity]) // Could be used on public Classes or Annotations

@Prototype(value = [Name for prototype entity]) // Could be used on public Classes or Annotations

@Jsonify // Enhance toString() method for an entity to return JSON string

```
