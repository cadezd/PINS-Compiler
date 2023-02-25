build:clean
	mkdir .build/
	cd .build/ && find ../src/ -type f -name "*.java" | xargs javac -cp ".:../lib/*" -d .

clean:
	rm -rf .build
