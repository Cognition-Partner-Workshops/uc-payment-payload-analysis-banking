# OnlineBankingRestAPI  
A Rest API developed with Spring Boot that allows users to perform banking transactions over the internet.
Time spent on the project : 160+ hours  
Line of code count : 9000+ lines  
Prototype project's line of code count and documentation time is not included.  

### Technologies  
Java 11   
Spring Boot(rest api)   
Maven(build automation tool)  
Lombok(eliminates boilerplate code)    
Hibernate(ORM/code-first approach)    
MapStruct(dto-entity mapper)    
Swagger(rest api UI and documentation)   
PostgreSql (relational database)   
H2 (relational in app database used for testing)   
Mockitoo (unit testing/mocking layers)      
JWT (jwt json token)    
Spring Security(autherization)     
Spring HATEOAS(hateoas)   
Spring JPA(transactions)   
Kafka(message broker)   
Docker(containerization)    
Mockaroo (fake data api)  

### Architecture   
https://ibb.co/NmHVL3b    

### Design of the packages, entities, and controllers    
https://ibb.co/yYDMvD2   
https://ibb.co/phwhSBX  

### To Run  
Pull the project.
Create a database named "online-banking-rest-api" in postgresql.  
Provide configuration through environment variables (do not put credentials in settings files):
```sh
export DB_URL=jdbc:postgresql://localhost:5432/online-banking-rest-api
export DB_USERNAME=postgres
export DB_PASSWORD  # supply through the local secret store
export JWT_SIGNING_KEY  # supply through the local secret store
export DB_DDL_AUTO=create  # initial local schema only; defaults to validate
```
The JWT signing key must be at least 64 bytes; the application refuses to start otherwise.

Accounts with the `ADMIN` role (column `ROLE_TYPE` in `CUS_CUSTOMER`) can list and delete customers; self-registration always creates `USER` accounts.

**From here, you can run the application if you will not use kafka messaging feature**
For Kafka, I used a docker container.   
Create a "docker-compose.yml" file and add the following code in it.  
```yml
version: "3"
services:
  zookeeper:
    image: wurstmeister/zookeeper
    ports:
      - "2181:2181"
  kafka:
    image: wurstmeister/kafka
    ports:
      - "9092:9092"
    environment:
      KAFKA_BROKER_ID: 101
      KAFKA_ADVERTISED_HOST_NAME: localhost
      KAFKA_ADVERTISED_PORT: 9092
      KAFKA_ZOOKEEPER_CONNECT: zookeeper:2181
    depends_on:
      - zookeeper
```
Open cmd and go to that file with "cd path".  
Open docker desktop(or docker daemon) on the background.  
Run command "docker compose up -d".  
Now, a container that has kafka in it is running.  
Run the project.
Open your browser and enter following URL : http://localhost:8080/swagger-ui/index.html

### Platform container

The caller workflow pins the OtterWorks chart, policies and reusable pipeline to
`19392116c2197d713811213778a7a1d75eb90cc0`. The first Docker stage runs the full
Maven verification lifecycle; building the runtime depends on that stage passing.
The existing Payments suite workflow is unchanged.

```sh
docker build --target test .
docker build -t banking:local .
docker run --rm --read-only --tmpfs /tmp:rw,nosuid,nodev,size=32m \
  --memory=256m --cpus=0.5 --cap-drop=ALL --security-opt=no-new-privileges \
  --env-file /path/to/runtime.env -p 8080:8080 banking:local
curl --fail http://localhost:8080/health
```

`GET /health` is public and checks database connectivity and application readiness;
it returns 503 when the service is refusing traffic. Other banking endpoints keep
their authentication requirements. Health details and other actuator endpoints are
not exposed. Application logs are JSON on stdout, with no file appender; SQL stdout
and the text banner are disabled.

`deploy/values.yaml` references the externally populated `banking-config` Secret
for `DB_USERNAME`, `DB_PASSWORD` and `JWT_SIGNING_KEY`. Override database/broker
addresses for the target environment. Provision the database schema before starting
with the default `DB_DDL_AUTO=validate`. `deploy/values-ci.yaml` uses an ephemeral H2
database and disables Kafka listeners only for kind smoke tests; its synthetic
credentials live in `deploy/kind-secrets.env` and must never be used in production.
The runtime runs as uid/gid 10001, with `/tmp` as its only writable volume and JVM
memory sized for the chart's 256Mi container limit.

### Documentation    

Detailed documentation link:  https://docdro.id/YaQaBgq

I do not check identity No (citizen id) by algorithm because I want this API to be international.   
There is no update for AccAccount because Accounts do not get updated.   
AccAccount has the method cancel instead of the method delete because Accounts should never be deleted.    
FindAll methods get active items, but findById methods get both active and passive items.  

The bank's interest rate and allocation fee,tax rates (by default 20% because kkdv + bsmv) are given as constant,
it can be implemented dynamicaly by pulling data from another API.    

Interest rate can be zero because some countries (there are 11 countries with sharia laws) prefer it that way.  

For accounts, iban no is generated as random numbers(simulated), although stored as string. This generation rules can be changed easily.
Cvv no is created random but should be created using: primary account number, four-digit expiration date, a pair of DES (Data Encryption Standard) keys and a three-digit service code.  
Credit card no is unique and created random but should be crated according to the Luhn algorithm.  
The probability of recurrence of the credit card no is not checked because very low(1e+16).  

Cut off day of the month is the date where your billing cycle happens, and you select this day when creating your credit card.  

The installment count limit is 30 years because housing loans has the maximum installment count in the domain with 30 years.   

In the sql package there are sql files that has test data in them for main tables created using Mockaroo.   
ACC_ACCOUNT and CRD_CREDIT_CARD table's queries need more work :  
use regular expression datatype to generate random data in custom format. Also use number instead of money for BigDecimals.

There is a simple producer consumer system that runs with Kafka.  
This system imitates the banking apps sending sms to users.  
You can send message to the consumer from the controller.  