# Metro Sahaya - container for free hosting (Render, Koyeb, Fly.io, ...)
FROM eclipse-temurin:21-jdk
WORKDIR /app
COPY . .
# H2 database driver (downloaded at build time, so it is not stored in git)
ADD https://repo1.maven.org/maven2/com/h2database/h2/2.2.224/h2-2.2.224.jar /app/h2.jar
RUN mkdir -p out && javac -encoding UTF-8 -cp h2.jar -d out src/MetroSahaya.java
ENV JAVA_TOOL_OPTIONS="-Dfile.encoding=UTF-8"
# the program reads the PORT environment variable set by the host
CMD ["java", "-cp", "out:h2.jar", "MetroSahaya"]
