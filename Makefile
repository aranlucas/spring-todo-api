.PHONY: dev

dev:
	portless run --name spring-todo-api ./gradlew --no-daemon bootRun
