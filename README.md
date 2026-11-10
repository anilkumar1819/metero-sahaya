# Metro Sahaya (Java + SQL)

Guided metro help for first-time travellers, with passenger details, demo payment and printable tickets.
Plain Java 17+ (built-in HttpServer + JDBC) and an H2 SQL database.

## Run
Windows (put the H2 jar in this folder, change the jar name if yours differs):

    mkdir out
    javac -encoding UTF-8 -cp h2-2.5.252.jar -d out src\MetroSahaya.java
    java -Dfile.encoding=UTF-8 -cp "out;h2-2.5.252.jar" MetroSahaya 8080

Mac/Linux: use `:` instead of `;` and `/` instead of `\`, or `H2_JAR=/path/to/h2.jar ./run.sh`.
Then open http://localhost:8080

Delete the `data/` folder to reset the database. A database made by the first version is upgraded automatically.

## Booking flow
1. Choose start and destination, press "Show me the way".
2. Press "Book ticket": choose number of passengers (1-6), enter name, age and gender for each,
   a contact name, a 10-digit mobile number, and a payment method (UPI / Card = demo, Cash = pay at counter).
3. You get a ticket with a code (MS + 8 digits). Print it, or view it later with code + last 4 digits of the mobile number.
Sample rule: children under 5 travel free. Payment is a DEMO: no real money is charged.

## API
- GET  /api/stations
- GET  /api/route?from=ID&to=ID&lang=en|hi|kn
- GET  /api/guide?lang=en|hi|kn
- POST /api/book   (form fields: from,to,contact_name,phone,pay,n,name1,age1,gender1,...)
- GET  /api/ticket?code=MS12345678&phone4=1234

Network, fares and tickets are sample data, not official.

## Put it online (free): GitHub + Render
1. Create a GitHub repository and upload everything in this folder (the .gitignore keeps data/, out/ and jars out).
2. On render.com sign in with GitHub, choose New > Web Service, pick the repository.
3. Language/Runtime: Docker (it finds the Dockerfile). Instance type: Free. Click Create Web Service.
4. After the build, Render shows your link: https://<name>.onrender.com
Notes: the free plan sleeps after 15 minutes idle, so the first open can take up to a minute.
The database lives inside the container, so tickets reset when the service restarts. That is fine for a demo.
