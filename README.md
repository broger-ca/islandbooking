## Setup 
- Create a database on postgres ('booking') for dev default user config is booking/booking (check application.yaml, there is 2 connection to postgres define)
- Start kafka on localhost ( kafka url define in application.yaml)
- ./gradlew run  

### get avalaible date
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