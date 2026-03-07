ifeq ($(shell uname),Darwin)
  JAVA_HOME ?= /opt/homebrew/opt/openjdk@21/libexec/openjdk.jdk/Contents/Home
else
  JAVA_HOME ?= /usr/lib/jvm/java-21-openjdk-amd64
endif
export JAVA_HOME
export PATH := $(JAVA_HOME)/bin:$(PATH)

DEV_DIR    := .dev
SCRIPT_DIR := scripts
VERSION    := $(shell grep '^appVersion=' gradle.properties | cut -d= -f2)
DMG_PATH   := build/compose/binaries/main/dmg/TimewGUI-$(VERSION).dmg

.PHONY: run build compile clean test package-dmg package-deb package-msi \
        run-bg stop wait-window screenshot dev-feedback \
        release ship version help \
        wt-new wt-list wt-remove wt-clean wt-parallel

test: ## Run all tests
	./gradlew test

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

version: ## Print current version
	@echo "$(VERSION)"

ship: ## Commit, push, and release (usage: make ship v=1.2.1 m="Fix something")
	@if [ -z "$(v)" ]; then echo "ERROR: version required, e.g. make ship v=1.2.1 m=\"Fix something\"" >&2; exit 1; fi
	@if [ -z "$(m)" ]; then echo "ERROR: message required, e.g. make ship v=1.2.1 m=\"Fix something\"" >&2; exit 1; fi
	@if [ -n "$$(git status --porcelain)" ]; then \
		echo "=== Committing changes ==="; \
		git add -A && git commit -m "$(m)"; \
	else \
		echo "=== Working tree clean, skipping commit ==="; \
	fi
	@echo "=== Pushing to origin ==="
	git push origin main
	@echo "=== Tagging v$(v) ==="
	git tag "v$(v)"
	git push origin "v$(v)"
	@echo "=== Tag v$(v) pushed — CI will build packages and create the release ==="

release: ## Build DMG and create a GitHub release (usage: make release)
	@echo "=== Building TimewGUI v$(VERSION) ==="
	$(MAKE) package-dmg
	@if [ ! -f "$(DMG_PATH)" ]; then \
		echo "ERROR: DMG not found at $(DMG_PATH)"; \
		echo "Looking for DMG..."; \
		find build/compose/binaries -name "*.dmg" 2>/dev/null; \
		exit 1; \
	fi
	@echo "=== Creating GitHub release v$(VERSION) ==="
	gh release create "v$(VERSION)" "$(DMG_PATH)" \
		--title "TimewGUI v$(VERSION)" \
		--generate-notes
	@echo "=== Release v$(VERSION) published ==="
	@echo "https://github.com/$$(gh repo view --json nameWithOwner -q .nameWithOwner)/releases/tag/v$(VERSION)"

wt-new: ## Create worktree + Claude session (usage: make wt-new b=feature/foo [base=main])
	@bash $(SCRIPT_DIR)/worktree-session.sh new "$(b)" $(if $(base),$(base))

wt-list: ## List active worktrees
	@bash $(SCRIPT_DIR)/worktree-session.sh list

wt-remove: ## Remove a worktree (usage: make wt-remove b=feature/foo)
	@bash $(SCRIPT_DIR)/worktree-session.sh remove "$(b)"

wt-clean: ## Remove all worktrees
	@bash $(SCRIPT_DIR)/worktree-session.sh clean

wt-parallel: ## Launch parallel Claude sessions (usage: make wt-parallel branches="feat/a feat/b fix/c")
	@bash $(SCRIPT_DIR)/worktree-session.sh parallel $(branches)

help: ## Show this help
	@grep -E '^[a-zA-Z_-]+:.*?## .*$$' $(MAKEFILE_LIST) | awk 'BEGIN {FS = ":.*?## "}; {printf "  \033[36m%-18s\033[0m %s\n", $$1, $$2}'

.DEFAULT_GOAL := help
