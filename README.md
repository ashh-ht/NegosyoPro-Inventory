# NegosyoPro-Inventory-System

THINGS YOU NEED TO HAVE PARA NDE MAG-ERROR:

1. install Extension Pack for Java by Microsoft
2. install Spring Boot Extension Pack by VMware


TO RUN THE WEB, go to TERMINAL and paste this:
./mvnw spring-boot:run

then, paste http://localhost:8080 in your browser and u should see the web.

OR

go to NegosyoproApplication.java and click the 'Run' above the public static void main and then go to http://localhost:8080
refresh the browser to see changes.


MVC FRAMEWORK:
1. MODEL:
    -u should see the 'model' folder inside src/main/java/com/inventory/negosyopro.
    -thats where all of the .java file will be placed for the logic and backend
    
    -u should also see the 'db' folder inside src/main/java/com/inventory/negosyopro.
    -thats where the db connection is placed. u just need to call the dbConnection function to access the db.
2. VIEW
    -u should see the 'resources' folder inside src/main.
    -thats where all of the frontend file will be placed.

    'static' folder:
        -all css and js files.
    'templates' folder:
        -all html files
3. CONTROLLER
    -u should see the controller' folder inside src/main/java/com/inventory/negosyopro.
    -thats where all of the controller will be placed.