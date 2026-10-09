.PHONY: dev-portless

# Keep the assigned PORT scoped to this Gradle invocation.
dev-portless:
	portless run --name spring-todo-api ./gradlew --no-daemon bootRun
