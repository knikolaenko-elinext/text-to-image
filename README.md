# Text To BMP Demo App

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
./gradlew.bat clean build
```

### Run Demo Application

```
java -jar ./text-to-image-web/build/libs/text-to-image-web-0.0.1-SNAPSHOT.jar
```
	
Now browse to the app at `http://localhost:8080/convert?text=Hello`

### Importing as Oracle stored procedure

Load the class on the server using the loadjava tool:
```
loadjava -user <user> text-to-image-core/build/libs/text-to-image-core-0.0.1-SNAPSHOT.jar
Password: <password>
```

Publish the stored procedure:
```
CREATE FUNCTION getImageFromText(text IN VARCHAR2) RETURN BLOB
AS LANGUAGE JAVA NAME 'by.knick.eli.TextRenderServiceImpl.renderTextIntoImage(java.lang.String) return byte[]';
```