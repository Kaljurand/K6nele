all: help

help:
	@echo Targets:
	@echo
	@echo "  lint:       runs lint"

lint:
	lint --html report.html app
