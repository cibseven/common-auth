# CIB seven common authentication

`common-auth` is a small Java library with the authentication contracts shared by CIB seven web applications. It defines the user model, login payloads, user-provider interfaces and authentication exceptions. It also has a default JWT (bearer token) implementation for issuing, validating and prolonging tokens.

The library holds interfaces only. Concrete providers (CIB seven engine, LDAP, Keycloak, ADFS, OAuth2, …) live in the applications that use it, mainly [cibseven-webclient](https://github.com/cibseven/cibseven-webclient).

## Usage

```xml
<dependency>
	<groupId>org.cibseven.webapp.auth</groupId>
	<artifactId>common-auth</artifactId>
	<version>1.4.0</version>
</dependency>
```

Releases are published to Maven Central. Snapshots are published to `artifacts.cibseven.org`.

`jakarta.servlet-api` is a `provided` dependency, so the host application must supply a Servlet 6 (Jakarta EE 10) container, for example Spring Boot 3.

## What's inside

All classes are under `org.cibseven.webapp.auth`.

| Type | Purpose |
|------|---------|
| `User` | Authenticated user: `getId()`, `getDisplayName()`, `getAuthToken()`, and `getUrlToken()` (defaults to the auth token). |
| `RegisteredUser` | A `User` that also has an e-mail address. |
| `Login` | Marker interface for login payloads. It is annotated with `@JsonTypeInfo(use = CLASS, property = "type")`, so a JSON body must name its concrete class in `type`. |
| `rest.StandardLogin` | Username/password `Login`. Send it as `{"type": "org.cibseven.webapp.auth.rest.StandardLogin", "username": "…", "password": "…"}`. |
| `providers.UserProvider<T extends Login>` | Core contract: `login`, `authenticate` (from an `HttpServletRequest`), `getUserInfo`, `logout`. |
| `providers.JwtUserProvider<T extends Login>` | `UserProvider` with default JWT handling (see below). |
| `providers.ResetEnabledProvider` | Optional: `requestPasswordReset(params, locale)` for providers that support password reset. |
| `UserManager<W, R>` | `UserProvider` that can also `create`, `update` and `delete` users. |
| `exception.AuthenticationException` | Base unchecked exception. `isNoAuth()` is `true` when no credentials were sent at all. |
| `exception.LoginException` | Login failed, for example because of wrong credentials. |
| `exception.TokenExpiredException` | The token has expired. If the token could be prolonged, `getData()[0]` holds the new token. |

## JWT provider

To use `JwtUserProvider`, implement `getSettings()`, `serialize(User)`, `deserialize(String json, String token)` and `verify(Claims)`, plus the `UserProvider` methods. The token handling is provided by default methods:

- **`authenticate(rq)`** reads `Authorization: Bearer <token>` and parses it. If the header is missing it throws an `AuthenticationException` with no data (`isNoAuth() == true`).
- **`createToken(settings, prolongable, verify, user)`** returns `"Bearer " + jwt`. The JWT is HS-signed with the secret, has the user id as subject, expires after `settings.getValid()`, and carries the serialized user plus the `prolongable` and `verify` flags as claims.
- **`parse(token, settings)`** validates the signature and restores the user with `deserialize`. If `verify` is set, it also calls `verify(claims)`, which must return `null` when the user changed after the token was issued (for example a password change). In that case the token is rejected.
- **Expired tokens:** if the token is `prolongable` and expired less than `settings.getProlong()` ago, and `verify(claims)` still returns a user, a `TokenExpiredException` is thrown that carries a fresh token. Otherwise it is thrown with no data. The caller should return the new token to the client, which retries with it.

`TokenSettings` supplies:

| Method | Meaning |
|--------|---------|
| `getSecret()` | **Base64-encoded** HMAC key. It must decode to at least 256 bits (jjwt requirement). |
| `getValid()` | How long a new token is valid. |
| `getProlong()` | How long after expiry a prolongable token may still be renewed. |

Minimal sketch:

```java
public class MyUserProvider implements JwtUserProvider<StandardLogin> {

	@Override
	public User login(StandardLogin login, HttpServletRequest rq) {
		MyUser user = checkCredentials(login.getUsername(), login.getPassword()); // throws LoginException
		user.setAuthToken(createToken(getSettings(), true, false, user));
		return user;
	}

	@Override public TokenSettings getSettings() { return settings; }
	@Override public String serialize(User user) { return mapper.writeValueAsString(user); }
	@Override public User deserialize(String json, String token) { /* read JSON, set token */ }
	@Override public User verify(Claims claims) { /* reload user, or null if changed */ }
	@Override public User getUserInfo(User user, String userId) { /* … */ }
	@Override public void logout(User user) { }
}
```

## Building

```bash
mvn clean package
```

The parent POM is `org.cibseven:release-parent`. CI builds with JDK 17 through the [Jenkinsfile](Jenkinsfile), which has optional stages to deploy to `artifacts.cibseven.org` and Maven Central.

To add missing Apache license headers to source files:

```bash
mvn com.mycila:license-maven-plugin:format -Padd-missing-copyright
```

## License

[Apache License 2.0](LICENSE). Copyright CIB software GmbH.
