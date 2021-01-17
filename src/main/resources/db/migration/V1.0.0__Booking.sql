CREATE TABLE public."BOOKING"
(
    id character(36) NOT NULL,
    email character varying(1024),
    firstname character varying(255),
    lastname character varying(255),
    PRIMARY KEY (id)
);

CREATE TABLE public."BOOKING_DATE"
(
    date date NOT NULL,
    "bookingId" character(36),
    PRIMARY KEY (date)
);
