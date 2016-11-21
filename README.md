# Text To BMP Demo App

For reviewing functionality you can run [Live example](http://139.59.147.15:8080/convert?text=Lorem%20ipsum%20dolor%20sit%20amet,%20consectetur%20adipiscing%20elit.%20Aliquam%20non%20massa%20consequat,%20tristique%20est%20vitae,%20luctus%20ante.%20Cras%20a%20aliquam%20sapien,%20ut%20faucibus%20enim.%20Nam%20ligula%20ante,%20varius%20sed%20ultricies%20non,%20hendrerit%20vitae%20lacus.%20Integer%20non%20elit%20vitae%20felis%20blandit%20blandit.%20Etiam%20venenatis%20massa%20quis%20odio%20sodales%20cursus.%20Sed%20et%20fermentum%20nisl.%20Etiam%20id%20porttitor%20nunc.)

### Font constants

Minimum font size: MINIMAL_FONT_SIZE = 10

Maximum font size: MAXIMUM_FONT_SIZE = PREFFERABLE_HEIGHT_SHORT = 68

Font family: FONT_FAMILY = "Serif"

Font color: FONT_COLOR = Color.BLACK

If text is too long for 196x196 version - it prints as much text as possible using minimum font, and cuts off the rest


### Prerequisites

You need git to clone the project and JDK 8 to build and run.

### Clone project

```
git clone https://github.com/knikolaenko-elinext/text-to-image.git
cd text-to-image
```
	
### Build the Application

*nix:
```
./gradlew clean build
```

Windows:
```
gradlew.bat clean build
```

### Run Demo Application on local machine

```
java -jar ./text-to-image-web/build/libs/text-to-image-web-0.0.1-SNAPSHOT.jar
```
Now browse to the app at [http://localhost:8080/convert?text=Hello](http://localhost:8080/convert?text=Hello)

### Run Demo Application in Docker

```
docker build -t knikolaenko/text-to-image .
docker run -d -p 8080:8080 --name text-to-image knikolaenko/text-to-image 
```

### Importing as Oracle stored procedure

SQL script defining stored procedure will be at text-to-image-core/src/generated/sql/by/knick/eli/textRenderer.sql:
```
sqlplus <user>/<password> < text-to-image-core/src/generated/sql/by/knick/eli/textRenderer.sql
```

Test call:
```
SELECT TextRenderer.renderTextIntoImage('Lorem ipsum dolor...') FROM DUAL;
```