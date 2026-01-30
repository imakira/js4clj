# Directories
ORG_DIR ?= ./docs
OUT_DIR ?= ./doc

# Find all org files
ORG_FILES := $(wildcard $(ORG_DIR)/*.org)
MD_FILES := $(patsubst $(ORG_DIR)/%.org,$(OUT_DIR)/%.md,$(ORG_FILES))

.PHONY: all clean help

docs-gen: $(MD_FILES)
	clj -X:docs
	echo "js4clj.coruscation.net" > docs/CNAME

$(OUT_DIR)/%.md: $(ORG_DIR)/%.org
	mkdir -p $(OUT_DIR)
	emacs --batch \
		--eval "(require 'ox-md)" \
		--eval "(setq org-export-with-toc nil)" \
		$< \
		--funcall org-md-export-to-markdown \
		&> /dev/null
	mv $(ORG_DIR)/$*.md $(OUT_DIR)/$*.md 2>/dev/null || true
