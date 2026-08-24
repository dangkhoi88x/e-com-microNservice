# .NET Learning API – Day 1 Design

## Purpose

Provide an isolated ASP.NET Core sample project for a Java backend developer to learn C# syntax, controllers, dependency injection, and a basic REST API.

## Scope

The sample lives in `dotnet-learning-api/` and does not modify any existing Java microservice.

It exposes an in-memory product catalogue with these endpoints:

- `GET /api/products`
- `GET /api/products/{id}`
- `POST /api/products`
- `PUT /api/products/{id}`
- `DELETE /api/products/{id}`

## Architecture

Requests flow from `ProductsController` to `IProductService` and its `ProductService` implementation. `ProductService` owns an in-memory `List<Product>`.

`Product` has `Id`, `Name`, `Price`, and `Stock`. Request and response DTOs keep the HTTP contract separate from the domain model.

`IProductService` is registered as a singleton in `Program.cs` so that the in-memory list remains available for the lifetime of the application. Swagger is enabled for interactive API testing.

## Learning focus

- C# classes, interfaces, records, properties, nullable values, generics, and exceptions.
- ASP.NET Core controller routing and model binding.
- Built-in dependency injection.
- `async`/`await`, `Task`, and `CancellationToken` in service and controller method signatures.
- LINQ operations where they make collection queries clear.

## Out of scope

Database, EF Core, authentication, Docker, and production authorization are reserved for later lessons.
