setup:
	clj -M -m cljs.main -co "{:deps-cmd \"yarn\"}" --install-deps

build:
	clj -M -m krell.main -v -co build.edn -c

repl:
	clj -M -m krell.main -co build.edn -r
