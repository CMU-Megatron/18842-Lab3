JFLAGS = -d ./$(BIN)
JC = javac
JAR = -classpath .:snakeyaml-1.13.jar

.SUFFIXES: .java .class

SRC = src
BIN = bin

SOURCES = \
	Application.java \
	ConfigurationParser.java \
	Constants.java \
	Message.java\
	MessageHandler.java \
	MessagePasser.java \
	Node.java \
	Rule.java \
	SocketListener.java \

SRC_PATH = $(addprefix ./$(SRC)/, $(SOURCES))

.PHONY: bin_dir clean

all: bin_dir
	$(JC) $(JFLAGS) $(JAR) $(SRC_PATH)

bin_dir:
	@mkdir -p $(BIN)

clean:
	$(RM) ./$(BIN)/*.class
