# API contract

Sign in through `/oauth2/authorization/auth0`. Todo operations use the resulting
OIDC browser session; this service does not configure bearer-token authentication.
Swagger UI (`/swagger-ui.html`) and OpenAPI (`/v3/api-docs`) are public.

## Todos

| Method and path | Result |
| --- | --- |
| `GET /todos` | A Spring Data page of the signed-in user's todos. `page` is zero-based; `size` defaults to 20 and is capped at 100. Default ordering is `id,asc`; override with `sort`. |
| `POST /todos` | `201` with the created todo. JSON body: `{"content":"Ship the API"}`. Content must be nonblank and at most 255 characters; invalid content returns `400`. |
| `GET /todos/{id}` | `200` with the owned todo, otherwise `404`. |
| `DELETE /todos/{id}` | `204` on deletion, otherwise `404`. |

Todo responses contain `id` and `content`, never the owner's email. List responses
place those records in `content` alongside pagination metadata.

For a read using an existing session cookie jar:

```shell
curl --cookie cookies.txt \
  'https://spring-todo-api.localhost/todos?page=0&size=20&sort=id,asc'
```

## Security requirements

Todo access requires a nonblank email claim of at most 255 characters; otherwise
it returns `403`. Invalid sort properties return `400`.

The authenticated email determines ownership; clients cannot choose an owner.
Missing IDs and another user's IDs both return `404`, including after a cached
read. Email is the persistent ownership key, so changing the identity provider's
email claim does not transfer existing todos to that new identity.

CSRF protection remains enabled for browser sessions. POST and DELETE require a
valid session-bound CSRF token in addition to the session cookie; a cookie alone
returns `403`. The service currently provides no dedicated token-discovery endpoint.
An API client integration must account for token delivery before relying on writes;
the HTTP tests supply tokens through Spring Security test support.
