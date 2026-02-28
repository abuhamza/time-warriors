JAVA_HOME ?= /opt/homebrew/opt/openjdk@21/libexec/openjdk.jdk/Contents/Home
export JAVA_HOME
export PATH := $(JAVA_HOME)/bin:$(PATH)

DEV_DIR    := .dev
SCRIPT_DIR := scripts

.PHONY: run build compile clean package-dmg package-deb package-msi \
        run-bg stop wait-window screenshot dev-feedback help

run: ## Start the application (foreground)
	./gradlew run

run-bg: ## Start the application in background with instance tracking
	@mkdir -p $(DEV_DIR)
	$(eval INSTANCE_ID := $(shell date +%s))
	@echo "$(INSTANCE_ID)" > $(DEV_DIR)/app.instance
	@echo "Starting app with instanceId=$(INSTANCE_ID) ..."
	@./gradlew run -PinstanceId=$(INSTANCE_ID) > $(DEV_DIR)/app.log 2>&1 &
	@echo "Waiting for JVM process ..."
	@for i in $$(seq 1 30); do \
		PID=$$(pgrep -f "timewgui.instanceId=$(INSTANCE_ID)" 2>/dev/null | head -1); \
		if [ -n "$$PID" ]; then \
			echo "$$PID" > $(DEV_DIR)/app.pid; \
			echo "App running  pid=$$PID  instance=$(INSTANCE_ID)"; \
			exit 0; \
		fi; \
		sleep 2; \
	done; \
	echo "ERROR: JVM process not found after 60s" >&2; exit 1

stop: ## Stop the background app instance
	@if [ -f $(DEV_DIR)/app.pid ]; then \
		PID=$$(cat $(DEV_DIR)/app.pid); \
		echo "Stopping pid=$$PID ..."; \
		kill "$$PID" 2>/dev/null || true; \
		rm -f $(DEV_DIR)/app.pid $(DEV_DIR)/app.instance; \
		echo "Stopped."; \
	else \
		echo "No running instance found ($(DEV_DIR)/app.pid missing)."; \
	fi

wait-window: ## Poll until the app window appears (up to 60s)
	@PID=$$(cat $(DEV_DIR)/app.pid 2>/dev/null); \
	if [ -z "$$PID" ]; then echo "ERROR: No app.pid found" >&2; exit 1; fi; \
	echo "Waiting for window (pid=$$PID) ..."; \
	for i in $$(seq 1 30); do \
		if bash $(SCRIPT_DIR)/dev-screenshot.sh --probe --pid "$$PID" >/dev/null 2>&1; then \
			echo "Window detected."; \
			exit 0; \
		fi; \
		sleep 2; \
	done; \
	echo "ERROR: Window not found after 60s" >&2; exit 1

screenshot: ## Take a screenshot of the running app window
	@PID=$$(cat $(DEV_DIR)/app.pid 2>/dev/null); \
	if [ -z "$$PID" ]; then echo "ERROR: No app.pid found" >&2; exit 1; fi; \
	bash $(SCRIPT_DIR)/dev-screenshot.sh --pid "$$PID"

dev-feedback: ## Full cycle: stop -> run-bg -> wait-window -> screenshot
	@if [ -f $(DEV_DIR)/app.pid ]; then $(MAKE) stop; fi
	$(MAKE) run-bg
	$(MAKE) wait-window
	$(MAKE) screenshot

build: ## Full build (compile + jar)
	./gradlew build

compile: ## Compile Kotlin sources
	./gradlew compileKotlin

clean: ## Clean build artifacts
	./gradlew clean

package-dmg: ## Package as macOS .dmg
	./gradlew packageDmg

package-deb: ## Package as Linux .deb
	./gradlew packageDeb

package-msi: ## Package as Windows .msi
	./gradlew packageMsi

help: ## Show this help
	@grep -E '^[a-zA-Z_-]+:.*?## .*$$' $(MAKEFILE_LIST) | awk 'BEGIN {FS = ":.*?## "}; {printf "  \033[36m%-18s\033[0m %s\n", $$1, $$2}'

.DEFAULT_GOAL := help
