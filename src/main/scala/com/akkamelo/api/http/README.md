This package is basically already implemented, since Akka HTTP and the endpoint in general are not directly related to the PCSA.

The exception is the Service, which needs to be implemented and wired correctly to the rest of the application. 
You shouldn't need to touch anything else.

However, I recommend deleting the CustomMarshalling class or some parts of it, to see what is breaking. 
You can always bring it back if you need it, or implement your own version.
It is a good exercise in understanding scala implicits, and how they are used.

### Important
The DTOs and server paths are in portuguese **on purpose**. 
It is to mimic a world where API or external contracts can be weirdly named, not well adapted to your internal domain, or not directly under your control. 

It is a good exercise to understand how to work with this kind of situation. 

Use the [API Contracts](docs/api-contracts.md) to understand the endpoints and their contracts. (The language is very similar to english, should be easy to understand)

Use the adapter pattern from Hexagonal Architecture to convert the external contracts to your desired internal contracts.

If you have any questions, feel free to ask.