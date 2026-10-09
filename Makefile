.PHONY: dev dev-direct

# Keep the assigned PORT scoped to this Gradle invocation.
dev:
	portless run --name spring-todo-api ./gradlew --no-daemon bootRun

dev-direct:
	./gradlew bootRun
