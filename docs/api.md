# API guide

The API uses Auth0 OpenID Connect login. The examples below assume that the caller already has an authenticated browser session or a valid session cookie.

## List todos

```shell
curl --cookie 'SESSION_COOKIE=value' \
  'http://localhost:8080/todos?page=0&size=20&sort=id,asc'
```

The response is a Spring `Page` containing only the authenticated user's todos. The exact pagination metadata is supplied by Spring Data; the important payload shape is:

```json
{
  "content": [
    {
      "id": 1,
      "content": "Ship the API"
    }
  ]
}
```

## Create a todo

```shell
curl --cookie 'SESSION_COOKIE=value' \
  --header 'Content-Type: application/json' \
  --data '{"content":"Ship the API"}' \
  --request POST \
  http://localhost:8080/todos
```

The server assigns the owner from the authenticated OIDC principal. The response status is `201 Created`.

## Read a todo

```shell
curl --cookie 'SESSION_COOKIE=value' \
  http://localhost:8080/todos/1
```

The response is `404 Not Found` when the ID does not exist or belongs to another user.

## Delete a todo

```shell
curl --cookie 'SESSION_COOKIE=value' \
  --request DELETE \
  http://localhost:8080/todos/1
```

The response is `204 No Content` when the todo is deleted. An ID owned by another user is treated as not found.

## Interactive documentation

When the application is running, Swagger UI is available at `/swagger-ui.html` and the OpenAPI document is available at `/v3/api-docs`. The documentation routes are public, but the todo operations still require authentication.
