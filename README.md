# Project was moved to new repo: https://github.com/longdt/vertx-orm
## Vertx-Orm-Mysql
Simple API focusing on scalability and low overhead.

Reactive and non blocking which able to handle many database connections with a single thread by use `Vertx-mysql-client`
### Release version 2.1.0
* Add `CrudRepository` methods update/updateDynamic with `Query` param
* Fix bug incorrect totalElements when find page
* Fix bug update non-existed entity
### Release version 2.0.0
* Upgrade Vertx to 4.0.0
* Removal of the Callback-API
* Optimize Query implementation to generate sql string effectively
## Developers
### Testing
Out of the box, the test suite runs a Docker container using TestContainers.
### Maven dependency
```
<dependency>
    <groupId>com.github.longdt</groupId>
    <artifactId>vertx-orm-mysql</artifactId>
    <version>2.1.0</version>
</dependency>

<dependency>
    <groupId>com.github.longdt</groupId>
    <artifactId>vertx-orm-codegen</artifactId>
    <version>2.0.0</version>
    <scope>provided</scope>
</dependency>
```
### Example
##### Define Entity class:
```
@Entity
@NamingStrategy(Case.SNAKE_CASE)
public class RuleTemplate {
    @Id
    private Integer id;
    private String name;
    @Convert(converter = ArgumentsConverter.class)
    private Map<String, ArgumentDescription> arguments;
    private String flinkJob;
    private boolean active;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    ...
    setter/getter methods
    ...
}
```
##### Define Repository:
```
@Repository(dialect = SqlDialect.MYSQL)
public interface RuleTemplateRepository extends CrudRepository<Integer, RuleTemplate> {
}
```
##### Compile project to generate implementation of Repository:
```
mvn clean compile
```
After compilation, `vertx-orm-codegen` creates `RuleTemplateRepositoryMysql` class which implement RuleTemplateRepository.
##### Create repository instance:
```
RuleTemplateRepository repository = new RuleTemplateRepositoryMysql(pool);
```
##### Now it's time to use. Let's try some simple methods:
###### insert
```
var template = new RuleTemplate();
...
repository.insert(template)
    .onComplete(ar -> {
        if (ar.succeeded()) {
            System.out.println(ar.result());
        } else {
            ar.cause().printStackTrace();
        }
    });
```
###### update
```
RuleTemplate template = ...;
template.setName("new template name");
...
repository.update(template)
    .onComplete(ar -> {
        if (ar.succeeded()) {
            System.out.println(ar.result());
        } else {
            ar.cause().printStackTrace();
        }
    });
```
###### find by id
```
repository.find(id)
    .onComplete(ar -> {
        if (ar.succeeded()) {
            System.out.println(ar.result());
        } else {
            ar.cause().printStackTrace();
        }
    });
```
###### find by query
```
import static com.github.longdt.vertxorm.repository.query.QueryFactory.*;

var query = QueryFactory.<RuleTemplate>and("active", 1);
repository.findAll(query)
    .onComplete(ar -> {
        if (ar.succeeded()) {
            System.out.println(ar.result());
        } else {
            ar.cause().printStackTrace();
        }
    });
```
###### find with paging
```
import static com.github.longdt.vertxorm.repository.query.QueryFactory.*;

var pageRequest = new PageRequest(1, 20);
var query = QueryFactory.<RuleTemplate>and("active", 1);
repository.findAll(query, pageRequest)
    .onComplete(ar -> {
        if (ar.succeeded()) {
            System.out.println(ar.result());
        } else {
            ar.cause().printStackTrace();
        }
    });
```
###### transaction
```
//find then update example
var id = 1;
repository.getPool()
        .withTransaction(conn -> repository.find(conn, id)     //find entity by id
                .map(entityOpt -> entityOpt.orElseThrow(() -> new EntityNotFoundException("id: " + id + " is not found")))
                .compose(entity -> {
                    //update entity
                    entity.setUpdatedAt(LocalDateTime.now());
                    return repository.update(conn, entity);
                }))
        .onComplete(ar -> {
            if (ar.succeeded()) {
                System.out.println(ar.result());
            } else {
                ar.cause().printStackTrace();
            }
        });
```
