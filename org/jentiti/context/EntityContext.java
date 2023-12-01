package org.jentiti.context;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import org.jentiti.annotation.Entity;
import org.jentiti.annotation.Prototype;
import org.jentiti.annotation.Singleton;
import org.jentiti.error.NoSuchEntityException;
import org.jentiti.xtend.intf.EntityPostProcessor;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.core.type.classreading.SimpleMetadataReaderFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class EntityContext {

    private String basePackage;
    private Logger logger;
    private List<EntityPostProcessor> entityPostProcessors;
    private Set<Class> classList;
    private List<Class> entityDefinition;
    private Map<String, Class> entityDefinitionMap;
    private Map<Class, Object> singletonMapByClass;
    private Map<String, Object> singletonMapByName;

    private void init() throws IOException {

        entityPostProcessors = new ArrayList<>();
        classList = new HashSet<>();
        entityDefinition = new ArrayList<>();
        entityDefinitionMap = new HashMap<>();
        singletonMapByClass = new HashMap<>();
        singletonMapByName = new HashMap<>();

        InputStream in = this.getClass().getResourceAsStream("/jentiti.properties");
        Properties pro = new Properties();
        InputStreamReader inputStreamReader = new InputStreamReader(in, StandardCharsets.UTF_8);
        pro.load(inputStreamReader);
        pro.load(in);
        if (basePackage == null) {
            try {
                basePackage = pro.getProperty("base-package").replace(".", "/");
            } catch (NullPointerException e) {
                logger.error("Property \"base-package\" wasn't specified. Set to <classpath> by default.");
                basePackage = "";
            } finally {
                in.close();
            }
        }
    }

    @SuppressWarnings("all")
    private void findAllClasses(String _package){

        //find all classes
        PathMatchingResourcePatternResolver resolver = new PathMatchingResourcePatternResolver();
        Resource[] resources = new Resource[0];
        try {
            resources = resolver.getResources("classpath:" + _package.replace(".","/") + "/**/*.class");
        } catch (IOException e) {
        }
        for (Resource res : resources) {
            String clsName = null;
            try {
                classList.add(Class.forName(new SimpleMetadataReaderFactory().getMetadataReader(res).getClassMetadata().getClassName()));
            } catch (ClassNotFoundException | ExceptionInInitializerError | NoClassDefFoundError | IOException e) {
            }
        }
    }

    @SuppressWarnings("deprecation")
    private void analyseByInterfaces(){

        for (Class clazz : classList) {
            // find classes implements EntityPostProcessor
            // added to instance list of entityPostProcessors
            if (EntityPostProcessor.class.isAssignableFrom(clazz)) {
                try {
                    entityPostProcessors.add((EntityPostProcessor) clazz.newInstance());
                } catch (InstantiationException | IllegalAccessException e) {
                    //e.printStackTrace();
                }
            }
        }
    }

    // TODO debug
    @SuppressWarnings("deprecation")
    private void analyseByAnnotations(){

        for (Class clazz : classList) {
            if (clazz.isAnnotationPresent(Entity.class)) {
                Entity annotation = (Entity) clazz.getAnnotation(Entity.class);
                String value = annotation.value();
                String scope = annotation.scope();
                if (scope.equals("prototype")) {
                    if (value.equals(""))
                        value = defaultEntityName(clazz);
                    entityDefinition.add(clazz);
                    entityDefinitionMap.put(value, clazz);
                } else if (scope.equals("singleton")) {
                    if (value.equals(""))
                        value = defaultEntityName(clazz);
                    try {
                        Object singletonObject = clazz.newInstance();
                        singletonMapByClass.put(clazz, singletonObject);
                        singletonMapByName.put(value, singletonObject);
                    } catch (InstantiationException | IllegalAccessException e) {
                        e.printStackTrace();
                    }
                }
            } else if (clazz.isAnnotationPresent(Prototype.class)) {
                Prototype annotation = (Prototype) clazz.getAnnotation(Prototype.class);
                String value = annotation.value();
                if (value.equals(""))
                    value = defaultEntityName(clazz);
                entityDefinition.add(clazz);
                entityDefinitionMap.put(value, clazz);
            } else if (clazz.isAnnotationPresent(Singleton.class)) {
                Singleton annotation = (Singleton) clazz.getAnnotation(Singleton.class);
                String value = annotation.value();
                if (value.equals(""))
                    value = defaultEntityName(clazz);
                try {
                    Object singletonObject = clazz.newInstance();
                    singletonMapByClass.put(clazz, singletonObject);
                    singletonMapByName.put(value, singletonObject);
                } catch (InstantiationException | IllegalAccessException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    private String defaultEntityName(Class clazz) {

        String _className = "classpath."+clazz.getName();
        String[] _classNameSplited;
        _classNameSplited = _className.split("\\.");
        int entityNameIndex = _classNameSplited.length-1;
        String entityName = _classNameSplited[entityNameIndex];
        return Character.toLowerCase(entityName.charAt(0)) + entityName.substring(1);
    }

    private void doSingletonEntityPostProcess(){

        if (entityPostProcessors.size() > 0){
            // singleton post process
            singletonMapByClass.forEach((k,v)->{
                entityPostProcessors.forEach((entityPostProcessor)->{
                    Object proxy = entityPostProcessor.postInstantiation(singletonMapByClass.get(k));
                    singletonMapByClass.replace(k,proxy);
                });
            });
            singletonMapByName.forEach((k,v)->{
                singletonMapByName.replace(k,singletonMapByClass.get(v.getClass()));
            });
        }
    }

    private Object doPrototypeEntityPostProcess(Object entity){

        Object proxy = entity;
        for (EntityPostProcessor entityPostProcessor : entityPostProcessors)
            proxy = entityPostProcessor.postInstantiation(proxy);
        return proxy;
    }

    private void doStart(){

        logger = LoggerFactory.getLogger(EntityContext.class);
        logger.info("Jentiti version: 0.2.3.");
        try {
            init();
        } catch (IOException e) {
            e.printStackTrace();
        }

        if (!basePackage.equals(""))
            logger.info("Scanning entities from \"" + basePackage.replace("/", ".") + "\".");
        else
            logger.info("Scanning entities from <classpath>.");
        findAllClasses("org.jentiti");
        findAllClasses(basePackage);
        logger.info("Finished scanning.");

        analyseByInterfaces();
        analyseByAnnotations();
        doSingletonEntityPostProcess();
    }

    private void logInfo(){

        entityPostProcessors.forEach((i)->{
            logger.info("EntityPostProcessor: " + i);
        });

        entityDefinition.forEach((c)->{
            logger.info("Prototype Definition: " + c.getName());
        });

        singletonMapByClass.forEach((k,v)->{
            logger.info("Singleton Definition: " + k.getName());
        });
    }

    public EntityContext(){

        this.basePackage = null;
        doStart();
        logInfo();
    }

    public EntityContext(String basePackage){

        this.basePackage = basePackage;
        doStart();
        logInfo();
    }

    @SuppressWarnings("all")
    public Object get(Class entityClass) throws NoSuchEntityException {

        if (entityDefinition.contains(entityClass)) {
            try {
                return doPrototypeEntityPostProcess(entityClass.newInstance());
            } catch (InstantiationException | IllegalAccessException e) {
                e.printStackTrace();
            }
        }
        else if (singletonMapByClass.containsKey(entityClass))
            return singletonMapByClass.get(entityClass);
        else
            throw new NoSuchEntityException("Entity \"" + entityClass + "\" doesn't exist in base package \""+basePackage.replace("/",".")+"\"");
        return null;
    }

    @SuppressWarnings("all")
    public Object get(String entityName) throws NoSuchEntityException {

        if (entityDefinitionMap.containsKey(entityName)) {
            try {
                return doPrototypeEntityPostProcess(entityDefinitionMap.get(entityName).newInstance());
            } catch (InstantiationException | IllegalAccessException e) {
                e.printStackTrace();
            }
        }
        else if (singletonMapByName.containsKey(entityName))
            return singletonMapByName.get(entityName);
        else
            throw new NoSuchEntityException("Entity \"" + entityName + "\" doesn't exist in base package \""+basePackage.replace("/",".")+"\"");
        return null;
    }
}
