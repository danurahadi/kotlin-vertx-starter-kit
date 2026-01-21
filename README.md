# Starter BE App using Kotlin and Vert.x

## Technologies
- Kotlin, Beautiful and Safe JVM Language with 100% Java Compatibility
- Dagger, Dependency Injection Framework, pure using code generation based on Annotation
- Vert.x, Tool-kit for Creating Reactive Application on JVM
- RxJava, Reactive Library to maintain Vert.x Nonblocking nature
- PostgreSQL, Powerful, open source object-relational database system
- Gradle, Complete Build System
- Docker, Container Technology for Easier and Uniform application development and deployment

## API Docs
- You can find the Postman Collection & Env JSON file on the docs/ folder

## Prerequisite
### To Build Application
- Java SDK
- Docker Engine and Docker-Compose
- Internet Connection

### To Deploy/Run Application
- Docker Engine and Docker-Compose
- Internet Connection to pull required docker image layers

## How to run on local machine with IntelliJ IDEA
- Install JDK 25. You can easily manage the JDK on your machine with SDKMAN. You can follow the SDKMAN! installation [here](https://sdkman.io/install)
- After SDKMAN successfully installed, you can choose one of JDK Distribution from 'sdk list java'.
- Install JDK with SDKMAN -> 'sdk install java [identifier]'. For example, 'sdk install java 25.0.1-tem'
- Make sure the JDK was successfully installed with running 'java -version'
- Install PostgreSQL Server on your machine. You can use docker compose, with sample compose file in the services folder
- Make sure you change the PostgreSQL data path in the compose file corresponds with your local machine
- Run 'docker compose up'
- Open the working directory (project) in the IntelliJ IDEA
- Open Project Settings (Ctrl + Alt + S)
- Find Gradle JVM and make sure it's use JDK 25
- Find Java Compiler and make sure the project bytecode version is 25
- Find Kotlin Compiler and make sure the Kotlin compiler version is 2.3.0 or above
- In the same Kotlin Compiler section, set API & Language Version to 2.3 and Target JVM version is 25
- Install Ebean enhancer plugin (latest version) and Restart the IDE
- Select Build in the header menu and Enable the Ebean enhancement plugin
- Create a new application-config.json file from the sample file inside backend/src/main/resources
- Change DB URL, User, & Password config based on your local machine PostgreSQL config
- For the first run, set ENABLE_DATA_INITIALIZER to true, so the default initial data can insert to the DB
- For the next run, set ENABLE_DATA_INITIALIZER to false, so it won't insert redundant initial data to the DB
- Right-click the main class Application (backend/src/main/kotlin/com.starter.app/app) and select Debug 'Application'
- Your application is ready!

## How to deploy to remote server (Cloud VPS)
- Run './gradlew clean compile stagingDocker' for Staging Deployment or './gradlew clean compile prodDocker' for Production Deployment
  This command will build the app, produce single executable JAR file inside dist/ folder, dan wrap that JAR file with all the files (Dockerfile, Docker Compose file, etc.)
  in the backend/deployment folder (depends on selected env) into a single ZIP file, that can be extracted to your remote server
- Extract the ZIP file and run the BE app in the server with Docker & Docker Compose
- Update Compose Env values with the appropriate config (Database, AWS S3, etc.)
- For the first run, set ENABLE_DATA_INITIALIZER to true, so the default initial data can insert to the DB
- For the next run, set ENABLE_DATA_INITIALIZER to false, so it won't insert redundant initial data to the DB
- Simply run 'docker compose build' then 'docker compose up'
- Your app is running & ready to use!


## How to publish the Docker Image to DockerHub and deploy to DigitalOcean App Platform via GitHub Actions
- Add some secrets & variables to your GitHub repository (based on the sample Workflow Files)
- Modify DockerHub registry & repository name on the workflow files
- Modify DigitalOcean project ID on the workflow files
- Modify some env values on the workflow files based on your needs / accounts
- For the first run, set ENABLE_DATA_INITIALIZER to true, so the default initial data can insert to the DB
- For the next run, set ENABLE_DATA_INITIALIZER to false, so it won't insert redundant initial data to the DB
- Modify region, domain, database and instance_size_slug config on the app spec files
- Push your code to staging or production branch then your GitHub Workflow will be running
- After the Workflow is Succeeded, your app will be running on the App Platform and ready to use

