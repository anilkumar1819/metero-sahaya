# 🚇 Metro Sahaya

A web tool that helps **first-time metro travelers from village and rural backgrounds** find their route, understand ticketing, and book a ticket, in **English, Hindi and Kannada**.

**🔗 Live demo:** https://metero-sahaya.onrender.com
*(Free hosting: if the app has been idle, the first load can take about a minute.)*

> Demo project: simplified map, sample fares and demo payment. Not official. Please use fake names and a fake mobile number.

## Features
- Step-by-step route guidance with interchange instructions and a "read aloud" button
- Guide to using the metro (entry, ticket, gate, platform, exit) in 3 languages
- Booking flow: passenger details for up to 6 travellers, demo payment, printable ticket
- Ticket lookup using the ticket code and last 4 digits of the mobile number
- Large text and simple screens for non-tech-savvy users

## Tech
- **Java 21**: built-in `HttpServer` and JDBC, no frameworks
- **SQL (H2)**: stations, lines, fares, tickets and passengers tables, with an `interchanges` view
- **Dijkstra's algorithm** for the best route, including line changes
- **HTML, CSS, JavaScript** front end
- Deployed on **Render** with Docker

## Run locally
1. Install JDK 17 or newer and download the H2 jar from h2database.com.
2. Compile and start:
   ```
   javac -encoding UTF-8 -cp h2.jar -d out src/MetroSahaya.java
   java -Dfile.encoding=UTF-8 -cp "out;h2.jar" MetroSahaya 8080
   ```
   (Use `:` instead of `;` on Mac or Linux.)
3. Open http://localhost:8080
