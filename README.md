# Properties Env Utility app

Java application to populate application.properties and .env files using k8s yml file.

## Creating jar file
1. Go to root directory of the project
2. Create the jar file using command below
    ```
    ./mvnw -B -DskipTests clean package
    ```
3. Prepare the application.properties/.env file and the k8s yaml file that contains the .env file.

## Usages - Commands available
### 1. setup env [...options]

Command used to setup the application.properties file using k8s yaml file by injecting env from yaml to .properties file. A new file will be created with suffix "-injected" in same directory as '-e' input

| Options        | Type     | Description                                                                                                         | Examples       |
|:---------------|:---------|:--------------------------------------------------------------------------------------------------------------------|:---------------|
| `-e`, `--env`  | `string` | **Required**. Full path of .env file to use. If not present, a .env file of same directory location will be created | .env           |
| `-y`, `--yaml` | `string` | **Required**. Full path of .yml file used as source of env values                                                   | deployment.yml |
| `-i`, `-image` | `string` | **Required**. Image name of the container where env file will be fetched from                                       | client-service |

### 2. setup env [...options]

Command used to setup the application.properties file using k8s yaml file by injecting env from yaml to .properties file. A new file will be created with suffix "-injected" in same directory as '-p' input

| Options              | Type     | Description                                                                   | Examples               |
|:---------------------|:---------|:------------------------------------------------------------------------------|:-----------------------|
| `-p`, `--properties` | `string` | **Required**. Full path of .properties file to use.                           | application.properties |
| `-y`, `--yaml`       | `string` | **Required**. Full path of .yml file used as source of env values             | deployment.yml         |
| `-i`, `-image`       | `string` | **Required**. Image name of the container where env file will be fetched from | client-service         |

## Example usage
1. java -jar .\properties-env-setup-1.0-SNAPSHOT.jar setup properties -p .\test-data\application.properties -y .\test-data\sample-deployment.yml -i client-service
