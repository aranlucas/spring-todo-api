.PHONY: dev

# Keep the assigned PORT scoped to this Gradle invocation.
dev:
	portless run --name spring-todo-api ./gradlew --no-daemon bootRun
