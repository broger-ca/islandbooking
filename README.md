## Setup & Run
The application needs :
- a postgresql db with at least create table privilege 
- a connection to kafka

The default configuration is pointing to a  localhost postgres:5432, dbname : booking , user/password : booking/booking

The default kafka configuration is pointing to localhost:9092

then run the application using 
```shell
./gradlew run  
```

if you want to override parameters you can do 
```shell
./gradlew run -Dvertx.pg.client.uri=postgresql://booking:booking@localhost:5432/booking \
 -Dflyway.datasources.default.url=jdbc:postgresql://localhost:5432/booking \
 -Dflyway.datasources.default.username=booking \
 -Dflyway.datasources.default.password=booking \
 -Dkafka.bootstraps.servers=localhost:9092  
```


## Postman 
[postman collection](booking.postman_collection.json)

### get avalaible date (to excluded)
GET http://localhost:8090/api/v1/booking/available?from=2021-01-18&to=2021-01-31

from and to are optional query parameter

### book date  [startdate, endDate[  (enddate excluded)
POST http://localhost:8090/api/v1/booking/
Body
```json
{
    "startDate" : "2021-01-20",
    "endDate" : "2021-01-22",

    "bookingInfo" : {
        "email" : "benoit.roger@gmail.com",
        "firstname":"benoit",
        "lastname": "roger"
    }
}
```

### update booking information
PUT http://localhost:8090/api/v1/booking/{bookingId}
Body
```json
{
        "email" : "benoit.roger2@gmail.com",
        "firstname":"benoit2",
        "lastname": "roger2"
    }
```

### cancel booking 
DELETE http://localhost:8090/api/v1/booking/{bookingId}